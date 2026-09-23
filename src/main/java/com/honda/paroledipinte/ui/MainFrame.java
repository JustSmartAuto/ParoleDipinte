package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.db.DatabaseService;
import com.honda.paroledipinte.image.CharacterGenerator;
import com.honda.paroledipinte.mqtt.MqttService;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import com.honda.paroledipinte.util.FontLoader;
import com.honda.paroledipinte.util.GlobalKvCore;
import com.honda.paroledipinte.model.StringPreset;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.util.List;

/**
 * 主窗口
 */
public class MainFrame extends JFrame {
    
    // 全局单例实例
    private static MainFrame instance;
    
    public static MainFrame getInstance() {
        return instance;
    }
    
    /**
     * 获取OCR测试面板
     */
    public OcrTestPanel getOcrTestPanel() {
        return ocrTestPanel;
    }
    
    /**
     * 获取OCR参数设置面板
     */
    public OcrSettingsPanel getOcrSettingsPanel() {
        return ocrSettingsPanel;
    }
    
    /**
     * 获取主标签页面板
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    
    private JTabbedPane tabbedPane;
    private LogPanel logPanel;
    private OcrSettingsPanel ocrSettingsPanel;
    private MqttSettingsPanel mqttSettingsPanel;
    private PathSettingsPanel pathSettingsPanel;
    private LuaScriptPanel textLocationScriptPanel;
    private LuaScriptPanel charClassifyScriptPanel;
    private LuaScriptPanel ocrPostProcessPanel;
    private OcrTestPanel ocrTestPanel;
    private ClassifyTestPanel classifyTestPanel;
    private ImagePreprocessPanel imagePreprocessPanel;
    private SoftwareSettingsPanel softwareSettingsPanel;
    private StringPresetPanel stringPresetPanel;
    
    private DatabaseService dbService;
    private MqttService mqttService;
    private Process mqttBrokerProcess;
    
    private JLabel statusLabel;
    private JButton startButton;
    private JButton stopButton;
    
    // 菜单项引用
    private JMenuItem showBaselineMenuItem;
    private JMenuItem showGridMenuItem;
    private JMenuItem splitRegionMenuItem;
    private JMenuItem mergeRegionMenuItem;
    private JMenuItem deleteRegionMenuItem;
    private JMenuItem updateLabelsMenuItem;
    private JMenuItem restoreParamsMenuItem;
    
    // 使用预设前的参数备份
    private StringPreset backupPresetParams;
    
    public MainFrame() {
        LogUtil.log("[MainFrame] 开始执行");
        instance = this;
        initComponents();
        initServices();
        checkAndStartMqttBroker();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        // 设置标题
        setTitle("识字涂色 (ParoleDipinte) - OCR服务");
        
        // 设置默认关闭操作
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // 设置窗口大小 (4:3比例)，从配置读取
        int windowWidth = ConfigManager.getWindowWidth();
        int windowHeight = ConfigManager.getWindowHeight();
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null);
        
        // 设置图标
        loadIcon();
        
        // 创建菜单栏
        setJMenuBar(createMenuBar());
        
        // 主面板 - 使用深蓝色背景
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 创建工具栏
        mainPanel.add(createToolBar(), BorderLayout.NORTH);
        
        // 创建标签页
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DarkBlueTheme.getBackgroundColor());
        tabbedPane.setForeground(DarkBlueTheme.getTextColor());
        
        // 文本定位脚本标签页（先创建，以便传递给OCR测试面板）
        textLocationScriptPanel = new LuaScriptPanel(com.honda.paroledipinte.lua.LuaScriptEngine.ScriptType.TEXT_LOCATION);
        
        // OCR识别测试标签页
        ocrTestPanel = new OcrTestPanel();
        ocrTestPanel.setLogCallback(this::log);
        ocrTestPanel.setDatabaseService(dbService);
        ocrTestPanel.setLuaScriptEngine(textLocationScriptPanel.getScriptEngine());
        tabbedPane.addTab("OCR识别测试", ocrTestPanel);
        
        // 字符分类测试标签页
        classifyTestPanel = new ClassifyTestPanel();
        classifyTestPanel.setDatabaseService(dbService);
        tabbedPane.addTab("字符分类测试", classifyTestPanel);
        
        // 图片预处理标签页
        imagePreprocessPanel = new ImagePreprocessPanel();
        tabbedPane.addTab("图片预处理", imagePreprocessPanel);
        
        // 软件日志标签页
        logPanel = new LogPanel();
        tabbedPane.addTab("软件日志", logPanel);
        
        // 注册LogUtil回调，将日志信息显示到软件日志界面
        LogUtil.addLogCallback(this::log);
        
        // OCR参数设置标签页
        ocrSettingsPanel = new OcrSettingsPanel(() -> {
            log("OCR参数已更新");
        });
        tabbedPane.addTab("OCR参数", ocrSettingsPanel);
        
        // MQTT设置面板（不添加到标签栏，只通过菜单访问）
        mqttSettingsPanel = new MqttSettingsPanel(() -> {
            log("MQTT设置已更新，重启服务后生效");
        });
        
        // 路径设置面板（不添加到标签栏，只通过菜单访问）
        pathSettingsPanel = new PathSettingsPanel(() -> {
            log("路径设置已更新");
        });
        
        // 软件设置面板（不添加到标签栏，只通过菜单访问）
        softwareSettingsPanel = new SoftwareSettingsPanel(() -> {
            log("软件设置已更新");
        });
        
        // 字符分类脚本面板（不添加到标签栏，只通过菜单访问）
        charClassifyScriptPanel = new LuaScriptPanel(com.honda.paroledipinte.lua.LuaScriptEngine.ScriptType.CHAR_CLASSIFY);
        
        // OCR后处理脚本面板（不添加到标签栏，只通过菜单访问）
        ocrPostProcessPanel = new LuaScriptPanel(com.honda.paroledipinte.lua.LuaScriptEngine.ScriptType.OCR_POST_PROCESS);
        
        // 编程面板（不添加到标签栏，只通过菜单访问）
        JPanel programmingPanel = createProgrammingPanel();
        
        // 关于面板（不添加到标签栏，只通过菜单访问）
        JPanel aboutPanel = createAboutPanel();
        
        // 字符串预设面板（不添加到标签栏，只通过菜单访问）
        stringPresetPanel = new StringPresetPanel();
        stringPresetPanel.setLogCallback(this::log);
        
        // 设置重连数据库回调
        stringPresetPanel.setReconnectCallback(() -> {
            try {
                log("正在重新初始化数据库服务...");
                
                // 关闭旧的数据库连接
                if (dbService != null) {
                    try {
                        dbService.close();
                    } catch (Exception ex) {
                        // 忽略关闭错误
                    }
                }
                
                // 创建并初始化新的数据库服务
                DatabaseService newDbService = new DatabaseService();
                newDbService.init();
                
                // 更新主数据库服务引用
                dbService = newDbService;
                
                // 更新其他面板的数据库服务
                ocrTestPanel.setDatabaseService(dbService);
                classifyTestPanel.setDatabaseService(dbService);
                
                log("数据库服务重新初始化完成");
                return dbService;
            } catch (Exception e) {
                log("数据库重连失败: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
        
        // 创建隐藏的标签页容器（用于菜单跳转）
        tabbedPane.addTab("MQTT设置", mqttSettingsPanel);
        tabbedPane.addTab("路径设置", pathSettingsPanel);
        tabbedPane.addTab("软件设置", softwareSettingsPanel);
        tabbedPane.addTab("文本定位脚本", textLocationScriptPanel);
        tabbedPane.addTab("字符分类脚本", charClassifyScriptPanel);
        tabbedPane.addTab("OCR后处理脚本", ocrPostProcessPanel);
        tabbedPane.addTab("编程", programmingPanel);
        tabbedPane.addTab("关于", aboutPanel);
        tabbedPane.addTab("字符串预设", stringPresetPanel);
        
        // 监听标签页切换事件，更新编辑菜单状态
        tabbedPane.addChangeListener(e -> updateEditMenuState());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 状态栏
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
        LogUtil.log("[windowClosing] 开始执行");
                closeApplication();
            }
        });
    }
    
    private void loadIcon() {
        LogUtil.log("[loadIcon] 开始执行");
        try {
            // 尝试加载图标
            BufferedImage icon = null;
            
            // 从资源加载
            try {
                icon = ImageIO.read(getClass().getResourceAsStream("/icon.png"));
            } catch (Exception e) {
                // 忽略
            }
            
            // 从文件加载
            if (icon == null) {
        LogUtil.log("[if] 开始执行");
                File iconFile = new File("elephant.png");
                if (iconFile.exists()) {
                    icon = ImageIO.read(iconFile);
                }
            }
            
            if (icon != null) {
                setIconImage(icon);
                
                // 设置Dock图标(macOS)
                try {
                    Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                    Object taskbar = taskbarClass.getMethod("getTaskbar").invoke(null);
                    taskbarClass.getMethod("setIconImage", Image.class).invoke(taskbar, icon);
                } catch (Exception ex) {
                    // 忽略（Java 8不支持Taskbar API）
                }
            }
        } catch (Exception e) {
            System.err.println("加载图标失败: " + e.getMessage());
        }
    }
    
    private JPanel createToolBar() {
        LogUtil.log("[createToolBar] 开始执行");
        JPanel toolbar = new JPanel(new BorderLayout(5, 5));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        toolbar.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 左侧标题
        JLabel titleLabel = new JLabel("识字涂色 (ParoleDipinte)");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        titleLabel.setForeground(DarkBlueTheme.getTextColor());
        toolbar.add(titleLabel, BorderLayout.WEST);
        
        // 右侧控制按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        startButton = new JButton("启动服务");
        startButton.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        startButton.setForeground(DarkBlueTheme.getTextColor());
        startButton.addActionListener(e -> startService());
        buttonPanel.add(startButton);
        
        stopButton = new JButton("停止服务");
        stopButton.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        stopButton.setForeground(DarkBlueTheme.getTextColor());
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopService());
        buttonPanel.add(stopButton);
        
        toolbar.add(buttonPanel, BorderLayout.EAST);
        
        return toolbar;
    }
    
    /**
     * 创建菜单栏
     */
    private JMenuBar createMenuBar() {
        LogUtil.log("[createMenuBar] 开始执行");
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(DarkBlueTheme.PRIMARY_DARK);
        menuBar.setForeground(DarkBlueTheme.getTextColor());
        
        // 设置菜单
        JMenu settingsMenu = createMenu("设置");
        settingsMenu.add(createMenuItem("MQTT设置", e -> switchToTab("MQTT设置")));
        settingsMenu.add(createMenuItem("路径设置", e -> switchToTab("路径设置")));
        settingsMenu.add(createMenuItem("软件设置", e -> switchToTab("软件设置")));
        menuBar.add(settingsMenu);
        
        // 参数菜单
        JMenu paramsMenu = createMenu("参数");
        paramsMenu.add(createMenuItem("OCR参数", e -> switchToTab("OCR参数")));
        paramsMenu.add(createMenuItem("字符串预设", e -> switchToTab("字符串预设")));
        paramsMenu.addSeparator();
        restoreParamsMenuItem = createMenuItem("还原参数", e -> restoreParameters());
        restoreParamsMenuItem.setEnabled(false);
        paramsMenu.add(restoreParamsMenuItem);
        menuBar.add(paramsMenu);
        
        // 编辑菜单
        JMenu editMenu = createMenu("编辑");
        showBaselineMenuItem = createMenuItem("显示/隐藏行基线", e -> toggleBaseline());
        showBaselineMenuItem.setEnabled(true);
        editMenu.add(showBaselineMenuItem);
        
        showGridMenuItem = createMenuItem("显示/隐藏单元格", e -> toggleGrid());
        showGridMenuItem.setEnabled(false);
        editMenu.add(showGridMenuItem);
        
        // 显示/隐藏标签
        JMenuItem showLabelsMenuItem = createMenuItem("显示/隐藏标签", e -> toggleLabels());
        showLabelsMenuItem.setEnabled(true);
        editMenu.add(showLabelsMenuItem);
        
        // 显示/隐藏得分
        JMenuItem showScoresMenuItem = createMenuItem("显示/隐藏得分", e -> toggleScores());
        showScoresMenuItem.setEnabled(true);
        editMenu.add(showScoresMenuItem);
        
        editMenu.addSeparator();
        
        splitRegionMenuItem = createMenuItem("拆分区域", e -> toggleSplitRegion());
        splitRegionMenuItem.setEnabled(true);
        editMenu.add(splitRegionMenuItem);
        
        mergeRegionMenuItem = createMenuItem("合并区域", e -> toggleMergeRegion());
        mergeRegionMenuItem.setEnabled(true);
        editMenu.add(mergeRegionMenuItem);
        
        deleteRegionMenuItem = createMenuItem("删除区域", e -> toggleDeleteRegion());
        deleteRegionMenuItem.setEnabled(true);
        editMenu.add(deleteRegionMenuItem);
        
        editMenu.addSeparator();
        
        updateLabelsMenuItem = createMenuItem("更新标签", e -> updateLabels());
        updateLabelsMenuItem.setEnabled(true);
        editMenu.add(updateLabelsMenuItem);
        menuBar.add(editMenu);
        
        // 脚本菜单
        JMenu scriptMenu = createMenu("脚本");
        scriptMenu.add(createMenuItem("文本定位脚本", e -> switchToTab("文本定位脚本")));
        scriptMenu.add(createMenuItem("字符分类脚本", e -> switchToTab("字符分类脚本")));
        scriptMenu.add(createMenuItem("OCR后处理脚本", e -> switchToTab("OCR后处理脚本")));
        menuBar.add(scriptMenu);
        
        // 更多菜单
        JMenu moreMenu = createMenu("更多");
        moreMenu.add(createMenuItem("编程", e -> switchToTab("编程")));
        moreMenu.add(createMenuItem("关于", e -> switchToTab("关于")));
        menuBar.add(moreMenu);
        
        return menuBar;
    }
    
    /**
     * 创建菜单
     */
    private JMenu createMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setForeground(DarkBlueTheme.getTextColor());
        menu.setBackground(DarkBlueTheme.PRIMARY_DARK);
        return menu;
    }
    
    /**
     * 创建菜单项
     */
    private JMenuItem createMenuItem(String text, java.awt.event.ActionListener action) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setForeground(DarkBlueTheme.getTextColor());
        menuItem.setBackground(DarkBlueTheme.PRIMARY_DARK);
        menuItem.addActionListener(action);
        return menuItem;
    }
    
    /**
     * 切换到指定标签页
     */
    private void switchToTab(String tabName) {
        int count = tabbedPane.getTabCount();
        for (int i = 0; i < count; i++) {
            if (tabbedPane.getTitleAt(i).equals(tabName)) {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }
    }
    
    /**
     * 备份当前OCR参数（在使用预设前调用）
     */
    public void backupParametersBeforePreset() {
        backupPresetParams = new StringPreset();
        backupPresetParams.setGrayscale(ConfigManager.getGrayscale());
        backupPresetParams.setGamma(ConfigManager.getGamma());
        backupPresetParams.setMinArea(ConfigManager.getMinArea());
        backupPresetParams.setMaxArea(ConfigManager.getMaxArea());
        backupPresetParams.setMinWidth(ConfigManager.getMinWidth());
        backupPresetParams.setMaxWidth(ConfigManager.getMaxWidth());
        backupPresetParams.setMinHeight(ConfigManager.getMinHeight());
        backupPresetParams.setMaxHeight(ConfigManager.getMaxHeight());
        backupPresetParams.setCharRelativePositionEnabled(ConfigManager.isCharRelativePositionEnabled());
        backupPresetParams.setCharRelativePositionsFromJson(ConfigManager.getCharRelativePositions());
        
        if (restoreParamsMenuItem != null) {
            restoreParamsMenuItem.setEnabled(true);
        }
        log("已备份当前OCR参数");
    }
    
    /**
     * 还原到使用预设前的参数
     */
    private void restoreParameters() {
        if (backupPresetParams == null) {
            JOptionPane.showMessageDialog(this, 
                "没有可还原的参数备份", 
                "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要还原到使用预设前的参数吗？", 
            "确认还原", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            ConfigManager.setGrayscale(backupPresetParams.getGrayscale());
            ConfigManager.setGamma(backupPresetParams.getGamma());
            ConfigManager.setMinArea(backupPresetParams.getMinArea());
            ConfigManager.setMaxArea(backupPresetParams.getMaxArea());
            ConfigManager.setMinWidth(backupPresetParams.getMinWidth());
            ConfigManager.setMaxWidth(backupPresetParams.getMaxWidth());
            ConfigManager.setMinHeight(backupPresetParams.getMinHeight());
            ConfigManager.setMaxHeight(backupPresetParams.getMaxHeight());
            ConfigManager.setCharRelativePositionEnabled(backupPresetParams.isCharRelativePositionEnabled());
            ConfigManager.setCharRelativePositions(backupPresetParams.getCharRelativePositionsJson());
            ConfigManager.saveConfig();
            
            // 刷新OCR参数界面
            if (ocrSettingsPanel != null) {
                ocrSettingsPanel.loadSettings();
            }
            
            // 清除当前预设
            if (ocrTestPanel != null) {
                ocrTestPanel.setCurrentPreset(null);
            }
            
            log("已还原到使用预设前的OCR参数");
            JOptionPane.showMessageDialog(this, 
                "参数已还原", 
                "还原成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * 更新编辑菜单状态
     * 仅在OCR识别测试界面时启用编辑菜单项
     */
    private void updateEditMenuState() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        String tabTitle = tabbedPane.getTitleAt(selectedIndex);
        boolean isOcrTestTab = "OCR识别测试".equals(tabTitle);
        
        if (showBaselineMenuItem != null) {
            showBaselineMenuItem.setEnabled(isOcrTestTab);
        }
        if (showGridMenuItem != null) {
            showGridMenuItem.setEnabled(isOcrTestTab);
        }
        if (splitRegionMenuItem != null) {
            splitRegionMenuItem.setEnabled(isOcrTestTab);
        }
        if (mergeRegionMenuItem != null) {
            mergeRegionMenuItem.setEnabled(isOcrTestTab);
        }
        if (deleteRegionMenuItem != null) {
            deleteRegionMenuItem.setEnabled(isOcrTestTab);
        }
        if (updateLabelsMenuItem != null) {
            updateLabelsMenuItem.setEnabled(isOcrTestTab);
        }
    }
    
    /**
     * 切换行基线显示
     */
    private void toggleBaseline() {
        if (ocrTestPanel != null) {
            ocrTestPanel.toggleBaseline();
            log("已切换行基线显示");
        }
    }
    
    /**
     * 切换单元格显示（占位功能）
     */
    private void toggleGrid() {
        log("显示/隐藏单元格功能（待实现）");
    }
    
    /**
     * 切换标签显示
     */
    private void toggleLabels() {
        if (ocrTestPanel != null) {
            ocrTestPanel.toggleLabels();
            log("已切换标签显示");
        }
    }
    
    /**
     * 切换得分显示
     */
    private void toggleScores() {
        if (ocrTestPanel != null) {
            ocrTestPanel.toggleScores();
            log("已切换得分显示");
        }
    }
    
    /**
     * 切换拆分区域模式
     */
    private void toggleSplitRegion() {
        if (ocrTestPanel != null) {
            ocrTestPanel.toggleSplitRegion();
            log("已切换拆分区域模式");
        }
    }
    
    /**
     * 切换合并区域模式
     */
    private void toggleMergeRegion() {
        if (ocrTestPanel != null) {
            ocrTestPanel.toggleMergeRegion();
            log("已切换合并区域模式");
        }
    }
    
    /**
     * 切换删除区域模式
     */
    private void toggleDeleteRegion() {
        if (ocrTestPanel != null) {
            ocrTestPanel.toggleDeleteRegion();
            log("已切换删除区域模式");
        }
    }
    
    /**
     * 更新标签
     * 根据正确字符串更新所有区域的标签
     */
    private void updateLabels() {
        if (ocrTestPanel != null) {
            ocrTestPanel.updateLabels();
        }
    }
    
    private JPanel createStatusBar() {
        LogUtil.log("[createStatusBar] 开始执行");
        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        statusBar.setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        statusLabel = new JLabel("就绪");
        statusLabel.setForeground(DarkBlueTheme.getSecondaryTextColor());
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        JLabel versionLabel = new JLabel("v1.0.59", JLabel.RIGHT);
        versionLabel.setForeground(DarkBlueTheme.TEXT_MUTED);
        statusBar.add(versionLabel, BorderLayout.EAST);
        
        return statusBar;
    }
    
    /**
     * 创建编程手册面板
     */
    private JPanel createProgrammingPanel() {
        LogUtil.log("[createProgrammingPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 使用RSyntaxTextArea显示Markdown
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        
        // 应用深蓝色主题到RSyntaxTextArea
        applyDarkBlueThemeToSyntax(textArea);
        
        // 加载PROGRAMMING.md内容
        String content = loadProgrammingContent();
        textArea.setText(content);
        textArea.setCaretPosition(0);
        
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.getGutter().setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建关于面板
     */
    private JPanel createAboutPanel() {
        LogUtil.log("[createAboutPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 检查README.md是否存在
        File readmeFile = new File("README.md");
        if (readmeFile.exists()) {
            // 使用RSyntaxTextArea显示Markdown
            RSyntaxTextArea textArea = new RSyntaxTextArea();
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
            textArea.setCodeFoldingEnabled(true);
            textArea.setAntiAliasingEnabled(true);
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            
            // 应用深蓝色主题到RSyntaxTextArea
            applyDarkBlueThemeToSyntax(textArea);
            
            // 加载README.md内容
            String content = loadReadmeContent();
            textArea.setText(content);
            textArea.setCaretPosition(0);
            
            RTextScrollPane scrollPane = new RTextScrollPane(textArea);
            scrollPane.setLineNumbersEnabled(true);
            scrollPane.setFoldIndicatorEnabled(true);
            scrollPane.getGutter().setBackground(DarkBlueTheme.PRIMARY_DARK);
            
            panel.add(scrollPane, BorderLayout.CENTER);
        } else {
            // 显示默认信息
            JTextArea aboutArea = new JTextArea();
            aboutArea.setEditable(false);
            aboutArea.setBackground(DarkBlueTheme.getBackgroundColor());
            aboutArea.setForeground(DarkBlueTheme.getTextColor());
            aboutArea.setFont(aboutArea.getFont().deriveFont(Font.PLAIN, 14));
            aboutArea.setText(
                "识字涂色 (ParoleDipinte) - OCR服务\n\n" +
                "版本: 1.0.0\n" +
                "构建日期: 2026-04-06\n\n" +
                "功能特点:\n" +
                "- 经典OCR识别\n" +
                "- 字符分类与相似度判断\n" +
                "- 动态阈值二值化\n" +
                "- MQTT通信支持\n" +
                "- Lua脚本扩展\n\n" +
                "技术栈:\n" +
                "- Java 8 + Swing\n" +
                "- FlatLaf 主题\n" +
                "- H2 Database\n" +
                "- JavaCV / OpenCV\n" +
                "- Paho MQTT\n" +
                "- LuaJ\n\n" +
                "© 2026 Honda"
            );
            
            panel.add(aboutArea, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    /**
     * 应用深蓝色主题到语法高亮编辑器
     */
    private void applyDarkBlueThemeToSyntax(RSyntaxTextArea textArea) {
        LogUtil.log("[applyDarkBlueThemeToSyntax] 开始执行");
        try {
            // 设置背景色
            textArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
            textArea.setForeground(DarkBlueTheme.getTextColor());
            textArea.setCaretColor(DarkBlueTheme.getTextColor());
            textArea.setSelectionColor(DarkBlueTheme.ACCENT);
            textArea.setCurrentLineHighlightColor(DarkBlueTheme.PRIMARY_LIGHT);
            
            // 设置语法高亮颜色
            textArea.setSyntaxScheme(createDarkBlueSyntaxScheme());
            
        } catch (Exception e) {
            // 如果自定义主题失败，回退到默认暗色主题
            try {
                Theme theme = Theme.load(getClass().getClassLoader().getResourceAsStream(
                        "org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
                theme.apply(textArea);
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 创建深蓝色主题的语法高亮方案
     */
    private org.fife.ui.rsyntaxtextarea.SyntaxScheme createDarkBlueSyntaxScheme() {
        org.fife.ui.rsyntaxtextarea.SyntaxScheme scheme = new org.fife.ui.rsyntaxtextarea.SyntaxScheme(
            new Font(Font.MONOSPACED, Font.PLAIN, 13)
        );
        
        // 设置各种token类型的样式
        // 注释
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.COMMENT_EOL, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_COMMENT, DarkBlueTheme.PRIMARY_DARK));
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.COMMENT_MULTILINE, 
            new org.fife.ui.rsyntaxtextarea.Style(DarkBlueTheme.SYNTAX_COMMENT, DarkBlueTheme.PRIMARY_DARK));
        scheme.setStyle(org.fife.ui.rsyntaxtextarea.Token.COMMENT_DOCUMENTATION, 
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
    
    /**
     * 加载PROGRAMMING.md内容
     */
    private String loadProgrammingContent() {
        LogUtil.log("[loadProgrammingContent] 开始执行");
        // 首先尝试从文件系统读取
        File docFile = new File("PROGRAMMING.md");
        if (docFile.exists()) {
            try (FileInputStream fis = new FileInputStream(docFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException e) {
                System.err.println("读取PROGRAMMING.md失败: " + e.getMessage());
            }
        }
        
        // 如果文件不存在，返回默认内容
        return "# 编程手册\n\nPROGRAMMING.md 文件未找到。\n\n请确保文件存在于应用程序目录中。";
    }
    
    /**
     * 加载README.md内容
     */
    private String loadReadmeContent() {
        LogUtil.log("[loadReadmeContent] 开始执行");
        // 首先尝试从文件系统读取
        File readmeFile = new File("README.md");
        if (readmeFile.exists()) {
            try (FileInputStream fis = new FileInputStream(readmeFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException e) {
                System.err.println("读取README.md失败: " + e.getMessage());
            }
        }
        
        return "README.md 文件未找到";
    }
    
    /**
     * 检查并启动MQTT Broker
     */
    private void checkAndStartMqttBroker() {
        LogUtil.log("[checkAndStartMqttBroker] 开始执行");
        
        // 检查是否设置了自动启动MQTT服务
        if (!ConfigManager.isAutoStartMqtt()) {
            log("软件设置中禁用了自动启动MQTT服务，跳过启动");
            return;
        }
        
        new Thread(() -> {
            try {
                // 检查8907端口是否已被占用
                if (isPortAvailable(8907)) {
                    log("8907端口可用，正在启动内置MQTT Broker...");
                    
                    // 尝试启动mosquitto
                    if (startMosquittoBroker()) {
                        log("MQTT Broker (mosquitto) 已启动在8907端口");
                    } else {
                        // 尝试使用Python的paho-mqtt
                        if (startPythonMqttBroker()) {
                            log("MQTT Broker (Python) 已启动在8907端口");
                        } else {
                            log("警告: 无法启动内置MQTT Broker，请手动启动");
                        }
                    }
                } else {
                    log("8907端口已被占用，假设MQTT Broker已运行");
                }
            } catch (Exception e) {
                log("启动MQTT Broker失败: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 检查端口是否可用
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 启动mosquitto broker
     */
    private boolean startMosquittoBroker() {
        LogUtil.log("[startMosquittoBroker] 开始执行");
        try {
            // 检查mosquitto是否安装
            Process checkProcess = Runtime.getRuntime().exec("which mosquitto");
            int exitCode = checkProcess.waitFor();
            
            if (exitCode != 0) {
                return false;
            }
            
            // 启动mosquitto
            mqttBrokerProcess = Runtime.getRuntime().exec(
                "mosquitto -p 8907 -d"
            );
            
            // 等待启动
            Thread.sleep(1000);
            
            return mqttBrokerProcess.isAlive();
        } catch (Exception e) {
            System.err.println("启动mosquitto失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 启动Python MQTT Broker
     */
    private boolean startPythonMqttBroker() {
        LogUtil.log("[startPythonMqttBroker] 开始执行");
        try {
            // 创建临时Python脚本
            File tempScript = File.createTempFile("mqtt_broker", ".py");
            tempScript.deleteOnExit();
            
            try (PrintWriter writer = new PrintWriter(tempScript)) {
                writer.println("#!/usr/bin/env python3");
                writer.println("import paho.mqtt.broker as broker");
                writer.println("import sys");
                writer.println("try:");
                writer.println("    broker.run(port=8907)");
                writer.println("except Exception as e:");
                writer.println("    print(f'Broker error: {e}')");
                writer.println("    sys.exit(1)");
            }
            
            // 启动Python broker
            mqttBrokerProcess = Runtime.getRuntime().exec(
                "python3 " + tempScript.getAbsolutePath()
            );
            
            // 等待启动
            Thread.sleep(2000);
            
            return mqttBrokerProcess.isAlive();
        } catch (Exception e) {
            System.err.println("启动Python MQTT Broker失败: " + e.getMessage());
            return false;
        }
    }
    
    private void initServices() {
        LogUtil.log("[initServices] 开始执行");
        new Thread(() -> {
            try {
                log("正在初始化服务...");
                
                // 初始化数据库
                dbService = new DatabaseService();
                dbService.init();
                log("数据库初始化完成");
                
                // 检查是否需要生成字符图片
                if (dbService.isEmpty()) {
                    log("数据库为空，开始生成5000个常用字符图片...");
                    generateCharacterImages();
                    log("字符图片生成完成");
                } else {
                    int count = dbService.getCharacterCount();
                    log("数据库中已有 " + count + " 个字符图片");
                }
                
                // 将数据库服务存储到全局数据交换
                GlobalKvCore.setValue("dbService", dbService);
                log("数据库服务已存储到全局数据交换");
                
                // 设置字符串预设面板的数据库服务
                SwingUtilities.invokeLater(() -> {
                    if (instance != null && instance.stringPresetPanel != null) {
                        // 从全局数据交换获取数据库服务
                        DatabaseService globalDbService = GlobalKvCore.get("dbService");
                        if (globalDbService != null) {
                            instance.stringPresetPanel.setDatabaseService(globalDbService);
                            log("字符串预设面板数据库服务已注入");
                        } else {
                            log("警告: 全局数据交换中数据库服务为null");
                        }
                    } else {
                        log("警告: 字符串预设面板未初始化，将在稍后重试");
                        // 延迟重试
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                SwingUtilities.invokeLater(() -> {
                                    if (instance != null && instance.stringPresetPanel != null) {
                                        DatabaseService globalDbService = GlobalKvCore.get("dbService");
                                        if (globalDbService != null) {
                                            instance.stringPresetPanel.setDatabaseService(globalDbService);
                                            log("字符串预设面板数据库服务已注入(延迟)");
                                        }
                                    }
                                });
                            } catch (InterruptedException ignored) {}
                        }).start();
                    }
                });
                
                // 初始化MQTT服务
                mqttService = new MqttService(dbService, this::log);
                
                // 设置Lua脚本引擎（在startService之前设置）
                if (textLocationScriptPanel != null) {
                    mqttService.setTextLocationScriptEngine(textLocationScriptPanel.getScriptEngine());
                }
                if (charClassifyScriptPanel != null) {
                    mqttService.setCharClassifyScriptEngine(charClassifyScriptPanel.getScriptEngine());
                }
                if (ocrPostProcessPanel != null) {
                    mqttService.setOcrPostProcessScriptEngine(ocrPostProcessPanel.getScriptEngine());
                }
                
                // 如果设置了自动启动，则启动服务
                if (ConfigManager.isAutoStart()) {
                    SwingUtilities.invokeLater(() -> startService());
                }
                
                log("服务初始化完成");
                
            } catch (Exception e) {
                log("服务初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private void generateCharacterImages() throws Exception {
        CharacterGenerator generator = new CharacterGenerator();
        List<String> chars = CharacterGenerator.getCommonCharacters();
        
        int count = 0;
        for (String ch : chars) {
            BufferedImage image = generator.generateCharacterImage(ch);
            dbService.saveCharacterImage(ch, image);
            count++;
            if (count % 500 == 0) {
                log("已生成 " + count + " / " + chars.size() + " 个字符图片");
            }
        }
        log("共生成 " + count + " 个字符图片");
    }
    
    private void startService() {
        LogUtil.log("[startService] 开始执行");
        if (mqttService != null) {
            new Thread(() -> {
                mqttService.start();
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    statusLabel.setText("服务运行中 - " + mqttService.getBroker());
                    statusLabel.setForeground(DarkBlueTheme.SUCCESS);
                });
            }).start();
        }
    }
    
    private void stopService() {
        LogUtil.log("[stopService] 开始执行");
        if (mqttService != null) {
            mqttService.stop();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("服务已停止");
            statusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        }
    }
    
    private void closeApplication() {
        LogUtil.log("[closeApplication] 开始执行");
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要退出程序吗？", 
            "确认退出", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            log("正在关闭应用程序...");
            
            // 停止MQTT服务
            if (mqttService != null) {
        LogUtil.log("[if] 开始执行");
                mqttService.stop();
            }
            
            // 停止MQTT Broker
            if (mqttBrokerProcess != null && mqttBrokerProcess.isAlive()) {
                mqttBrokerProcess.destroy();
            }
            
            // 关闭数据库
            if (dbService != null) {
        LogUtil.log("[if] 开始执行");
                try {
                    dbService.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // 关闭日志缓冲区
            if (logPanel != null) {
                logPanel.shutdown();
            }
            
            dispose();
            System.exit(0);
        }
    }
    
    private void log(String message) {
        LogUtil.log("[log] 开始执行");
        if (logPanel != null) {
            // 确保在UI线程中更新日志
            if (SwingUtilities.isEventDispatchThread()) {
                logPanel.appendLog(message);
            } else {
                SwingUtilities.invokeLater(() -> logPanel.appendLog(message));
            }
        }
        System.out.println(message);
    }
}
