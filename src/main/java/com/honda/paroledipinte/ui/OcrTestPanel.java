package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.db.DatabaseService;
import com.honda.paroledipinte.db.XmlPresetStorage;
import com.honda.paroledipinte.image.*;
import com.honda.paroledipinte.lua.LuaScriptEngine;
import com.honda.paroledipinte.model.CharRelativePosition;
import com.honda.paroledipinte.model.Shape;
import com.honda.paroledipinte.model.StringPreset;
import com.honda.paroledipinte.model.XAnyLabelingJson;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import com.honda.paroledipinte.util.GlobalKvCore;
import com.honda.paroledipinte.util.ModelResourceLoader;
import com.honda.paroledipinte.util.XAnyLabelingJsonLoader;

import java.util.stream.Collectors;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.Base64;

/**
 * OCR识别测试面板
 * 用户可以打开或拖入图片进行OCR测试，快速调节参数
 * 支持打开/拖入XAnyLabeling JSON文件并显示
 */
public class OcrTestPanel extends JPanel {
    
    private OcrResultPanel resultPanel;
    private JTextArea correctStringField;  // 改为JTextArea支持多行
    private JLabel statusLabel;
    private JLabel presetNameLabel;  // 当前预设名称显示
    

    
    private DatabaseService dbService;
    private LuaScriptEngine luaScriptEngine;
    private BufferedImage currentImage;
    private List<Shape> currentShapes;  // 保存当前识别的字符区域
    
    // 当前使用的字符串预设
    private StringPreset currentPreset;
    
    // 日志记录回调
    private Consumer<String> logCallback;
    
    public OcrTestPanel() {
        LogUtil.log("[OcrTestPanel] 开始执行");
        initComponents();
        setupDragAndDrop();
    }
    
    /**
     * 设置数据库服务
     */
    public void setDatabaseService(DatabaseService dbService) {
        this.dbService = dbService;
    }
    
    /**
     * 获取数据库服务（优先从全局数据交换获取）
     * 解决通过构造函数注入时dbService可能为null的问题
     */
    private DatabaseService getDbService() {
        // 首先尝试从全局数据交换获取
        DatabaseService globalDbService = GlobalKvCore.get("dbService");
        if (globalDbService != null) {
            return globalDbService;
        }
        // 回退到本地引用
        return dbService;
    }
    
    /**
     * 设置Lua脚本引擎
     */
    public void setLuaScriptEngine(LuaScriptEngine engine) {
        this.luaScriptEngine = engine;
    }
    
    /**
     * 设置正确字符串（用于从预设填充）
     * @param text 正确字符串
     */
    public void setCorrectString(String text) {
        if (correctStringField != null) {
            correctStringField.setText(text);
        }
    }
    
    /**
     * 设置当前使用的字符串预设
     * @param preset 字符串预设
     */
    public void setCurrentPreset(StringPreset preset) {
        this.currentPreset = preset;
        if (preset != null) {
            log("已设置当前预设: " + preset.getPresetName());
            // 同时设置正确字符串
            if (preset.getCorrectString() != null && !preset.getCorrectString().isEmpty()) {
                setCorrectString(preset.getCorrectString());
            }
            if (presetNameLabel != null) {
                presetNameLabel.setText("当前预设: " + preset.getPresetName());
                presetNameLabel.setForeground(DarkBlueTheme.SUCCESS);
            }
        } else {
            if (presetNameLabel != null) {
                presetNameLabel.setText("当前预设: 无");
                presetNameLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
            }
        }
    }
    
    /**
     * 获取当前使用的字符串预设
     * @return 当前预设，未设置返回null
     */
    public StringPreset getCurrentPreset() {
        return currentPreset;
    }
    
    /**
     * 获取正确字符串输入框
     */
    public JTextArea getCorrectStringField() {
        return correctStringField;
    }
    
    /**
     * 设置日志回调
     */
    public void setLogCallback(Consumer<String> logCallback) {
        this.logCallback = logCallback;
    }
    
    /**
     * 记录日志
     */
    private void log(String message) {
        LogUtil.log("[log] 开始执行");
        if (logCallback != null) {
            logCallback.accept("[OCR识别测试] " + message);
        }
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.35);
        splitPane.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 左侧控制面板
        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);
        
        // 右侧结果展示面板
        resultPanel = new OcrResultPanel();
        splitPane.setRightComponent(resultPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    private JPanel createLeftPanel() {
        LogUtil.log("[createLeftPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(350, 0));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 顶部：拖放区域和打开按钮
        JPanel topPanel = createDropPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        
        // 底部：正确字符串和运行按钮
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDropPanel() {
        LogUtil.log("[createDropPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 拖放区域
        JPanel dropArea = new JPanel();
        dropArea.setLayout(new BorderLayout());
        dropArea.setBorder(BorderFactory.createDashedBorder(DarkBlueTheme.BORDER_LIGHT, 2, 5));
        dropArea.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        dropArea.setPreferredSize(new Dimension(0, 150));
        
        JLabel dropLabel = new JLabel("<html><center>[拖放图片或JSON到此处]<br>或点击选择文件</center></html>", JLabel.CENTER);
        dropLabel.setForeground(DarkBlueTheme.getTextColor());
        dropLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        dropArea.add(dropLabel, BorderLayout.CENTER);
        
        // 点击打开文件
        dropArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
        LogUtil.log("[mouseClicked] 开始执行");
                openFile();
            }
        });
        
        panel.add(dropArea, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton openImageBtn = createStyledButton("[打开图片]");
        openImageBtn.addActionListener(e -> openImageFile());
        buttonPanel.add(openImageBtn);
        
        JButton openJsonBtn = createStyledButton("[打开JSON]");
        openJsonBtn.addActionListener(e -> openJsonFile());
        buttonPanel.add(openJsonBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    

    private JPanel createBottomPanel() {
        LogUtil.log("[createBottomPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 正确字符串输入（5行文本区域）
        JPanel stringPanel = new JPanel(new BorderLayout(5, 5));
        stringPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        JLabel correctStringLabel = new JLabel("正确字符串:");
        correctStringLabel.setForeground(DarkBlueTheme.getTextColor());
        stringPanel.add(correctStringLabel, BorderLayout.NORTH);
        
        correctStringField = new JTextArea(5, 20);
        correctStringField.setBackground(DarkBlueTheme.PRIMARY_DARK);
        correctStringField.setForeground(DarkBlueTheme.getTextColor());
        correctStringField.setCaretColor(DarkBlueTheme.getTextColor());
        correctStringField.setToolTipText("输入图片中预期的字符序列，用于对比识别结果");
        correctStringField.setLineWrap(true);
        correctStringField.setWrapStyleWord(true);
        correctStringField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(correctStringField);
        scrollPane.setBackground(DarkBlueTheme.getBackgroundColor());
        scrollPane.setBorder(BorderFactory.createLineBorder(DarkBlueTheme.BORDER));
        stringPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(stringPanel, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton runBtn = createStyledButton("[经典OCR识别]");
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD, 14));
        runBtn.setBackground(DarkBlueTheme.SUCCESS);
        runBtn.addActionListener(e -> runOcr());
        buttonPanel.add(runBtn);
        
        JButton paddleOcrBtn = createStyledButton("[飞桨OCR识别]");
        paddleOcrBtn.setFont(paddleOcrBtn.getFont().deriveFont(Font.BOLD, 14));
        paddleOcrBtn.setBackground(new Color(0x00, 0x96, 0xC7)); // 飞桨蓝色
        paddleOcrBtn.addActionListener(e -> runPaddleOcr());
        buttonPanel.add(paddleOcrBtn);
        
        JButton toggleScoreBtn = createStyledButton("[显示/隐藏得分]");
        toggleScoreBtn.setFont(toggleScoreBtn.getFont().deriveFont(Font.BOLD, 12));
        toggleScoreBtn.setBackground(DarkBlueTheme.ACCENT);
        toggleScoreBtn.addActionListener(e -> resultPanel.toggleScores());
        // buttonPanel.add(toggleScoreBtn);

        // [添加到预设] 按钮
        JButton addToPresetBtn = createStyledButton("[添加到预设]");
        addToPresetBtn.setFont(addToPresetBtn.getFont().deriveFont(Font.BOLD, 12));
        addToPresetBtn.setBackground(new Color(0x2E, 0x8B, 0x57)); // 海绿色
        addToPresetBtn.addActionListener(e -> addToPreset());
        buttonPanel.add(addToPresetBtn);
        
        JButton screenshotBtn = createStyledButton("[截图保存字符]");
        screenshotBtn.setFont(screenshotBtn.getFont().deriveFont(Font.BOLD, 12));
        screenshotBtn.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        screenshotBtn.addActionListener(e -> saveCharacterScreenshots());
        buttonPanel.add(screenshotBtn);
        
        JButton exportJsonBtn = createStyledButton("[导出JSON]");
        exportJsonBtn.setFont(exportJsonBtn.getFont().deriveFont(Font.BOLD, 12));
        exportJsonBtn.setBackground(new Color(0x8B, 0x45, 0x13)); // 棕色
        exportJsonBtn.addActionListener(e -> exportToJson());
        buttonPanel.add(exportJsonBtn);
        
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        // 预设名称标签
        presetNameLabel = new JLabel("当前预设: 无");
        presetNameLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        presetNameLabel.setForeground(DarkBlueTheme.ACCENT);
        presetNameLabel.setFont(presetNameLabel.getFont().deriveFont(Font.BOLD));
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        statusLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        
        JPanel southPanel = new JPanel(new GridLayout(2, 1));
        southPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        southPanel.add(presetNameLabel);
        southPanel.add(statusLabel);
        panel.add(southPanel, BorderLayout.SOUTH);
        
        return panel;
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
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        return button;
    }
    
    private void setupDragAndDrop() {
        DropTarget dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {

            @Override
            public void drop(DropTargetDropEvent e) {
        LogUtil.log("[drop] 开始执行");
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = e.getTransferable();
                    
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            File file = files.get(0);
                            handleDroppedFile(file);
                        }
                    }
                } catch (Exception ex) {
                    showStatus("拖放失败: " + ex.getMessage(), true);
                    log("拖放文件失败: " + ex.getMessage());
                }
            }
        }, true);
        setDropTarget(dropTarget);
    }
    
    /**
     * 处理拖放的文件
     */
    private void handleDroppedFile(File file) {
        LogUtil.log("[handleDroppedFile] 开始执行");
        log("接收到拖放文件: " + file.getName());
        
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".json")) {
            loadJsonFile(file);
        } else if (isImageFile(fileName)) {
            loadImageFile(file);
        } else {
            showStatus("不支持的文件类型", true);
            log("不支持的文件类型: " + fileName);
        }
    }
    
    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || fileName.endsWith(".bmp") || 
               fileName.endsWith(".gif");
    }
    
    /**
     * 打开文件（通用）
     */
    private void openFile() {
        LogUtil.log("[openFile] 开始执行");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择文件");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "支持的文件 (*.jpg, *.jpeg, *.png, *.bmp, *.gif, *.json)", 
            "jpg", "jpeg", "png", "bmp", "gif", "json"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            handleDroppedFile(file);
        }
    }
    
    private void openImageFile() {
        LogUtil.log("[openImageFile] 开始执行");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择图片文件");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "图片文件 (*.jpg, *.jpeg, *.png, *.bmp, *.gif)", 
            "jpg", "jpeg", "png", "bmp", "gif"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadImageFile(chooser.getSelectedFile());
        }
    }
    
    private void openJsonFile() {
        LogUtil.log("[openJsonFile] 开始执行");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择XAnyLabeling JSON文件");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "JSON文件 (*.json)", "json"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadJsonFile(chooser.getSelectedFile());
        }
    }
    
    /**
     * 加载JSON文件
     */
    private void loadJsonFile(File file) {
        LogUtil.log("[loadJsonFile] 开始执行");
        log("开始加载JSON文件: " + file.getAbsolutePath());
        showStatus("正在加载JSON文件...", false);
        
        SwingWorker<XAnyLabelingJsonLoader.LoadResult, Void> worker = new SwingWorker<XAnyLabelingJsonLoader.LoadResult, Void>() {
            @Override
            protected XAnyLabelingJsonLoader.LoadResult doInBackground() {
                return XAnyLabelingJsonLoader.loadFromFile(file);
            }
            
            @Override
            protected void done() {
        LogUtil.log("[done] 开始执行");
                try {
                    XAnyLabelingJsonLoader.LoadResult result = get();
                    if (result.isSuccess()) {
                        currentImage = result.getImage();
                        currentShapes = result.getShapes();  // 保存JSON中的字符区域
                        resultPanel.setResult(result.getImage(), result.getShapes());
                        showStatus(result.getMessage(), false);
                        log("JSON加载成功 - " + result.getMessage());
                    } else {
                        showStatus(result.getMessage(), true);
                        log("JSON加载失败 - " + result.getMessage());
                    }
                } catch (Exception e) {
                    showStatus("加载JSON失败: " + e.getMessage(), true);
                    log("加载JSON异常: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void loadImageFile(File file) {
        LogUtil.log("[loadImageFile] 开始执行");
        log("开始加载图片文件: " + file.getAbsolutePath());
        try {
            showStatus("正在加载图片...", false);
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                showStatus("无法读取图片文件", true);
                log("无法读取图片文件: " + file.getName());
                return;
            }
            
            currentImage = image;
            resultPanel.setImage(image);
            String msg = "图片已加载: " + file.getName() + " (" + image.getWidth() + "x" + image.getHeight() + ")";
            showStatus(msg, false);
            log(msg);
            
        } catch (Exception e) {
            showStatus("加载图片失败: " + e.getMessage(), true);
            log("加载图片失败: " + e.getMessage());
        }
    }
    
    private void runOcr() {
        LogUtil.log("[runOcr] 开始执行");
        if (currentImage == null) {
            showStatus("请先加载图片", true);
            log("OCR识别失败: 未加载图片");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        log("开始OCR识别，图片尺寸: " + currentImage.getWidth() + "x" + currentImage.getHeight());
        
        // 检查是否有预设，如果有则使用预设参数
        boolean usingPreset = (currentPreset != null);
        if (usingPreset) {
            log("使用预进行OCR识别: " + currentPreset.getPresetName());
        }
        
        try {
            showStatus("正在运行OCR识别...", false);
            
            // 从预设或ConfigManager获取参数（优先使用预设参数）
            int ocrGray = usingPreset ? currentPreset.getGrayscale() : ConfigManager.getGrayscale();
            double gamma = usingPreset ? currentPreset.getGamma() : ConfigManager.getGamma();
            int minArea = usingPreset ? currentPreset.getMinArea() : ConfigManager.getMinArea();
            int maxArea = usingPreset ? currentPreset.getMaxArea() : ConfigManager.getMaxArea();
            int minWidth = usingPreset ? currentPreset.getMinWidth() : ConfigManager.getMinWidth();
            int minHeight = usingPreset ? currentPreset.getMinHeight() : ConfigManager.getMinHeight();
            int maxWidth = usingPreset ? currentPreset.getMaxWidth() : ConfigManager.getMaxWidth();
            int maxHeight = usingPreset ? currentPreset.getMaxHeight() : ConfigManager.getMaxHeight();
            
            // 获取新功能参数（从ConfigManager，预设中暂不包含这些参数）
            boolean forceWidthSplit = ConfigManager.isForceWidthSplitEnabled();
            int forceSplitWidth = ConfigManager.getForceSplitWidth();
            boolean multiLineSort = ConfigManager.isMultiLineSortEnabled();
            int rowGapThreshold = ConfigManager.getRowGapThreshold();
            
            // 获取行列间距参数
            int minRowGap = ConfigManager.getMinRowGap();
            int maxRowGap = ConfigManager.getMaxRowGap();
            int minColGap = ConfigManager.getMinColGap();
            int maxColGap = ConfigManager.getMaxColGap();
            
            // 获取字符内部空隙参数
            int maxInternalVerticalGap = ConfigManager.getMaxInternalVerticalGap();
            int maxInternalHorizontalGap = ConfigManager.getMaxInternalHorizontalGap();
            
            // 获取忽略边界区域参数
            int ignoreBorderWidth = ConfigManager.getIgnoreBorderWidth();
            
            log(String.format("OCR参数 - 二值化阈值:%d, Gamma:%.2f, 面积:%d-%d, 宽度:%d-%d, 高度:%d-%d",
                    ocrGray, gamma, minArea, maxArea, minWidth, maxWidth, minHeight, maxHeight));
            log(String.format("新增功能 - 强制宽度分割:%s(宽度:%d), 多行排序:%s(阈值:%d)",
                    forceWidthSplit, forceSplitWidth, multiLineSort, rowGapThreshold));
            log(String.format("行列间距 - 行间距:%d-%d, 列间距:%d-%d", minRowGap, maxRowGap, minColGap, maxColGap));
            log(String.format("字符内部空隙 - 垂直:%d, 水平:%d, 忽略边界:%d像素", 
                    maxInternalVerticalGap, maxInternalHorizontalGap, ignoreBorderWidth));
            
            // 设置图像处理参数
            ImageProcessor.setGammaValue(gamma);
            
            // 转换为Mat
            Mat inputMat = ImageProcessor.bufferedImageToMat(currentImage);
            
            // 获取correctString
            String correctString = correctStringField.getText().trim();
            
            // 文本区域检测（使用完整的参数）
            TextBlobDetector detector = new TextBlobDetector(
                ocrGray, minArea, minWidth, minHeight, maxWidth, maxHeight, false,
                forceWidthSplit, multiLineSort, forceSplitWidth, rowGapThreshold,
                maxInternalVerticalGap, maxInternalHorizontalGap, ignoreBorderWidth
            );
            
            // 设置Lua脚本引擎和correctString
            if (luaScriptEngine != null) {
                detector.setLuaScriptEngine(luaScriptEngine);
                detector.setCorrectString(correctString);
                log("已设置文本定位脚本引擎，correctString: " + (correctString.isEmpty() ? "(空)" : "'" + correctString + "'"));
            }
            
            // 设置预设参数到检测器（供Lua脚本访问）
            if (usingPreset && currentPreset != null) {
                java.util.Map<String, Object> presetParams = new java.util.HashMap<>();
                presetParams.put("presetName", currentPreset.getPresetName());
                presetParams.put("correctString", currentPreset.getCorrectString());
                presetParams.put("grayscale", currentPreset.getGrayscale());
                presetParams.put("gamma", currentPreset.getGamma());
                presetParams.put("minArea", currentPreset.getMinArea());
                presetParams.put("maxArea", currentPreset.getMaxArea());
                presetParams.put("minWidth", currentPreset.getMinWidth());
                presetParams.put("maxWidth", currentPreset.getMaxWidth());
                presetParams.put("minHeight", currentPreset.getMinHeight());
                presetParams.put("maxHeight", currentPreset.getMaxHeight());
                presetParams.put("charRelativePositionEnabled", currentPreset.isCharRelativePositionEnabled());
                
                // 字符相对位置信息转换为List<Map>
                if (currentPreset.hasCharRelativePositions()) {
                    java.util.List<java.util.Map<String, Object>> charPosList = new java.util.ArrayList<>();
                    for (CharRelativePosition pos : currentPreset.getCharRelativePositions()) {
                        java.util.Map<String, Object> posMap = new java.util.HashMap<>();
                        posMap.put("charIndex", pos.getCharIndex());
                        posMap.put("character", pos.getCharacter());
                        posMap.put("relativeX", pos.getRelativeX());
                        posMap.put("relativeY", pos.getRelativeY());
                        posMap.put("width", pos.getWidth());
                        posMap.put("height", pos.getHeight());
                        posMap.put("minWidth", pos.getMinWidth());
                        posMap.put("maxWidth", pos.getMaxWidth());
                        posMap.put("minHeight", pos.getMinHeight());
                        posMap.put("maxHeight", pos.getMaxHeight());
                        posMap.put("spacing", pos.getSpacing());
                        posMap.put("minSpacing", pos.getMinSpacing());
                        posMap.put("maxSpacing", pos.getMaxSpacing());
                        posMap.put("charType", pos.getCharTypeDisplayName());
                        charPosList.add(posMap);
                    }
                    presetParams.put("charRelativePositions", charPosList);
                }
                
                detector.setPresetParams(presetParams);
            }
            
            TextBlobDetector.DetectionResult detectionResult = detector.detect(inputMat);
            
            // 保存原始字符框（OCR参数分割出的结果）
            List<TextBlobDetector.Region> rawRegions = new java.util.ArrayList<>(detectionResult.textRegions);
            detector.setRawRegions(rawRegions);
            
            // 处理识别结果
            List<Shape> shapes = new ArrayList<>();
            String correctStringCleaned = correctString.replaceAll("\\s+", "");  // 去除所有空白字符
            char[] correctChars = correctStringCleaned.isEmpty() ? new char[0] : correctStringCleaned.toCharArray();
            
            log("检测到 " + detectionResult.textRegions.size() + " 个文本区域");
            
            // 如果有预设且启用了字符相对位置，使用字符相对位置修正区域
            List<TextBlobDetector.Region> finalRegions = detectionResult.textRegions;
            if (usingPreset && currentPreset.isCharRelativePositionEnabled() && 
                currentPreset.hasCharRelativePositions() && !detectionResult.textRegions.isEmpty()) {
                
                log("使用字符相对位置修正区域...");
                List<CharRelativePosition> charPositions = currentPreset.getCharRelativePositions();
                
                // 使用首个检测到的区域作为基准点
                TextBlobDetector.Region firstRegion = detectionResult.textRegions.get(0);
                finalRegions = detector.detectByRelativePosition(inputMat, charPositions, firstRegion);
                
                // 保存修正后字符框
                detector.setCorrectedRegions(new java.util.ArrayList<>(finalRegions));
                
                log("字符相对位置修正完成: " + finalRegions.size() + " 个区域");
            } else {
                // 没有修正时，修正后字符框与原始字符框相同
                detector.setCorrectedRegions(new java.util.ArrayList<>(finalRegions));
            }
            
            // 如果有预设且启用了文本定位脚本，执行脚本
            // 注意：脚本已在detect()方法中作为后处理执行
            if (usingPreset && currentPreset.isTextLocationScriptEnabled() && 
                currentPreset.getTextLocationScript() != null && !currentPreset.getTextLocationScript().isEmpty()) {
                log("文本定位脚本已在detect()中执行");
            }
            
            for (int i = 0; i < finalRegions.size(); i++) {
                TextBlobDetector.Region region = finalRegions.get(i);
                
                // 确定字符标签
                String label;
                if (i < correctChars.length) {
                    label = String.valueOf(correctChars[i]);
                } else {
                    label = "?";
                }
                
                // 创建Shape对象
                Shape shape = new Shape();
                shape.setLabel(label);
                shape.setScore(0.5);
                
                // 设置四个角点坐标
                List<List<Double>> points = new ArrayList<>();
                points.add(Arrays.asList((double) region.x, (double) region.y));
                points.add(Arrays.asList((double) (region.x + region.width), (double) region.y));
                points.add(Arrays.asList((double) (region.x + region.width), (double) (region.y + region.height)));
                points.add(Arrays.asList((double) region.x, (double) (region.y + region.height)));
                shape.setPoints(points);
                
                shapes.add(shape);
            }
            
            // 如果有预设且启用了字符分类脚本，执行脚本
            if (usingPreset && currentPreset.isCharClassifyScriptEnabled() && 
                currentPreset.getCharClassifyScript() != null && !currentPreset.getCharClassifyScript().isEmpty()) {
                log("执行字符分类脚本...");
                // 脚本执行逻辑待实现
                // TODO: 执行Lua脚本进行字符分类
            }
            
            // 如果有预设且启用了OCR后处理脚本，执行脚本
            if (usingPreset && currentPreset.isOcrPostProcessScriptEnabled() && 
                currentPreset.getOcrPostProcessScript() != null && !currentPreset.getOcrPostProcessScript().isEmpty()) {
                log("执行OCR后处理脚本...");
                // 脚本执行逻辑待实现
                // TODO: 执行Lua脚本进行OCR后处理
            }
            
            // 显示结果
            resultPanel.setShapes(shapes);
            currentShapes = shapes;  // 保存当前识别的字符区域
            
            // 保存可视化结果
            String imageDir = ConfigManager.getImageDir();
            File imgDir = new File(imageDir);
            if (!imgDir.exists()) {
                imgDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
            String timestamp = sdf.format(new Date());
            File visFile = new File(imageDir, "ocr_test_" + timestamp + ".png");
            opencv_imgcodecs.imwrite(visFile.getAbsolutePath(), detectionResult.visualizedImage);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            String params = String.format("阈值:%d Gamma:%.2f 面积:%d-%d 宽:%d-%d 高:%d-%d", 
                ocrGray, gamma, minArea, maxArea, minWidth, maxWidth, minHeight, maxHeight);
            String msg = String.format("OCR识别完成: 检测到 %d 个字符，耗时 %dms [%s]", 
                shapes.size(), elapsedTime, params);
            showStatus(msg, false);
            log(msg);
            
            // 在结果面板显示处理信息
            resultPanel.setProcessInfo(String.format("处理时间: %dms | %s", elapsedTime, params));
            
            // 释放资源
            inputMat.release();
            detectionResult.visualizedImage.release();
            
        } catch (Exception e) {
            showStatus("OCR识别失败: " + e.getMessage(), true);
            log("OCR识别失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示当前使用的OCR参数（从ConfigManager读取）
     */
    private void showCurrentParameters() {
        LogUtil.log("[showCurrentParameters] 开始执行");
        log("当前OCR参数（来自OCR参数设置界面）:");
        log(String.format("  二值化阈值:%d, Gamma:%.2f", ConfigManager.getGrayscale(), ConfigManager.getGamma()));
        log(String.format("  面积范围:%d-%d", ConfigManager.getMinArea(), ConfigManager.getMaxArea()));
        log(String.format("  宽度范围:%d-%d, 高度范围:%d-%d", 
                ConfigManager.getMinWidth(), ConfigManager.getMaxWidth(),
                ConfigManager.getMinHeight(), ConfigManager.getMaxHeight()));
    }
    
    private void showStatus(String message, boolean isError) {
        LogUtil.log("[showStatus] 开始执行");
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? DarkBlueTheme.ERROR : DarkBlueTheme.SUCCESS);
    }
    
    /**
     * 运行飞桨OCR识别
     * 调用honda-paddleocr-lib进行OCR识别，并将结果填入正确字符串输入框
     */
    private void runPaddleOcr() {
        LogUtil.log("[runPaddleOcr] 开始执行");
        if (currentImage == null) {
            showStatus("请先加载图片", true);
            log("飞桨OCR识别失败: 未加载图片");
            return;
        }
        
        log("开始飞桨OCR识别，图片尺寸: " + currentImage.getWidth() + "x" + currentImage.getHeight());
        showStatus("正在运行飞桨OCR识别...", false);
        
        // 在后台线程中执行OCR识别
        new Thread(() -> {
            try {
                // 保存当前图片到临时文件
                String tempDir = System.getProperty("java.io.tmpdir");
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
                File tempImageFile = new File(tempDir, "paddleocr_" + timestamp + ".png");
                ImageIO.write(currentImage, "PNG", tempImageFile);
                
                log("临时图片已保存: " + tempImageFile.getAbsolutePath());
                
                // 初始化模型资源（从fatjar中提取到临时文件）
                if (!ModelResourceLoader.isAvailable()) {
                    throw new IOException("模型资源初始化失败，请检查fatjar中是否包含模型文件");
                }
                
                String detModelPath = ModelResourceLoader.getDetModelPath();
                String recModelPath = ModelResourceLoader.getRecModelPath();
                String keysPath = ModelResourceLoader.getDictFilePath();
                
                log("使用嵌入式模型文件:");
                log("  检测模型: " + detModelPath);
                log("  识别模型: " + recModelPath);
                log("  字典文件: " + keysPath);
                
                // 创建飞桨OCR引擎并初始化
                com.honda.paddleocr.PaddleOcrEngine engine = new com.honda.paddleocr.PaddleOcrEngine();
                engine.initialize(detModelPath, recModelPath, keysPath);
                
                log("飞桨OCR引擎初始化完成");
                
                // 运行OCR识别
                long startTime = System.currentTimeMillis();
                List<com.honda.paddleocr.OcrResult> results = engine.runOcr(tempImageFile.getAbsolutePath());
                long elapsedTime = System.currentTimeMillis() - startTime;
                
                // 释放引擎资源
                engine.close();
                
                // 删除临时文件
                tempImageFile.delete();
                
                // 处理识别结果
                if (results == null || results.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        showStatus("飞桨OCR识别完成: 未检测到文本", false);
                        log("飞桨OCR识别完成: 未检测到文本");
                    });
                    return;
                }
                
                // 按位置排序结果（从左到右，从上到下）
                results.sort((a, b) -> {
                    // 先按Y坐标排序（行）
                    double yA = a.getBoxPoints().stream().mapToDouble(p -> p.getY()).average().orElse(0);
                    double yB = b.getBoxPoints().stream().mapToDouble(p -> p.getY()).average().orElse(0);
                    if (Math.abs(yA - yB) > 20) { // 如果Y坐标差异大于20像素，认为是不同行
                        return Double.compare(yA, yB);
                    }
                    // 同行内按X坐标排序
                    double xA = a.getBoxPoints().stream().mapToDouble(p -> p.getX()).average().orElse(0);
                    double xB = b.getBoxPoints().stream().mapToDouble(p -> p.getX()).average().orElse(0);
                    return Double.compare(xA, xB);
                });
                
                // 构建识别文本
                StringBuilder sb = new StringBuilder();
                for (com.honda.paddleocr.OcrResult result : results) {
                    if (sb.length() > 0) {
                        sb.append("\n"); // 多行文本用换行符分隔
                    }
                    sb.append(result.getText());
                }
                
                String recognizedText = sb.toString();
                
                // 转换为Shape列表用于显示
                List<Shape> shapes = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    com.honda.paddleocr.OcrResult result = results.get(i);
                    Shape shape = new Shape();
                    shape.setLabel(result.getText());
                    shape.setScore(result.getAverageConfidence());
                    
                    // 转换飞桨OCR的坐标点为Shape格式
                    List<List<Double>> points = new ArrayList<>();
                    for (com.honda.paddleocr.Point p : result.getBoxPoints()) {
                        List<Double> point = new ArrayList<>();
                        point.add(p.getX());
                        point.add(p.getY());
                        points.add(point);
                    }
                    shape.setPoints(points);
                    shapes.add(shape);
                }
                
                // 更新UI
                final int resultCount = results.size();
                final String finalRecognizedText = recognizedText;
                SwingUtilities.invokeLater(() -> {
                    // 将识别结果填入正确字符串输入框
                    correctStringField.setText(finalRecognizedText);
                    
                    // 显示识别结果
                    currentShapes = shapes;
                    resultPanel.setShapes(shapes);
                    
                    String msg = String.format("飞桨OCR识别完成: 检测到 %d 个文本区域，耗时 %dms", resultCount, elapsedTime);
                    showStatus(msg, false);
                    log(msg);
                    
                    // 显示详细结果
                    for (int i = 0; i < results.size(); i++) {
                        com.honda.paddleocr.OcrResult result = results.get(i);
                        log(String.format("  [%d] %s (置信度: %.2f%%)", 
                                i + 1, 
                                result.getText(), 
                                result.getAverageConfidence() * 100));
                    }
                });
                
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                SwingUtilities.invokeLater(() -> {
                    showStatus("飞桨OCR识别失败: " + errorMsg, true);
                    log("飞桨OCR识别失败: " + errorMsg);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * 保存字符截图
     * 每个字符截图放入字符名称文件夹，添加时间戳后缀
     * 特殊字符使用ASCII码表示，未分类字符放入"未分类"文件夹
     */
    private void saveCharacterScreenshots() {
        LogUtil.log("[saveCharacterScreenshots] 开始执行");
        
        if (currentImage == null) {
            showStatus("请先加载图片或运行OCR识别", true);
            log("截图失败: 未加载图片");
            return;
        }
        
        if (currentShapes == null || currentShapes.isEmpty()) {
            showStatus("没有可截图的字符区域", true);
            log("截图失败: 没有字符区域");
            return;
        }
        
        String correctString = correctStringField.getText().trim();
        boolean hasCorrectString = !correctString.isEmpty();
        
        // 创建保存目录 - 使用图片文件夹路径设置
        String imageDir = ConfigManager.getImageDir();
        File charBaseDir = new File(imageDir, "character_images");
        if (!charBaseDir.exists()) {
            charBaseDir.mkdirs();
        }
        
        // 未分类目录
        File uncategorizedDir = new File(charBaseDir, "未分类");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        int savedCount = 0;
        int errorCount = 0;
        
        log("开始保存字符截图，共 " + currentShapes.size() + " 个区域");
        
        for (int i = 0; i < currentShapes.size(); i++) {
            Shape shape = currentShapes.get(i);
            
            try {
                // 确定字符标签
                String charLabel;
                if (hasCorrectString && i < correctString.length()) {
                    // 优先使用correctString输入的字符
                    charLabel = String.valueOf(correctString.charAt(i));
                } else if (shape.getLabel() != null && !shape.getLabel().isEmpty() && !"?".equals(shape.getLabel())) {
                    // 使用shape自带的label（如从JSON加载的标注）
                    charLabel = shape.getLabel();
                } else if (!hasCorrectString) {
                    // 没有输入correctString且shape没有label，放入未分类
                    charLabel = "未分类";
                } else {
                    // correctString长度不够
                    charLabel = "未分类";
                }
                
                // 处理特殊字符（操作系统保留字符）
                String folderName = sanitizeFolderName(charLabel);
                
                // 创建字符目录
                File charDir = new File(charBaseDir, folderName);
                if (!charDir.exists()) {
                    charDir.mkdirs();
                }
                
                // 计算截图区域
                List<List<Double>> points = shape.getPoints();
                if (points == null || points.size() < 4) {
                    continue;
                }
                
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                
                for (List<Double> point : points) {
                    if (point.size() >= 2) {
                        int x = point.get(0).intValue();
                        int y = point.get(1).intValue();
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
                
                // 确保坐标在图片范围内
                minX = Math.max(0, minX);
                minY = Math.max(0, minY);
                maxX = Math.min(currentImage.getWidth(), maxX);
                maxY = Math.min(currentImage.getHeight(), maxY);
                
                int width = maxX - minX;
                int height = maxY - minY;
                
                if (width <= 0 || height <= 0) {
                    log("跳过无效区域: " + charLabel);
                    continue;
                }
                
                // 截取字符图片
                BufferedImage charImage = currentImage.getSubimage(minX, minY, width, height);
                
                // 生成文件名：字符_时间戳.png
                String timestamp = sdf.format(new Date());
                String fileName = folderName + "_" + timestamp + ".png";
                File outputFile = new File(charDir, fileName);
                
                // 保存图片
                ImageIO.write(charImage, "PNG", outputFile);
                savedCount++;
                
                log("保存字符截图: " + charLabel + " -> " + outputFile.getName());
                
            } catch (Exception e) {
                errorCount++;
                log("保存字符截图失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        String msg = String.format("字符截图保存完成: 成功 %d 个, 失败 %d 个", savedCount, errorCount);
        showStatus(msg, errorCount > 0);
        log(msg + "，保存位置: " + charBaseDir.getAbsolutePath());
        log("截图文件夹按字符名称分类，可在 路径设置 中修改图片保存路径");
    }
    
    /**
     * 处理文件夹名称，将所有ASCII字符（0-127）转换为ASCII码表示
     * 非ASCII字符（如中文）保持原样
     * 用于创建字符截图保存的文件夹名称
     */
    private String sanitizeFolderName(String charLabel) {
        if (charLabel == null || charLabel.isEmpty()) {
            return "未分类";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : charLabel.toCharArray()) {
            if (c <= 127) {
                // ASCII字符（0-127）使用ASCII码表示
                result.append("_").append((int) c).append("_");
            } else {
                // 非ASCII字符（如中文）保持原样
                result.append(c);
            }
        }
        
        // 如果结果为空，返回"未分类"
        String folderName = result.toString();
        if (folderName.isEmpty()) {
            return "未分类";
        }
        
        return folderName;
    }
    
    /**
     * 添加到预设功能
     * 弹出预设选择对话框，将当前OCR结果（图片、字符位置、OCR参数）保存到选中的预设
     */
    private void addToPreset() {
        LogUtil.log("[addToPreset] 开始执行");
        
        if (currentImage == null) {
            showStatus("请先加载图片", true);
            log("添加到预设失败: 未加载图片");
            return;
        }
        
        if (currentShapes == null || currentShapes.isEmpty()) {
            showStatus("没有可保存的识别结果", true);
            log("添加到预设失败: 没有识别结果");
            return;
        }
        
        // 获取存储方式
        String storageMode = ConfigManager.getStringPresetStorageMode();
        List<StringPreset> presets;
        
        try {
            if ("xml".equals(storageMode)) {
                XmlPresetStorage xmlStorage = new XmlPresetStorage();
                presets = xmlStorage.getAllPresets();
            } else {
                // 使用getDbService()从全局数据交换获取数据库服务
                DatabaseService currentDbService = getDbService();
                if (currentDbService == null) {
                    showStatus("数据库服务未初始化", true);
                    log("添加到预设失败: 数据库服务未初始化");
                    return;
                }
                presets = currentDbService.getAllStringPresets();
            }
            
            if (presets.isEmpty()) {
                // 没有预设，询问是否创建新预设
                int result = JOptionPane.showConfirmDialog(this, 
                    "当前没有预设，是否创建新预设？", 
                    "创建预设", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    createNewPresetWithCurrentData();
                }
                return;
            }
            
            // 创建预设选择对话框
            String[] presetNames = presets.stream()
                .map(p -> p.getId() + ": " + p.getPresetName())
                .toArray(String[]::new);
            
            String selected = (String) JOptionPane.showInputDialog(this,
                "选择要更新的预设：",
                "添加到预设",
                JOptionPane.QUESTION_MESSAGE,
                null,
                presetNames,
                presetNames[0]);
            
            if (selected == null) {
                return; // 用户取消
            }
            
            // 解析选中的预设ID
            int presetId = Integer.parseInt(selected.split(":")[0]);
            StringPreset selectedPreset = presets.stream()
                .filter(p -> p.getId() == presetId)
                .findFirst()
                .orElse(null);
            
            if (selectedPreset == null) {
                showStatus("选择的预设不存在", true);
                return;
            }
            
            // 更新预设数据
            updatePresetWithCurrentData(selectedPreset);
            
            // 保存预设
            if ("xml".equals(storageMode)) {
                XmlPresetStorage xmlStorage = new XmlPresetStorage();
                xmlStorage.updatePreset(selectedPreset);
            } else {
                DatabaseService currentDbService = getDbService();
                if (currentDbService != null) {
                    currentDbService.updateStringPreset(selectedPreset);
                } else {
                    showStatus("数据库服务未初始化", true);
                    log("保存预设失败: 数据库服务未初始化");
                    return;
                }
            }
            
            showStatus("已成功添加到预设: " + selectedPreset.getPresetName(), false);
            log("已将当前OCR结果添加到预设: " + selectedPreset.getPresetName());
            
        } catch (Exception e) {
            showStatus("添加到预设失败: " + e.getMessage(), true);
            log("添加到预设失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用当前数据创建新预设
     */
    private void createNewPresetWithCurrentData() {
        String presetName = JOptionPane.showInputDialog(this, 
            "请输入预设名称：", 
            "新预设");
        
        if (presetName == null || presetName.trim().isEmpty()) {
            return;
        }
        
        StringPreset newPreset = new StringPreset();
        newPreset.setPresetName(presetName.trim());
        updatePresetWithCurrentData(newPreset);
        
        try {
            String storageMode = ConfigManager.getStringPresetStorageMode();
            if ("xml".equals(storageMode)) {
                XmlPresetStorage xmlStorage = new XmlPresetStorage();
                int newId = xmlStorage.addPreset(newPreset);
                newPreset.setId(newId);
            } else {
                DatabaseService currentDbService = getDbService();
                if (currentDbService != null) {
                    int newId = currentDbService.addStringPreset(newPreset);
                    newPreset.setId(newId);
                } else {
                    showStatus("数据库服务未初始化", true);
                    log("创建预设失败: 数据库服务未初始化");
                    return;
                }
            }
            showStatus("已创建新预设: " + presetName, false);
            log("已创建新预设: " + presetName + " (ID=" + newPreset.getId() + ")");
        } catch (Exception e) {
            showStatus("创建预设失败: " + e.getMessage(), true);
            log("创建预设失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用当前OCR数据更新预设
     */
    private void updatePresetWithCurrentData(StringPreset preset) {
        // 更新字符串
        String correctString = correctStringField.getText().trim();
        if (!correctString.isEmpty()) {
            preset.setCorrectString(correctString);
        }
        
        // 更新标准图片
        if (currentImage != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(currentImage, "PNG", baos);
                preset.setStandardImage(baos.toByteArray());
            } catch (Exception e) {
                log("保存图片到预设失败: " + e.getMessage());
            }
        }
        
        // 更新OCR参数（从ConfigManager获取当前参数）
        preset.setGrayscale(ConfigManager.getGrayscale());
        preset.setGamma(ConfigManager.getGamma());
        preset.setMinArea(ConfigManager.getMinArea());
        preset.setMaxArea(ConfigManager.getMaxArea());
        preset.setMinWidth(ConfigManager.getMinWidth());
        preset.setMaxWidth(ConfigManager.getMaxWidth());
        preset.setMinHeight(ConfigManager.getMinHeight());
        preset.setMaxHeight(ConfigManager.getMaxHeight());
        
        // 更新字符相对位置（从当前识别结果生成）
        if (currentShapes != null && !currentShapes.isEmpty()) {
            List<CharRelativePosition> positions = new ArrayList<>();
            
            // 按X坐标排序（从左到右）
            List<Shape> sortedShapes = new ArrayList<>(currentShapes);
            sortedShapes.sort((s1, s2) -> {
                double x1 = s1.getPoints().get(0).get(0);
                double x2 = s2.getPoints().get(0).get(0);
                return Double.compare(x1, x2);
            });
            
            int baseX = 0;
            int baseY = 0;
            
            for (int i = 0; i < sortedShapes.size(); i++) {
                Shape shape = sortedShapes.get(i);
                CharRelativePosition pos = new CharRelativePosition();
                pos.setCharIndex(i);
                
                // 获取字符标签
                String charLabel = shape.getLabel();
                if (charLabel == null || charLabel.isEmpty() || "?".equals(charLabel)) {
                    // 尝试从correctString获取
                    if (i < correctString.length()) {
                        charLabel = String.valueOf(correctString.charAt(i));
                    } else {
                        charLabel = "?";
                    }
                }
                pos.setCharacter(charLabel);
                
                // 计算区域坐标和尺寸
                List<List<Double>> points = shape.getPoints();
                if (points != null && points.size() >= 4) {
                    int minX = Integer.MAX_VALUE;
                    int minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE;
                    int maxY = Integer.MIN_VALUE;
                    
                    for (List<Double> point : points) {
                        if (point.size() >= 2) {
                            int x = point.get(0).intValue();
                            int y = point.get(1).intValue();
                            minX = Math.min(minX, x);
                            minY = Math.min(minY, y);
                            maxX = Math.max(maxX, x);
                            maxY = Math.max(maxY, y);
                        }
                    }
                    
                    int width = maxX - minX;
                    int height = maxY - minY;
                    
                    // 首字符作为基准点(0,0)
                    if (i == 0) {
                        baseX = minX;
                        baseY = minY;
                        pos.setRelativeX(0);
                        pos.setRelativeY(0);
                    } else {
                        pos.setRelativeX(minX - baseX);
                        pos.setRelativeY(minY - baseY);
                    }
                    
                    pos.setWidth(width);
                    pos.setHeight(height);
                    
                    // 设置默认范围（±30%）
                    pos.setMinWidth((int) (width * 0.7));
                    pos.setMaxWidth((int) (width * 1.3));
                    pos.setMinHeight((int) (height * 0.7));
                    pos.setMaxHeight((int) (height * 1.3));
                    
                    // 计算与前字符的间距
                    if (i > 0 && !positions.isEmpty()) {
                        CharRelativePosition prevPos = positions.get(i - 1);
                        int spacing = pos.getRelativeX() - (prevPos.getRelativeX() + prevPos.getWidth());
                        pos.setSpacing(Math.max(0, spacing));
                        pos.setMinSpacing(0);
                        pos.setMaxSpacing(Math.max(20, spacing + 10));
                    } else {
                        pos.setSpacing(0);
                        pos.setMinSpacing(0);
                        pos.setMaxSpacing(20);
                    }
                }
                
                positions.add(pos);
            }
            
            preset.setCharRelativePositions(positions);
            preset.setCharRelativePositionEnabled(true);
            log("已生成 " + positions.size() + " 个字符的相对位置信息");
        }
    }
    
    /**
     * 导出当前识别结果为XAnyLabeling JSON格式
     * 保存到图片文件夹的JSON标注目录，文件名添加时间戳后缀
     */
    private void exportToJson() {
        LogUtil.log("[exportToJson] 开始执行");
        
        if (currentImage == null) {
            showStatus("请先加载图片", true);
            log("导出JSON失败: 未加载图片");
            return;
        }
        
        if (currentShapes == null || currentShapes.isEmpty()) {
            showStatus("没有可导出的识别结果", true);
            log("导出JSON失败: 没有识别结果");
            return;
        }
        
        try {
            // 创建JSON标注目录
            String imageDir = ConfigManager.getImageDir();
            File jsonDir = new File(imageDir, "JSON标注");
            if (!jsonDir.exists()) {
                jsonDir.mkdirs();
            }
            
            // 生成文件名：使用原图片名或默认名 + 时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String baseFileName = "ocr_result_" + timestamp;
            
            // 构建XAnyLabelingJson对象
            XAnyLabelingJson jsonData = new XAnyLabelingJson();
            jsonData.setVersion("3.3.4");
            jsonData.setFlags(new HashMap<>());
            jsonData.setShapes(currentShapes);
            jsonData.setImagePath(baseFileName + ".png");
            jsonData.setImageWidth(currentImage.getWidth());
            jsonData.setImageHeight(currentImage.getHeight());
            
            // 将图片转换为base64编码
            String imageBase64 = imageToBase64(currentImage);
            jsonData.setImageData(imageBase64);
            
            // 生成JSON文件路径
            File jsonFile = new File(jsonDir, baseFileName + ".json");
            
            // 写入JSON文件
            String jsonString = jsonData.toJson();
            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(jsonString);
            }
            
            // 同时保存图片文件
            File imageFile = new File(jsonDir, baseFileName + ".png");
            ImageIO.write(currentImage, "PNG", imageFile);
            
            String msg = String.format("JSON导出成功: %d 个标注，保存到 %s", 
                    currentShapes.size(), jsonFile.getName());
            showStatus(msg, false);
            log(msg);
            log("导出路径: " + jsonDir.getAbsolutePath());
            
        } catch (Exception e) {
            String errorMsg = "导出JSON失败: " + e.getMessage();
            showStatus(errorMsg, true);
            log(errorMsg);
            e.printStackTrace();
        }
    }
    
    /**
     * 将BufferedImage转换为Base64编码的字符串
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    /**
     * 切换行基线显示
     */
    public void toggleBaseline() {
        if (resultPanel != null) {
            resultPanel.toggleBaseline();
        }
    }
    
    /**
     * 切换标签显示
     */
    public void toggleLabels() {
        if (resultPanel != null) {
            resultPanel.toggleLabels();
        }
    }
    
    /**
     * 切换得分显示
     */
    public void toggleScores() {
        if (resultPanel != null) {
            resultPanel.toggleScores();
        }
    }
    
    /**
     * 切换拆分区域模式
     */
    public void toggleSplitRegion() {
        if (resultPanel != null) {
            resultPanel.toggleSplitRegion();
        }
    }
    
    /**
     * 切换合并区域模式
     */
    public void toggleMergeRegion() {
        if (resultPanel != null) {
            resultPanel.toggleMergeRegion();
        }
    }
    
    /**
     * 切换删除区域模式
     */
    public void toggleDeleteRegion() {
        if (resultPanel != null) {
            resultPanel.toggleDeleteRegion();
        }
    }
    
    /**
     * 更新标签
     * 根据正确字符串更新所有区域的标签
     * 用于合并区域或拆分区域后更新标签与区域的对应关系
     */
    public void updateLabels() {
        if (currentShapes == null || currentShapes.isEmpty()) {
            showStatus("没有可更新的区域", true);
            log("更新标签失败: 没有区域");
            return;
        }
        
        String correctString = correctStringField.getText().trim();
        if (correctString.isEmpty()) {
            showStatus("请先输入正确字符串", true);
            log("更新标签失败: 未输入正确字符串");
            return;
        }
        
        // 清理字符串（移除空白字符）
        String cleanedString = correctString.replaceAll("\\s+", "");
        char[] chars = cleanedString.toCharArray();
        
        // 更新每个区域的标签
        int updatedCount = 0;
        for (int i = 0; i < currentShapes.size(); i++) {
            Shape shape = currentShapes.get(i);
            if (i < chars.length) {
                String newLabel = String.valueOf(chars[i]);
                shape.setLabel(newLabel);
                updatedCount++;
            } else {
                // 如果字符串长度不够，将剩余区域标记为"?"
                shape.setLabel("?");
            }
        }
        
        // 刷新显示
        if (resultPanel != null) {
            resultPanel.setShapes(currentShapes);
        }
        
        String msg = String.format("已更新 %d 个区域的标签，共 %d 个区域", 
                updatedCount, currentShapes.size());
        showStatus(msg, false);
        log(msg);
    }
}
