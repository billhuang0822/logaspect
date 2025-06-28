package com.tsb.logging.mask;

import org.springframework.stereotype.Component;

@Component("star")
public class StarMaskStrategy implements MaskStrategy {
    @Override
    public String mask(String original) {
        return "****";
    }
}