package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;

/**
 * 路径设置面板
 */
public class PathSettingsPanel extends JPanel {
    
    private JTextField dataDirField;
    private JTextField imageDirField;
    private JTextField logDirField;
    
    private Runnable onSettingsChanged;
    
    public PathSettingsPanel(Runnable onSettingsChanged) {
        LogUtil.log("[PathSettingsPanel] 开始执行");
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
        
        // 数据库目录
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("数据库文件夹:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        dataDirField = new JTextField(30);
        dataDirField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(dataDirField, gbc);
        gbc.gridx = 2; gbc.gridy = row++; gbc.weightx = 0;
        JButton dataBrowseBtn = new JButton("浏览...");
        dataBrowseBtn.addActionListener(e -> browseDirectory(dataDirField));
        formPanel.add(dataBrowseBtn, gbc);
        
        // 图片目录
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("图片文件夹:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        imageDirField = new JTextField(30);
        imageDirField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(imageDirField, gbc);
        gbc.gridx = 2; gbc.gridy = row++; gbc.weightx = 0;
        JButton imageBrowseBtn = new JButton("浏览...");
        imageBrowseBtn.addActionListener(e -> browseDirectory(imageDirField));
        formPanel.add(imageBrowseBtn, gbc);
        
        // 日志目录
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("日志文件夹:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        logDirField = new JTextField(30);
        logDirField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
        LogUtil.log("[focusLost] 开始执行");
                saveSettings();
            }
        });
        formPanel.add(logDirField, gbc);
        gbc.gridx = 2; gbc.gridy = row++; gbc.weightx = 0;
        JButton logBrowseBtn = new JButton("浏览...");
        logBrowseBtn.addActionListener(e -> browseDirectory(logDirField));
        formPanel.add(logBrowseBtn, gbc);
        
        // 说明文本
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        JTextArea infoArea = new JTextArea(
            "路径说明:\n" +
            "- 数据库文件夹: 存储字符图片数据库(H2)\n" +
            "- 图片文件夹: 存储OCR识别的字符截图\n" +
            "- 日志文件夹: 存储软件运行日志\n\n" +
            "默认路径都在用户主目录下的 ampinspection 文件夹中"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(getBackground());
        infoArea.setFont(infoArea.getFont().deriveFont(Font.PLAIN));
        infoArea.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        formPanel.add(infoArea, gbc);
        
        add(formPanel, BorderLayout.NORTH);
        
        // 添加保存按钮
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
        
        JButton openDirButton = new JButton("打开数据目录");
        openDirButton.addActionListener(e -> openDataDirectory());
        buttonPanel.add(openDirButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void browseDirectory(JTextField textField) {
        LogUtil.log("[browseDirectory] 开始执行");
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择文件夹");
        
        String currentPath = textField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists()) {
                chooser.setCurrentDirectory(currentDir);
            }
        }
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            textField.setText(selectedDir.getAbsolutePath());
            saveSettings();
        }
    }
    
    private void openDataDirectory() {
        LogUtil.log("[openDataDirectory] 开始执行");
        try {
            String dataDir = ConfigManager.getDataDir();
            File dir = new File(dataDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Desktop.getDesktop().open(dir.getParentFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "无法打开目录: " + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadSettings() {
        LogUtil.log("[loadSettings] 开始执行");
        dataDirField.setText(ConfigManager.getDataDir());
        imageDirField.setText(ConfigManager.getImageDir());
        logDirField.setText(ConfigManager.getLogDir());
    }
    
    private void saveSettings() {
        LogUtil.log("[saveSettings] 开始执行");
        ConfigManager.setDataDir(dataDirField.getText().trim());
        ConfigManager.setImageDir(imageDirField.getText().trim());
        ConfigManager.setLogDir(logDirField.getText().trim());
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
            String home = System.getProperty("user.home");
            dataDirField.setText(home + "/ampinspection/data");
            imageDirField.setText(home + "/ampinspection/images");
            logDirField.setText(home + "/ampinspection/logs");
            saveSettings();
        }
    }
}
