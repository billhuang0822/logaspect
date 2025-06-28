package com.tsb.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class PageConfig {
	@Bean
	public InternalResourceViewResolver jspViewResolver() {
	    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
	    resolver.setPrefix("/jsp/"); // 指向 webapp/jsp/
	    resolver.setSuffix(".jsp");
	    return resolver;
	}
}
