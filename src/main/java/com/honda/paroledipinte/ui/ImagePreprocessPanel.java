package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.image.ImageProcessor;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import com.honda.paroledipinte.image.CharacterGenerator;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 图片预处理标签页
 * 支持拖入图片，调节图像二值化参数并查看图像二值化效果
 * 包含脚本子标签页，提供调试控制台，可以覆盖软件内部的二值化逻辑
 */
public class ImagePreprocessPanel extends JPanel {
    
    // 图像显示面板
    private JLabel originalImageLabel;
    private JLabel processedImageLabel;
    private JLabel infoLabel;
    
    // 参数控件
    private JSlider grayscaleSlider;
    private JLabel grayscaleValueLabel;
    private JSlider gammaSlider;
    private JLabel gammaValueLabel;
    private JSlider thresholdOffsetSlider;
    private JLabel thresholdOffsetValueLabel;
    private JCheckBox invertCheckBox;
    
    // 当前图像
    private BufferedImage currentImage;
    private Mat currentMat;
    
    // 脚本面板
    private PreprocessScriptPanel scriptPanel;
    
    public ImagePreprocessPanel() {
        LogUtil.log("[ImagePreprocessPanel] 开始执行");
        initComponents();
        setupDragAndDrop();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 创建主标签页
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(DarkBlueTheme.getBackgroundColor());
        tabbedPane.setForeground(DarkBlueTheme.getTextColor());
        
        // 预处理面板
        JPanel preprocessPanel = createPreprocessPanel();
        tabbedPane.addTab("图像二值化", preprocessPanel);
        
        // 脚本面板
        scriptPanel = new PreprocessScriptPanel();
        tabbedPane.addTab("脚本", scriptPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createPreprocessPanel() {
        LogUtil.log("[createPreprocessPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 左侧控制面板
        JPanel leftPanel = createControlPanel();
        panel.add(leftPanel, BorderLayout.WEST);
        
        // 右侧图像显示面板
        JPanel imagePanel = createImageDisplayPanel();
        panel.add(imagePanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        LogUtil.log("[createControlPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 顶部：拖放区域
        JPanel dropPanel = createDropPanel();
        panel.add(dropPanel, BorderLayout.NORTH);
        
        // 中部：参数设置
        JPanel paramPanel = createParamPanel();
        JScrollPane paramScrollPane = new JScrollPane(paramPanel);
        paramScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "二值化参数",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        paramScrollPane.setBackground(DarkBlueTheme.getBackgroundColor());
        paramScrollPane.getViewport().setBackground(DarkBlueTheme.getBackgroundColor());
        panel.add(paramScrollPane, BorderLayout.CENTER);
        
        // 底部：按钮
        JPanel buttonPanel = createButtonPanel();
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createDropPanel() {
        LogUtil.log("[createDropPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 拖放区域
        JPanel dropArea = new JPanel();
        dropArea.setLayout(new BorderLayout());
        dropArea.setBorder(BorderFactory.createDashedBorder(DarkBlueTheme.BORDER_LIGHT, 2, 5));
        dropArea.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        dropArea.setPreferredSize(new Dimension(0, 100));
        
        JLabel dropLabel = new JLabel("<html><center>[拖放图片到此处]<br>或点击选择文件</center></html>", JLabel.CENTER);
        dropLabel.setForeground(DarkBlueTheme.getTextColor());
        dropLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        dropArea.add(dropLabel, BorderLayout.CENTER);
        
        // 点击打开文件
        dropArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
        LogUtil.log("[mouseClicked] 开始执行");
                openImageFile();
            }
        });
        
        panel.add(dropArea, BorderLayout.CENTER);
        
        // 打开文件按钮
        JButton openBtn = createStyledButton("[打开图片]");
        openBtn.addActionListener(e -> openImageFile());
        panel.add(openBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createParamPanel() {
        LogUtil.log("[createParamPanel] 开始执行");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        
        // 二值化阈值
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel grayscaleTitle = new JLabel("二值化阈值:");
        grayscaleTitle.setForeground(DarkBlueTheme.getTextColor());
        panel.add(grayscaleTitle, gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel grayscalePanel = new JPanel(new BorderLayout(5, 0));
        grayscalePanel.setBackground(DarkBlueTheme.getBackgroundColor());
        grayscaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, ConfigManager.getGrayscale());
        grayscaleSlider.setBackground(DarkBlueTheme.getBackgroundColor());
        grayscaleValueLabel = new JLabel(String.valueOf(grayscaleSlider.getValue()), JLabel.CENTER);
        grayscaleValueLabel.setForeground(DarkBlueTheme.getTextColor());
        grayscaleValueLabel.setPreferredSize(new Dimension(40, 20));
        grayscaleSlider.addChangeListener(e -> {
            grayscaleValueLabel.setText(String.valueOf(grayscaleSlider.getValue()));
            if (!grayscaleSlider.getValueIsAdjusting()) {
                updateProcessedImage();
            }
        });
        grayscalePanel.add(grayscaleSlider, BorderLayout.CENTER);
        grayscalePanel.add(grayscaleValueLabel, BorderLayout.EAST);
        panel.add(grayscalePanel, gbc);
        
        // Gamma系数
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel gammaTitle = new JLabel("Gamma系数:");
        gammaTitle.setForeground(DarkBlueTheme.getTextColor());
        panel.add(gammaTitle, gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel gammaPanel = new JPanel(new BorderLayout(5, 0));
        gammaPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        gammaSlider = new JSlider(JSlider.HORIZONTAL, 10, 200, (int)(ConfigManager.getGamma() * 100));
        gammaSlider.setBackground(DarkBlueTheme.getBackgroundColor());
        gammaValueLabel = new JLabel(String.format("%.2f", gammaSlider.getValue() / 100.0), JLabel.CENTER);
        gammaValueLabel.setForeground(DarkBlueTheme.getTextColor());
        gammaValueLabel.setPreferredSize(new Dimension(50, 20));
        gammaSlider.addChangeListener(e -> {
            gammaValueLabel.setText(String.format("%.2f", gammaSlider.getValue() / 100.0));
            if (!gammaSlider.getValueIsAdjusting()) {
                updateProcessedImage();
            }
        });
        gammaPanel.add(gammaSlider, BorderLayout.CENTER);
        gammaPanel.add(gammaValueLabel, BorderLayout.EAST);
        panel.add(gammaPanel, gbc);
        
        // 阈值偏移量
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel offsetTitle = new JLabel("阈值偏移量:");
        offsetTitle.setForeground(DarkBlueTheme.getTextColor());
        panel.add(offsetTitle, gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel offsetPanel = new JPanel(new BorderLayout(5, 0));
        offsetPanel.setBackground(DarkBlueTheme.getBackgroundColor());
        thresholdOffsetSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, (int)ConfigManager.getThresholdOffset());
        thresholdOffsetSlider.setBackground(DarkBlueTheme.getBackgroundColor());
        thresholdOffsetValueLabel = new JLabel(String.valueOf(thresholdOffsetSlider.getValue()), JLabel.CENTER);
        thresholdOffsetValueLabel.setForeground(DarkBlueTheme.getTextColor());
        thresholdOffsetValueLabel.setPreferredSize(new Dimension(40, 20));
        thresholdOffsetSlider.addChangeListener(e -> {
            thresholdOffsetValueLabel.setText(String.valueOf(thresholdOffsetSlider.getValue()));
            if (!thresholdOffsetSlider.getValueIsAdjusting()) {
                updateProcessedImage();
            }
        });
        offsetPanel.add(thresholdOffsetSlider, BorderLayout.CENTER);
        offsetPanel.add(thresholdOffsetValueLabel, BorderLayout.EAST);
        panel.add(offsetPanel, gbc);
        
        // 反色选项
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        invertCheckBox = new JCheckBox("反色处理");
        invertCheckBox.setBackground(DarkBlueTheme.getBackgroundColor());
        invertCheckBox.setForeground(DarkBlueTheme.getTextColor());
        invertCheckBox.addActionListener(e -> updateProcessedImage());
        panel.add(invertCheckBox, gbc);
        
        // 说明文本
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JTextArea infoArea = new JTextArea(
            "参数说明:\n" +
            "• 二值化阈值: 0-255，值越大白色区域越多\n" +
            "• Gamma系数: 调整图像对比度，0.1-2.0\n" +
            "• 阈值偏移量: 动态阈值的偏移调整\n" +
            "• 反色处理: 交换黑白颜色\n\n" +
            "提示: 调节参数可实时预览效果"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(panel.getBackground());
        infoArea.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        infoArea.setFont(infoArea.getFont().deriveFont(Font.PLAIN));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(infoArea, gbc);
        
        // 添加弹性空间
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        LogUtil.log("[createButtonPanel] 开始执行");
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        JButton applyBtn = createStyledButton("[应用预处理]");
        applyBtn.setFont(applyBtn.getFont().deriveFont(Font.BOLD, 12));
        applyBtn.setBackground(DarkBlueTheme.SUCCESS);
        applyBtn.addActionListener(e -> applyPreprocess());
        panel.add(applyBtn);
        
        JButton saveImageBtn = createStyledButton("[保存图片]");
        saveImageBtn.addActionListener(e -> saveProcessedImage());
        panel.add(saveImageBtn);
        
        JButton saveBtn = createStyledButton("[保存参数为默认]");
        saveBtn.addActionListener(e -> saveParameters());
        panel.add(saveBtn);
        
        JButton resetBtn = createStyledButton("[重置参数]");
        resetBtn.addActionListener(e -> resetParameters());
        panel.add(resetBtn);
        
        return panel;
    }
    
    private JPanel createImageDisplayPanel() {
        LogUtil.log("[createImageDisplayPanel] 开始执行");
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setBackground(DarkBlueTheme.getBackgroundColor());
        
        // 原图面板
        JPanel originalPanel = new JPanel(new BorderLayout());
        originalPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "原始图像",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        originalPanel.setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        originalImageLabel = new JLabel("请拖入或打开图片", JLabel.CENTER);
        originalImageLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        originalPanel.add(originalImageLabel, BorderLayout.CENTER);
        
        // 处理后图像面板
        JPanel processedPanel = new JPanel(new BorderLayout());
        processedPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            "二值化效果",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            DarkBlueTheme.TEXT_SECONDARY
        ));
        processedPanel.setBackground(DarkBlueTheme.PRIMARY_DARK);
        
        processedImageLabel = new JLabel("调节参数查看效果", JLabel.CENTER);
        processedImageLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        processedPanel.add(processedImageLabel, BorderLayout.CENTER);
        
        panel.add(originalPanel);
        panel.add(processedPanel);
        
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
                        java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            loadImageFile(files.get(0));
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ImagePreprocessPanel.this, 
                        "拖放失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }, true);
        setDropTarget(dropTarget);
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
            LogUtil.log("[openImageFile] 打开文件成功");
        }
    }
    
    private void loadImageFile(File file) {
        LogUtil.log("[loadImageFile] 开始执行: " + file.getAbsolutePath());
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                LogUtil.log("[loadImageFile] 无法读取图片文件");
                JOptionPane.showMessageDialog(this, 
                    "无法读取图片文件", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            LogUtil.log("[loadImageFile] 图片读取成功: " + image.getWidth() + "x" + image.getHeight());
            currentImage = image;
            
            // 确保图像转换为BGR格式以便OpenCV处理
            BufferedImage convertedImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = convertedImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            
            currentMat = ImageProcessor.bufferedImageToMat(convertedImage);
            LogUtil.log("[loadImageFile] 转换为Mat成功: " + currentMat.cols() + "x" + currentMat.rows() + ", channels=" + currentMat.channels());
            
            // 显示原图
            displayImage(originalImageLabel, image);
            
            // 更新处理后图像
            updateProcessedImage();
            LogUtil.log("[loadImageFile] 执行完成");
            
        } catch (Exception e) {
            LogUtil.log("[loadImageFile] 错误: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "加载图片失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void displayImage(JLabel label, BufferedImage image) {
        LogUtil.log("[displayImage] 开始执行, 图像尺寸: " + (image != null ? image.getWidth() + "x" + image.getHeight() : "null"));
        if (image == null) {
            SwingUtilities.invokeLater(() -> {
                label.setIcon(null);
                label.setText("无图像");
                label.revalidate();
                label.repaint();
            });
            LogUtil.log("[displayImage] 图像为null，跳过显示");
            return;
        }
        
        // 计算缩放尺寸 - 使用面板实际大小
        Container parent = label.getParent();
        int maxWidth = 400;
        int maxHeight = 500;
        if (parent != null) {
            maxWidth = Math.max(200, parent.getWidth() - 20);
            maxHeight = Math.max(200, parent.getHeight() - 40);
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        double scaleX = (double) maxWidth / width;
        double scaleY = (double) maxHeight / height;
        double scale = Math.min(scaleX, scaleY);
        
        if (scale < 1.0) {
            width = (int) (width * scale);
            height = (int) (height * scale);
        }
        
        final int finalWidth = width;
        final int finalHeight = height;
        
        // 在EDT线程中更新UI
        SwingUtilities.invokeLater(() -> {
            // 缩放图像
            Image scaled = image.getScaledInstance(finalWidth, finalHeight, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaled));
            label.setText(null);
            
            // 强制刷新显示
            label.revalidate();
            label.repaint();
            LogUtil.log("[displayImage] 图像显示完成: " + finalWidth + "x" + finalHeight);
        });
    }
    
    private void updateProcessedImage() {
        LogUtil.log("[updateProcessedImage] 开始执行");
        if (currentMat == null) {
            LogUtil.log("[updateProcessedImage] currentMat 为 null，跳过处理");
            return;
        }
        if (currentMat.empty()) {
            LogUtil.log("[updateProcessedImage] currentMat 为空，跳过处理");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取参数
            int grayscale = grayscaleSlider.getValue();
            double gamma = gammaSlider.getValue() / 100.0;
            double thresholdOffset = thresholdOffsetSlider.getValue();
            boolean invert = invertCheckBox.isSelected();
            
            LogUtil.log("[updateProcessedImage] 参数: grayscale=" + grayscale + ", gamma=" + gamma + ", thresholdOffset=" + thresholdOffset + ", invert=" + invert);
            
            // 设置参数
            ImageProcessor.setGrayscaleValue(grayscale);
            ImageProcessor.setGammaValue(gamma);
            ImageProcessor.setThresholdOffset(thresholdOffset);
            
            // 执行预处理
            Mat processed = preprocessWithParams(currentMat, grayscale, gamma, thresholdOffset, invert);
            
            // 转换为BufferedImage并显示
            BufferedImage processedImage = ImageProcessor.matToBufferedImage(processed);
            displayImage(processedImageLabel, processedImage);
            
            processed.release();
            
            // 显示处理时间和参数
            long elapsedTime = System.currentTimeMillis() - startTime;
            String params = String.format("阈值:%d Gamma:%.2f 偏移:%d %s", 
                grayscale, gamma, (int)thresholdOffset, invert ? "反色" : "");
            infoLabel.setText(String.format("处理时间: %dms | %s", elapsedTime, params));
            
            LogUtil.log("[updateProcessedImage] 执行完成，耗时 " + elapsedTime + "ms");
            
        } catch (Exception e) {
            LogUtil.log("[updateProcessedImage] 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 使用指定参数进行预处理
     */
    private Mat preprocessWithParams(Mat src, int grayscale, double gamma, double thresholdOffset, boolean invert) {
        LogUtil.log("[preprocessWithParams] 开始执行");
        Mat gray;
        if (src.channels() > 1) {
            gray = new Mat();
            opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY);
        } else {
            gray = src.clone();
        }
        
        // Gamma校正
        Mat gammaCorrected = gammaCorrection(gray, gamma);
        gray.release();
        
        // 计算动态阈值
        Scalar meanScalar = org.bytedeco.opencv.global.opencv_core.mean(gammaCorrected);
        double meanValue = meanScalar.get(0);
        double threshold = meanValue + thresholdOffset;
        meanScalar.deallocate();
        
        // 确保阈值在有效范围内
        threshold = Math.max(0, Math.min(255, threshold));
        
        // 二值化
        Mat binary = new Mat();
        int threshType = invert ? opencv_imgproc.THRESH_BINARY_INV : opencv_imgproc.THRESH_BINARY;
        opencv_imgproc.threshold(gammaCorrected, binary, threshold, 255, threshType);
        gammaCorrected.release();
        
        LogUtil.log("[preprocessWithParams] 执行完成");
        return binary;
    }
    
    /**
     * Gamma校正
     */
    private Mat gammaCorrection(Mat src, double gamma) {
        LogUtil.log("[gammaCorrection] 开始执行");
        Mat lut = new Mat(1, 256, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
        byte[] lutData = new byte[256];
        for (int i = 0; i < 256; i++) {
            lutData[i] = (byte) Math.round(Math.pow(i / 255.0, gamma) * 255.0);
        }
        lut.data().put(lutData);
        
        Mat dst = new Mat();
        org.bytedeco.opencv.global.opencv_core.LUT(src, lut, dst);
        lut.release();
        LogUtil.log("[gammaCorrection] 执行完成");
        return dst;
    }
    
    private void applyPreprocess() {
        LogUtil.log("[applyPreprocess] 开始执行");
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, 
                "请先加载图片", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        updateProcessedImage();
        
        JOptionPane.showMessageDialog(this, 
            "预处理参数已应用", "成功", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void saveParameters() {
        LogUtil.log("[saveParameters] 开始执行");
        ConfigManager.setGrayscale(grayscaleSlider.getValue());
        ConfigManager.setGamma(gammaSlider.getValue() / 100.0);
        ConfigManager.setThresholdOffset(thresholdOffsetSlider.getValue());
        ConfigManager.saveConfig();
        
        JOptionPane.showMessageDialog(this, 
            "参数已保存为默认值", "成功", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void resetParameters() {
        LogUtil.log("[resetParameters] 开始执行");
        grayscaleSlider.setValue(128);
        gammaSlider.setValue(50);
        thresholdOffsetSlider.setValue(-20);
        invertCheckBox.setSelected(false);
        
        updateProcessedImage();
    }
    
    /**
     * 保存处理后的图片到预处理目录
     */
    private void saveProcessedImage() {
        LogUtil.log("[saveProcessedImage] 开始执行");
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, 
                "请先加载图片", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            // 获取图片保存目录
            String imageDir = ConfigManager.getImageDir();
            File preprocessDir = new File(imageDir, "预处理");
            if (!preprocessDir.exists()) {
                preprocessDir.mkdirs();
            }
            
            // 生成带时间戳的文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String fileName = "preprocess_" + timestamp + ".png";
            File saveFile = new File(preprocessDir, fileName);
            
            // 获取当前处理后的图像
            int grayscale = grayscaleSlider.getValue();
            double gamma = gammaSlider.getValue() / 100.0;
            double thresholdOffset = thresholdOffsetSlider.getValue();
            boolean invert = invertCheckBox.isSelected();
            
            Mat processed = preprocessWithParams(currentMat, grayscale, gamma, thresholdOffset, invert);
            BufferedImage processedImage = ImageProcessor.matToBufferedImage(processed);
            
            // 保存图片
            ImageIO.write(processedImage, "PNG", saveFile);
            processed.release();
            
            LogUtil.log("[saveProcessedImage] 图片已保存: " + saveFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this, 
                "图片已保存到:\n" + saveFile.getAbsolutePath(), "保存成功", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            LogUtil.log("[saveProcessedImage] 错误: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "保存图片失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
