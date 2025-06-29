package com.tsb.logging;

import org.slf4j.Logger;

public class SafeExceptionLogger {
    /**
     * 安全列印 Exception 基本資訊，只顯示類型與前幾層 stacktrace（檔名＋行數）。
     */
    public static void log(Logger logger, Throwable ex, int stackDepth) {
        if (ex == null || logger == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Exception: ").append(ex.getClass().getSimpleName());
        StackTraceElement[] stackTrace = ex.getStackTrace();
        int max = Math.min(stackDepth, stackTrace.length);
        for (int i = 0; i < max; i++) {
            StackTraceElement el = stackTrace[i];
            sb.append(" | at ").append(el.getFileName()).append(":").append(el.getLineNumber());
        }
        logger.error(sb.toString());
    }

    // 預設顯示4層
    public static void log(Logger logger, Throwable ex) {
        log(logger, ex, 4);
    }
}