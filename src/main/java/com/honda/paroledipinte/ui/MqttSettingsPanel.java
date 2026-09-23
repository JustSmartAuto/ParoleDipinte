package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * MQTT设置面板
 */
public class MqttSettingsPanel extends JPanel {
    
    private JTextField brokerField;
    private JTextField ocrTopicField;
    private JTextField classifyTopicField;
    private JTextField binarizeTopicField;
    private JCheckBox autoStartCheckBox;
    private JCheckBox returnImageDataCheckBox;
    
    private Runnable onSettingsChanged;
    
    public MqttSettingsPanel(Runnable onSettingsChanged) {
        LogUtil.log("[MqttSettingsPanel] 开始执行");
        this.onSettingsChanged = onSettingsChanged;
        initComponents();
        loadSettings();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        
        // Broker地址
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("MQTT Broker:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        brokerField = new JTextField(30);
        brokerField.setToolTipText("格式: tcp://host:port");
        brokerField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(brokerField, gbc);
        
        // OCR主题
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("经典OCR主题:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        ocrTopicField = new JTextField(30);
        ocrTopicField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(ocrTopicField, gbc);
        
        // 分类主题
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("字符分类主题:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        classifyTopicField = new JTextField(30);
        classifyTopicField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(classifyTopicField, gbc);
        
        // 二值化主题
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("图像二值化主题:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        binarizeTopicField = new JTextField(30);
        binarizeTopicField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(binarizeTopicField, gbc);
        
        // 自动启动
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("启动设置:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        autoStartCheckBox = new JCheckBox("启动时自动连接MQTT");
        autoStartCheckBox.addActionListener(e -> saveSettings());
        formPanel.add(autoStartCheckBox, gbc);
        
        // 返回imageData字段设置
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("响应设置:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        returnImageDataCheckBox = new JCheckBox("OCR响应返回imageData字段（增加带宽占用）");
        returnImageDataCheckBox.setToolTipText("启用后OCR响应将包含可视化结果图片的base64编码，会显著增加响应大小");
        returnImageDataCheckBox.addActionListener(e -> saveSettings());
        formPanel.add(returnImageDataCheckBox, gbc);
        
        // 说明文本
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JTextArea infoArea = new JTextArea(
            "MQTT主题说明:\n" +
            "- 经典OCR: 接收OCR识别请求，返回识别结果\n" +
            "- 字符分类: 接收单字符分类请求，返回相似度分数\n" +
            "- 图像二值化: 接收图像二值化请求，返回二值化结果\n\n" +
            "响应设置说明:\n" +
            "- imageData字段: 包含可视化结果图片的base64编码，会显著增加响应大小\n\n" +
            "修改设置后需要重启MQTT服务才能生效"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(getBackground());
        infoArea.setFont(infoArea.getFont().deriveFont(Font.PLAIN));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        formPanel.add(infoArea, gbc);
        
        add(formPanel, BorderLayout.NORTH);
        
        // 添加保存按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("保存设置");
        saveButton.addActionListener(e -> {
            saveSettings();
            JOptionPane.showMessageDialog(this, "设置已保存\n修改将在重启MQTT服务后生效", "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(saveButton);
        
        JButton resetButton = new JButton("恢复默认");
        resetButton.addActionListener(e -> resetToDefaults());
        buttonPanel.add(resetButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadSettings() {
        LogUtil.log("[loadSettings] 开始执行");
        brokerField.setText(ConfigManager.getBroker());
        ocrTopicField.setText(ConfigManager.getOcrTopic());
        classifyTopicField.setText(ConfigManager.getClassifyTopic());
        binarizeTopicField.setText(ConfigManager.getBinarizeTopic());
        autoStartCheckBox.setSelected(ConfigManager.isAutoStart());
        returnImageDataCheckBox.setSelected(ConfigManager.isMqttReturnImageData());
    }
    
    private void saveSettings() {
        LogUtil.log("[saveSettings] 开始执行");
        ConfigManager.setBroker(brokerField.getText().trim());
        ConfigManager.setOcrTopic(ocrTopicField.getText().trim());
        ConfigManager.setClassifyTopic(classifyTopicField.getText().trim());
        ConfigManager.setBinarizeTopic(binarizeTopicField.getText().trim());
        ConfigManager.setAutoStart(autoStartCheckBox.isSelected());
        ConfigManager.setMqttReturnImageData(returnImageDataCheckBox.isSelected());
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
            brokerField.setText("tcp://127.0.0.1:8907");
            ocrTopicField.setText("/ampinspection/ocr/260407/raw");
            classifyTopicField.setText("/ampinspection/classify/260407/raw");
            binarizeTopicField.setText("/ampinspection/binarize/260407/raw");
            autoStartCheckBox.setSelected(true);
            returnImageDataCheckBox.setSelected(false);
            saveSettings();
        }
    }
}
