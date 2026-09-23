package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.model.CharRelativePosition;
import com.honda.paroledipinte.model.StringPreset;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OCR参数设置面板
 */
public class OcrSettingsPanel extends JPanel {
    
    private JTextField serviceField;
    private JSlider grayscaleSlider;
    private JLabel grayscaleLabel;
    private JSlider gammaSlider;
    private JLabel gammaLabel;
    private JSpinner minAreaSpinner;
    private JSpinner maxAreaSpinner;
    private JSpinner minWidthSpinner;
    private JSpinner minHeightSpinner;
    private JSpinner maxWidthSpinner;
    private JSpinner maxHeightSpinner;
    private JSpinner minRowGapSpinner;
    private JSpinner maxRowGapSpinner;
    private JSpinner minColGapSpinner;
    private JSpinner maxColGapSpinner;
    
    // 新增功能控件
    private JCheckBox forceWidthSplitCheckBox;
    private JSpinner forceSplitWidthSpinner;
    private JCheckBox multiLineSortCheckBox;
    private JSpinner rowGapThresholdSpinner;
    
    // 字符内部空隙参数
    private JSpinner maxInternalVerticalGapSpinner;
    private JSpinner maxInternalHorizontalGapSpinner;
    
    // 忽略边界区域参数
    private JSpinner ignoreBorderWidthSpinner;
    
    // 字符相对位置编辑组件
    private JCheckBox charRelativePositionEnabled;
    private DefaultTableModel charPosTableModel;
    private JTable charPosTable;
    private JTextArea correctStringArea;
    
    private Runnable onSettingsChanged;
    private AtomicBoolean isLoading = new AtomicBoolean(false);
    
    public OcrSettingsPanel(Runnable onSettingsChanged) {
        LogUtil.log("[OcrSettingsPanel] 开始执行");
        this.onSettingsChanged = onSettingsChanged;
        initComponents();
        loadSettings();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建主分割面板：上方OCR参数表单，下方字符相对位置
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        
        // 上半部分：OCR参数表单（带滚动条）
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        
        // 服务地址
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("OCR服务地址:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        serviceField = new JTextField(30);
        formPanel.add(serviceField, gbc);
        
        // 二值化阈值
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("二值化阈值:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel grayscalePanel = new JPanel(new BorderLayout(5, 0));
        grayscaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
        grayscaleLabel = new JLabel("128", JLabel.CENTER);
        grayscaleLabel.setPreferredSize(new Dimension(40, 20));
        grayscaleSlider.addChangeListener(e -> {
            grayscaleLabel.setText(String.valueOf(grayscaleSlider.getValue()));
        });
        grayscalePanel.add(grayscaleSlider, BorderLayout.CENTER);
        grayscalePanel.add(grayscaleLabel, BorderLayout.EAST);
        formPanel.add(grayscalePanel, gbc);
        
        // Gamma系数
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Gamma系数:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel gammaPanel = new JPanel(new BorderLayout(5, 0));
        gammaSlider = new JSlider(JSlider.HORIZONTAL, 10, 200, 50);
        gammaLabel = new JLabel("0.50", JLabel.CENTER);
        gammaLabel.setPreferredSize(new Dimension(50, 20));
        gammaSlider.addChangeListener(e -> {
            double gamma = gammaSlider.getValue() / 100.0;
            gammaLabel.setText(String.format("%.2f", gamma));
        });
        gammaPanel.add(gammaSlider, BorderLayout.CENTER);
        gammaPanel.add(gammaLabel, BorderLayout.EAST);
        formPanel.add(gammaPanel, gbc);
        
        // 面积参数
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("最小字符面积:", minAreaSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 10000, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(createSpinnerPanel("最大字符面积:", maxAreaSpinner = new JSpinner(new SpinnerNumberModel(7000, 1, 1000000, 10))), gbc);
        
        // 宽度参数
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("最小字符宽度:", minWidthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 1000, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(createSpinnerPanel("最大字符宽度:", maxWidthSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 2000, 1))), gbc);
        
        // 高度参数
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("最小字符高度:", minHeightSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 1000, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(createSpinnerPanel("最大字符高度:", maxHeightSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 2000, 1))), gbc);
        
        // 行间距参数（水平方向字符间距）
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("行间距最小值:", minRowGapSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(createSpinnerPanel("行间距最大值:", maxRowGapSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 500, 1))), gbc);
        
        // 列间距参数（垂直方向字符间距）
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("列间距最小值:", minColGapSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(createSpinnerPanel("列间距最大值:", maxColGapSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 500, 1))), gbc);
        
        // 字符内部空隙参数
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("字符内部垂直空隙最大值:", maxInternalVerticalGapSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 500, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        formPanel.add(createSpinnerPanel("字符内部水平空隙最大值:", maxInternalHorizontalGapSpinner = new JSpinner(new SpinnerNumberModel(20, 0, 1000, 1))), gbc);
        
        // 忽略边界区域参数
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(createSpinnerPanel("忽略图片边界区域(像素):", ignoreBorderWidthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1))), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        
        // 分隔线
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 10, 5);
        formPanel.add(new JSeparator(), gbc);
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 1;
        
        // 强制宽度分割
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("强制宽度分割:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        forceWidthSplitCheckBox = new JCheckBox("启用强制宽度分割");
        formPanel.add(forceWidthSplitCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("分割宽度:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        forceSplitWidthSpinner = new JSpinner(new SpinnerNumberModel(31, 5, 200, 1));
        formPanel.add(forceSplitWidthSpinner, gbc);
        
        // 多行排序
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("多行排序:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        multiLineSortCheckBox = new JCheckBox("启用多行排序");
        formPanel.add(multiLineSortCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("行间距阈值:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        rowGapThresholdSpinner = new JSpinner(new SpinnerNumberModel(20, 5, 100, 1));
        formPanel.add(rowGapThresholdSpinner, gbc);
        
        // 添加说明标签
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel noteLabel = new JLabel("说明: 以上参数为默认值，实际处理时会优先使用MQTT请求中的参数");
        noteLabel.setForeground(Color.GRAY);
        noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC));
        formPanel.add(noteLabel, gbc);
        
        formWrapper.add(formPanel, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("保存设置");
        saveButton.addActionListener(e -> {
            saveSettings();
            JOptionPane.showMessageDialog(this, "设置已保存", "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(saveButton);
        
        JButton resetButton = new JButton("恢复默认");
        resetButton.addActionListener(e -> resetToDefaults());
        buttonPanel.add(resetButton);
        
        formWrapper.add(buttonPanel, BorderLayout.SOUTH);
        
        JScrollPane topScrollPane = new JScrollPane(formWrapper);
        topScrollPane.setBorder(BorderFactory.createEmptyBorder());
        topScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        topPanel.add(topScrollPane, BorderLayout.CENTER);
        
        splitPane.setTopComponent(topPanel);
        
        // 下半部分：字符相对位置面板
        JPanel charPosPanel = createCharRelativePositionPanel();
        splitPane.setBottomComponent(charPosPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    /**
     * 创建字符相对位置面板
     */
    private JPanel createCharRelativePositionPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "字符相对位置", 0, 0, null, DarkBlueTheme.getTextColor()
        ));
        
        // 顶部：启用复选框、字符串输入和按钮
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        charRelativePositionEnabled = new JCheckBox("启用字符相对位置定位");
        charRelativePositionEnabled.setForeground(DarkBlueTheme.getTextColor());
        charRelativePositionEnabled.setBackground(DarkBlueTheme.getBackgroundColor());
        topPanel.add(charRelativePositionEnabled);
        
        topPanel.add(new JLabel("字符串:"));
        correctStringArea = new JTextArea(1, 15);
        correctStringArea.setBackground(DarkBlueTheme.PRIMARY_DARK);
        correctStringArea.setForeground(DarkBlueTheme.getTextColor());
        correctStringArea.setCaretColor(DarkBlueTheme.getTextColor());
        correctStringArea.setLineWrap(true);
        correctStringArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        topPanel.add(new JScrollPane(correctStringArea));
        
        JButton generateBtn = new JButton("从字符串生成");
        generateBtn.addActionListener(e -> generateCharPositionsFromString());
        topPanel.add(generateBtn);
        
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
        
        // 底部：说明标签
        JLabel hintLabel = new JLabel("提示：根据首字符位置和其他字符的相对偏移量定位所有字符");
        hintLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        hintLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(hintLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSpinnerPanel(String label, JSpinner spinner) {
        LogUtil.log("[createSpinnerPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(spinner, BorderLayout.CENTER);
        return panel;
    }
    
    public void loadSettings() {
        LogUtil.log("[loadSettings] 开始执行");
        isLoading.set(true);
        serviceField.setText(ConfigManager.getService());
        
        // 加载二值化阈值并更新标签
        int grayscale = ConfigManager.getGrayscale();
        grayscaleSlider.setValue(grayscale);
        grayscaleLabel.setText(String.valueOf(grayscale));
        
        // 加载Gamma系数并更新标签
        double gamma = ConfigManager.getGamma();
        gammaSlider.setValue((int) (gamma * 100));
        gammaLabel.setText(String.format("%.2f", gamma));
        
        minAreaSpinner.setValue(ConfigManager.getMinArea());
        maxAreaSpinner.setValue(ConfigManager.getMaxArea());
        minWidthSpinner.setValue(ConfigManager.getMinWidth());
        minHeightSpinner.setValue(ConfigManager.getMinHeight());
        maxWidthSpinner.setValue(ConfigManager.getMaxWidth());
        maxHeightSpinner.setValue(ConfigManager.getMaxHeight());
        minRowGapSpinner.setValue(ConfigManager.getMinRowGap());
        maxRowGapSpinner.setValue(ConfigManager.getMaxRowGap());
        minColGapSpinner.setValue(ConfigManager.getMinColGap());
        maxColGapSpinner.setValue(ConfigManager.getMaxColGap());
        
        // 加载新功能设置
        forceWidthSplitCheckBox.setSelected(ConfigManager.isForceWidthSplitEnabled());
        forceSplitWidthSpinner.setValue(ConfigManager.getForceSplitWidth());
        multiLineSortCheckBox.setSelected(ConfigManager.isMultiLineSortEnabled());
        rowGapThresholdSpinner.setValue(ConfigManager.getRowGapThreshold());
        
        // 加载字符内部空隙参数
        maxInternalVerticalGapSpinner.setValue(ConfigManager.getMaxInternalVerticalGap());
        maxInternalHorizontalGapSpinner.setValue(ConfigManager.getMaxInternalHorizontalGap());
        
        // 加载忽略边界区域参数
        ignoreBorderWidthSpinner.setValue(ConfigManager.getIgnoreBorderWidth());
        
        // 加载字符相对位置设置
        charRelativePositionEnabled.setSelected(ConfigManager.isCharRelativePositionEnabled());
        loadCharPositionsFromConfig();
        
        isLoading.set(false);
        LogUtil.log("[loadSettings] 执行完成 - 已从配置文件加载OCR参数");
    }
    
    /**
     * 设置字符相对位置信息到界面（用于从字符串预设同步）
     * @param positions 字符相对位置列表
     * @param correctString 正确字符串
     * @param enabled 是否启用
     */
    public void setCharRelativePositions(List<CharRelativePosition> positions, String correctString, boolean enabled) {
        charRelativePositionEnabled.setSelected(enabled);
        if (correctString != null) {
            correctStringArea.setText(correctString);
        }
        charPosTableModel.setRowCount(0);
        if (positions != null) {
            for (CharRelativePosition pos : positions) {
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
    
    private void loadCharPositionsFromConfig() {
        charPosTableModel.setRowCount(0);
        String json = ConfigManager.getCharRelativePositions();
        if (json != null && !json.isEmpty()) {
            try {
                Gson gson = new Gson();
                List<CharRelativePosition> positions = gson.fromJson(json,
                    new TypeToken<List<CharRelativePosition>>(){}.getType());
                if (positions != null) {
                    for (CharRelativePosition pos : positions) {
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
            } catch (Exception e) {
                LogUtil.log("[loadCharPositionsFromConfig] 加载失败: " + e.getMessage());
            }
        }
    }
    
    private void saveSettings() {
        LogUtil.log("[saveSettings] 开始执行");
        if (isLoading.get()) {
            return;
        }
        ConfigManager.setService(serviceField.getText().trim());
        ConfigManager.setGrayscale(grayscaleSlider.getValue());
        ConfigManager.setGamma(gammaSlider.getValue() / 100.0);
        ConfigManager.setMinArea((Integer) minAreaSpinner.getValue());
        ConfigManager.setMaxArea((Integer) maxAreaSpinner.getValue());
        ConfigManager.setMinWidth((Integer) minWidthSpinner.getValue());
        ConfigManager.setMinHeight((Integer) minHeightSpinner.getValue());
        ConfigManager.setMaxWidth((Integer) maxWidthSpinner.getValue());
        ConfigManager.setMaxHeight((Integer) maxHeightSpinner.getValue());
        ConfigManager.setMinRowGap((Integer) minRowGapSpinner.getValue());
        ConfigManager.setMaxRowGap((Integer) maxRowGapSpinner.getValue());
        ConfigManager.setMinColGap((Integer) minColGapSpinner.getValue());
        ConfigManager.setMaxColGap((Integer) maxColGapSpinner.getValue());
        
        // 保存新功能设置
        ConfigManager.setForceWidthSplitEnabled(forceWidthSplitCheckBox.isSelected());
        ConfigManager.setForceSplitWidth((Integer) forceSplitWidthSpinner.getValue());
        ConfigManager.setMultiLineSortEnabled(multiLineSortCheckBox.isSelected());
        ConfigManager.setRowGapThreshold((Integer) rowGapThresholdSpinner.getValue());
        
        // 保存字符内部空隙参数
        ConfigManager.setMaxInternalVerticalGap((Integer) maxInternalVerticalGapSpinner.getValue());
        ConfigManager.setMaxInternalHorizontalGap((Integer) maxInternalHorizontalGapSpinner.getValue());
        
        // 保存忽略边界区域参数
        ConfigManager.setIgnoreBorderWidth((Integer) ignoreBorderWidthSpinner.getValue());
        
        // 保存字符相对位置设置
        ConfigManager.setCharRelativePositionEnabled(charRelativePositionEnabled.isSelected());
        String json = readCharPositionsToJson();
        ConfigManager.setCharRelativePositions(json);
        
        ConfigManager.saveConfig();
        
        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }
    
    private String readCharPositionsToJson() {
        List<CharRelativePosition> positions = new ArrayList<>();
        for (int i = 0; i < charPosTableModel.getRowCount(); i++) {
            CharRelativePosition pos = new CharRelativePosition();
            pos.setCharIndex((Integer) charPosTableModel.getValueAt(i, 0));
            pos.setCharacter((String) charPosTableModel.getValueAt(i, 1));
            pos.setCharType(CharRelativePosition.detectCharType(pos.getCharacter()));
            pos.setRelativeX((Integer) charPosTableModel.getValueAt(i, 3));
            pos.setRelativeY((Integer) charPosTableModel.getValueAt(i, 4));
            pos.setWidth((Integer) charPosTableModel.getValueAt(i, 5));
            pos.setHeight((Integer) charPosTableModel.getValueAt(i, 6));
            pos.setSpacing((Integer) charPosTableModel.getValueAt(i, 7));
            positions.add(pos);
        }
        if (positions.isEmpty()) {
            return "";
        }
        Gson gson = new Gson();
        return gson.toJson(positions);
    }
    
    /**
     * 从字符串生成字符位置
     */
    private void generateCharPositionsFromString() {
        String correctString = correctStringArea.getText();
        if (correctString == null || correctString.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先输入字符串", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        StringPreset tempPreset = new StringPreset();
        tempPreset.generateDefaultCharRelativePositions(correctString);
        
        charPosTableModel.setRowCount(0);
        for (CharRelativePosition pos : tempPreset.getCharRelativePositions()) {
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
        
        JOptionPane.showMessageDialog(this, 
            "已根据字符串生成 " + tempPreset.getCharRelativePositions().size() + " 个字符位置", 
            "提示", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void resetToDefaults() {
        LogUtil.log("[resetToDefaults] 开始执行");
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要恢复默认设置吗？", 
            "确认", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            serviceField.setText("");
            grayscaleSlider.setValue(128);
            gammaSlider.setValue(50);
            minAreaSpinner.setValue(15);
            maxAreaSpinner.setValue(100000);
            minWidthSpinner.setValue(3);
            minHeightSpinner.setValue(3);
            maxWidthSpinner.setValue(500);
            maxHeightSpinner.setValue(200);
            minRowGapSpinner.setValue(2);
            maxRowGapSpinner.setValue(50);
            minColGapSpinner.setValue(2);
            maxColGapSpinner.setValue(50);
            
            // 重置新功能设置
            forceWidthSplitCheckBox.setSelected(false);
            forceSplitWidthSpinner.setValue(31);
            multiLineSortCheckBox.setSelected(false);
            rowGapThresholdSpinner.setValue(20);
            
            // 重置字符内部空隙参数（默认 MaxHeight - MinHeight = 200-3=197, MaxWidth - MinWidth = 500-3=497）
            maxInternalVerticalGapSpinner.setValue(197);
            maxInternalHorizontalGapSpinner.setValue(497);
            
            // 重置忽略边界区域参数
            ignoreBorderWidthSpinner.setValue(5);
            
            // 重置字符相对位置
            charRelativePositionEnabled.setSelected(false);
            correctStringArea.setText("");
            charPosTableModel.setRowCount(0);
            
            saveSettings();
        }
    }
}
