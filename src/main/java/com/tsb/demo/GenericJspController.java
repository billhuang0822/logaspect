package com.tsb.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class GenericJspController {

    @RequestMapping(value = "/**")
    public String forwardJsp(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 避免循環：只處理不含點（.）的路徑
        if (path.contains(".") || path.equals("/")) {
            return null; // 放行給靜態資源或 JSP
        }

        // 去除開頭的斜線
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path; // 例如 foo/bar/baz -> foo/bar/baz.jsp
    }
}