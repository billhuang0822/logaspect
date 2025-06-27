package com.tsb.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class GenericJspController {

    @RequestMapping(value = "/**")
    public String forwardJsp(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 只處理不含點(.)且不是/api開頭的路徑
        if (path.equals("/") || path.contains(".") || path.startsWith("/api")) {
            return null;
        }

        // 防止 circular（只對原生路徑起作用）
        String bestPattern = (String) request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
        if (bestPattern != null && bestPattern.equals("/**")) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        }

        return null;
    }
}