package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * 软件设置面板
 */
public class SoftwareSettingsPanel extends JPanel {
    
    private JCheckBox autoStartMqttCheckBox;
    private JSpinner windowWidthSpinner;
    private JSpinner windowHeightSpinner;
    private JLabel aspectRatioLabel;
    
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 750;
    private static final double ASPECT_RATIO = 4.0 / 3.0;
    
    private Runnable onSettingsChanged;
    
    public SoftwareSettingsPanel(Runnable onSettingsChanged) {
        LogUtil.log("[SoftwareSettingsPanel] 开始执行");
        this.onSettingsChanged = onSettingsChanged;
        initComponents();
        loadSettings();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 创建表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // MQTT自动启动设置
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel mqttLabel = new JLabel("MQTT服务:");
        mqttLabel.setForeground(DarkBlueTheme.getTextColor());
        formPanel.add(mqttLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        autoStartMqttCheckBox = new JCheckBox("软件启动时自动启动MQTT服务");
        autoStartMqttCheckBox.setBackground(DarkBlueTheme.getBackgroundColor());
        autoStartMqttCheckBox.setForeground(DarkBlueTheme.getTextColor());
        autoStartMqttCheckBox.addActionListener(e -> saveSettings());
        formPanel.add(autoStartMqttCheckBox, gbc);
        
        // 分隔线
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 15, 10);
        JSeparator separator1 = new JSeparator();
        separator1.setForeground(DarkBlueTheme.BORDER);
        formPanel.add(separator1, gbc);
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 1;
        
        // 字符串预设存储方式设置
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel storageTitle = new JLabel("字符串预设存储方式");
        storageTitle.setFont(storageTitle.getFont().deriveFont(Font.BOLD, 14));
        storageTitle.setForeground(DarkBlueTheme.ACCENT);
        formPanel.add(storageTitle, gbc);
        gbc.gridwidth = 1;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel storageLabel = new JLabel("存储方式:");
        storageLabel.setForeground(DarkBlueTheme.getTextColor());
        formPanel.add(storageLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        String[] storageOptions = {"XML文件 (推荐)", "数据库"};
        JComboBox<String> storageComboBox = new JComboBox<>(storageOptions);
        storageComboBox.setBackground(DarkBlueTheme.PRIMARY_DARK);
        storageComboBox.setForeground(DarkBlueTheme.getTextColor());
        // 设置当前值
        String currentMode = ConfigManager.getStringPresetStorageMode();
        storageComboBox.setSelectedIndex("xml".equals(currentMode) ? 0 : 1);
        storageComboBox.addActionListener(e -> {
            String selectedMode = storageComboBox.getSelectedIndex() == 0 ? "xml" : "database";
            ConfigManager.setStringPresetStorageMode(selectedMode);
            saveSettings();
        });
        formPanel.add(storageComboBox, gbc);
        
        // 存储方式说明
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JTextArea storageInfoArea = new JTextArea(
            "• XML文件方式: 数据存储在XML文件中，无需数据库，适合单机使用\n" +
            "• 数据库方式: 数据存储在H2数据库中，支持更复杂的查询"
        );
        storageInfoArea.setEditable(false);
        storageInfoArea.setBackground(DarkBlueTheme.getBackgroundColor());
        storageInfoArea.setForeground(DarkBlueTheme.getSecondaryTextColor());
        storageInfoArea.setFont(storageInfoArea.getFont().deriveFont(Font.PLAIN, 11));
        formPanel.add(storageInfoArea, gbc);
        gbc.gridwidth = 1;
        
        // 分隔线
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 15, 10);
        JSeparator separator2 = new JSeparator();
        separator2.setForeground(DarkBlueTheme.BORDER);
        formPanel.add(separator2, gbc);
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 1;
        
        // 窗口尺寸设置标题
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel windowSizeTitle = new JLabel("窗口尺寸设置 (保持 4:3 比例)");
        windowSizeTitle.setFont(windowSizeTitle.getFont().deriveFont(Font.BOLD, 14));
        windowSizeTitle.setForeground(DarkBlueTheme.ACCENT);
        formPanel.add(windowSizeTitle, gbc);
        gbc.gridwidth = 1;
        
        // 窗口宽度
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel widthLabel = new JLabel("窗口宽度:");
        widthLabel.setForeground(DarkBlueTheme.getTextColor());
        formPanel.add(widthLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel widthPanel = new JPanel(new BorderLayout(10, 0));
        widthPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        windowWidthSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_WIDTH, 800, 1920, 10));
        windowWidthSpinner.setPreferredSize(new Dimension(120, 25));
        windowWidthSpinner.addChangeListener(new ChangeListener() {
            private boolean isAdjusting = false;
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isAdjusting) return;
                isAdjusting = true;
                updateHeightFromWidth();
                saveSettings();
                isAdjusting = false;
            }
        });
        widthPanel.add(windowWidthSpinner, BorderLayout.WEST);
        JLabel widthUnitLabel = new JLabel("像素");
        widthUnitLabel.setForeground(DarkBlueTheme.getSecondaryTextColor());
        widthPanel.add(widthUnitLabel, BorderLayout.CENTER);
        formPanel.add(widthPanel, gbc);
        
        // 窗口高度
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel heightLabel = new JLabel("窗口高度:");
        heightLabel.setForeground(DarkBlueTheme.getTextColor());
        formPanel.add(heightLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel heightPanel = new JPanel(new BorderLayout(10, 0));
        heightPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        windowHeightSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_HEIGHT, 600, 1440, 10));
        windowHeightSpinner.setPreferredSize(new Dimension(120, 25));
        windowHeightSpinner.addChangeListener(new ChangeListener() {
            private boolean isAdjusting = false;
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isAdjusting) return;
                isAdjusting = true;
                updateWidthFromHeight();
                saveSettings();
                isAdjusting = false;
            }
        });
        heightPanel.add(windowHeightSpinner, BorderLayout.WEST);
        JLabel heightUnitLabel = new JLabel("像素");
        heightUnitLabel.setForeground(DarkBlueTheme.getSecondaryTextColor());
        heightPanel.add(heightUnitLabel, BorderLayout.CENTER);
        formPanel.add(heightPanel, gbc);
        
        // 比例显示
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel ratioLabel = new JLabel("当前比例:");
        ratioLabel.setForeground(DarkBlueTheme.getTextColor());
        formPanel.add(ratioLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        aspectRatioLabel = new JLabel("4:3 (1.33:1)");
        aspectRatioLabel.setForeground(DarkBlueTheme.SUCCESS);
        aspectRatioLabel.setFont(aspectRatioLabel.getFont().deriveFont(Font.BOLD));
        formPanel.add(aspectRatioLabel, gbc);
        
        // 快速预设按钮
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel presetLabel = new JLabel("快速预设:");
        presetLabel.setForeground(DarkBlueTheme.getTextColor());
        formPanel.add(presetLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton smallBtn = createPresetButton("小 (800x600)", 800, 600);
        JButton defaultBtn = createPresetButton("默认 (1000x750)", 1000, 750);
        JButton largeBtn = createPresetButton("大 (1200x900)", 1200, 900);
        JButton xlargeBtn = createPresetButton("超大 (1600x1200)", 1600, 1200);
        
        presetPanel.add(smallBtn);
        presetPanel.add(defaultBtn);
        presetPanel.add(largeBtn);
        presetPanel.add(xlargeBtn);
        formPanel.add(presetPanel, gbc);
        
        // 说明文本
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        JTextArea infoArea = new JTextArea(
            "设置说明:\n" +
            "• MQTT自动启动: 软件启动时自动启动内置MQTT Broker服务\n" +
            "• 窗口尺寸: 设置软件启动时的窗口大小，保持4:3比例\n" +
            "• 修改窗口尺寸后，重启软件才能生效"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(DarkBlueTheme.getBackgroundColor());
        infoArea.setForeground(DarkBlueTheme.getSecondaryTextColor());
        infoArea.setFont(infoArea.getFont().deriveFont(Font.PLAIN, 12));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        formPanel.add(infoArea, gbc);
        
        add(formPanel, BorderLayout.NORTH);
        
        // 添加保存按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton saveButton = new JButton("保存设置");
        saveButton.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        saveButton.setForeground(DarkBlueTheme.getTextColor());
        saveButton.addActionListener(e -> {
            saveSettings();
            JOptionPane.showMessageDialog(this, "设置已保存\n窗口尺寸将在重启软件后生效", "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(saveButton);
        
        JButton resetButton = new JButton("恢复默认");
        resetButton.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        resetButton.setForeground(DarkBlueTheme.getTextColor());
        resetButton.addActionListener(e -> resetToDefaults());
        buttonPanel.add(resetButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JButton createPresetButton(String text, int width, int height) {
        JButton btn = new JButton(text);
        btn.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        btn.setForeground(DarkBlueTheme.getTextColor());
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 11));
        btn.addActionListener(e -> {
            windowWidthSpinner.setValue(width);
            windowHeightSpinner.setValue(height);
            updateAspectRatioLabel();
            saveSettings();
        });
        return btn;
    }
    
    private void updateHeightFromWidth() {
        int width = (Integer) windowWidthSpinner.getValue();
        int height = (int) Math.round(width / ASPECT_RATIO);
        // 确保高度在有效范围内
        height = Math.max(600, Math.min(1440, height));
        windowHeightSpinner.setValue(height);
        updateAspectRatioLabel();
    }
    
    private void updateWidthFromHeight() {
        int height = (Integer) windowHeightSpinner.getValue();
        int width = (int) Math.round(height * ASPECT_RATIO);
        // 确保宽度在有效范围内
        width = Math.max(800, Math.min(1920, width));
        windowWidthSpinner.setValue(width);
        updateAspectRatioLabel();
    }
    
    private void updateAspectRatioLabel() {
        int width = (Integer) windowWidthSpinner.getValue();
        int height = (Integer) windowHeightSpinner.getValue();
        double ratio = (double) width / height;
        aspectRatioLabel.setText(String.format("%d:%d (%.2f:1)", width, height, ratio));
    }
    
    private void loadSettings() {
        LogUtil.log("[loadSettings] 开始执行");
        autoStartMqttCheckBox.setSelected(ConfigManager.isAutoStartMqtt());
        windowWidthSpinner.setValue(ConfigManager.getWindowWidth());
        windowHeightSpinner.setValue(ConfigManager.getWindowHeight());
        updateAspectRatioLabel();
    }
    
    private void saveSettings() {
        LogUtil.log("[saveSettings] 开始执行");
        ConfigManager.setAutoStartMqtt(autoStartMqttCheckBox.isSelected());
        ConfigManager.setWindowWidth((Integer) windowWidthSpinner.getValue());
        ConfigManager.setWindowHeight((Integer) windowHeightSpinner.getValue());
        ConfigManager.saveConfig();
        
        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }
    
    private void resetToDefaults() {
        LogUtil.log("[resetToDefaults] 开始执行");
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要恢复默认设置吗？", 
            "确认", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            autoStartMqttCheckBox.setSelected(true);
            windowWidthSpinner.setValue(DEFAULT_WIDTH);
            windowHeightSpinner.setValue(DEFAULT_HEIGHT);
            updateAspectRatioLabel();
            saveSettings();
        }
    }
}
