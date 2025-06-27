package com.tsb.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class LoginController {
    // 處理登入
    @PostMapping("/api/login")
    public ModelAndView doLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request
    ) {
        // 這裡用簡單的帳密驗證（正式請改用 DB/LDAP 等）
        if ("admin".equals(username) && "1234".equals(password)) {
            // 登入成功，導向 home.jsp
            return new ModelAndView("/home");
        } else {
            // 登入失敗，導向 error.jsp
            return new ModelAndView("/error");
        }
    }
}