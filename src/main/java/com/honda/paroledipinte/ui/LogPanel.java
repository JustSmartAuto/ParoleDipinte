package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.lua.LogScriptEngine;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 日志面板
 * 显示软件日志，每100行自动分页
 * 包含脚本子标签页，支持日志过滤和自动任务
 */
public class LogPanel extends JPanel {
    
    private static final int PAGE_SIZE = 100;
    private static final int MAX_LOGS_DEFAULT = 10000; // 默认最大日志条数
    private static final int BUFFER_TIME_MS = 10; // 缓冲时间10ms
    private static final int BUFFER_SIZE_THRESHOLD = 50; // 缓冲数量阈值
    
    // 日志显示组件
    private RSyntaxTextArea logArea;
    private JLabel pageLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JButton clearButton;
    private JTextField pageInputField;
    private JButton jumpButton;
    
    // 脚本组件
    private JTabbedPane tabbedPane;
    private RSyntaxTextArea scriptArea;
    private RSyntaxTextArea consoleArea;
    private JCheckBox enableCheckBox;
    private JLabel scriptStatusLabel;
    
    // 数据
    private List<String> allLogs;
    private List<String> filteredLogs;
    private int currentPage;
    private int totalPages;
    private int filteredCount;
    
    // 日志缓冲区
    private final BlockingQueue<String> logBuffer;
    private final ScheduledExecutorService bufferExecutor;
    private volatile boolean isBuffering;
    
    private SimpleDateFormat dateFormat;
    private LogScriptEngine scriptEngine;
    
    public LogPanel() {
        // LogUtil.log("[LogPanel] 开始执行");
        this.allLogs = new ArrayList<>();
        this.filteredLogs = new ArrayList<>();
        this.currentPage = 1;
        this.totalPages = 1;
        this.filteredCount = 0;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.scriptEngine = new LogScriptEngine();
        
        // 初始化日志缓冲区
        this.logBuffer = new LinkedBlockingQueue<>();
        this.bufferExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LogBuffer-Thread");
            t.setDaemon(true);
            return t;
        });
        this.isBuffering = true;
        
        initComponents();
        loadScript();
        startBufferProcessor();
    }
    
    private void initComponents() {
        // LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 创建标签页
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DarkBlueTheme.getBackgroundColor());
        tabbedPane.setForeground(DarkBlueTheme.getTextColor());
        
        // 日志显示标签页
        JPanel logDisplayPanel = createLogDisplayPanel();
        tabbedPane.addTab("日志", logDisplayPanel);
        
        // 脚本标签页
        JPanel scriptPanel = createScriptPanel();
        tabbedPane.addTab("脚本", scriptPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * 创建日志显示面板
     */
    private JPanel createLogDisplayPanel() {
        // LogUtil.log("[createLogDisplayPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 日志显示区域
        logArea = new RSyntaxTextArea();
        logArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        logArea.setCodeFoldingEnabled(false);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // 应用深蓝色主题
        applyDarkBlueThemeToTextArea(logArea);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBackground(DarkBlueTheme.PRIMARY_DARK);
        scrollPane.getViewport().setBackground(DarkBlueTheme.PRIMARY_DARK);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        controlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DarkBlueTheme.BORDER));
        
        prevButton = createStyledButton("<< 上一页");
        prevButton.setEnabled(false);
        prevButton.addActionListener(e -> goToPage(currentPage - 1));
        controlPanel.add(prevButton);
        
        pageLabel = new JLabel("第 1 页 / 共 1 页");
        pageLabel.setForeground(DarkBlueTheme.getSecondaryTextColor());
        controlPanel.add(pageLabel);
        
        nextButton = createStyledButton("下一页 >>");
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> goToPage(currentPage + 1));
        controlPanel.add(nextButton);
        
        controlPanel.add(Box.createHorizontalStrut(10));
        
        // 页码跳转功能
        JLabel jumpLabel = new JLabel("跳转到:");
        jumpLabel.setForeground(DarkBlueTheme.getSecondaryTextColor());
        controlPanel.add(jumpLabel);
        
        pageInputField = new JTextField(4);
        pageInputField.setBackground(DarkBlueTheme.PRIMARY_DARK);
        pageInputField.setForeground(DarkBlueTheme.getTextColor());
        pageInputField.setCaretColor(DarkBlueTheme.getTextColor());
        pageInputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        pageInputField.addActionListener(e -> jumpToPage());
        controlPanel.add(pageInputField);
        
        jumpButton = createStyledButton("跳转");
        jumpButton.addActionListener(e -> jumpToPage());
        controlPanel.add(jumpButton);
        
        controlPanel.add(Box.createHorizontalStrut(20));
        
        clearButton = createStyledButton("清空日志");
        clearButton.addActionListener(e -> clearLogs());
        controlPanel.add(clearButton);
        
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建脚本面板
     */
    private JPanel createScriptPanel() {
        // LogUtil.log("[createScriptPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 顶部控制面板
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        enableCheckBox = new JCheckBox("启用脚本");
        enableCheckBox.setBackground(DarkBlueTheme.getBackgroundColor());
        enableCheckBox.setForeground(DarkBlueTheme.getTextColor());
        enableCheckBox.addActionListener(e -> {
            saveScript();
            updateScriptStatus();
        });
        leftPanel.add(enableCheckBox);
        
        scriptStatusLabel = new JLabel("状态: 未启用");
        scriptStatusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        leftPanel.add(scriptStatusLabel);
        
        controlPanel.add(leftPanel, BorderLayout.WEST);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton validateBtn = createStyledButton("验证脚本");
        validateBtn.addActionListener(e -> validateScript());
        rightPanel.add(validateBtn);
        
        JButton testBtn = createStyledButton("测试运行");
        testBtn.addActionListener(e -> testScript());
        rightPanel.add(testBtn);
        
        JButton defaultBtn = createStyledButton("恢复默认");
        defaultBtn.addActionListener(e -> loadDefaultScript());
        rightPanel.add(defaultBtn);
        
        controlPanel.add(rightPanel, BorderLayout.EAST);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
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
        
        // 应用深蓝色主题
        applyDarkBlueThemeToTextArea(scriptArea);
        // 设置Lua语法高亮
        scriptArea.setSyntaxScheme(createLuaSyntaxScheme());
        
        RTextScrollPane scriptScrollPane = new RTextScrollPane(scriptArea);
        scriptScrollPane.setLineNumbersEnabled(true);
        scriptScrollPane.setFoldIndicatorEnabled(true);
        scriptScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "Lua脚本 (filter函数: 过滤日志, task函数: 定时任务)",
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
        
        // 应用深蓝色主题
        applyDarkBlueThemeToTextArea(consoleArea);
        
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
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton saveBtn = createStyledButton("保存脚本");
        saveBtn.addActionListener(e -> saveScript());
        buttonPanel.add(saveBtn);
        
        JButton clearConsoleBtn = createStyledButton("清空控制台");
        clearConsoleBtn.addActionListener(e -> consoleArea.setText(""));
        buttonPanel.add(clearConsoleBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置控制台输出回调
        scriptEngine.setConsoleOutputCallback(message -> {
            SwingUtilities.invokeLater(() -> {
                consoleArea.append(message + "\n");
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            });
        });
        
        return panel;
    }
    
    /**
     * 创建样式化的按钮
     */
    private JButton createStyledButton(String text) {
        // LogUtil.log("[createStyledButton] 开始执行");
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
     * 应用深蓝色主题到文本区域
     */
    private void applyDarkBlueThemeToTextArea(RSyntaxTextArea textArea) {
        // LogUtil.log("[applyDarkBlueThemeToTextArea] 开始执行");
        textArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
        textArea.setForeground(DarkBlueTheme.getTextColor());
        textArea.setCaretColor(DarkBlueTheme.getTextColor());
        textArea.setSelectionColor(DarkBlueTheme.ACCENT);
        textArea.setCurrentLineHighlightColor(DarkBlueTheme.PRIMARY_LIGHT);
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
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_KEYWORD, DarkBlueTheme.PRIMARY_DARK, 
                new Font(Font.MONOSPACED, Font.BOLD, 13)));
        
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
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_TYPE, DarkBlueTheme.PRIMARY_DARK,
                new Font(Font.MONOSPACED, Font.BOLD, 13)));
        
        // 分隔符
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.SEPARATOR, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.TEXT_SECONDARY, DarkBlueTheme.PRIMARY_DARK));
        
        return scheme;
    }
    
    /**
     * 加载脚本
     */
    private void loadScript() {
        // LogUtil.log("[loadScript] 开始执行");
        String script = ConfigManager.getLogScript();
        boolean enabled = ConfigManager.isLogScriptEnabled();
        
        if (script == null || script.isEmpty()) {
            script = LogScriptEngine.getDefaultScript();
        }
        
        scriptArea.setText(script);
        enableCheckBox.setSelected(enabled);
        
        scriptEngine.setScript(script);
        scriptEngine.setEnabled(enabled);
        
        updateScriptStatus();
    }
    
    /**
     * 保存脚本
     */
    private void saveScript() {
        // LogUtil.log("[saveScript] 开始执行");
        String script = scriptArea.getText();
        boolean enabled = enableCheckBox.isSelected();
        
        ConfigManager.setLogScript(script);
        ConfigManager.setLogScriptEnabled(enabled);
        ConfigManager.saveConfig();
        
        scriptEngine.setScript(script);
        scriptEngine.setEnabled(enabled);
        
        updateScriptStatus();
    }
    
    /**
     * 更新脚本状态
     */
    private void updateScriptStatus() {
        // LogUtil.log("[updateScriptStatus] 开始执行");
        boolean enabled = enableCheckBox.isSelected();
        if (enabled) {
            String error = scriptEngine.getLastCompileError();
            if (error != null) {
                scriptStatusLabel.setText("状态: 编译错误");
                scriptStatusLabel.setForeground(DarkBlueTheme.ERROR);
            } else {
                scriptStatusLabel.setText("状态: 已启用");
                scriptStatusLabel.setForeground(DarkBlueTheme.SUCCESS);
            }
        } else {
            scriptStatusLabel.setText("状态: 未启用");
            scriptStatusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        }
    }
    
    /**
     * 验证脚本
     */
    private void validateScript() {
        // LogUtil.log("[validateScript] 开始执行");
        String script = scriptArea.getText();
        LogScriptEngine.ValidationResult result = scriptEngine.validateScript(script);
        
        consoleArea.setText("");
        consoleArea.append("=== 脚本验证结果 ===\n\n");
        consoleArea.append(result.getDetails());
        
        if (result.isValid()) {
            saveScript();
        }
    }
    
    /**
     * 测试脚本
     */
    private void testScript() {
        // LogUtil.log("[testScript] 开始执行");
        String script = scriptArea.getText();
        String output = scriptEngine.testScript(script);
        
        consoleArea.setText("");
        consoleArea.append(output);
    }
    
    /**
     * 加载默认脚本
     */
    private void loadDefaultScript() {
        // LogUtil.log("[loadDefaultScript] 开始执行");
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要恢复默认脚本吗？当前脚本内容将被替换。", 
            "确认", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            String template = LogScriptEngine.getDefaultScript();
            scriptArea.setText(template);
            saveScript();
        }
    }
    
    /**
     * 添加日志
     * 使用缓冲区批量处理，防止界面卡顿
     */
    public void appendLog(String message) {
        String timestamp = dateFormat.format(new Date());
        String logLine = "[" + timestamp + "] " + message;
        
        // 将日志放入缓冲区（非阻塞）
        logBuffer.offer(logLine);
    }
    
    /**
     * 处理单条日志（由缓冲区处理器调用）
     */
    private void processLog(String logLine) {
        // 提取原始消息（去掉时间戳前缀）
        String message = logLine;
        int idx = logLine.indexOf("] ");
        if (idx > 0) {
            message = logLine.substring(idx + 2);
        }
        
        // 执行filter函数决定是否保留此日志
        String timestamp = dateFormat.format(new Date());
        boolean shouldKeep = scriptEngine.executeFilter(timestamp, message, "INFO");
        
        allLogs.add(logLine);
        
        if (shouldKeep) {
            filteredLogs.add(logLine);
        } else {
            filteredCount++;
        }
        
        // 执行task函数检查是否需要清空
        LogScriptEngine.TaskResult taskResult = scriptEngine.executeTask(
            allLogs.size(), filteredCount, filteredLogs.size());
        
        if (taskResult.shouldClear()) {
            // 自动清空日志
            String clearMessage = taskResult.getMessage();
            if (clearMessage == null || clearMessage.isEmpty()) {
                clearMessage = "日志达到限制，自动清空";
            }
            
            allLogs.clear();
            filteredLogs.clear();
            filteredCount = 0;
            currentPage = 1;
            
            // 添加清空提示
            String clearTimestamp = dateFormat.format(new Date());
            String clearLogLine = "[" + clearTimestamp + "] [系统] " + clearMessage;
            allLogs.add(clearLogLine);
            filteredLogs.add(clearLogLine);
        }
    }
    
    /**
     * 跳转到指定页
     */
    private void goToPage(int page) {
        // LogUtil.log("[goToPage] 开始执行");
        if (page < 1 || page > totalPages) {
            return;
        }
        currentPage = page;
        refreshDisplay();
        updatePageLabel();
    }
    
    /**
     * 从输入框跳转到指定页
     */
    private void jumpToPage() {
        String input = pageInputField.getText().trim();
        if (input.isEmpty()) {
            return;
        }
        
        try {
            int page = Integer.parseInt(input);
            if (page < 1 || page > totalPages) {
                JOptionPane.showMessageDialog(this,
                    String.format("请输入有效的页码 (1-%d)", totalPages),
                    "提示",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            goToPage(page);
            pageInputField.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "请输入有效的数字",
                "错误",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 刷新显示
     */
    private void refreshDisplay() {
        // LogUtil.log("[refreshDisplay] 开始执行");
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredLogs.size());
        
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(filteredLogs.get(i)).append("\n");
        }
        
        logArea.setText(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    /**
     * 更新页码标签
     */
    private void updatePageLabel() {
        // LogUtil.log("[updatePageLabel] 开始执行");
        pageLabel.setText(String.format("第 %d 页 / 共 %d 页 (显示 %d / 总计 %d 条, 已过滤 %d 条)", 
            currentPage, totalPages, filteredLogs.size(), allLogs.size(), filteredCount));
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }
    
    /**
     * 清空日志
     */
    private void clearLogs() {
        // LogUtil.log("[clearLogs] 开始执行");
        allLogs.clear();
        filteredLogs.clear();
        filteredCount = 0;
        currentPage = 1;
        totalPages = 1;
        refreshDisplay();
        updatePageLabel();
    }
    
    /**
     * 获取所有日志
     */
    public List<String> getAllLogs() {
        return new ArrayList<>(allLogs);
    }
    
    /**
     * 获取过滤后的日志
     */
    public List<String> getFilteredLogs() {
        return new ArrayList<>(filteredLogs);
    }
    
    /**
     * 启动缓冲区处理器
     * 定期将缓冲的日志批量更新到界面
     */
    private void startBufferProcessor() {
        // 每10ms执行一次，将缓冲的日志批量处理
        bufferExecutor.scheduleAtFixedRate(() -> {
            if (!isBuffering || logBuffer.isEmpty()) {
                return;
            }
            
            // 批量取出日志
            List<String> batch = new ArrayList<>();
            logBuffer.drainTo(batch, BUFFER_SIZE_THRESHOLD);
            
            if (!batch.isEmpty()) {
                // 在UI线程中批量更新
                SwingUtilities.invokeLater(() -> {
                    for (String logLine : batch) {
                        processLog(logLine);
                    }
                    // 批量更新UI
                    updateUIAfterBatch();
                });
            }
        }, BUFFER_TIME_MS, BUFFER_TIME_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 批量处理后更新UI
     */
    private void updateUIAfterBatch() {
        // 计算总页数
        totalPages = (filteredLogs.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (totalPages < 1) totalPages = 1;
        
        // 如果在最后一页，自动切换到新页
        if (currentPage == totalPages - 1 || currentPage == totalPages) {
            currentPage = totalPages;
            refreshDisplay();
        }
        
        updatePageLabel();
    }
    
    /**
     * 停止缓冲区处理
     */
    public void shutdown() {
        isBuffering = false;
        bufferExecutor.shutdown();
        try {
            if (!bufferExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                bufferExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            bufferExecutor.shutdownNow();
        }
        
        // 清空缓冲区
        List<String> remaining = new ArrayList<>();
        logBuffer.drainTo(remaining);
        if (!remaining.isEmpty()) {
            for (String logLine : remaining) {
                processLog(logLine);
            }
            updateUIAfterBatch();
        }
    }
}
