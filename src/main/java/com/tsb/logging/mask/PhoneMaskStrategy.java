package com.tsb.logging.mask;

import org.springframework.stereotype.Component;

@Component("phone")
public class PhoneMaskStrategy implements MaskStrategy {
    @Override
    public String mask(String original) {
        if (original == null) return null;
        return original.replaceAll("\\d{4,}", "****");
    }
}