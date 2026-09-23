package com.honda.paroledipinte.lua;

import com.honda.paroledipinte.util.LogUtil;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Consumer;

/**
 * 日志脚本引擎
 * 提供日志过滤和任务处理的Lua脚本支持
 */
public class LogScriptEngine {
    
    private Globals globals;
    private String script;
    private boolean enabled;
    private String lastError;
    private String lastCompileError;
    private Consumer<String> consoleOutputCallback;
    
    // 默认脚本 - filter返回true不过滤，task在10000条时清空
    private static final String DEFAULT_SCRIPT = 
        "-- 日志处理脚本\n" +
        "-- filter函数: 返回true表示保留日志，false表示过滤掉\n" +
        "-- log参数: {timestamp=时间戳, message=日志内容, level=级别}\n\n" +
        "function filter(log)\n" +
        "    -- 默认不过滤任何日志\n" +
        "    return true\n" +
        "end\n\n" +
        "-- task函数: 定期执行的任务\n" +
        "-- stats参数: {total=总条数, filtered=已过滤数, current=当前显示数}\n" +
        "-- 返回值: {action=动作, message=消息}\n" +
        "-- action可选: \"clear\"(清空), \"none\"(无操作)\n\n" +
        "function task(stats)\n" +
        "    -- 当日志达到10000条时自动清空\n" +
        "    if stats.total >= 10000 then\n" +
        "        return {\n" +
        "            action = \"clear\",\n" +
        "            message = \"日志达到10000条，自动清空\"\n" +
        "        }\n" +
        "    end\n" +
        "    return {action = \"none\"}\n" +
        "end";
    
    /**
     * 脚本执行结果
     */
    public static class ScriptResult {
        private final boolean success;
        private final Object data;
        private final String error;
        
        public ScriptResult(boolean success, Object data, String error) {
        // LogUtil.log("[ScriptResult] 开始执行");
            this.success = success;
            this.data = data;
            this.error = error;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Object getData() {
            return data;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * 任务执行结果
     */
    public static class TaskResult {
        private final String action;
        private final String message;
        
        public TaskResult(String action, String message) {
        // LogUtil.log("[TaskResult] 开始执行");
            this.action = action;
            this.message = message;
        }
        
        public String getAction() {
            return action;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean shouldClear() {
        // LogUtil.log("[shouldClear] 开始执行");
            return "clear".equals(action);
        }
    }
    
    public LogScriptEngine() {
        // LogUtil.log("[LogScriptEngine] 开始执行");
        this.globals = JsePlatform.standardGlobals();
        this.enabled = false;
        this.lastError = null;
        this.lastCompileError = null;
        registerUtilityFunctions();
    }
    
    /**
     * 注册实用函数到Lua环境
     */
    private void registerUtilityFunctions() {
        // LogUtil.log("[registerUtilityFunctions] 开始执行");
        // print函数 - 输出到控制台回调
        globals.set("print", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
        // LogUtil.log("[invoke] 开始执行");
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) sb.append("\t");
                    sb.append(args.arg(i).tojstring());
                }
                String output = sb.toString();
                System.out.println(output);
                if (consoleOutputCallback != null) {
                    consoleOutputCallback.accept(output);
                }
                return NONE;
            }
        });
        
        // contains: 检查字符串是否包含子串
        globals.set("contains", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                String text = arg1.tojstring();
                String search = arg2.tojstring();
                return LuaValue.valueOf(text.contains(search));
            }
        });
        
        // starts_with: 检查字符串是否以指定前缀开头
        globals.set("starts_with", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                String text = arg1.tojstring();
                String prefix = arg2.tojstring();
                return LuaValue.valueOf(text.startsWith(prefix));
            }
        });
        
        // ends_with: 检查字符串是否以指定后缀结尾
        globals.set("ends_with", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                String text = arg1.tojstring();
                String suffix = arg2.tojstring();
                return LuaValue.valueOf(text.endsWith(suffix));
            }
        });
        
        // matches: 检查字符串是否匹配正则表达式
        globals.set("matches", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                String text = arg1.tojstring();
                String pattern = arg2.tojstring();
                try {
                    return LuaValue.valueOf(text.matches(pattern));
                } catch (Exception e) {
                    return LuaValue.FALSE;
                }
            }
        });
    }
    
    /**
     * 设置控制台输出回调
     */
    public void setConsoleOutputCallback(Consumer<String> callback) {
        this.consoleOutputCallback = callback;
    }
    
    /**
     * 设置脚本
     */
    public void setScript(String script) {
        this.script = script;
        this.lastCompileError = null;
        
        if (script != null && !script.trim().isEmpty()) {
            try {
                globals.load(script).call();
                enabled = true;
            } catch (Exception e) {
                lastCompileError = e.getMessage();
                System.err.println("Lua脚本编译错误: " + e.getMessage());
                enabled = false;
            }
        } else {
            enabled = false;
        }
    }
    
    /**
     * 获取脚本
     */
    public String getScript() {
        return script;
    }
    
    /**
     * 获取默认脚本
     */
    public static String getDefaultScript() {
        return DEFAULT_SCRIPT;
    }
    
    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 执行filter函数
     * @param timestamp 时间戳
     * @param message 日志消息
     * @param level 日志级别
     * @return true表示保留，false表示过滤掉
     */
    public boolean executeFilter(String timestamp, String message, String level) {
        // LogUtil.log("[executeFilter] 开始执行");
        if (!enabled || script == null) {
            return true; // 默认不过滤
        }
        
        try {
            LuaValue func = globals.get("filter");
            if (func.isnil()) {
                return true; // 没有filter函数，默认不过滤
            }
            
            // 创建日志表
            LuaValue logTable = LuaValue.tableOf();
            logTable.set("timestamp", timestamp);
            logTable.set("message", message);
            logTable.set("level", level);
            
            // 调用函数
            Varargs result = func.invoke(logTable);
            return result.arg1().toboolean();
            
        } catch (Exception e) {
            lastError = e.getMessage();
            return true; // 出错时默认不过滤
        }
    }
    
    /**
     * 执行task函数
     * @param total 总日志数
     * @param filtered 已过滤数
     * @param current 当前显示数
     * @return 任务执行结果
     */
    public TaskResult executeTask(int total, int filtered, int current) {
        // LogUtil.log("[executeTask] 开始执行");
        if (!enabled || script == null) {
            // 默认行为：10000条时清空
            if (total >= 10000) {
                return new TaskResult("clear", "日志达到10000条，自动清空");
            }
            return new TaskResult("none", null);
        }
        
        try {
            LuaValue func = globals.get("task");
            if (func.isnil()) {
                // 没有task函数，使用默认行为
                if (total >= 10000) {
                    return new TaskResult("clear", "日志达到10000条，自动清空");
                }
                return new TaskResult("none", null);
            }
            
            // 创建统计表
            LuaValue statsTable = LuaValue.tableOf();
            statsTable.set("total", total);
            statsTable.set("filtered", filtered);
            statsTable.set("current", current);
            
            // 调用函数
            Varargs result = func.invoke(statsTable);
            LuaValue resultTable = result.arg1();
            
            if (!resultTable.istable()) {
                return new TaskResult("none", null);
            }
            
            String action = resultTable.get("action").tojstring();
            String msg = resultTable.get("message").tojstring();
            
            return new TaskResult(action, msg);
            
        } catch (Exception e) {
            lastError = e.getMessage();
            // 出错时使用默认行为
            if (total >= 10000) {
        // LogUtil.log("[if] 开始执行");
                return new TaskResult("clear", "日志达到10000条，自动清空");
            }
            return new TaskResult("none", null);
        }
    }
    
    /**
     * 验证脚本语法
     */
    public ValidationResult validateScript(String script) {
        // LogUtil.log("[validateScript] 开始执行");
        if (script == null || script.trim().isEmpty()) {
            return new ValidationResult(false, "脚本为空", "脚本内容不能为空");
        }
        
        // 创建新的Globals用于测试
        Globals testGlobals = JsePlatform.standardGlobals();
        
        ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            PrintStream captureStream = new PrintStream(outputCapture);
            System.setOut(captureStream);
            System.setErr(captureStream);
            
            // 尝试加载脚本
            testGlobals.load(script).call();
            
            // 检查是否包含必要的函数
            boolean hasFilter = !testGlobals.get("filter").isnil();
            boolean hasTask = !testGlobals.get("task").isnil();
            
            String output = outputCapture.toString("UTF-8");
            StringBuilder message = new StringBuilder();
            message.append("✓ 脚本语法正确\n");
            if (hasFilter) {
                message.append("✓ 发现filter函数\n");
            } else {
                message.append("⚠ 未发现filter函数（将使用默认不过滤）\n");
            }
            if (hasTask) {
                message.append("✓ 发现task函数\n");
            } else {
                message.append("⚠ 未发现task函数（将使用默认行为）\n");
            }
            if (!output.isEmpty()) {
                message.append("\n【脚本输出】\n").append(output);
            }
            
            return new ValidationResult(true, "验证通过", message.toString().trim());
            
        } catch (LuaError e) {
            String output;
            try {
                output = outputCapture.toString("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                output = outputCapture.toString();
            }
            StringBuilder error = new StringBuilder();
            error.append("✗ 脚本错误: ").append(e.getMessage()).append("\n");
            if (!output.isEmpty()) {
                error.append("\n【脚本输出】\n").append(output);
            }
            return new ValidationResult(false, "验证失败: " + e.getMessage(), error.toString().trim());
        } catch (Exception e) {
            return new ValidationResult(false, "验证失败: " + e.getMessage(), e.getMessage());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    
    /**
     * 测试执行脚本
     */
    public String testScript(String script) {
        // LogUtil.log("[testScript] 开始执行");
        if (script == null || script.trim().isEmpty()) {
            return "错误: 脚本为空";
        }
        
        Globals testGlobals = JsePlatform.standardGlobals();
        
        ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            PrintStream captureStream = new PrintStream(outputCapture);
            System.setOut(captureStream);
            System.setErr(captureStream);
            
            // 加载并执行脚本
            testGlobals.load(script).call();
            
            StringBuilder result = new StringBuilder();
            result.append("=== 脚本加载成功 ===\n\n");
            
            // 检查必要的函数
            boolean hasFilter = !testGlobals.get("filter").isnil();
            boolean hasTask = !testGlobals.get("task").isnil();
            
            if (hasFilter) {
                result.append("✓ 发现filter函数\n");
                
                // 测试filter函数
                try {
                    LuaValue filterFunc = testGlobals.get("filter");
                    LuaValue testLog = LuaValue.tableOf();
                    testLog.set("timestamp", "2024-01-01 12:00:00");
                    testLog.set("message", "测试日志消息");
                    testLog.set("level", "INFO");
                    
                    Varargs filterResult = filterFunc.invoke(testLog);
                    boolean keep = filterResult.arg1().toboolean();
                    result.append("  测试filter: 返回 ").append(keep).append("\n");
                } catch (Exception e) {
                    result.append("  测试filter失败: ").append(e.getMessage()).append("\n");
                }
            } else {
                result.append("⚠ 未发现filter函数（将使用默认不过滤）\n");
            }
            
            if (hasTask) {
                result.append("✓ 发现task函数\n");
                
                // 测试task函数
                try {
                    LuaValue taskFunc = testGlobals.get("task");
                    LuaValue testStats = LuaValue.tableOf();
                    testStats.set("total", 100);
                    testStats.set("filtered", 10);
                    testStats.set("current", 90);
                    
                    Varargs taskResult = taskFunc.invoke(testStats);
                    LuaValue resultTable = taskResult.arg1();
                    if (resultTable.istable()) {
                        String action = resultTable.get("action").tojstring();
                        result.append("  测试task: action=").append(action).append("\n");
                    }
                } catch (Exception e) {
                    result.append("  测试task失败: ").append(e.getMessage()).append("\n");
                }
            } else {
                result.append("⚠ 未发现task函数（将使用默认行为）\n");
            }
            
            String output = outputCapture.toString("UTF-8");
            if (!output.isEmpty()) {
                result.append("\n=== 脚本输出 ===\n").append(output);
            }
            
            return result.toString();
            
        } catch (LuaError e) {
            String output;
            try {
                output = outputCapture.toString("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                output = outputCapture.toString();
            }
            StringBuilder result = new StringBuilder();
            result.append("=== 脚本错误 ===\n");
            result.append("错误: ").append(e.getMessage()).append("\n");
            if (!output.isEmpty()) {
                result.append("\n=== 脚本输出 ===\n").append(output);
            }
            return result.toString();
        } catch (Exception e) {
            return "=== 执行错误 ===\n" + e.getMessage();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
    
    /**
     * 获取最后错误
     */
    public String getLastError() {
        return lastError;
    }
    
    /**
     * 获取编译错误
     */
    public String getLastCompileError() {
        return lastCompileError;
    }
    
    /**
     * 脚本验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String summary;
        private final String details;
        
        public ValidationResult(boolean valid, String summary, String details) {
        // LogUtil.log("[ValidationResult] 开始执行");
            this.valid = valid;
            this.summary = summary;
            this.details = details;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public String getDetails() {
            return details;
        }
    }
}
