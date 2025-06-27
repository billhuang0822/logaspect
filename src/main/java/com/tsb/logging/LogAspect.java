package com.tsb.logging;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class LogAspect {
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    @Around("(execution(* bull..*(..)) || execution(* com.tsb..*(..)) || execution(* com.taishinbank..*(..))) && !execution(* com.tsb.logging..*(..))")
    public Object logWithMasking(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();

        boolean included = properties.getIncludePackages().stream().anyMatch(className::startsWith);
        boolean excluded = excludePackages.stream().anyMatch(className::startsWith);
        if (!included || excluded) return joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> clazz = method.getDeclaringClass();

        EnableLog classLog = clazz.getAnnotation(EnableLog.class);
        EnableLog methodLog = method.getAnnotation(EnableLog.class);
        DisableLog classDisable = clazz.getAnnotation(DisableLog.class);
        DisableLog methodDisable = method.getAnnotation(DisableLog.class);

        boolean shouldLog;
        if (LogAspectProperties.Mode.ENABLE_BY_DEFAULT.equals(properties.getMode())) {
            shouldLog = methodDisable == null && classDisable == null;
        } else {
            shouldLog = methodLog != null || classLog != null;
        }

        if (!shouldLog) {
            log.debug("[SKIPPED] {}.{} logging disabled", clazz.getSimpleName(), method.getName());
            return joinPoint.proceed();
        }

        Map<String, String> reqFieldFormatMap = new HashMap<>();
        Map<String, String> resFieldFormatMap = new HashMap<>();
        Set<String> maskFormats = new HashSet<>();

        if (classLog != null) {
            reqFieldFormatMap.putAll(parseFieldFormat(classLog.maskRequestFieldFormats()));
            resFieldFormatMap.putAll(parseFieldFormat(classLog.maskResponseFieldFormats()));
            maskFormats.addAll(Arrays.asList(classLog.maskFormats()));
        }
        if (methodLog != null) {
            reqFieldFormatMap.putAll(parseFieldFormat(methodLog.maskRequestFieldFormats()));
            resFieldFormatMap.putAll(parseFieldFormat(methodLog.maskResponseFieldFormats()));
            maskFormats.addAll(Arrays.asList(methodLog.maskFormats()));
        }

        List<Object> maskedArgs = new ArrayList<>();
        for (Object arg : joinPoint.getArgs()) {
            maskedArgs.add(deepCopyAndMask(arg, reqFieldFormatMap, maskFormats));
        }
        for (int i = 0; i < maskedArgs.size(); i++) {
            log.info("[REQ] {}.{} arg[{}] = {}", clazz.getSimpleName(), method.getName(), i, maskedArgs.get(i));
        }

        Object result = joinPoint.proceed();
        Object maskedResult = deepCopyAndMask(result, resFieldFormatMap, maskFormats);
        log.info("[RES] {}.{} Result: {}", clazz.getSimpleName(), method.getName(), maskedResult);
        return result;
    }

    private Map<String, String> parseFieldFormat(String[] arr) {
        Map<String, String> map = new HashMap<>();
        for (String s : arr) {
            String[] kv = s.split("=", 2);
            map.put(kv[0], kv.length == 2 ? kv[1] : "default");
        }
        return map;
    }

    private Object deepCopyAndMask(Object original, Map<String, String> fieldFormatMap, Set<String> formats) {
        if (original == null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> root = objectMapper.convertValue(original, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            for (String fieldPath : fieldFormatMap.keySet()) {
                maskByPath(root, fieldPath.split("\\."), fieldFormatMap.get(fieldPath));
            }
            if (!formats.isEmpty()) {
                maskByFormat(root, formats);
            }
            return root;
        } catch (Exception e) {
            log.warn("Masking failed: {}", e.getMessage());
            return original;
        }
    }

    @SuppressWarnings("unchecked")
    private void maskByPath(Object current, String[] pathParts, String format) {
        if (current == null || pathParts.length == 0) return;
        String fieldName = pathParts[0];
        String[] nextPath = Arrays.copyOfRange(pathParts, 1, pathParts.length);

        if (current instanceof Map<?, ?> map) {
            Map<String, Object> modifiableMap = (Map<String, Object>) map;
            Object next = modifiableMap.get(fieldName);
            if (nextPath.length == 0 && next instanceof String str) {
                modifiableMap.put(fieldName, MaskingRegistry.maskByFormat(str, format));
            } else {
                maskByPath(next, nextPath, format);
            }
        } else if (current instanceof List<?> list) {
            for (Object item : list) {
                maskByPath(item, pathParts, format);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void maskByFormat(Object obj, Set<String> formats) {
        if (obj == null || formats.isEmpty()) return;

        if (obj instanceof List<?> list) {
            for (Object item : list) {
                maskByFormat(item, formats);
            }
        } else if (obj instanceof Map<?, ?> rawMap) {
            Map<Object, Object> map = (Map<Object, Object>) rawMap;
            for (Object key : map.keySet()) {
                Object val = map.get(key);
                if (key instanceof String) {
                    if (val instanceof String str) {
                        map.put(key, tryMaskByFormats(str, formats));
                    } else {
                        maskByFormat(val, formats);
                    }
                }
            }
        }
    }

    private String tryMaskByFormats(String value, Set<String> formats) {
        for (String format : formats) {
            if (MaskingRegistry.getFormatCheckers().getOrDefault(format, v -> false).test(value)) {
                return MaskingRegistry.maskByFormat(value, format);
            }
        }
        return value;
    }
}