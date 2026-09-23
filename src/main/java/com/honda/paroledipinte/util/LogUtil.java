package com.honda.paroledipinte.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 日志工具类
 * 提供统一的日志输出功能
 */
public class LogUtil {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static boolean debugEnabled = true;
    
    // 日志回调列表，用于将日志发送到UI界面
    private static final List<Consumer<String>> logCallbacks = new ArrayList<>();
    
    // 防止递归调用的标志
    private static final ThreadLocal<Boolean> isNotifying = ThreadLocal.withInitial(() -> false);
    
    /**
     * 添加日志回调
     * @param callback 回调函数，接收日志字符串
     */
    public static void addLogCallback(Consumer<String> callback) {
        if (callback != null && !logCallbacks.contains(callback)) {
            logCallbacks.add(callback);
        }
    }
    
    /**
     * 移除日志回调
     * @param callback 要移除的回调函数
     */
    public static void removeLogCallback(Consumer<String> callback) {
        logCallbacks.remove(callback);
    }
    
    /**
     * 通知所有回调
     * @param message 日志消息
     */
    private static void notifyCallbacks(String message) {
        // 防止递归调用
        if (isNotifying.get()) {
            return;
        }
        
        isNotifying.set(true);
        try {
            for (Consumer<String> callback : logCallbacks) {
                try {
                    callback.accept(message);
                } catch (Exception e) {
                    // 忽略回调中的异常，避免影响主流程
                }
            }
        } finally {
            isNotifying.set(false);
        }
    }
    
    /**
     * 设置是否启用调试日志
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }
    
    /**
     * 获取当前时间戳字符串
     */
    private static String getTimestamp() {
        return DATE_FORMAT.format(new Date());
    }
    
    /**
     * 获取调用者信息
     */
    private static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // stackTrace[0]是getStackTrace方法
        // stackTrace[1]是getCallerInfo方法
        // stackTrace[2]是log/debug/info/warn/error方法
        // stackTrace[3]是实际调用日志方法的方法
        if (stackTrace.length >= 4) {
            StackTraceElement caller = stackTrace[3];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();
            int lineNumber = caller.getLineNumber();
            // 提取简单类名
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0) {
                className = className.substring(lastDot + 1);
            }
            return String.format("[%s.%s:%d]", className, methodName, lineNumber);
        }
        return "[Unknown]";
    }
    
    /**
     * 输出调试日志
     * @param message 日志消息
     */
    public static void log(String message) {
        if (!debugEnabled) {
            return;
        }
        String logLine = String.format("[%s] %s %s", getTimestamp(), getCallerInfo(), message);
        System.out.println(logLine);
        // 通知UI回调
        notifyCallbacks(logLine);
    }
    
    /**
     * 输出调试日志（带参数）
     * @param format 格式字符串
     * @param args 参数
     */
    public static void log(String format, Object... args) {
        if (!debugEnabled) {
            return;
        }
        String message = String.format(format, args);
        log(message);
    }
    
    /**
     * 输出调试日志
     */
    public static void debug(String message) {
        log("[DEBUG] " + message);
    }
    
    /**
     * 输出调试日志（带参数）
     */
    public static void debug(String format, Object... args) {
        log("[DEBUG] " + String.format(format, args));
    }
    
    /**
     * 输出信息日志
     */
    public static void info(String message) {
        log("[INFO] " + message);
    }
    
    /**
     * 输出信息日志（带参数）
     */
    public static void info(String format, Object... args) {
        log("[INFO] " + String.format(format, args));
    }
    
    /**
     * 输出警告日志
     */
    public static void warn(String message) {
        log("[WARN] " + message);
    }
    
    /**
     * 输出警告日志（带参数）
     */
    public static void warn(String format, Object... args) {
        log("[WARN] " + String.format(format, args));
    }
    
    /**
     * 输出错误日志
     */
    public static void error(String message) {
        log("[ERROR] " + message);
    }
    
    /**
     * 输出错误日志（带参数）
     */
    public static void error(String format, Object... args) {
        log("[ERROR] " + String.format(format, args));
    }
    
    /**
     * 输出错误日志（带异常）
     */
    public static void error(String message, Throwable e) {
        log("[ERROR] " + message + " - " + e.getMessage());
        if (debugEnabled) {
            e.printStackTrace();
        }
    }
    
    /**
     * 记录方法进入
     */
    public static void enter() {
        log("[ENTER]");
    }
    
    /**
     * 记录方法退出
     */
    public static void exit() {
        log("[EXIT]");
    }
    
    /**
     * 记录方法退出（带返回值）
     */
    public static void exit(Object result) {
        log("[EXIT] return: %s", result);
    }
}
