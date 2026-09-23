package com.honda.paroledipinte.lua;

import com.honda.paroledipinte.util.LogUtil;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Lua脚本引擎
 * 提供文本区域定位和字符分类的Lua脚本支持
 */
public class LuaScriptEngine {
    
    private Globals globals;
    private String script;
    private boolean enabled;
    private String lastError;
    private String lastCompileError;
    private ScriptType scriptType;
    
    public enum ScriptType {
        TEXT_LOCATION,    // 文本区域定位
        CHAR_CLASSIFY,    // 字符分类
        OCR_POST_PROCESS  // OCR后处理（强制宽度分割、多行排序）
    }
    
    /**
     * 脚本执行结果
     */
    public static class ScriptResult {
        private final boolean success;
        private final Object data;
        private final String error;
        
        public ScriptResult(boolean success, Object data, String error) {
        LogUtil.log("[ScriptResult] 开始执行");
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
    
    public LuaScriptEngine(ScriptType type) {
        LogUtil.log("[LuaScriptEngine] 开始执行");
        this.scriptType = type;
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
        LogUtil.log("[registerUtilityFunctions] 开始执行");
        // print函数 - 输出到控制台
        globals.set("print", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
        LogUtil.log("[invoke] 开始执行");
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) sb.append("\t");
                    sb.append(args.arg(i).tojstring());
                }
                System.out.println(sb.toString());
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
        
        // abs: 绝对值
        globals.set("abs", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                return LuaValue.valueOf(Math.abs(arg.todouble()));
            }
        });
        
        // min: 最小值
        globals.set("min", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(Math.min(arg1.todouble(), arg2.todouble()));
            }
        });
        
        // max: 最大值
        globals.set("max", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(Math.max(arg1.todouble(), arg2.todouble()));
            }
        });
        
        // sqrt: 平方根
        globals.set("sqrt", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                return LuaValue.valueOf(Math.sqrt(arg.todouble()));
            }
        });
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
     * 验证脚本语法
     */
    public ValidationResult validateScript(String script) {
        LogUtil.log("[validateScript] 开始执行");
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
            String requiredFunc;
            switch (scriptType) {
                case TEXT_LOCATION:
                    requiredFunc = "locate_text";
                    break;
                case CHAR_CLASSIFY:
                    requiredFunc = "classify_char";
                    break;
                case OCR_POST_PROCESS:
                    requiredFunc = "post_process";
                    break;
                default:
                    requiredFunc = "main";
            }
            LuaValue func = testGlobals.get(requiredFunc);
            boolean hasFunc = !func.isnil();
            
            String output = outputCapture.toString("UTF-8");
            StringBuilder message = new StringBuilder();
            message.append("✓ 脚本语法正确\n");
            if (hasFunc) {
                message.append("✓ 发现").append(requiredFunc).append("函数\n");
            } else {
                message.append("⚠ 未发现").append(requiredFunc).append("函数\n");
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
        LogUtil.log("[testScript] 开始执行");
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
            String requiredFunc;
            switch (scriptType) {
                case TEXT_LOCATION:
                    requiredFunc = "locate_text";
                    break;
                case CHAR_CLASSIFY:
                    requiredFunc = "classify_char";
                    break;
                case OCR_POST_PROCESS:
                    requiredFunc = "post_process";
                    break;
                default:
                    requiredFunc = "main";
            }
            LuaValue func = testGlobals.get(requiredFunc);
            if (!func.isnil()) {
                result.append("✓ 发现").append(requiredFunc).append("函数\n");
            } else {
                result.append("⚠ 未发现").append(requiredFunc).append("函数\n");
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
     * 执行文本区域定位脚本（带OCR参数和正确字符串）
     * @param imageInfo 图像信息 {width, height}
     * @param regions 检测到的区域列表
     * @param ocrParams OCR参数表
     * @param correctString 正确字符串（可为null或空）
     * @return 脚本执行结果
     */
    public ScriptResult executeTextLocation(Map<String, Object> imageInfo, 
                                             List<Map<String, Object>> regions,
                                             Map<String, Object> ocrParams,
                                             String correctString) {
        return executeTextLocation(imageInfo, regions, ocrParams, correctString, null, null, null);
    }
    
    /**
     * 执行文本区域定位脚本（完整版，带预设参数和原始/修正区域）
     * @param imageInfo 图像信息 {width, height}
     * @param regions 检测到的区域列表（修正后的区域）
     * @param ocrParams OCR参数表
     * @param correctString 正确字符串（可为null或空）
     * @param presetParams 预设参数表（可为null）
     * @param rawRegions 原始分割出的字符框（可为null）
     * @param correctedRegions 字符相对位置修正后的字符框（可为null）
     * @return 脚本执行结果
     */
    public ScriptResult executeTextLocation(Map<String, Object> imageInfo, 
                                             List<Map<String, Object>> regions,
                                             Map<String, Object> ocrParams,
                                             String correctString,
                                             Map<String, Object> presetParams,
                                             List<Map<String, Object>> rawRegions,
                                             List<Map<String, Object>> correctedRegions) {
        LogUtil.log("[executeTextLocation] 开始执行");
        if (!enabled || script == null || scriptType != ScriptType.TEXT_LOCATION) {
            return new ScriptResult(false, null, "脚本未启用或类型不匹配");
        }
        
        try {
            LuaValue func = globals.get("locate_text");
            if (func.isnil()) {
                return new ScriptResult(false, null, "未找到locate_text函数");
            }
            
            // 创建Lua表
            LuaValue imageTable = createLuaTable(imageInfo);
            LuaValue regionsTable = createLuaArray(regions);
            LuaValue paramsTable = createLuaTable(ocrParams != null ? ocrParams : new HashMap<>());
            LuaValue correctStringValue = correctString != null ? LuaValue.valueOf(correctString) : LuaValue.NIL;
            
            // 注册预设参数到全局变量（如果提供）
            if (presetParams != null && !presetParams.isEmpty()) {
                LuaValue presetTable = createLuaTable(presetParams);
                globals.set("preset", presetTable);
            } else {
                globals.set("preset", LuaValue.NIL);
            }
            
            // 注册原始字符框和修正后字符框到全局变量
            if (rawRegions != null) {
                globals.set("rawRegions", createLuaArray(rawRegions));
            } else {
                globals.set("rawRegions", LuaValue.NIL);
            }
            
            if (correctedRegions != null) {
                globals.set("correctedRegions", createLuaArray(correctedRegions));
            } else {
                globals.set("correctedRegions", LuaValue.NIL);
            }
            
            // 调用函数（新签名：locate_text(imageInfo, regions, ocrParams, correctString)）
            // 使用invoke(LuaValue, Varargs)形式传递多个参数
            Varargs args = LuaValue.varargsOf(new LuaValue[]{regionsTable, paramsTable, correctStringValue});
            Varargs result = func.invoke(imageTable, args);
            
            // 解析结果
            List<Map<String, Object>> resultRegions = parseRegionsTable(result.arg1());
            
            return new ScriptResult(true, resultRegions, null);
            
        } catch (Exception e) {
            lastError = e.getMessage();
            return new ScriptResult(false, null, e.getMessage());
        }
    }
    
    /**
     * 执行文本区域定位脚本（兼容旧版本，不带OCR参数）
     */
    public ScriptResult executeTextLocation(Map<String, Object> imageInfo, List<Map<String, Object>> regions) {
        return executeTextLocation(imageInfo, regions, null, null, null, null, null);
    }
    
    /**
     * 执行字符分类脚本
     */
    public ScriptResult executeCharClassify(Map<String, Object> charInfo, List<Map<String, Object>> candidates) {
        LogUtil.log("[executeCharClassify] 开始执行");
        if (!enabled || script == null || scriptType != ScriptType.CHAR_CLASSIFY) {
            return new ScriptResult(false, null, "脚本未启用或类型不匹配");
        }
        
        try {
            LuaValue func = globals.get("classify_char");
            if (func.isnil()) {
                return new ScriptResult(false, null, "未找到classify_char函数");
            }
            
            // 创建Lua表
            LuaValue charTable = createLuaTable(charInfo);
            LuaValue candidatesTable = createLuaArray(candidates);
            
            // 调用函数
            Varargs result = func.invoke(charTable, candidatesTable);
            
            // 解析结果
            Map<String, Object> classification = parseClassificationTable(result.arg1());
            
            return new ScriptResult(true, classification, null);
            
        } catch (Exception e) {
            lastError = e.getMessage();
            return new ScriptResult(false, null, e.getMessage());
        }
    }
    
    /**
     * 创建Lua表（增强版，支持更多类型）
     */
    private LuaValue createLuaTable(Map<String, Object> map) {
        LogUtil.log("[createLuaTable] 开始执行");
        LuaValue table = LuaValue.tableOf();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            setLuaValue(table, key, value);
        }
        return table;
    }
    
    /**
     * 设置Lua值（支持多种Java类型）
     */
    private void setLuaValue(LuaValue table, Object key, Object value) {
        if (value == null) {
            return;
        }
        
        LuaValue luaKey = key instanceof Integer ? LuaValue.valueOf((Integer) key) : LuaValue.valueOf((String) key);
        LuaValue luaValue = toLuaValue(value);
        if (luaValue != null) {
            table.set(luaKey, luaValue);
        }
    }
    
    /**
     * 将Java对象转换为LuaValue
     */
    private LuaValue toLuaValue(Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof String) {
            return LuaValue.valueOf((String) value);
        } else if (value instanceof Integer) {
            return LuaValue.valueOf((Integer) value);
        } else if (value instanceof Long) {
            return LuaValue.valueOf(((Long) value).intValue());
        } else if (value instanceof Double) {
            return LuaValue.valueOf((Double) value);
        } else if (value instanceof Float) {
            return LuaValue.valueOf(((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            return LuaValue.valueOf((Boolean) value);
        } else if (value instanceof Map) {
            return createLuaTable((Map<String, Object>) value);
        } else if (value instanceof List) {
            return createLuaArrayFromList((List<?>) value);
        }
        return LuaValue.valueOf(value.toString());
    }
    
    /**
     * 创建Lua数组（从通用List）
     */
    private LuaValue createLuaArrayFromList(List<?> list) {
        LogUtil.log("[createLuaArrayFromList] 开始执行");
        LuaValue table = LuaValue.tableOf();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Map) {
                table.set(i + 1, createLuaTable((Map<String, Object>) item));
            } else {
                setLuaValue(table, i + 1, item);
            }
        }
        return table;
    }
    
    /**
     * 创建Lua数组
     */
    private LuaValue createLuaArray(List<Map<String, Object>> list) {
        LogUtil.log("[createLuaArray] 开始执行");
        LuaValue table = LuaValue.tableOf();
        for (int i = 0; i < list.size(); i++) {
            table.set(i + 1, createLuaTable(list.get(i)));
        }
        return table;
    }
    
    /**
     * 解析区域表
     */
    private List<Map<String, Object>> parseRegionsTable(LuaValue table) {
        LogUtil.log("[parseRegionsTable] 开始执行");
        List<Map<String, Object>> regions = new ArrayList<>();
        
        if (!table.istable()) {
            return regions;
        }
        
        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs n = table.next(k);
            k = n.arg1();
            LuaValue v = n.arg(2);
            if (k.isnil()) break;
            
            if (v.istable()) {
                Map<String, Object> region = new HashMap<>();
                region.put("x", v.get("x").toint());
                region.put("y", v.get("y").toint());
                region.put("width", v.get("width").toint());
                region.put("height", v.get("height").toint());
                regions.add(region);
            }
        }
        
        return regions;
    }
    
    /**
     * 解析分类结果表
     */
    private Map<String, Object> parseClassificationTable(LuaValue table) {
        LogUtil.log("[parseClassificationTable] 开始执行");
        Map<String, Object> result = new HashMap<>();
        
        if (!table.istable()) {
            return result;
        }
        
        result.put("label", table.get("label").tojstring());
        result.put("score", table.get("score").todouble());
        
        return result;
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
     * 执行OCR后处理脚本
     */
    public ScriptResult executeOcrPostProcess(Map<String, Object> imageInfo, 
                                               List<Map<String, Object>> regions,
                                               Map<String, Object> options) {
        LogUtil.log("[executeOcrPostProcess] 开始执行");
        if (!enabled || script == null || scriptType != ScriptType.OCR_POST_PROCESS) {
            return new ScriptResult(false, null, "脚本未启用或类型不匹配");
        }
        
        try {
            LuaValue func = globals.get("post_process");
            if (func.isnil()) {
                return new ScriptResult(false, null, "未找到post_process函数");
            }
            
            // 创建Lua表
            LuaValue imageTable = createLuaTable(imageInfo);
            LuaValue regionsTable = createLuaArray(regions);
            LuaValue optionsTable = createLuaTable(options);
            
            // 调用函数
            Varargs result = func.invoke(imageTable, regionsTable, optionsTable);
            
            // 解析结果
            List<Map<String, Object>> resultRegions = parseRegionsTable(result.arg1());
            
            return new ScriptResult(true, resultRegions, null);
            
        } catch (Exception e) {
            lastError = e.getMessage();
            return new ScriptResult(false, null, e.getMessage());
        }
    }
    
    /**
     * 获取默认脚本模板
     */
    public static String getDefaultScript(ScriptType type) {
        switch (type) {
            case TEXT_LOCATION:
                return "-- 文本区域定位脚本\n" +
                       "-- imageInfo包含: width, height\n" +
                       "-- regions是检测到的区域列表（已修正后），每个区域包含: x, y, width, height\n" +
                       "-- ocrParams包含: grayscale, gamma, minArea, maxArea, minWidth, maxWidth, minHeight, maxHeight\n" +
                       "-- correctString: 用户提供的正确字符串（可能为nil）\n" +
                       "-- preset: 当前使用的预设参数表（可能为nil）\n" +
                       "-- rawRegions: OCR参数分割出的原始字符框（可能为nil）\n" +
                       "-- correctedRegions: 字符相对位置修正后的字符框（可能为nil）\n" +
                       "-- 返回处理后的区域列表\n\n" +
                       "function locate_text(imageInfo, regions, ocrParams, correctString)\n" +
                       "    print(\"图像尺寸: \" .. imageInfo.width .. \"x\" .. imageInfo.height)\n" +
                       "    print(\"检测到 \" .. #regions .. \" 个区域\")\n" +
                       "    \n" +
                       "    -- 打印OCR参数\n" +
                       "    if ocrParams then\n" +
                       "        print(\"OCR参数 - 阈值:\" .. ocrParams.grayscale .. \" Gamma:\" .. ocrParams.gamma)\n" +
                       "        print(\"尺寸限制 - 宽:\" .. ocrParams.minWidth .. \"-\" .. ocrParams.maxWidth ..\n" +
                       "              \" 高:\" .. ocrParams.minHeight .. \"-\" .. ocrParams.maxHeight)\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 打印预设信息\n" +
                       "    if preset then\n" +
                       "        print(\"使用预设: \" .. preset.presetName)\n" +
                       "        print(\"正确字符串: \" .. (preset.correctString or \"\"))\n" +
                       "        if rawRegions then\n" +
                       "            print(\"原始字符框数: \" .. #rawRegions)\n" +
                       "        end\n" +
                       "        if correctedRegions then\n" +
                       "            print(\"修正后字符框数: \" .. #correctedRegions)\n" +
                       "        end\n" +
                       "        if preset.charRelativePositions then\n" +
                       "            print(\"字符相对位置数: \" .. #preset.charRelativePositions)\n" +
                       "        end\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 使用字符位置推断及修正算法\n" +
                       "    local result = infer_char_positions(regions, correctString, preset)\n" +
                       "    \n" +
                       "    print(\"处理后 \" .. #result .. \" 个区域\")\n" +
                       "    return result\n" +
                       "end\n" +
                       "\n" +
                       "-- 字符位置推断及修正算法\n" +
                       "-- 根据需求文档的字符位置推断逻辑实现\n" +
                       "function infer_char_positions(regions, correctString, preset)\n" +
                       "    if not correctString or #correctString == 0 then\n" +
                       "        print(\"无正确字符串，跳过推断\")\n" +
                       "        return regions\n" +
                       "    end\n" +
                       "    \n" +
                       "    local expectedCount = utf8.len(correctString) or #correctString\n" +
                       "    local actualCount = #regions\n" +
                       "    print(\"预期字符数: \" .. expectedCount .. \", 实际框数: \" .. actualCount)\n" +
                       "    \n" +
                       "    -- 获取参考框信息（优先使用correctedRegions，其次rawRegions）\n" +
                       "    local refRegions = correctedRegions or rawRegions or regions\n" +
                       "    \n" +
                       "    -- 情况1和2：框数量匹配且尺寸成一定比例\n" +
                       "    if actualCount == expectedCount then\n" +
                       "        print(\"情况1/2: 框数量匹配\")\n" +
                       "        -- 按X坐标排序\n" +
                       "        table.sort(regions, function(a, b) return a.x < b.x end)\n" +
                       "        return regions\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 情况3：分割错误（框数量不匹配）\n" +
                       "    print(\"情况3: 分割错误，进行修正推断\")\n" +
                       "    \n" +
                       "    -- 步骤1：根据框[0]尺寸修正框'[0]的分割\n" +
                       "    local corrected = {}\n" +
                       "    local firstRef = refRegions[1]\n" +
                       "    if firstRef then\n" +
                       "        -- 检查首字符是否需要分割或合并\n" +
                       "        local firstExpectedWidth = get_expected_width(0, preset)\n" +
                       "        if firstExpectedWidth and firstExpectedWidth > 0 then\n" +
                       "            local widthRatio = firstRef.width / firstExpectedWidth\n" +
                       "            print(\"首字符宽度比: \" .. string.format(\"%.2f\", widthRatio))\n" +
                       "            if widthRatio > 1.5 then\n" +
                       "                -- 过宽，可能是多个字符被合并了（如\"AB\"当成一个字符）\n" +
                       "                local splitCount = math.max(2, math.floor(widthRatio + 0.5))\n" +
                       "                local charWidth = math.floor(firstRef.width / splitCount)\n" +
                       "                for j = 0, splitCount - 1 do\n" +
                       "                    local x = firstRef.x + j * charWidth\n" +
                       "                    local width = charWidth\n" +
                       "                    if j == splitCount - 1 then\n" +
                       "                        width = firstRef.x + firstRef.width - x\n" +
                       "                    end\n" +
                       "                    table.insert(corrected, {x=x, y=firstRef.y, width=width, height=firstRef.height})\n" +
                       "                end\n" +
                       "                print(\"首字符过宽，分割为 \" .. splitCount .. \" 个框\")\n" +
                       "            elseif widthRatio < 0.7 then\n" +
                       "                -- 过窄，可能是一个字符被拆分了（如\"利\"当成两个字符）\n" +
                       "                -- 尝试与下一个框合并\n" +
                       "                if #refRegions > 1 then\n" +
                       "                    local nextRef = refRegions[2]\n" +
                       "                    local mergedWidth = firstRef.width + nextRef.width\n" +
                       "                    local gap = nextRef.x - (firstRef.x + firstRef.width)\n" +
                       "                    if gap <= 3 then\n" +
                       "                        table.insert(corrected, {x=firstRef.x, y=firstRef.y, width=mergedWidth + gap, height=math.max(firstRef.height, nextRef.height)})\n" +
                       "                        print(\"首字符过窄，与下一个框合并\")\n" +
                       "                    else\n" +
                       "                        table.insert(corrected, firstRef)\n" +
                       "                    end\n" +
                       "                else\n" +
                       "                    table.insert(corrected, firstRef)\n" +
                       "                end\n" +
                       "            else\n" +
                       "                table.insert(corrected, firstRef)\n" +
                       "            end\n" +
                       "        else\n" +
                       "            table.insert(corrected, firstRef)\n" +
                       "        end\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 处理剩余参考框\n" +
                       "    for i = 2, #refRegions do\n" +
                       "        table.insert(corrected, refRegions[i])\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 步骤2和3：对准字符串第一个和最后一个字符位置\n" +
                       "    -- 步骤4：调整字符框间距\n" +
                       "    corrected = adjust_spacing(corrected, expectedCount, preset)\n" +
                       "    \n" +
                       "    -- 步骤5：label(框') = label(框) -- 由Java层处理\n" +
                       "    return corrected\n" +
                       "end\n" +
                       "\n" +
                       "-- 获取预期字符宽度（从preset的charRelativePositions）\n" +
                       "function get_expected_width(index, preset)\n" +
                       "    if preset and preset.charRelativePositions and preset.charRelativePositions[index + 1] then\n" +
                       "        return preset.charRelativePositions[index + 1].width\n" +
                       "    end\n" +
                       "    return nil\n" +
                       "end\n" +
                       "\n" +
                       "-- 调整字符框间距\n" +
                       "function adjust_spacing(regions, expectedCount, preset)\n" +
                       "    if #regions == 0 then\n" +
                       "        return regions\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 按X坐标排序\n" +
                       "    table.sort(regions, function(a, b) return a.x < b.x end)\n" +
                       "    \n" +
                       "    -- 如果框数量仍不匹配，使用强制宽度分割\n" +
                       "    if #regions ~= expectedCount and expectedCount > 0 then\n" +
                       "        local totalWidth = regions[#regions].x + regions[#regions].width - regions[1].x\n" +
                       "        local avgWidth = math.floor(totalWidth / expectedCount)\n" +
                       "        local firstX = regions[1].x\n" +
                       "        local y = regions[1].y\n" +
                       "        local height = regions[1].height\n" +
                       "        \n" +
                       "        local forced = {}\n" +
                       "        for i = 0, expectedCount - 1 do\n" +
                       "            local x = firstX + i * avgWidth\n" +
                       "            local width = avgWidth\n" +
                       "            if i == expectedCount - 1 then\n" +
                       "                width = regions[#regions].x + regions[#regions].width - x\n" +
                       "            end\n" +
                       "            table.insert(forced, {x=x, y=y, width=width, height=height})\n" +
                       "        end\n" +
                       "        print(\"强制宽度分割: \" .. #regions .. \" -> \" .. expectedCount .. \" 个框\")\n" +
                       "        return forced\n" +
                       "    end\n" +
                       "    \n" +
                       "    return regions\n" +
                       "end\n" +
                       "\n" +
                       "-- 根据correctString判断全角/半角并调整合并策略\n" +
                       "function get_max_merge_width(ocrParams, correctString)\n" +
                       "    local maxMergeWidth = ocrParams and ocrParams.maxWidth or 500\n" +
                       "    if correctString and #correctString > 0 then\n" +
                       "        local firstChar = correctString:sub(1, 1)\n" +
                       "        local charCode = utf8.codepoint(firstChar) or firstChar:byte(1)\n" +
                       "        if charCode <= 127 then\n" +
                       "            maxMergeWidth = math.floor(maxMergeWidth / 2)\n" +
                       "        end\n" +
                       "    end\n" +
                       "    return maxMergeWidth\n" +
                       "end";
            case CHAR_CLASSIFY:
                return "-- 字符分类脚本\n" +
                       "-- charInfo包含: x, y, width, height, image_data\n" +
                       "-- candidates是候选字符列表，每个包含: char, score\n" +
                       "-- 返回分类结果: {label=字符, score=分数}\n\n" +
                       "function classify_char(charInfo, candidates)\n" +
                       "    print(\"字符位置: \" .. charInfo.x .. \",\" .. charInfo.y)\n" +
                       "    print(\"候选字符数: \" .. #candidates)\n" +
                       "    \n" +
                       "    -- 示例: 返回分数最高的候选\n" +
                       "    local best = candidates[1]\n" +
                       "    for i, candidate in ipairs(candidates) do\n" +
                       "        if candidate.score > best.score then\n" +
                       "            best = candidate\n" +
                       "        end\n" +
                       "    end\n" +
                       "    \n" +
                       "    return {label=best.char, score=best.score}\n" +
                       "end";
            case OCR_POST_PROCESS:
                return "-- OCR后处理脚本\n" +
                       "-- 实现字符区域查找、强制宽度分割、多行排序功能\n" +
                       "-- imageInfo包含: width, height\n" +
                       "-- regions是检测到的区域列表，每个区域包含: x, y, width, height\n" +
                       "-- options包含: forceWidthSplit, splitWidth, multiLineSort, rowGapThreshold\n" +
                       "-- 返回处理后的区域列表\n\n" +
                       "function post_process(imageInfo, regions, options)\n" +
                       "    print(\"图像尺寸: \" .. imageInfo.width .. \"x\" .. imageInfo.height)\n" +
                       "    print(\"检测到 \" .. #regions .. \" 个区域\")\n" +
                       "    print(\"选项: 强制分割=\" .. tostring(options.forceWidthSplit) .. \", 多行排序=\" .. tostring(options.multiLineSort))\n" +
                       "    \n" +
                       "    local result = regions\n" +
                       "    \n" +
                       "    -- 1. 强制宽度分割\n" +
                       "    if options.forceWidthSplit then\n" +
                       "        result = force_width_split(result, options.splitWidth)\n" +
                       "        print(\"强制分割后: \" .. #result .. \" 个区域\")\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 2. 多行排序\n" +
                       "    if options.multiLineSort then\n" +
                       "        result = sort_multiline(result, options.rowGapThreshold)\n" +
                       "        print(\"多行排序完成\")\n" +
                       "    else\n" +
                       "        -- 单行模式：按X坐标排序\n" +
                       "        table.sort(result, function(a, b) return a.x < b.x end)\n" +
                       "        print(\"单行排序完成\")\n" +
                       "    end\n" +
                       "    \n" +
                       "    return result\n" +
                       "end\n" +
                       "\n" +
                       "-- 强制宽度分割函数\n" +
                       "function force_width_split(regions, splitWidth)\n" +
                       "    local result = {}\n" +
                       "    for i, region in ipairs(regions) do\n" +
                       "        local charCount = math.max(1, math.floor(region.width / splitWidth + 0.5))\n" +
                       "        if charCount > 1 then\n" +
                       "            local charWidth = math.floor(region.width / charCount)\n" +
                       "            for j = 0, charCount - 1 do\n" +
                       "                local x = region.x + j * charWidth\n" +
                       "                local width = charWidth\n" +
                       "                if j == charCount - 1 then\n" +
                       "                    width = region.x + region.width - x\n" +
                       "                end\n" +
                       "                table.insert(result, {x=x, y=region.y, width=width, height=region.height})\n" +
                       "            end\n" +
                       "        else\n" +
                       "            table.insert(result, region)\n" +
                       "        end\n" +
                       "    end\n" +
                       "    return result\n" +
                       "end\n" +
                       "\n" +
                       "-- 多行排序函数\n" +
                       "function sort_multiline(regions, rowGapThreshold)\n" +
                       "    -- 按Y坐标排序\n" +
                       "    local sorted = {}\n" +
                       "    for i, r in ipairs(regions) do sorted[i] = r end\n" +
                       "    table.sort(sorted, function(a, b) return a.y < b.y end)\n" +
                       "    \n" +
                       "    -- 按行分组\n" +
                       "    local rows = {}\n" +
                       "    local currentRow = {}\n" +
                       "    for i, region in ipairs(sorted) do\n" +
                       "        if #currentRow == 0 then\n" +
                       "            table.insert(currentRow, region)\n" +
                       "        else\n" +
                       "            local last = currentRow[#currentRow]\n" +
                       "            if math.abs(region.y - last.y) < rowGapThreshold then\n" +
                       "                table.insert(currentRow, region)\n" +
                       "            else\n" +
                       "                table.insert(rows, currentRow)\n" +
                       "                currentRow = {region}\n" +
                       "            end\n" +
                       "        end\n" +
                       "    end\n" +
                       "    if #currentRow > 0 then\n" +
                       "        table.insert(rows, currentRow)\n" +
                       "    end\n" +
                       "    \n" +
                       "    -- 对每行按X坐标排序，然后合并\n" +
                       "    local result = {}\n" +
                       "    for i, row in ipairs(rows) do\n" +
                       "        table.sort(row, function(a, b) return a.x < b.x end)\n" +
                       "        for j, region in ipairs(row) do\n" +
                       "            table.insert(result, region)\n" +
                       "        end\n" +
                       "    end\n" +
                       "    return result\n" +
                       "end";
            default:
                return "-- 默认脚本\nfunction main()\n    print(\"Hello from Lua\")\nend";
        }
    }
    
    /**
     * 脚本验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String summary;
        private final String details;
        
        public ValidationResult(boolean valid, String summary, String details) {
        LogUtil.log("[ValidationResult] 开始执行");
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
    
    /**
     * 判断字符是否为全角字符
     * 全角字符：中文字符、全角标点等（Unicode编码大于127）
     * 半角字符：ASCII字符（0-127）
     */
    public static boolean isFullWidthChar(char c) {
        return c > 127;
    }
    
    /**
     * 根据correctString判断文本类型
     * @return true-全角文本, false-半角文本
     */
    public static boolean isFullWidthText(String correctString) {
        if (correctString == null || correctString.isEmpty()) {
            return true; // 默认为全角
        }
        return isFullWidthChar(correctString.charAt(0));
    }
}
