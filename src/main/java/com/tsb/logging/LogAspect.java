package com.tsb.logging;

import com.tsb.logging.mask.MaskStrategy;
import com.tsb.logging.pattern.PatternStrategy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Aspect
@Component
public class LogAspect {
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    @Value("${logaspect.exclude-packages:}")
    private List<String> excludePackages;

    @Value("${logaspect.log-mode:LOG_ALL}")
    private String logMode;

    @Autowired
    private Map<String, MaskStrategy> maskStrategies;

    @Autowired
    private Map<String, PatternStrategy> patternStrategies;

    @Pointcut(
        "execution(* com.tsb..*(..)) || execution(* bull..*(..)) || execution(* com.taishinbank..*(..))"
    )
    private void logTargets() {}

    @Around("logTargets() && !within(com.tsb.logging..*)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        String className = targetClass.getName();
        String methodName = joinPoint.getSignature().getName();
        Object result = null;
        Throwable thrown = null;
        try {
            if (!CollectionUtils.isEmpty(excludePackages)) {
                for (String pkg : excludePackages) {
                    if (className.startsWith(pkg)) {
                        return joinPoint.proceed();
                    }
                }
            }
            if ("LOG_NONE".equalsIgnoreCase(logMode)) {
                return joinPoint.proceed();
            }

            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            if (hasDisableLog(method, targetClass)) {
                return joinPoint.proceed();
            }

            EnableLog enableLog = getEnableLog(method, targetClass);

            Object[] args = joinPoint.getArgs();
            String[] argNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Map<String, Object> argMap = new LinkedHashMap<>();
            for (int i = 0; i < argNames.length; i++) {
                argMap.put(argNames[i], args[i]);
            }

            Map<String, String> fieldMaskMap = new HashMap<>();
            if (enableLog != null) {
                // parse maskFields: field:maskName
                for (String f : enableLog.maskFields()) {
                    if (f.contains(":")) {
                        String[] arr = f.split(":", 2);
                        fieldMaskMap.put(arr[0], arr[1]);
                    } else {
                        fieldMaskMap.put(f, "star");
                    }
                }
                maskFields(argMap, fieldMaskMap, enableLog.patterns());
                log.info("[ENABLELOG] {}.{} args: {}", className, methodName, argMap);
            } else {
                log.info("[LOG] {}.{} args: {}", className, methodName, argMap);
            }

            result = joinPoint.proceed();

            if (enableLog != null && result != null) {
                Object maskedResult = maskObject(result, fieldMaskMap, enableLog.patterns());
                log.info("[ENABLELOG] {}.{} result: {}", className, methodName, maskedResult);
            } else if (result != null) {
                log.info("[LOG] {}.{} result: {}", className, methodName, result);
            }

            return result;
        } catch (Throwable ex) {
            thrown = ex;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (thrown != null) {
                log.info("[TIME] {}.{} failed cost={}ms", className, methodName, duration);
            } else {
                log.info("[TIME] {}.{} cost={}ms", className, methodName, duration);
            }
        }
    }

    private boolean hasDisableLog(Method method, Class<?> targetClass) {
        return AnnotationUtils.findAnnotation(method, DisableLog.class) != null
            || AnnotationUtils.findAnnotation(targetClass, DisableLog.class) != null;
    }

    private EnableLog getEnableLog(Method method, Class<?> targetClass) {
        EnableLog enableLog = AnnotationUtils.findAnnotation(method, EnableLog.class);
        if (enableLog == null) {
            enableLog = AnnotationUtils.findAnnotation(targetClass, EnableLog.class);
        }
        return enableLog;
    }

    private void maskFields(Map<String, Object> argMap, Map<String, String> fieldMaskMap, String[] patterns) {
        for (String key : argMap.keySet()) {
            Object val = argMap.get(key);
            if (fieldMaskMap.containsKey(key)) {
                argMap.put(key, getMaskValue(fieldMaskMap.get(key), val));
            } else if (patterns != null && patterns.length > 0 && val instanceof String) {
                argMap.put(key, maskByPattern((String) val, patterns));
            } else if (val instanceof Map || isPojo(val)) {
                argMap.put(key, maskObject(val, fieldMaskMap, patterns));
            }
        }
    }

    private Object maskObject(Object obj, Map<String, String> fieldMaskMap, String[] patterns) {
        if (obj == null) return null;
        if (obj instanceof Map) {
            Map<String, Object> map = new LinkedHashMap<>((Map)obj);
            maskFields(map, fieldMaskMap, patterns);
            return map;
        } else if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            List<Object> masked = new ArrayList<>();
            for (Object item : col) {
                masked.add(maskObject(item, fieldMaskMap, patterns));
            }
            return masked;
        } else if (obj instanceof String && patterns != null && patterns.length > 0) {
            return maskByPattern((String) obj, patterns);
        } else if (isPojo(obj)) {
            try {
                Object maskedPojo = obj.getClass().getDeclaredConstructor().newInstance();
                for (Field field : obj.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (fieldMaskMap.containsKey(field.getName())) {
                        field.set(maskedPojo, getMaskValue(fieldMaskMap.get(field.getName()), value));
                    } else if (patterns != null && patterns.length > 0 && value instanceof String) {
                        field.set(maskedPojo, maskByPattern((String) value, patterns));
                    } else if (value instanceof Map || isPojo(value)) {
                        field.set(maskedPojo, maskObject(value, fieldMaskMap, patterns));
                    } else {
                        field.set(maskedPojo, value);
                    }
                }
                return maskedPojo;
            } catch (Exception e) {
                return obj;
            }
        }
        return obj;
    }

    private String maskByPattern(String value, String[] patterns) {
        String masked = value;
        for (String patternName : patterns) {
            PatternStrategy patternStrategy = patternStrategies.get(patternName);
            MaskStrategy maskStrategy = maskStrategies.get(patternName);
            if (patternStrategy != null && patternStrategy.matches(masked)) {
                if(maskStrategy != null){
                    masked = maskStrategy.mask(masked);
                }else{
                    masked = getMaskValue("star", masked);
                }
            }
        }
        return masked;
    }

    private String getMaskValue(String maskName, Object value) {
        MaskStrategy maskStrategy = maskStrategies.get(maskName);
        if(maskStrategy == null) maskStrategy = maskStrategies.get("star");
        return maskStrategy.mask(value == null ? null : value.toString());
    }

    private boolean isPojo(Object obj) {
        if (obj == null) return false;
        Package pkg = obj.getClass().getPackage();
        if (pkg == null) return true;
        String pkgName = pkg.getName();
        return !(pkgName.startsWith("java.") || pkgName.startsWith("javax.") || pkgName.startsWith("jakarta.") || pkgName.startsWith("org.springframework."));
    }
}