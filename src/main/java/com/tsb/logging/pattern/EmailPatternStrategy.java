package com.tsb.logging.pattern;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component("email")
public class EmailPatternStrategy implements PatternStrategy {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_.\\-]+@[a-zA-Z0-9.-]+");

    @Override
    public boolean matches(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).find();
    }
}