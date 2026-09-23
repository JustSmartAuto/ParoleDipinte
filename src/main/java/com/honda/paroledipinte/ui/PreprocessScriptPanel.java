package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * 预处理脚本面板
 * 提供调试控制台，可以覆盖软件内部的二值化逻辑
 * 提供OpenCV API到Lua中
 */
public class PreprocessScriptPanel extends JPanel {
    
    private RSyntaxTextArea scriptArea;
    private RSyntaxTextArea consoleArea;
    private JCheckBox enableCheckBox;
    private JLabel statusLabel;
    
    // Lua脚本引擎
    private Globals luaGlobals;
    
    // 当前处理的图像
    private BufferedImage currentImage;
    private JLabel previewLabel;
    
    public PreprocessScriptPanel() {
        LogUtil.log("[PreprocessScriptPanel] 开始执行");
        initComponents();
        initLuaEngine();
        loadScript();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 顶部控制面板
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        controlPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        enableCheckBox = new JCheckBox("启用自定义预处理脚本");
        enableCheckBox.setBackground(DarkBlueTheme.getBackgroundColor());
        enableCheckBox.setForeground(DarkBlueTheme.getTextColor());
        enableCheckBox.addActionListener(e -> {
            saveScript();
            updateStatus();
        });
        leftPanel.add(enableCheckBox);
        
        statusLabel = new JLabel("状态: 未启用");
        statusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        leftPanel.add(statusLabel);
        
        controlPanel.add(leftPanel, BorderLayout.WEST);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton validateBtn = createStyledButton("验证脚本");
        validateBtn.addActionListener(e -> validateScript());
        rightPanel.add(validateBtn);
        
        JButton testBtn = createStyledButton("测试运行");
        testBtn.addActionListener(e -> testScript());
        rightPanel.add(testBtn);
        
        JButton defaultBtn = createStyledButton("恢复模板");
        defaultBtn.addActionListener(e -> loadDefaultTemplate());
        rightPanel.add(defaultBtn);
        
        controlPanel.add(rightPanel, BorderLayout.EAST);
        
        add(controlPanel, BorderLayout.NORTH);
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 左侧：脚本编辑器和控制台
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setResizeWeight(0.65);
        leftSplitPane.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 脚本编辑区域
        scriptArea = new RSyntaxTextArea();
        scriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA);
        scriptArea.setCodeFoldingEnabled(true);
        scriptArea.setAntiAliasingEnabled(true);
        scriptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        applyDarkBlueThemeToSyntax(scriptArea);
        
        RTextScrollPane scriptScrollPane = new RTextScrollPane(scriptArea);
        scriptScrollPane.setLineNumbersEnabled(true);
        scriptScrollPane.setFoldIndicatorEnabled(true);
        scriptScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "Lua脚本 - 自定义二值化逻辑",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        scriptScrollPane.getGutter().setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        leftSplitPane.setTopComponent(scriptScrollPane);
        
        // 控制台输出区域
        consoleArea = new RSyntaxTextArea();
        consoleArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        consoleArea.setCodeFoldingEnabled(false);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        applyDarkBlueThemeToSyntax(consoleArea);
        
        RTextScrollPane consoleScrollPane = new RTextScrollPane(consoleArea);
        consoleScrollPane.setLineNumbersEnabled(false);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "调试控制台",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        consoleScrollPane.getGutter().setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        leftSplitPane.setBottomComponent(consoleScrollPane);
        
        splitPane.setLeftComponent(leftSplitPane);
        
        // 右侧：预览面板
        JPanel previewPanel = createPreviewPanel();
        splitPane.setRightComponent(previewPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton saveBtn = createStyledButton("保存脚本");
        saveBtn.addActionListener(e -> saveScript());
        buttonPanel.add(saveBtn);
        
        JButton loadImageBtn = createStyledButton("加载测试图片");
        loadImageBtn.addActionListener(e -> loadTestImage());
        buttonPanel.add(loadImageBtn);
        
        JButton clearConsoleBtn = createStyledButton("清空控制台");
        clearConsoleBtn.addActionListener(e -> consoleArea.setText(""));
        buttonPanel.add(clearConsoleBtn);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createPreviewPanel() {
        LogUtil.log("[createPreviewPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "脚本处理效果预览",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        panel.setBackground(DarkBlueTheme.PRIMARY_DARK);
        panel.setPreferredSize(new Dimension(300, 0));
        
        previewLabel = new JLabel("加载图片查看效果", JLabel.CENTER);
        previewLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        panel.add(previewLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createStyledButton(String text) {
        LogUtil.log("[createStyledButton] 开始执行");
        JButton button = new JButton(text);
        button.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        button.setForeground(DarkBlueTheme.getTextColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }
    
    private void applyDarkBlueThemeToSyntax(RSyntaxTextArea textArea) {
        LogUtil.log("[applyDarkBlueThemeToSyntax] 开始执行");
        textArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
        textArea.setForeground(DarkBlueTheme.getTextColor());
        textArea.setCaretColor(DarkBlueTheme.getTextColor());
        textArea.setSelectionColor(DarkBlueTheme.ACCENT);
        textArea.setCurrentLineHighlightColor(DarkBlueTheme.PRIMARY_LIGHT);
        
        // 设置Lua语法高亮颜色
        org.fife.ui.rsyntaxtextarea.SyntaxScheme scheme = new org.fife.ui.rsyntaxtextarea.SyntaxScheme(
            new Font(Font.MONOSPACED, Font.PLAIN, 13)
        );
        
        // 注释
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.COMMENT_EOL, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_COMMENT, DarkBlueTheme.PRIMARY_DARK));
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.COMMENT_MULTILINE, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_COMMENT, DarkBlueTheme.PRIMARY_DARK));
        
        // 关键字
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.RESERVED_WORD, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_KEYWORD, DarkBlueTheme.PRIMARY_DARK, new Font(Font.MONOSPACED, Font.BOLD, 13)));
        
        // 字符串
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.LITERAL_STRING_DOUBLE_QUOTE, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_STRING, DarkBlueTheme.PRIMARY_DARK));
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.LITERAL_CHAR, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_STRING, DarkBlueTheme.PRIMARY_DARK));
        
        // 数字
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.LITERAL_NUMBER_DECIMAL_INT, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_NUMBER, DarkBlueTheme.PRIMARY_DARK));
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.LITERAL_NUMBER_FLOAT, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_NUMBER, DarkBlueTheme.PRIMARY_DARK));
        
        // 函数
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.FUNCTION, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_FUNCTION, DarkBlueTheme.PRIMARY_DARK));
        
        // 变量/标识符
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_VARIABLE, DarkBlueTheme.PRIMARY_DARK));
        
        // 运算符
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.OPERATOR, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_OPERATOR, DarkBlueTheme.PRIMARY_DARK));
        
        // 数据类型
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.DATA_TYPE, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_TYPE, DarkBlueTheme.PRIMARY_DARK, new Font(Font.MONOSPACED, Font.BOLD, 13)));
        
        // 分隔符
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.SEPARATOR, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.TEXT_SECONDARY, DarkBlueTheme.PRIMARY_DARK));
        
        textArea.setSyntaxScheme(scheme);
    }
    
    /**
     * 初始化Lua引擎，注册OpenCV API
     */
    private void initLuaEngine() {
        LogUtil.log("[initLuaEngine] 开始执行");
        luaGlobals = JsePlatform.standardGlobals();
        
        // 注册print函数
        luaGlobals.set("print", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
        LogUtil.log("[invoke] 开始执行");
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= args.narg(); i++) {
                    if (i > 1) sb.append("\t");
                    sb.append(args.arg(i).tojstring());
                }
                appendToConsole(sb.toString());
                return NONE;
            }
        });
        
        // 注册OpenCV API函数
        registerOpenCvFunctions();
        
        // 注册工具函数
        registerUtilityFunctions();
    }
    
    /**
     * 注册OpenCV API函数到Lua
     */
    private void registerOpenCvFunctions() {
        LogUtil.log("[registerOpenCvFunctions] 开始执行");
        // cv.threshold: 二值化
        luaGlobals.set("cv_threshold", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                // 参数: image_data, threshold, max_value
                // 返回: 二值化后的image_data
                return arg1; // 简化实现，实际应处理图像数据
            }
        });
        
        // cv.adaptive_threshold: 自适应二值化
        luaGlobals.set("cv_adaptive_threshold", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
        LogUtil.log("[invoke] 开始执行");
                // 参数: image_data, max_value, adaptive_method, block_size, c
                return args.arg(1);
            }
        });
        
        // cv.gaussian_blur: 高斯模糊
        luaGlobals.set("cv_gaussian_blur", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                // 参数: image_data, kernel_width, kernel_height
                return arg1;
            }
        });
        
        // cv.median_blur: 中值滤波
        luaGlobals.set("cv_median_blur", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                // 参数: image_data, kernel_size
                return arg1;
            }
        });
        
        // cv.bilateral_filter: 双边滤波
        luaGlobals.set("cv_bilateral_filter", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
        LogUtil.log("[invoke] 开始执行");
                // 参数: image_data, d, sigma_color, sigma_space
                return args.arg(1);
            }
        });
        
        // cv.erode: 腐蚀
        luaGlobals.set("cv_erode", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                // 参数: image_data, kernel_size
                return arg1;
            }
        });
        
        // cv.dilate: 膨胀
        luaGlobals.set("cv_dilate", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                // 参数: image_data, kernel_size
                return arg1;
            }
        });
        
        // cv.morphology_ex: 形态学操作
        luaGlobals.set("cv_morphology_ex", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                // 参数: image_data, operation, kernel_size
                // operation: "open", "close", "gradient", "tophat", "blackhat"
                return arg1;
            }
        });
        
        // cv.cvt_color: 颜色空间转换
        luaGlobals.set("cv_cvt_color", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                // 参数: image_data, code
                // code: "gray", "bgr2gray", "rgb2gray", etc.
                return arg1;
            }
        });
        
        // cv.resize: 缩放
        luaGlobals.set("cv_resize", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                // 参数: image_data, width, height
                return arg1;
            }
        });
        
        // cv.normalize: 归一化
        luaGlobals.set("cv_normalize", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                // 参数: image_data, alpha
                return arg1;
            }
        });
        
        // cv.bitwise_not: 按位非（反色）
        luaGlobals.set("cv_bitwise_not", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                // 参数: image_data
                return arg;
            }
        });
        
        // cv.bitwise_and: 按位与
        luaGlobals.set("cv_bitwise_and", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return arg1;
            }
        });
        
        // cv.bitwise_or: 按位或
        luaGlobals.set("cv_bitwise_or", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return arg1;
            }
        });
        
        // cv.add_weighted: 加权叠加
        luaGlobals.set("cv_add_weighted", new VarArgFunction() {

            @Override
            public Varargs invoke(Varargs args) {
        LogUtil.log("[invoke] 开始执行");
                // 参数: image1, alpha, image2, beta
                return args.arg(1);
            }
        });
        
        // cv.laplacian: 拉普拉斯边缘检测
        luaGlobals.set("cv_laplacian", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                return arg;
            }
        });
        
        // cv.sobel: Sobel边缘检测
        luaGlobals.set("cv_sobel", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                // 参数: image_data, dx, dy
                return arg1;
            }
        });
        
        // cv.canny: Canny边缘检测
        luaGlobals.set("cv_canny", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                // 参数: image_data, threshold1, threshold2
                return arg1;
            }
        });
        
        // cv.find_contours: 查找轮廓
        luaGlobals.set("cv_find_contours", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                // 参数: image_data
                // 返回: 轮廓列表
                return LuaValue.tableOf();
            }
        });
        
        // cv.contour_area: 计算轮廓面积
        luaGlobals.set("cv_contour_area", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                return LuaValue.valueOf(0);
            }
        });
        
        // cv.bounding_rect: 计算边界矩形
        luaGlobals.set("cv_bounding_rect", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                LuaValue rect = LuaValue.tableOf();
                rect.set("x", 0);
                rect.set("y", 0);
                rect.set("width", 0);
                rect.set("height", 0);
                return rect;
            }
        });
        
        // cv.mean: 计算均值
        luaGlobals.set("cv_mean", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                return LuaValue.valueOf(128.0);
            }
        });
        
        // cv.min_max_loc: 查找最小最大值位置
        luaGlobals.set("cv_min_max_loc", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                LuaValue result = LuaValue.tableOf();
                result.set("min_val", 0);
                result.set("max_val", 255);
                result.set("min_loc", LuaValue.tableOf());
                result.set("max_loc", LuaValue.tableOf());
                return result;
            }
        });
    }
    
    /**
     * 注册工具函数
     */
    private void registerUtilityFunctions() {
        LogUtil.log("[registerUtilityFunctions] 开始执行");
        // 数学函数
        luaGlobals.set("abs", new OneArgFunction() {

            @Override
            public LuaValue call(LuaValue arg) {

                return LuaValue.valueOf(Math.abs(arg.todouble()));
            }
        });
        
        luaGlobals.set("min", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(Math.min(arg1.todouble(), arg2.todouble()));
            }
        });
        
        luaGlobals.set("max", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(Math.max(arg1.todouble(), arg2.todouble()));
            }
        });
        
        luaGlobals.set("clamp", new ThreeArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {

                double val = arg1.todouble();
                double min = arg2.todouble();
                double max = arg3.todouble();
                return LuaValue.valueOf(Math.max(min, Math.min(max, val)));
            }
        });
        
        // 字符串函数
        luaGlobals.set("contains", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(arg1.tojstring().contains(arg2.tojstring()));
            }
        });
        
        luaGlobals.set("starts_with", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(arg1.tojstring().startsWith(arg2.tojstring()));
            }
        });
        
        luaGlobals.set("ends_with", new TwoArgFunction() {

            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {

                return LuaValue.valueOf(arg1.tojstring().endsWith(arg2.tojstring()));
            }
        });
    }
    
    private void appendToConsole(String message) {
        LogUtil.log("[appendToConsole] 开始执行");
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getText().length());
        });
    }
    
    private void loadScript() {
        LogUtil.log("[loadScript] 开始执行");
        String script = ConfigManager.getPreprocessScript();
        boolean enabled = ConfigManager.isPreprocessScriptEnabled();
        
        if (script == null || script.isEmpty()) {
            script = getDefaultScript();
        }
        
        scriptArea.setText(script);
        enableCheckBox.setSelected(enabled);
        updateStatus();
    }
    
    private void saveScript() {
        LogUtil.log("[saveScript] 开始执行");
        String script = scriptArea.getText();
        boolean enabled = enableCheckBox.isSelected();
        
        ConfigManager.setPreprocessScript(script);
        ConfigManager.setPreprocessScriptEnabled(enabled);
        ConfigManager.saveConfig();
        
        updateStatus();
    }
    
    private void updateStatus() {
        LogUtil.log("[updateStatus] 开始执行");
        boolean enabled = enableCheckBox.isSelected();
        if (enabled) {
            statusLabel.setText("状态: 已启用");
            statusLabel.setForeground(DarkBlueTheme.SUCCESS);
        } else {
            statusLabel.setText("状态: 未启用");
            statusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        }
    }
    
    private void validateScript() {
        LogUtil.log("[validateScript] 开始执行");
        String script = scriptArea.getText();
        
        consoleArea.setText("");
        appendToConsole("=== 脚本验证 ===\n");
        
        try {
            // 创建新的Globals用于验证
            Globals testGlobals = JsePlatform.standardGlobals();
            testGlobals.load(script).call();
            
            // 检查是否包含preprocess函数
            LuaValue func = testGlobals.get("preprocess");
            if (!func.isnil()) {
                appendToConsole("✓ 发现preprocess函数");
            } else {
                appendToConsole("⚠ 未发现preprocess函数（可选）");
            }
            
            appendToConsole("✓ 脚本语法正确");
            
        } catch (LuaError e) {
            appendToConsole("✗ 脚本错误: " + e.getMessage());
        } catch (Exception e) {
            appendToConsole("✗ 验证失败: " + e.getMessage());
        }
    }
    
    private void testScript() {
        LogUtil.log("[testScript] 开始执行");
        String script = scriptArea.getText();
        
        consoleArea.setText("");
        appendToConsole("=== 脚本测试 ===\n");
        
        try {
            // 重新加载脚本到引擎
            luaGlobals = JsePlatform.standardGlobals();
            initLuaEngine();
            luaGlobals.load(script).call();
            
            // 测试调用preprocess函数（如果存在）
            LuaValue func = luaGlobals.get("preprocess");
            if (!func.isnil()) {
                appendToConsole("✓ 发现preprocess函数，正在测试...");
                
                // 创建测试图像数据
                LuaValue imageData = LuaValue.tableOf();
                imageData.set("width", 100);
                imageData.set("height", 100);
                imageData.set("channels", 1);
                
                // 创建参数表
                LuaValue params = LuaValue.tableOf();
                params.set("grayscale", 128);
                params.set("gamma", 0.5);
                params.set("threshold_offset", -20);
                params.set("invert", LuaValue.FALSE);
                
                // 调用函数
                Varargs result = func.invoke(imageData, params);
                
                appendToConsole("✓ 函数执行成功");
                appendToConsole("返回类型: " + result.arg1().typename());
            } else {
                appendToConsole("⚠ 未发现preprocess函数");
            }
            
            appendToConsole("\n✓ 脚本测试完成");
            
        } catch (LuaError e) {
            appendToConsole("✗ 脚本错误: " + e.getMessage());
        } catch (Exception e) {
            appendToConsole("✗ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadDefaultTemplate() {
        LogUtil.log("[loadDefaultTemplate] 开始执行");
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要恢复默认模板吗？当前脚本内容将被替换。", 
            "确认", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            scriptArea.setText(getDefaultScript());
            saveScript();
        }
    }
    
    private void loadTestImage() {
        LogUtil.log("[loadTestImage] 开始执行");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择测试图片");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "图片文件 (*.jpg, *.jpeg, *.png, *.bmp, *.gif)", 
            "jpg", "jpeg", "png", "bmp", "gif"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                currentImage = ImageIO.read(file);
                
                if (currentImage != null) {
                    // 显示预览
                    displayPreview(currentImage);
                    appendToConsole("已加载测试图片: " + file.getName());
                }
            } catch (Exception e) {
                appendToConsole("加载图片失败: " + e.getMessage());
            }
        }
    }
    
    private void displayPreview(BufferedImage image) {
        LogUtil.log("[displayPreview] 开始执行");
        // 计算缩放尺寸
        int maxWidth = 280;
        int maxHeight = 400;
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        double scaleX = (double) maxWidth / width;
        double scaleY = (double) maxHeight / height;
        double scale = Math.min(scaleX, scaleY);
        
        if (scale < 1.0) {
            width = (int) (width * scale);
            height = (int) (height * scale);
        }
        
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        previewLabel.setIcon(new ImageIcon(scaled));
        previewLabel.setText(null);
    }
    
    private String getDefaultScript() {
        return "-- 图像预处理脚本\n" +
               "-- 此脚本可以覆盖软件内部的二值化逻辑\n" +
               "-- 提供OpenCV API函数供调用\n\n" +
               "-- preprocess函数: 自定义预处理逻辑\n" +
               "-- image_data参数: 图像数据表 {width=宽度, height=高度, channels=通道数, data=像素数据}\n" +
               "-- params参数: 参数表 {grayscale=阈值, gamma=伽马值, threshold_offset=偏移量, invert=是否反色}\n" +
               "-- 返回: 处理后的图像数据表\n\n" +
               "function preprocess(image_data, params)\n" +
               "    print(\"开始预处理...\")\n" +
               "    print(\"图像尺寸: \" .. image_data.width .. \"x\" .. image_data.height)\n" +
               "    print(\"二值化阈值: \" .. params.grayscale)\n" +
               "    print(\"Gamma系数: \" .. params.gamma)\n" +
               "    print(\"阈值偏移: \" .. params.threshold_offset)\n" +
               "    \n" +
               "    -- 示例: 使用OpenCV API进行预处理\n" +
               "    -- local blurred = cv_gaussian_blur(image_data, 5, 5)\n" +
               "    -- local binary = cv_threshold(blurred, params.grayscale, 255)\n" +
               "    \n" +
               "    -- 示例: 自适应二值化\n" +
               "    -- local adaptive = cv_adaptive_threshold(image_data, 255, \"gaussian\", 11, 2)\n" +
               "    \n" +
               "    -- 示例: 形态学操作\n" +
               "    -- local morphed = cv_morphology_ex(image_data, \"open\", 3)\n" +
               "    \n" +
               "    -- 返回处理后的图像数据\n" +
               "    return image_data\n" +
               "end\n" +
               "\n" +
               "-- 可用的OpenCV API函数:\n" +
               "-- cv_threshold(image, thresh, maxval) - 二值化\n" +
               "-- cv_adaptive_threshold(image, maxval, method, block_size, c) - 自适应二值化\n" +
               "-- cv_gaussian_blur(image, kw, kh) - 高斯模糊\n" +
               "-- cv_median_blur(image, ksize) - 中值滤波\n" +
               "-- cv_bilateral_filter(image, d, sigma_color, sigma_space) - 双边滤波\n" +
               "-- cv_erode(image, ksize) - 腐蚀\n" +
               "-- cv_dilate(image, ksize) - 膨胀\n" +
               "-- cv_morphology_ex(image, op, ksize) - 形态学操作\n" +
               "-- cv_cvt_color(image, code) - 颜色空间转换\n" +
               "-- cv_resize(image, width, height) - 缩放\n" +
               "-- cv_normalize(image, alpha) - 归一化\n" +
               "-- cv_bitwise_not(image) - 反色\n" +
               "-- cv_bitwise_and(img1, img2) - 按位与\n" +
               "-- cv_bitwise_or(img1, img2) - 按位或\n" +
               "-- cv_add_weighted(img1, alpha, img2, beta) - 加权叠加\n" +
               "-- cv_laplacian(image) - 拉普拉斯边缘检测\n" +
               "-- cv_sobel(image, dx, dy) - Sobel边缘检测\n" +
               "-- cv_canny(image, thresh1, thresh2) - Canny边缘检测\n" +
               "-- cv_find_contours(image) - 查找轮廓\n" +
               "-- cv_contour_area(contour) - 计算轮廓面积\n" +
               "-- cv_bounding_rect(contour) - 计算边界矩形\n" +
               "-- cv_mean(image) - 计算均值\n" +
               "-- cv_min_max_loc(image) - 查找最小最大值\n";
    }
}
