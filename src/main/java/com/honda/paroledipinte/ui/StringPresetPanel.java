package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.db.DatabaseService;
import com.honda.paroledipinte.db.XmlPresetStorage;
import com.honda.paroledipinte.model.CharRelativePosition;
import com.honda.paroledipinte.model.StringPreset;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import com.honda.paroledipinte.util.GlobalKvCore;
import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.ui.MainFrame;
import com.honda.paroledipinte.ui.OcrTestPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.stream.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 字符串预设面板
 * 管理OCR测试的字符串预设配置
 */
public class StringPresetPanel extends JPanel {
    
    private JTable presetTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;
    private Consumer<String> logCallback;
    
    // 编辑对话框组件
    private JTextField nameField;
    private JTextArea stringArea;
    private JLabel imageLabel;
    private byte[] currentImageData;
    
    // OCR参数编辑组件
    private JSpinner grayscaleSpinner;
    private JSpinner gammaSpinner;
    private JSpinner minAreaSpinner;
    private JSpinner maxAreaSpinner;
    private JSpinner minWidthSpinner;
    private JSpinner maxWidthSpinner;
    private JSpinner minHeightSpinner;
    private JSpinner maxHeightSpinner;
    
    // 脚本编辑组件
    private RSyntaxTextArea textLocationScriptArea;
    private RSyntaxTextArea charClassifyScriptArea;
    private RSyntaxTextArea ocrPostProcessScriptArea;
    private JCheckBox textLocationScriptEnabled;
    private JCheckBox charClassifyScriptEnabled;
    private JCheckBox ocrPostProcessScriptEnabled;
    
    // 字符相对位置编辑组件
    private JCheckBox charRelativePositionEnabled;
    private DefaultTableModel charPosTableModel;
    private JTable charPosTable;
    
    // 当前编辑的预设
    private StringPreset currentPreset;
    private boolean isEditing = false;
    
    // 数据库状态显示组件
    private JLabel dbStatusLabel;
    private JLabel dbRecordCountLabel;
    private JLabel storageModeLabel;
    private JButton reconnectBtn;
    private static final String STATUS_DISCONNECTED = "未连接";
    private static final String STATUS_CONNECTING = "连接中";
    private static final String STATUS_CONNECTED = "已连接(XML)";
    
    // 存储方式
    private String storageMode; // "database" 或 "xml"
    private XmlPresetStorage xmlStorage;
    
    // 重连数据库回调
    private Supplier<DatabaseService> reconnectCallback;
    
    // 图像预览组件
    private JLabel imagePreviewLabel;
    private static final int PREVIEW_WIDTH = 200;
    private static final int PREVIEW_HEIGHT = 150;
    
    public StringPresetPanel() {
        LogUtil.log("[StringPresetPanel] 开始执行");
        // 初始化存储方式
        this.storageMode = ConfigManager.getStringPresetStorageMode();
        if ("xml".equals(storageMode)) {
            this.xmlStorage = new XmlPresetStorage();
        }
        initComponents();
    }
    
    public void setDatabaseService(DatabaseService dbService) {
        // 仅在数据库模式下注入数据库服务
        if ("database".equals(storageMode)) {
            log("正在注入数据库服务...");
            this.dbService = dbService;
            if (dbService != null) {
                log("数据库服务已注入，正在更新状态...");
                updateDbStatus();
                loadPresets();
            } else {
                log("警告: 数据库服务为null");
                updateDbStatus();
            }
        }
    }
    
    /**
     * 重新加载存储方式配置
     */
    public void reloadStorageMode() {
        String newMode = ConfigManager.getStringPresetStorageMode();
        if (!newMode.equals(this.storageMode)) {
            this.storageMode = newMode;
            if ("xml".equals(storageMode)) {
                this.xmlStorage = new XmlPresetStorage();
                this.dbService = null;
            }
            updateDbStatus();
            loadPresets();
            log("存储方式已切换为: " + ("xml".equals(storageMode) ? "XML方式" : "数据库方式"));
        }
    }
    
    public void setLogCallback(Consumer<String> logCallback) {
        this.logCallback = logCallback;
    }
    
    /**
     * 设置重连数据库回调
     * @param reconnectCallback 返回新的DatabaseService实例
     */
    public void setReconnectCallback(Supplier<DatabaseService> reconnectCallback) {
        this.reconnectCallback = reconnectCallback;
    }
    
    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept("[字符串预设] " + message);
        }
    }
    
    /**
     * 获取数据库服务（优先从全局数据交换获取）
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
    
    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBackground(DarkBlueTheme.getBackgroundColor());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建顶部状态栏（包含图像预览区）
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.NORTH);
        
        // 创建中间区域（表格）
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        // 创建操作按钮面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建中间面板（包含表格）
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 创建表格
        String[] columnNames = {"序号", "预设名称", "字符串", "有图片", "OCR参数", "字符相对位置", "文本定位脚本", "字符分类脚本", "OCR后处理脚本"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格只读
            }
        };
        
        presetTable = new JTable(tableModel);
        presetTable.setBackground(DarkBlueTheme.PRIMARY_DARK);
        presetTable.setForeground(DarkBlueTheme.getTextColor());
        presetTable.setSelectionBackground(DarkBlueTheme.ACCENT);
        presetTable.setSelectionForeground(DarkBlueTheme.getTextColor());
        presetTable.setGridColor(DarkBlueTheme.BORDER);
        presetTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // 设置列宽
        presetTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        presetTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        presetTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        presetTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        presetTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        presetTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        presetTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        presetTable.getColumnModel().getColumn(7).setPreferredWidth(80);
        presetTable.getColumnModel().getColumn(8).setPreferredWidth(80);
        
        // 设置渲染器
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(DarkBlueTheme.PRIMARY_DARK);
        renderer.setForeground(DarkBlueTheme.getTextColor());
        for (int i = 0; i < presetTable.getColumnCount(); i++) {
            presetTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        // 单击选中显示图片，双击编辑
        presetTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = presetTable.getSelectedRow();
                if (row >= 0) {
                    // 无论单击还是双击，都先更新图片预览
                    updateImagePreview(row);
                }
                if (e.getClickCount() == 2) {
                    editSelectedPreset();
                }
            }
        });
        
        // 添加列表选择监听器，当选中项改变时更新图片预览
        presetTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = presetTable.getSelectedRow();
                if (row >= 0) {
                    updateImagePreview(row);
                } else {
                    clearImagePreview();
                }
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(presetTable);
        tableScroll.setBackground(DarkBlueTheme.getBackgroundColor());
        panel.add(tableScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 更新图像预览
     */
    private void updateImagePreview(int row) {
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            StringPreset preset = getPresetById(id);
            if (preset != null && preset.hasStandardImage()) {
                byte[] imageData = preset.getStandardImage();
                if (imageData != null && imageData.length > 0) {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                    if (image != null) {
                        displayImageInPreview(image);
                        return;
                    }
                }
            }
            // 没有图片或加载失败，显示占位符
            showNoImagePlaceholder();
        } catch (Exception e) {
            log("加载预览图片失败: " + e.getMessage());
            showNoImagePlaceholder();
        }
    }
    
    /**
     * 在预览区显示图片
     */
    private void displayImageInPreview(BufferedImage image) {
        if (imagePreviewLabel == null) return;
        
        // 计算缩放后的尺寸，保持宽高比
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        
        double scaleX = (double) PREVIEW_WIDTH / imgWidth;
        double scaleY = (double) PREVIEW_HEIGHT / imgHeight;
        double scale = Math.min(scaleX, scaleY);
        
        int scaledWidth = (int) (imgWidth * scale);
        int scaledHeight = (int) (imgHeight * scale);
        
        // 缩放图片
        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imagePreviewLabel.setIcon(new ImageIcon(scaledImage));
        imagePreviewLabel.setText(null);
    }
    
    /**
     * 清除图像预览
     */
    private void clearImagePreview() {
        if (imagePreviewLabel != null) {
            showNoImagePlaceholder();
        }
    }
    
    /**
     * 显示无图片占位符
     */
    private void showNoImagePlaceholder() {
        if (imagePreviewLabel != null) {
            imagePreviewLabel.setIcon(null);
            imagePreviewLabel.setText("无图片");
            imagePreviewLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        }
    }
    
    /**
     * 创建状态面板（包含图像预览区）
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        // 左侧：图像预览区
        JPanel previewPanel = createImagePreviewPanel();
        panel.add(previewPanel, BorderLayout.WEST);
        
        // 中间：数据库状态
        JPanel centerPanel = createDbStatusPanel();
        panel.add(centerPanel, BorderLayout.CENTER);
        
        // 右侧：刷新按钮和重连按钮
        JPanel rightPanel = createButtonPanelForStatus();
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * 创建图像预览面板
     */
    private JPanel createImagePreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "预设图片预览", 0, 0, null, DarkBlueTheme.getTextColor()
        ));
        panel.setPreferredSize(new Dimension(PREVIEW_WIDTH + 20, PREVIEW_HEIGHT + 30));
        
        // 图像预览标签
        imagePreviewLabel = new JLabel("无图片", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        imagePreviewLabel.setBackground(DarkBlueTheme.PRIMARY_DARK);
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(DarkBlueTheme.BORDER));
        
        panel.add(imagePreviewLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建数据库状态面板
     */
    private JPanel createDbStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        
        // 存储方式显示
        JLabel modeLabel = new JLabel("存储方式:");
        modeLabel.setForeground(DarkBlueTheme.getTextColor());
        panel.add(modeLabel);
        
        storageModeLabel = new JLabel("xml".equals(storageMode) ? "XML文件" : "数据库");
        storageModeLabel.setForeground("xml".equals(storageMode) ? DarkBlueTheme.SUCCESS : DarkBlueTheme.ACCENT);
        storageModeLabel.setFont(storageModeLabel.getFont().deriveFont(Font.BOLD));
        panel.add(storageModeLabel);
        
        JLabel dbLabel = new JLabel("状态:");
        dbLabel.setForeground(DarkBlueTheme.getTextColor());
        panel.add(dbLabel);
        
        dbStatusLabel = new JLabel("xml".equals(storageMode) ? STATUS_CONNECTED : STATUS_DISCONNECTED);
        dbStatusLabel.setForeground("xml".equals(storageMode) ? DarkBlueTheme.SUCCESS : DarkBlueTheme.ERROR);
        dbStatusLabel.setFont(dbStatusLabel.getFont().deriveFont(Font.BOLD));
        panel.add(dbStatusLabel);
        
        // 记录数显示
        JLabel recordLabel = new JLabel("记录数:");
        recordLabel.setForeground(DarkBlueTheme.getTextColor());
        panel.add(recordLabel);
        
        dbRecordCountLabel = new JLabel("0");
        dbRecordCountLabel.setForeground(DarkBlueTheme.getSecondaryTextColor());
        panel.add(dbRecordCountLabel);
        
        return panel;
    }
    
    /**
     * 创建状态栏的按钮面板
     */
    private JPanel createButtonPanelForStatus() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        
        JButton refreshStatusBtn = createStyledButton("刷新状态");
        refreshStatusBtn.addActionListener(e -> updateDbStatus());
        
        reconnectBtn = createStyledButton("重连数据库");
        reconnectBtn.setToolTipText("重新连接数据库");
        reconnectBtn.addActionListener(e -> reconnectDatabase());
        reconnectBtn.setVisible("database".equals(storageMode));
        
        panel.add(refreshStatusBtn);
        panel.add(reconnectBtn);
        
        return panel;
    }
    
    /**
     * 更新数据库状态显示
     */
    public void updateDbStatus() {
        if (dbStatusLabel == null || dbRecordCountLabel == null) {
            return;
        }
        
        // 更新存储方式显示
        if (storageModeLabel != null) {
            storageModeLabel.setText("xml".equals(storageMode) ? "XML文件" : "数据库");
            storageModeLabel.setForeground("xml".equals(storageMode) ? DarkBlueTheme.SUCCESS : DarkBlueTheme.ACCENT);
        }
        
        // XML模式
        if ("xml".equals(storageMode)) {
            dbStatusLabel.setText(STATUS_CONNECTED);
            dbStatusLabel.setForeground(DarkBlueTheme.SUCCESS);
            try {
                int count = xmlStorage.getAllPresets().size();
                dbRecordCountLabel.setText(String.valueOf(count));
            } catch (Exception e) {
                dbRecordCountLabel.setText("0");
            }
            if (reconnectBtn != null) {
                reconnectBtn.setVisible(false);
            }
            return;
        }
        
        // 数据库模式
        if (reconnectBtn != null) {
            reconnectBtn.setVisible(true);
        }
        
        // 从全局数据交换获取数据库服务
        DatabaseService currentDbService = getDbService();
        
        if (currentDbService == null) {
            dbStatusLabel.setText(STATUS_DISCONNECTED);
            dbStatusLabel.setForeground(DarkBlueTheme.ERROR);
            dbRecordCountLabel.setText("0");
            // 未连接时启用重连按钮
            if (reconnectBtn != null) {
                reconnectBtn.setEnabled(true);
            }
        } else {
            // 尝试获取记录数来验证连接状态
            try {
                int count = currentDbService.getAllStringPresets().size();
                dbStatusLabel.setText(STATUS_CONNECTED);
                dbStatusLabel.setForeground(DarkBlueTheme.SUCCESS);
                dbRecordCountLabel.setText(String.valueOf(count));
                log("数据库连接正常，共 " + count + " 条预设记录");
                // 已连接时禁用重连按钮
                if (reconnectBtn != null) {
                    reconnectBtn.setEnabled(false);
                }
            } catch (Exception e) {
                dbStatusLabel.setText(STATUS_CONNECTING);
                dbStatusLabel.setForeground(Color.ORANGE);
                dbRecordCountLabel.setText("?");
                log("数据库状态检查失败: " + e.getMessage());
                // 连接异常时启用重连按钮
                if (reconnectBtn != null) {
                    reconnectBtn.setEnabled(true);
                }
            }
        }
    }
    
    /**
     * 重连数据库
     */
    private void reconnectDatabase() {
        if (reconnectCallback == null) {
            JOptionPane.showMessageDialog(this, 
                "重连功能未配置", 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 禁用重连按钮防止重复点击
        reconnectBtn.setEnabled(false);
        reconnectBtn.setText("连接中...");
        
        // 在后台线程中执行重连
        new Thread(() -> {
            try {
                log("正在重新连接数据库...");
                
                // 调用回调获取新的数据库服务
                DatabaseService newDbService = reconnectCallback.get();
                
                if (newDbService != null) {
                    // 更新数据库服务（本地和全局）
                    this.dbService = newDbService;
                    GlobalKvCore.setValue("dbService", newDbService);
                    
                    SwingUtilities.invokeLater(() -> {
                        updateDbStatus();
                        loadPresets();
                        JOptionPane.showMessageDialog(this, 
                            "数据库重连成功！", 
                            "重连成功", 
                            JOptionPane.INFORMATION_MESSAGE);
                    });
                    
                    log("数据库重连成功");
                } else {
                    SwingUtilities.invokeLater(() -> {
                        reconnectBtn.setEnabled(true);
                        reconnectBtn.setText("重连数据库");
                        JOptionPane.showMessageDialog(this, 
                            "数据库重连失败：无法创建数据库服务", 
                            "重连失败", 
                            JOptionPane.ERROR_MESSAGE);
                    });
                    log("数据库重连失败：回调返回null");
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    reconnectBtn.setEnabled(true);
                    reconnectBtn.setText("重连数据库");
                    JOptionPane.showMessageDialog(this, 
                        "数据库重连失败：" + e.getMessage(), 
                        "重连失败", 
                        JOptionPane.ERROR_MESSAGE);
                });
                log("数据库重连失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton useBtn = createStyledButton("使用预设");
        useBtn.addActionListener(e -> useSelectedPreset());
        panel.add(useBtn);
        
        JButton editBtn = createStyledButton("编辑预设");
        editBtn.addActionListener(e -> editSelectedPreset());
        panel.add(editBtn);
        
        JButton saveBtn = createStyledButton("保存预设");
        saveBtn.addActionListener(e -> saveCurrentPreset());
        panel.add(saveBtn);
        
        JButton addBtn = createStyledButton("新增预设");
        addBtn.addActionListener(e -> addNewPreset());
        panel.add(addBtn);
        
        JButton deleteBtn = createStyledButton("删除预设");
        deleteBtn.addActionListener(e -> deleteSelectedPreset());
        panel.add(deleteBtn);
        
        JButton exportBtn = createStyledButton("导出XML");
        exportBtn.addActionListener(e -> exportToXml());
        panel.add(exportBtn);
        
        JButton importBtn = createStyledButton("导入XML");
        importBtn.addActionListener(e -> importFromXml());
        panel.add(importBtn);
        
        JButton refreshBtn = createStyledButton("刷新");
        refreshBtn.addActionListener(e -> loadPresets());
        panel.add(refreshBtn);
        
        return panel;
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(DarkBlueTheme.PRIMARY);
        button.setForeground(DarkBlueTheme.getTextColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        return button;
    }
    
    /**
     * 加载所有预设到表格
     */
    private void loadPresets() {
        try {
            tableModel.setRowCount(0);
            List<StringPreset> presets;
            
            // 根据存储方式选择数据源
            if ("xml".equals(storageMode)) {
                if (xmlStorage == null) {
                    xmlStorage = new XmlPresetStorage();
                }
                presets = xmlStorage.getAllPresets();
            } else {
                // 从全局数据交换获取数据库服务
                DatabaseService currentDbService = getDbService();
                if (currentDbService == null) {
                    log("数据库服务未初始化，请等待数据库初始化完成");
                    updateDbStatus();
                    return;
                }
                presets = currentDbService.getAllStringPresets();
            }
            
            for (StringPreset preset : presets) {
                Object[] row = {
                    preset.getId(),
                    preset.getPresetName(),
                    preset.getCorrectString(),
                    preset.hasStandardImage() ? "是" : "否",
                    String.format("G:%d,γ:%.1f", preset.getGrayscale(), preset.getGamma()),
                    preset.isCharRelativePositionEnabled() ? "启用" : "禁用",
                    preset.isTextLocationScriptEnabled() ? "启用" : "禁用",
                    preset.isCharClassifyScriptEnabled() ? "启用" : "禁用",
                    preset.isOcrPostProcessScriptEnabled() ? "启用" : "禁用"
                };
                tableModel.addRow(row);
            }
            
            log("已加载 " + presets.size() + " 个预设");
            
            // 更新状态显示
            updateDbStatus();
            
            // 清除图片预览
            clearImagePreview();
        } catch (Exception e) {
            log("加载预设失败: " + e.getMessage());
            e.printStackTrace();
            updateDbStatus();
        }
    }
    
    /**
     * 获取单个预设（根据存储方式）
     */
    private StringPreset getPresetById(int id) throws Exception {
        if ("xml".equals(storageMode)) {
            return xmlStorage.getPreset(id);
        } else {
            DatabaseService currentDbService = getDbService();
            if (currentDbService == null) {
                throw new Exception("数据库服务未初始化");
            }
            return currentDbService.getStringPreset(id);
        }
    }
    
    /**
     * 使用选中的预设
     * 将预设的字符串填入OCR识别测试的正确字符串输入框，然后打开OCR识别测试界面
     */
    private void useSelectedPreset() {
        int row = presetTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个预设", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            StringPreset preset = getPresetById(id);
            if (preset != null) {
                // 获取MainFrame实例
                MainFrame mainFrame = MainFrame.getInstance();
                if (mainFrame != null) {
                    // 先备份当前参数
                    mainFrame.backupParametersBeforePreset();
                    
                    // 获取OCR测试面板并设置预设
                    OcrTestPanel ocrTestPanel = mainFrame.getOcrTestPanel();
                    if (ocrTestPanel != null) {
                        // 设置完整的预设信息（包括OCR参数、字符相对位置、脚本等）
                        ocrTestPanel.setCurrentPreset(preset);
                        log("已将预设应用到OCR识别测试: " + preset.getPresetName());
                    }
                    
                    // 同步OCR参数和字符相对位置到OCR参数界面
                    OcrSettingsPanel ocrSettingsPanel = mainFrame.getOcrSettingsPanel();
                    if (ocrSettingsPanel != null) {
                        // 同步OCR参数
                        ConfigManager.setGrayscale(preset.getGrayscale());
                        ConfigManager.setGamma(preset.getGamma());
                        ConfigManager.setMinArea(preset.getMinArea());
                        ConfigManager.setMaxArea(preset.getMaxArea());
                        ConfigManager.setMinWidth(preset.getMinWidth());
                        ConfigManager.setMaxWidth(preset.getMaxWidth());
                        ConfigManager.setMinHeight(preset.getMinHeight());
                        ConfigManager.setMaxHeight(preset.getMaxHeight());
                        
                        // 同步字符相对位置
                        ConfigManager.setCharRelativePositionEnabled(preset.isCharRelativePositionEnabled());
                        ConfigManager.setCharRelativePositions(preset.getCharRelativePositionsJson());
                        
                        // 刷新OCR参数界面
                        ocrSettingsPanel.loadSettings();
                        log("已将预设的OCR参数和字符相对位置同步到OCR参数界面");
                    }
                    
                    // 切换到OCR识别测试标签页
                    JTabbedPane tabbedPane = mainFrame.getTabbedPane();
                    if (tabbedPane != null) {
                        int ocrTabIndex = -1;
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            if ("OCR识别测试".equals(tabbedPane.getTitleAt(i))) {
                                ocrTabIndex = i;
                                break;
                            }
                        }
                        if (ocrTabIndex >= 0) {
                            tabbedPane.setSelectedIndex(ocrTabIndex);
                            log("已切换到OCR识别测试界面");
                        }
                    }
                    
                    log("使用预设:" + preset.getPresetName() + ", " + preset.getCorrectString());
                    
                } else {
                    log("无法获取主窗口实例");
                    JOptionPane.showMessageDialog(this, 
                        "预设 \"" + preset.getPresetName() + "\" 已准备好使用\n" +
                        "字符串: " + preset.getCorrectString(), 
                        "使用预设", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            log("使用预设失败: " + e.getMessage());
        }
    }
    
    /**
     * 编辑选中的预设
     */
    private void editSelectedPreset() {
        int row = presetTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个预设", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            StringPreset preset = getPresetById(id);
            if (preset != null) {
                currentPreset = preset;
                isEditing = true;
                showEditDialog(preset);
            }
        } catch (Exception e) {
            log("加载预设失败: " + e.getMessage());
        }
    }
    
    /**
     * 新增预设
     */
    private void addNewPreset() {
        currentPreset = new StringPreset();
        currentPreset.setPresetName("新预设");
        isEditing = false;
        showEditDialog(currentPreset);
    }
    
    /**
     * 保存当前预设
     * @return 是否保存成功
     */
    private boolean saveCurrentPreset() {
        if (currentPreset == null) {
            JOptionPane.showMessageDialog(this, "没有正在编辑的预设", "提示", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 从全局数据交换获取数据库服务
        DatabaseService currentDbService = getDbService();
        if (currentDbService == null) {
            log("数据库服务未初始化，请等待数据库初始化完成");
            JOptionPane.showMessageDialog(this, "数据库服务未初始化，请等待数据库初始化完成", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 验证必填字段
        String presetName = nameField.getText().trim();
        if (presetName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "预设名称不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 从编辑组件读取值
        currentPreset.setPresetName(presetName);
        currentPreset.setCorrectString(stringArea.getText());
        currentPreset.setStandardImage(currentImageData);
        
        currentPreset.setGrayscale((Integer) grayscaleSpinner.getValue());
        currentPreset.setGamma((Double) gammaSpinner.getValue());
        currentPreset.setMinArea((Integer) minAreaSpinner.getValue());
        currentPreset.setMaxArea((Integer) maxAreaSpinner.getValue());
        currentPreset.setMinWidth((Integer) minWidthSpinner.getValue());
        currentPreset.setMaxWidth((Integer) maxWidthSpinner.getValue());
        currentPreset.setMinHeight((Integer) minHeightSpinner.getValue());
        currentPreset.setMaxHeight((Integer) maxHeightSpinner.getValue());
        
        // 安全地读取脚本值（如果用户没有打开脚本标签页，可能为null）
        if (textLocationScriptArea != null) {
            currentPreset.setTextLocationScript(textLocationScriptArea.getText());
        }
        if (charClassifyScriptArea != null) {
            currentPreset.setCharClassifyScript(charClassifyScriptArea.getText());
        }
        if (ocrPostProcessScriptArea != null) {
            currentPreset.setOcrPostProcessScript(ocrPostProcessScriptArea.getText());
        }
        
        if (textLocationScriptEnabled != null) {
            currentPreset.setTextLocationScriptEnabled(textLocationScriptEnabled.isSelected());
        }
        if (charClassifyScriptEnabled != null) {
            currentPreset.setCharClassifyScriptEnabled(charClassifyScriptEnabled.isSelected());
        }
        if (ocrPostProcessScriptEnabled != null) {
            currentPreset.setOcrPostProcessScriptEnabled(ocrPostProcessScriptEnabled.isSelected());
        }
        
        // 保存字符相对位置信息
        if (charRelativePositionEnabled != null) {
            currentPreset.setCharRelativePositionEnabled(charRelativePositionEnabled.isSelected());
        }
        if (charPosTable != null) {
            currentPreset.setCharRelativePositions(readCharPositionsFromTable());
        }
        
        try {
            if (isEditing) {
                if ("xml".equals(storageMode)) {
                    xmlStorage.updatePreset(currentPreset);
                } else {
                    currentDbService.updateStringPreset(currentPreset);
                }
                log("已更新预设: " + currentPreset.getPresetName());
            } else {
                int newId;
                if ("xml".equals(storageMode)) {
                    newId = xmlStorage.addPreset(currentPreset);
                } else {
                    newId = currentDbService.addStringPreset(currentPreset);
                }
                currentPreset.setId(newId);
                log("已新增预设: " + currentPreset.getPresetName() + " (ID=" + newId + ")");
            }
            loadPresets();
            return true;
        } catch (Exception e) {
            log("保存预设失败: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "保存预设失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * 删除选中的预设
     */
    private void deleteSelectedPreset() {
        int row = presetTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一个预设", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要删除预设 \"" + name + "\" 吗？", 
            "确认删除", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                if ("xml".equals(storageMode)) {
                    xmlStorage.deletePreset(id);
                } else {
                    DatabaseService currentDbService = getDbService();
                    if (currentDbService == null) {
                        throw new Exception("数据库服务未初始化");
                    }
                    currentDbService.deleteStringPreset(id);
                }
                log("已删除预设: " + name);
                loadPresets();
            } catch (Exception e) {
                log("删除预设失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 显示编辑对话框
     */
    private void showEditDialog(StringPreset preset) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            isEditing ? "编辑预设" : "新增预设", true);
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 基本信息面板
        JPanel basicPanel = createBasicInfoPanel(preset);
        panel.add(basicPanel, BorderLayout.NORTH);
        
        // OCR参数和脚本面板
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DarkBlueTheme.getBackgroundColor());
        tabbedPane.setForeground(DarkBlueTheme.getTextColor());
        
        tabbedPane.addTab("OCR参数", createOcrParamsPanel(preset));
        tabbedPane.addTab("字符相对位置", createCharRelativePositionPanel(preset));
        tabbedPane.addTab("文本定位脚本", createScriptPanel(preset.getTextLocationScript(), 
            preset.isTextLocationScriptEnabled(), textLocationScriptArea, textLocationScriptEnabled, 0));
        tabbedPane.addTab("字符分类脚本", createScriptPanel(preset.getCharClassifyScript(), 
            preset.isCharClassifyScriptEnabled(), charClassifyScriptArea, charClassifyScriptEnabled, 1));
        tabbedPane.addTab("OCR后处理脚本", createScriptPanel(preset.getOcrPostProcessScript(), 
            preset.isOcrPostProcessScriptEnabled(), ocrPostProcessScriptArea, ocrPostProcessScriptEnabled, 2));
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        // 保存按钮
        JButton saveBtn = createStyledButton("保存");
        saveBtn.addActionListener(e -> {
            if (saveCurrentPreset()) {
                dialog.dispose();
            }
        });
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        btnPanel.add(saveBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private JPanel createBasicInfoPanel(StringPreset preset) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER), 
            "基本信息", 0, 0, null, DarkBlueTheme.getTextColor()));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 序号（只读）
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createLabel("序号:"), gbc);
        gbc.gridx = 1;
        JTextField idField = new JTextField(10);
        idField.setText(String.valueOf(preset.getId()));
        idField.setEditable(false);
        idField.setBackground(DarkBlueTheme.PRIMARY_DARK);
        idField.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        panel.add(idField, gbc);
        
        // 预设名称
        gbc.gridx = 2;
        panel.add(createLabel("预设名称:"), gbc);
        gbc.gridx = 3;
        nameField = new JTextField(20);
        nameField.setText(preset.getPresetName());
        nameField.setBackground(DarkBlueTheme.PRIMARY_DARK);
        nameField.setForeground(DarkBlueTheme.getTextColor());
        panel.add(nameField, gbc);
        
        // 字符串
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(createLabel("字符串:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        stringArea = new JTextArea(3, 40);
        stringArea.setText(preset.getCorrectString());
        stringArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
        stringArea.setForeground(DarkBlueTheme.getTextColor());
        stringArea.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(stringArea);
        panel.add(scroll, gbc);
        
        // 标准图片
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(createLabel("标准图片:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        imageLabel = new JLabel("无图片");
        imageLabel.setForeground(DarkBlueTheme.getTextColor());
        if (preset.getStandardImage() != null) {
            imageLabel.setText("已设置 (" + preset.getStandardImage().length + " bytes)");
        }
        panel.add(imageLabel, gbc);
        gbc.gridx = 3; gbc.gridwidth = 1;
        JButton browseBtn = createStyledButton("浏览...");
        browseBtn.addActionListener(e -> browseImage());
        panel.add(browseBtn, gbc);
        
        currentImageData = preset.getStandardImage();
        
        return panel;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(DarkBlueTheme.getTextColor());
        return label;
    }
    
    private JPanel createOcrParamsPanel(StringPreset preset) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 二值化阈值
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createLabel("二值化阈值:"), gbc);
        gbc.gridx = 1;
        grayscaleSpinner = new JSpinner(new SpinnerNumberModel(preset.getGrayscale(), 0, 255, 1));
        panel.add(grayscaleSpinner, gbc);
        
        // Gamma系数
        gbc.gridx = 2;
        panel.add(createLabel("Gamma系数:"), gbc);
        gbc.gridx = 3;
        gammaSpinner = new JSpinner(new SpinnerNumberModel(preset.getGamma(), 0.1, 2.0, 0.1));
        panel.add(gammaSpinner, gbc);
        
        // 面积范围
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createLabel("最小面积:"), gbc);
        gbc.gridx = 1;
        minAreaSpinner = new JSpinner(new SpinnerNumberModel(preset.getMinArea(), 1, 1000000, 1));
        panel.add(minAreaSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("最大面积:"), gbc);
        gbc.gridx = 3;
        maxAreaSpinner = new JSpinner(new SpinnerNumberModel(preset.getMaxArea(), 1, 1000000, 1));
        panel.add(maxAreaSpinner, gbc);
        
        // 宽度范围
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createLabel("最小宽度:"), gbc);
        gbc.gridx = 1;
        minWidthSpinner = new JSpinner(new SpinnerNumberModel(preset.getMinWidth(), 1, 1000, 1));
        panel.add(minWidthSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("最大宽度:"), gbc);
        gbc.gridx = 3;
        maxWidthSpinner = new JSpinner(new SpinnerNumberModel(preset.getMaxWidth(), 1, 1000, 1));
        panel.add(maxWidthSpinner, gbc);
        
        // 高度范围
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createLabel("最小高度:"), gbc);
        gbc.gridx = 1;
        minHeightSpinner = new JSpinner(new SpinnerNumberModel(preset.getMinHeight(), 1, 1000, 1));
        panel.add(minHeightSpinner, gbc);
        gbc.gridx = 2;
        panel.add(createLabel("最大高度:"), gbc);
        gbc.gridx = 3;
        maxHeightSpinner = new JSpinner(new SpinnerNumberModel(preset.getMaxHeight(), 1, 1000, 1));
        panel.add(maxHeightSpinner, gbc);
        
        return panel;
    }
    
    /**
     * 创建字符相对位置面板
     */
    private JPanel createCharRelativePositionPanel(StringPreset preset) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 顶部：启用复选框和按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        charRelativePositionEnabled = new JCheckBox("启用字符相对位置定位", preset.isCharRelativePositionEnabled());
        charRelativePositionEnabled.setForeground(DarkBlueTheme.getTextColor());
        charRelativePositionEnabled.setBackground(DarkBlueTheme.getBackgroundColor());
        topPanel.add(charRelativePositionEnabled);
        
        JButton generateBtn = createStyledButton("从字符串生成");
        generateBtn.addActionListener(e -> generateCharPositionsFromString());
        topPanel.add(generateBtn);
        
        JButton autoCalcBtn = createStyledButton("自动计算位置");
        autoCalcBtn.setToolTipText("根据当前OCR识别结果自动计算相对位置");
        autoCalcBtn.addActionListener(e -> autoCalculateCharPositions());
        topPanel.add(autoCalcBtn);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        // 中间：字符位置表格
        String[] columnNames = {"索引", "字符", "类型", "相对X", "相对Y", "宽度", "高度", "间距"};
        charPosTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 3; // 只有数值列可编辑
            }
        };
        
        charPosTable = new JTable(charPosTableModel);
        charPosTable.setBackground(DarkBlueTheme.PRIMARY_DARK);
        charPosTable.setForeground(DarkBlueTheme.getTextColor());
        charPosTable.setSelectionBackground(DarkBlueTheme.ACCENT);
        charPosTable.setSelectionForeground(DarkBlueTheme.getTextColor());
        charPosTable.setGridColor(DarkBlueTheme.BORDER);
        
        // 设置列宽
        charPosTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        charPosTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        charPosTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        charPosTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        charPosTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        charPosTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        charPosTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        charPosTable.getColumnModel().getColumn(7).setPreferredWidth(60);
        
        // 设置渲染器
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBackground(DarkBlueTheme.PRIMARY_DARK);
        renderer.setForeground(DarkBlueTheme.getTextColor());
        for (int i = 0; i < charPosTable.getColumnCount(); i++) {
            charPosTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        
        JScrollPane scroll = new JScrollPane(charPosTable);
        scroll.setBackground(DarkBlueTheme.getBackgroundColor());
        panel.add(scroll, BorderLayout.CENTER);
        
        // 加载现有数据
        loadCharPositionsToTable(preset);
        
        // 底部：说明标签
        JLabel hintLabel = new JLabel("提示：根据首字符位置和其他字符的相对偏移量定位所有字符");
        hintLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        hintLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(hintLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 加载字符位置到表格
     */
    private void loadCharPositionsToTable(StringPreset preset) {
        charPosTableModel.setRowCount(0);
        if (preset.hasCharRelativePositions()) {
            for (CharRelativePosition pos : preset.getCharRelativePositions()) {
                Object[] row = {
                    pos.getCharIndex(),
                    pos.getCharacter(),
                    pos.getCharTypeDisplayName(),
                    pos.getRelativeX(),
                    pos.getRelativeY(),
                    pos.getWidth(),
                    pos.getHeight(),
                    pos.getSpacing()
                };
                charPosTableModel.addRow(row);
            }
        }
    }
    
    /**
     * 从表格读取字符位置
     */
    private List<CharRelativePosition> readCharPositionsFromTable() {
        List<CharRelativePosition> positions = new ArrayList<>();
        for (int i = 0; i < charPosTableModel.getRowCount(); i++) {
            CharRelativePosition pos = new CharRelativePosition();
            pos.setCharIndex((Integer) charPosTableModel.getValueAt(i, 0));
            pos.setCharacter((String) charPosTableModel.getValueAt(i, 1));
            // 字符类型从字符串反查
            String typeName = (String) charPosTableModel.getValueAt(i, 2);
            pos.setCharType(CharRelativePosition.detectCharType(pos.getCharacter()));
            pos.setRelativeX((Integer) charPosTableModel.getValueAt(i, 3));
            pos.setRelativeY((Integer) charPosTableModel.getValueAt(i, 4));
            pos.setWidth((Integer) charPosTableModel.getValueAt(i, 5));
            pos.setHeight((Integer) charPosTableModel.getValueAt(i, 6));
            pos.setSpacing((Integer) charPosTableModel.getValueAt(i, 7));
            positions.add(pos);
        }
        return positions;
    }
    
    /**
     * 从字符串生成字符位置
     */
    private void generateCharPositionsFromString() {
        String correctString = stringArea.getText();
        if (correctString == null || correctString.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先输入字符串", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        currentPreset.generateDefaultCharRelativePositions(correctString);
        loadCharPositionsToTable(currentPreset);
        log("已根据字符串生成 " + currentPreset.getCharRelativePositions().size() + " 个字符位置");
    }
    
    /**
     * 自动计算字符位置（从OCR识别结果）
     * 这是一个占位方法，实际计算需要与OcrTestPanel交互
     */
    private void autoCalculateCharPositions() {
        JOptionPane.showMessageDialog(this, 
            "请在OCR识别测试界面运行识别后，使用\"保存到预设\"功能自动计算字符位置", 
            "提示", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private JPanel createScriptPanel(String script, boolean enabled, 
                                     RSyntaxTextArea scriptArea, JCheckBox enabledCheck,
                                     int scriptType) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 启用复选框
        JCheckBox checkBox = new JCheckBox("启用脚本", enabled);
        checkBox.setForeground(DarkBlueTheme.getTextColor());
        checkBox.setBackground(DarkBlueTheme.getBackgroundColor());
        panel.add(checkBox, BorderLayout.NORTH);
        
        // 脚本编辑区
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setText(script != null ? script : "");
        textArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
        textArea.setForeground(DarkBlueTheme.getTextColor());
        textArea.setCaretColor(DarkBlueTheme.getTextColor());
        
        RTextScrollPane scroll = new RTextScrollPane(textArea);
        scroll.setLineNumbersEnabled(true);
        panel.add(scroll, BorderLayout.CENTER);
        
        // 保存引用到类字段 (0=文本定位, 1=字符分类, 2=OCR后处理)
        switch (scriptType) {
            case 0:
                this.textLocationScriptArea = textArea;
                this.textLocationScriptEnabled = checkBox;
                break;
            case 1:
                this.charClassifyScriptArea = textArea;
                this.charClassifyScriptEnabled = checkBox;
                break;
            case 2:
                this.ocrPostProcessScriptArea = textArea;
                this.ocrPostProcessScriptEnabled = checkBox;
                break;
        }
        
        return panel;
    }
    
    private void browseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    currentImageData = baos.toByteArray();
                    imageLabel.setText("已选择: " + file.getName() + " (" + currentImageData.length + " bytes)");
                }
            } catch (Exception e) {
                log("加载图片失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 导出为XML
     */
    private void exportToXml() {
        if ("database".equals(storageMode) && getDbService() == null) return;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("XML文件", "xml"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".xml")) {
                file = new File(file.getAbsolutePath() + ".xml");
            }
            
            try {
                List<StringPreset> presets;
                if ("xml".equals(storageMode)) {
                    presets = xmlStorage.getAllPresets();
                } else {
                    DatabaseService currentDbService = getDbService();
                    if (currentDbService == null) {
                        throw new Exception("数据库服务未初始化");
                    }
                    presets = currentDbService.getAllStringPresets();
                }
                
                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                XMLStreamWriter writer = factory.createXMLStreamWriter(new FileWriter(file));
                
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("StringPresets");
                
                for (StringPreset preset : presets) {
                    writer.writeStartElement("Preset");
                    writer.writeAttribute("id", String.valueOf(preset.getId()));
                    
                    writeXmlElement(writer, "PresetName", preset.getPresetName());
                    writeXmlElement(writer, "CorrectString", preset.getCorrectString());
                    
                    // 导出标准图片的base64编码
                    String imageBase64 = preset.getStandardImageBase64();
                    writeXmlElement(writer, "StandardImageBase64", imageBase64 != null ? imageBase64 : "");
                    
                    writeXmlElement(writer, "Grayscale", String.valueOf(preset.getGrayscale()));
                    writeXmlElement(writer, "Gamma", String.valueOf(preset.getGamma()));
                    writeXmlElement(writer, "MinArea", String.valueOf(preset.getMinArea()));
                    writeXmlElement(writer, "MaxArea", String.valueOf(preset.getMaxArea()));
                    writeXmlElement(writer, "MinWidth", String.valueOf(preset.getMinWidth()));
                    writeXmlElement(writer, "MaxWidth", String.valueOf(preset.getMaxWidth()));
                    writeXmlElement(writer, "MinHeight", String.valueOf(preset.getMinHeight()));
                    writeXmlElement(writer, "MaxHeight", String.valueOf(preset.getMaxHeight()));
                    writeXmlElement(writer, "TextLocationScript", preset.getTextLocationScript());
                    writeXmlElement(writer, "CharClassifyScript", preset.getCharClassifyScript());
                    writeXmlElement(writer, "OcrPostProcessScript", preset.getOcrPostProcessScript());
                    writeXmlElement(writer, "TextLocationScriptEnabled", String.valueOf(preset.isTextLocationScriptEnabled()));
                    writeXmlElement(writer, "CharClassifyScriptEnabled", String.valueOf(preset.isCharClassifyScriptEnabled()));
                    writeXmlElement(writer, "OcrPostProcessScriptEnabled", String.valueOf(preset.isOcrPostProcessScriptEnabled()));
                    
                    writer.writeEndElement(); // Preset
                }
                
                writer.writeEndElement(); // StringPresets
                writer.writeEndDocument();
                writer.close();
                
                log("已导出 " + presets.size() + " 个预设到: " + file.getAbsolutePath());
            } catch (Exception e) {
                log("导出失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void writeXmlElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(value != null ? value : "");
        writer.writeEndElement();
    }
    
    /**
     * 从XML导入
     */
    private void importFromXml() {
        if ("database".equals(storageMode) && dbService == null) return;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("XML文件", "xml"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            
            try {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(file));
                
                StringPreset currentPreset = null;
                String currentElement = null;
                int count = 0;
                
                while (reader.hasNext()) {
                    int event = reader.next();
                    
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT:
                            String elementName = reader.getLocalName();
                            if ("Preset".equals(elementName)) {
                                currentPreset = new StringPreset();
                            } else if (currentPreset != null) {
                                currentElement = elementName;
                            }
                            break;
                            
                        case XMLStreamConstants.CHARACTERS:
                            if (currentPreset != null && currentElement != null) {
                                String text = reader.getText().trim();
                                if (!text.isEmpty()) {
                                    switch (currentElement) {
                                        case "PresetName": currentPreset.setPresetName(text); break;
                                        case "CorrectString": currentPreset.setCorrectString(text); break;
                                        case "StandardImageBase64": 
                                            if (!text.isEmpty()) {
                                                currentPreset.setStandardImageFromBase64(text);
                                            }
                                            break;
                                        case "Grayscale": currentPreset.setGrayscale(Integer.parseInt(text)); break;
                                        case "Gamma": currentPreset.setGamma(Double.parseDouble(text)); break;
                                        case "MinArea": currentPreset.setMinArea(Integer.parseInt(text)); break;
                                        case "MaxArea": currentPreset.setMaxArea(Integer.parseInt(text)); break;
                                        case "MinWidth": currentPreset.setMinWidth(Integer.parseInt(text)); break;
                                        case "MaxWidth": currentPreset.setMaxWidth(Integer.parseInt(text)); break;
                                        case "MinHeight": currentPreset.setMinHeight(Integer.parseInt(text)); break;
                                        case "MaxHeight": currentPreset.setMaxHeight(Integer.parseInt(text)); break;
                                        case "TextLocationScript": currentPreset.setTextLocationScript(text); break;
                                        case "CharClassifyScript": currentPreset.setCharClassifyScript(text); break;
                                        case "OcrPostProcessScript": currentPreset.setOcrPostProcessScript(text); break;
                                        case "TextLocationScriptEnabled": currentPreset.setTextLocationScriptEnabled(Boolean.parseBoolean(text)); break;
                                        case "CharClassifyScriptEnabled": currentPreset.setCharClassifyScriptEnabled(Boolean.parseBoolean(text)); break;
                                        case "OcrPostProcessScriptEnabled": currentPreset.setOcrPostProcessScriptEnabled(Boolean.parseBoolean(text)); break;
                                    }
                                }
                            }
                            break;
                            
                        case XMLStreamConstants.END_ELEMENT:
                            if ("Preset".equals(reader.getLocalName()) && currentPreset != null) {
                                if ("xml".equals(storageMode)) {
                                    xmlStorage.addPreset(currentPreset);
                                } else {
                                    dbService.addStringPreset(currentPreset);
                                }
                                count++;
                                currentPreset = null;
                            }
                            currentElement = null;
                            break;
                    }
                }
                
                reader.close();
                log("已从 " + file.getAbsolutePath() + " 导入 " + count + " 个预设");
                loadPresets();
            } catch (Exception e) {
                log("导入失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
