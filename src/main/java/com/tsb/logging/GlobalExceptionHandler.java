package com.tsb.logging;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 處理所有 Exception
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        // 可以把 exception 資訊帶到 error.jsp
        model.addAttribute("errorMessage", ex.getMessage());
        return "error"; // 對應 /WEB-INF/jsp/error.jsp
    }
}