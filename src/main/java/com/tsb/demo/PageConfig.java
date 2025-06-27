package com.tsb.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class PageConfig {
	@Bean
	public InternalResourceViewResolver jspViewResolver() {
	    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
	    resolver.setPrefix("/"); // 直接 webapp 根目錄
	    resolver.setSuffix(".jsp");
	    return resolver;
	}
}
