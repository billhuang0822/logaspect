package com.tsb.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 處理所有 Exception
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        // Log 錯誤類別（SimpleName）和 stack trace 前 4 層的 filename:lineNumber
        StringBuilder sb = new StringBuilder();
        sb.append("Exception: ").append(ex.getClass().getSimpleName());
        StackTraceElement[] stackTrace = ex.getStackTrace();
        int maxDepth = Math.min(4, stackTrace.length);
        for (int i = 0; i < maxDepth; i++) {
            StackTraceElement ste = stackTrace[i];
            sb.append(" | at ").append(ste.getFileName()).append(":").append(ste.getLineNumber());
        }
        logger.error(sb.toString());

        // 可以把 exception 資訊帶到 error.jsp
        model.addAttribute("errorMessage", ex.getMessage());
        return "error"; // 對應 /WEB-INF/jsp/error.jsp
    }
}