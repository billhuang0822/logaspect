package com.tsb.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tsb.logging.LogAspect;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class GenericJspController {
	private static final Logger log = LoggerFactory.getLogger(GenericJspController.class);
	@RequestMapping(value = "/**")
	public String forwardJsp(HttpServletRequest request) {
	    String path = request.getRequestURI();
	    log.debug("path=>{}",path);
	    // 過濾不需要處理的路徑
	    if (path.equals("/") || path.contains(".") || path.startsWith("/api") || path.startsWith("/jsp/")) {
	        return null;
	    }

	    // 取得純路徑（去掉前導斜線）
	    if (path.startsWith("/")) {
	        path = path.substring(1);
	    }

	    // 回傳時不加 .jsp，ViewResolver 會自動加
	    return path;
	}
}