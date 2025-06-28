package com.tsb.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class GenericJspController {

    @RequestMapping(value = "/**")
    public String forwardJsp(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 過濾根目錄、靜態資源、API
        if (path.equals("/") || path.contains(".") || path.startsWith("/api")) {
            return null;
        }

        // 將 /xxx/yyy 轉為 xxx/yyy，交給 ViewResolver 處理
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}