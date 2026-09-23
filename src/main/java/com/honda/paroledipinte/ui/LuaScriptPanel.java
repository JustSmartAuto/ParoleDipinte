package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.lua.LuaScriptEngine;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * Lua脚本编辑面板
 */
public class LuaScriptPanel extends JPanel {
    
    private LuaScriptEngine.ScriptType scriptType;
    private RSyntaxTextArea scriptArea;
    private RSyntaxTextArea consoleArea;
    private JCheckBox enableCheckBox;
    private JLabel statusLabel;
    
    private LuaScriptEngine scriptEngine;
    
    public LuaScriptPanel(LuaScriptEngine.ScriptType type) {
        LogUtil.log("[LuaScriptPanel] 开始执行");
        this.scriptType = type;
        this.scriptEngine = new LuaScriptEngine(type);
        initComponents();
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
        
        enableCheckBox = new JCheckBox("启用脚本");
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
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 脚本编辑区域
        scriptArea = new RSyntaxTextArea();
        scriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA);
        scriptArea.setCodeFoldingEnabled(true);
        scriptArea.setAntiAliasingEnabled(true);
        scriptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        
        // 应用深蓝色主题到脚本编辑器
        applyDarkBlueThemeToSyntax(scriptArea);
        
        RTextScrollPane scriptScrollPane = new RTextScrollPane(scriptArea);
        scriptScrollPane.setLineNumbersEnabled(true);
        scriptScrollPane.setFoldIndicatorEnabled(true);
        scriptScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "Lua脚本",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        scriptScrollPane.getGutter().setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        splitPane.setTopComponent(scriptScrollPane);
        
        // 控制台输出区域
        consoleArea = new RSyntaxTextArea();
        consoleArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        consoleArea.setCodeFoldingEnabled(false);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // 应用深蓝色主题到控制台
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
        
        splitPane.setBottomComponent(consoleScrollPane);
        
        add(splitPane, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton saveBtn = createStyledButton("保存脚本");
        saveBtn.addActionListener(e -> saveScript());
        buttonPanel.add(saveBtn);
        
        JButton clearConsoleBtn = createStyledButton("清空控制台");
        clearConsoleBtn.addActionListener(e -> consoleArea.setText(""));
        buttonPanel.add(clearConsoleBtn);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建样式化的按钮
     */
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
    
    /**
     * 应用深蓝色主题到语法高亮编辑器
     */
    private void applyDarkBlueThemeToSyntax(RSyntaxTextArea textArea) {
        LogUtil.log("[applyDarkBlueThemeToSyntax] 开始执行");
        textArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
        textArea.setForeground(DarkBlueTheme.getTextColor());
        textArea.setCaretColor(DarkBlueTheme.getTextColor());
        textArea.setSelectionColor(DarkBlueTheme.ACCENT);
        textArea.setCurrentLineHighlightColor(DarkBlueTheme.PRIMARY_LIGHT);
        
        // 设置Lua语法高亮颜色
        textArea.setSyntaxScheme(createLuaSyntaxScheme());
    }
    
    /**
     * 创建Lua语法高亮方案
     */
    private org.fife.ui.rsyntaxtextarea.SyntaxScheme createLuaSyntaxScheme() {
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
        
        return scheme;
    }
    
    private void loadScript() {
        LogUtil.log("[loadScript] 开始执行");
        String script = "";
        boolean enabled = false;
        
        if (scriptType == LuaScriptEngine.ScriptType.TEXT_LOCATION) {
            script = ConfigManager.getTextLocationScript();
            enabled = ConfigManager.isTextLocationEnabled();
        } else if (scriptType == LuaScriptEngine.ScriptType.CHAR_CLASSIFY) {
            script = ConfigManager.getCharClassifyScript();
            enabled = ConfigManager.isCharClassifyEnabled();
        } else if (scriptType == LuaScriptEngine.ScriptType.OCR_POST_PROCESS) {
            script = ConfigManager.getOcrPostProcessScript();
            enabled = ConfigManager.isOcrPostProcessEnabled();
        }
        
        if (script == null || script.isEmpty()) {
            script = LuaScriptEngine.getDefaultScript(scriptType);
        }
        
        scriptArea.setText(script);
        enableCheckBox.setSelected(enabled);
        
        scriptEngine.setScript(script);
        scriptEngine.setEnabled(enabled);
        
        updateStatus();
    }
    
    private void saveScript() {
        LogUtil.log("[saveScript] 开始执行");
        String script = scriptArea.getText();
        boolean enabled = enableCheckBox.isSelected();
        
        if (scriptType == LuaScriptEngine.ScriptType.TEXT_LOCATION) {
            ConfigManager.setTextLocationScript(script);
            ConfigManager.setTextLocationEnabled(enabled);
        } else if (scriptType == LuaScriptEngine.ScriptType.CHAR_CLASSIFY) {
            ConfigManager.setCharClassifyScript(script);
            ConfigManager.setCharClassifyEnabled(enabled);
        } else if (scriptType == LuaScriptEngine.ScriptType.OCR_POST_PROCESS) {
            ConfigManager.setOcrPostProcessScript(script);
            ConfigManager.setOcrPostProcessEnabled(enabled);
        }
        
        ConfigManager.saveConfig();
        
        scriptEngine.setScript(script);
        scriptEngine.setEnabled(enabled);
        
        updateStatus();
    }
    
    private void updateStatus() {
        LogUtil.log("[updateStatus] 开始执行");
        boolean enabled = enableCheckBox.isSelected();
        if (enabled) {
            String error = scriptEngine.getLastCompileError();
            if (error != null) {
                statusLabel.setText("状态: 编译错误");
                statusLabel.setForeground(DarkBlueTheme.ERROR);
            } else {
                statusLabel.setText("状态: 已启用");
                statusLabel.setForeground(DarkBlueTheme.SUCCESS);
            }
        } else {
            statusLabel.setText("状态: 未启用");
            statusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        }
    }
    
    private void validateScript() {
        LogUtil.log("[validateScript] 开始执行");
        String script = scriptArea.getText();
        LuaScriptEngine.ValidationResult result = scriptEngine.validateScript(script);
        
        consoleArea.setText("");
        consoleArea.append("=== 脚本验证结果 ===\n\n");
        consoleArea.append(result.getDetails());
        
        if (result.isValid()) {
            saveScript();
        }
    }
    
    private void testScript() {
        LogUtil.log("[testScript] 开始执行");
        String script = scriptArea.getText();
        String output = scriptEngine.testScript(script);
        
        consoleArea.setText("");
        consoleArea.append(output);
    }
    
    private void loadDefaultTemplate() {
        LogUtil.log("[loadDefaultTemplate] 开始执行");
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要恢复默认模板吗？当前脚本内容将被替换。", 
            "确认", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            String template = LuaScriptEngine.getDefaultScript(scriptType);
            scriptArea.setText(template);
            saveScript();
        }
    }
    
    /**
     * 获取脚本引擎
     */
    public LuaScriptEngine getScriptEngine() {
        return scriptEngine;
    }
}
