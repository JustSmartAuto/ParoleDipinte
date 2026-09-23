package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.db.DatabaseService;
import com.honda.paroledipinte.image.CharacterGenerator;
import com.honda.paroledipinte.image.ImageProcessor;
import com.honda.paroledipinte.model.Shape;
import org.bytedeco.opencv.opencv_core.Mat;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 字符分类测试面板
 * 用户可以打开或拖入单字符图片进行分类测试，快速调节参数
 */
public class ClassifyTestPanel extends JPanel {
    
    private OcrResultPanel resultPanel;
    private JTextField targetCharField;
    private JLabel statusLabel;
    private JLabel resultLabel;
    
    // Gamma参数控件
    private JSlider gammaSlider;
    private JLabel gammaLabel;
    
    // 灰度值参数控件
    private JSlider grayscaleSlider;
    private JLabel grayscaleLabel;
    
    private DatabaseService dbService;
    private BufferedImage currentImage;
    
    public ClassifyTestPanel() {
        LogUtil.log("[ClassifyTestPanel] 开始执行");
        initComponents();
        setupDragAndDrop();
    }
    
    public void setDatabaseService(DatabaseService dbService) {
        this.dbService = dbService;
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.35);
        
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
        
        // 顶部：拖放区域和打开按钮
        JPanel topPanel = createDropPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        
        // 中部：参数设置
        JPanel paramPanel = createParamPanel();
        panel.add(paramPanel, BorderLayout.CENTER);
        
        // 底部：目标字符和运行按钮
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createDropPanel() {
        LogUtil.log("[createDropPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 拖放区域
        JPanel dropArea = new JPanel();
        dropArea.setLayout(new BorderLayout());
        dropArea.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 2, 5));
        dropArea.setBackground(new Color(60, 60, 60));
        dropArea.setPreferredSize(new Dimension(0, 120));
        
        JLabel dropLabel = new JLabel("<html><center>[拖放单字符图片到此处]<br>或点击选择文件</center></html>", JLabel.CENTER);
        dropLabel.setForeground(Color.WHITE);
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
        JButton openBtn = new JButton("[打开图片]");
        openBtn.addActionListener(e -> openImageFile());
        panel.add(openBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createParamPanel() {
        LogUtil.log("[createParamPanel] 开始执行");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("分类参数设置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int row = 0;
        
        // 二值化阈值
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("二值化阈值:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel grayscalePanel = new JPanel(new BorderLayout(5, 0));
        grayscaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, ConfigManager.getGrayscale());
        grayscaleLabel = new JLabel(String.valueOf(grayscaleSlider.getValue()), JLabel.CENTER);
        grayscaleLabel.setPreferredSize(new Dimension(40, 20));
        grayscaleSlider.addChangeListener(e -> {
            grayscaleLabel.setText(String.valueOf(grayscaleSlider.getValue()));
        });
        grayscalePanel.add(grayscaleSlider, BorderLayout.CENTER);
        grayscalePanel.add(grayscaleLabel, BorderLayout.EAST);
        panel.add(grayscalePanel, gbc);
        
        // Gamma系数
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Gamma系数:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
        JPanel gammaPanel = new JPanel(new BorderLayout(5, 0));
        gammaSlider = new JSlider(JSlider.HORIZONTAL, 10, 200, (int)(ConfigManager.getGamma() * 100));
        gammaLabel = new JLabel(String.format("%.2f", gammaSlider.getValue() / 100.0), JLabel.CENTER);
        gammaLabel.setPreferredSize(new Dimension(50, 20));
        gammaSlider.addChangeListener(e -> {
            gammaLabel.setText(String.format("%.2f", gammaSlider.getValue() / 100.0));
        });
        gammaPanel.add(gammaSlider, BorderLayout.CENTER);
        gammaPanel.add(gammaLabel, BorderLayout.EAST);
        panel.add(gammaPanel, gbc);
        
        // 说明文本
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JTextArea infoArea = new JTextArea(
            "字符分类说明:\n" +
            "1. 加载单字符图片\n" +
            "2. 输入预期字符\n" +
            "3. 点击运行分类\n" +
            "4. 查看相似度分数\n\n" +
            "Gamma系数影响图像预处理效果，\n" +
            "可根据图片亮度调节。"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(panel.getBackground());
        infoArea.setFont(infoArea.getFont().deriveFont(Font.PLAIN));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(infoArea, gbc);
        
        // 添加弹性空间
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        LogUtil.log("[createBottomPanel] 开始执行");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 目标字符输入
        JPanel charPanel = new JPanel(new BorderLayout(5, 5));
        charPanel.add(new JLabel("预期字符:"), BorderLayout.WEST);
        targetCharField = new JTextField(5);
        targetCharField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        targetCharField.setHorizontalAlignment(JTextField.CENTER);
        targetCharField.setToolTipText("输入图片中预期的单个字符");
        charPanel.add(targetCharField, BorderLayout.CENTER);
        panel.add(charPanel, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        JButton runBtn = new JButton("[运行字符分类]");
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD, 14));
        runBtn.setBackground(new Color(33, 150, 243));
        runBtn.setForeground(Color.WHITE);
        runBtn.setOpaque(true);
        runBtn.addActionListener(e -> runClassify());
        buttonPanel.add(runBtn);
        
        JButton saveImageBtn = new JButton("[保存图片]");
        saveImageBtn.addActionListener(e -> saveCurrentImage());
        buttonPanel.add(saveImageBtn);
        
        JButton saveParamBtn = new JButton("[保存参数为默认]");
        saveParamBtn.addActionListener(e -> saveParameters());
        buttonPanel.add(saveParamBtn);
        
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        // 结果标签
        resultLabel = new JLabel("相似度: -");
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 16));
        resultLabel.setHorizontalAlignment(JLabel.CENTER);
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panel.add(resultLabel, BorderLayout.SOUTH);
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        panel.add(statusLabel, BorderLayout.PAGE_END);
        
        return panel;
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
                            loadImageFile(files.get(0));
                        }
                    }
                } catch (Exception ex) {
                    showStatus("拖放失败: " + ex.getMessage(), true);
                }
            }
        }, true);
        setDropTarget(dropTarget);
    }
    
    private void openImageFile() {
        LogUtil.log("[openImageFile] 开始执行");
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择单字符图片文件");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "图片文件 (*.jpg, *.jpeg, *.png, *.bmp, *.gif)", 
            "jpg", "jpeg", "png", "bmp", "gif"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadImageFile(chooser.getSelectedFile());
        }
    }
    
    private void loadImageFile(File file) {
        LogUtil.log("[loadImageFile] 开始执行");
        try {
            showStatus("正在加载图片...", false);
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                showStatus("无法读取图片文件", true);
                return;
            }
            
            currentImage = image;
            resultPanel.setImage(image);
            resultLabel.setText("相似度: -");
            showStatus("图片已加载: " + file.getName() + " (" + image.getWidth() + "x" + image.getHeight() + ")", false);
            
        } catch (Exception e) {
            showStatus("加载图片失败: " + e.getMessage(), true);
        }
    }
    
    private void runClassify() {
        LogUtil.log("[runClassify] 开始执行");
        if (dbService == null) {
            showStatus("数据库服务未初始化，请稍后再试", true);
            return;
        }
        if (currentImage == null) {
            showStatus("请先加载图片", true);
            return;
        }
        
        String targetChar = targetCharField.getText().trim();
        if (targetChar.isEmpty()) {
            showStatus("请输入预期字符", true);
            return;
        }
        
        // 只取第一个字符
        if (targetChar.length() > 1) {
            targetChar = targetChar.substring(0, 1);
            targetCharField.setText(targetChar);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            showStatus("正在运行字符分类...", false);
            
            // 获取参数
            int grayscale = grayscaleSlider.getValue();
            double gamma = gammaSlider.getValue() / 100.0;
            
            // 设置图像处理参数
            ImageProcessor.setGrayscaleValue(grayscale);
            ImageProcessor.setGammaValue(gamma);
            
            // 转换为Mat
            Mat inputMat = ImageProcessor.bufferedImageToMat(currentImage);
            
            // 加载或生成参考图像
            BufferedImage refImage = dbService.loadCharacterImage(targetChar);
            if (refImage == null) {
                CharacterGenerator generator = new CharacterGenerator();
                refImage = generator.generateCharacterImage(targetChar);
                dbService.saveCharacterImage(targetChar, refImage);
            }
            
            Mat refMat = ImageProcessor.bufferedImageToMat(refImage);
            
            // 计算相似度
            double score = ImageProcessor.compareSimilarity(refMat, inputMat);
            
            // 显示结果
            String resultText = String.format("相似度: %.2f%% (%.4f)", score * 100, score);
            resultLabel.setText(resultText);
            
            // 根据分数设置颜色
            if (score >= 0.8) {
        LogUtil.log("[if] 开始执行");
                resultLabel.setForeground(new Color(76, 175, 80)); // 绿色
            } else if (score >= 0.5) {
        LogUtil.log("[if] 开始执行");
                resultLabel.setForeground(new Color(255, 193, 7)); // 黄色
            } else {
                resultLabel.setForeground(new Color(244, 67, 54)); // 红色
            }
            
            // 创建Shape显示结果
            List<Shape> shapes = new ArrayList<>();
            Shape shape = new Shape();
            shape.setLabel(targetChar + " (" + String.format("%.0f%%", score * 100) + ")");
            shape.setScore(score);
            
            // 设置四个角点坐标（整个图片区域）
            List<List<Double>> points = new ArrayList<>();
            points.add(Arrays.asList(0.0, 0.0));
            points.add(Arrays.asList((double) currentImage.getWidth(), 0.0));
            points.add(Arrays.asList((double) currentImage.getWidth(), (double) currentImage.getHeight()));
            points.add(Arrays.asList(0.0, (double) currentImage.getHeight()));
            shape.setPoints(points);
            
            shapes.add(shape);
            resultPanel.setShapes(shapes);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            String params = String.format("灰度:%d Gamma:%.2f", grayscale, gamma);
            showStatus(String.format("字符分类完成: [%s] 相似度 %.4f 耗时 %dms [%s]", 
                targetChar, score, elapsedTime, params), false);
            
            // 在结果面板显示处理信息
            resultPanel.setProcessInfo(String.format("处理时间: %dms | %s | 相似度: %.2f%%", 
                elapsedTime, params, score * 100));
            
            // 释放资源
            inputMat.release();
            refMat.release();
            
        } catch (Exception e) {
            showStatus("字符分类失败: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
    
    private void saveParameters() {
        LogUtil.log("[saveParameters] 开始执行");
        ConfigManager.setGrayscale(grayscaleSlider.getValue());
        ConfigManager.setGamma(gammaSlider.getValue() / 100.0);
        ConfigManager.saveConfig();
        
        showStatus("参数已保存为默认值", false);
    }
    
    private void showStatus(String message, boolean isError) {
        LogUtil.log("[showStatus] 开始执行");
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : Color.GREEN);
    }
    
    /**
     * 保存当前图片到分类测试目录
     */
    private void saveCurrentImage() {
        LogUtil.log("[saveCurrentImage] 开始执行");
        if (currentImage == null) {
            showStatus("请先加载图片", true);
            return;
        }
        
        try {
            // 获取图片保存目录
            String imageDir = ConfigManager.getImageDir();
            File classifyDir = new File(imageDir, "分类测试");
            if (!classifyDir.exists()) {
                classifyDir.mkdirs();
            }
            
            // 获取目标字符作为文件夹名
            String targetChar = targetCharField.getText().trim();
            if (targetChar.isEmpty()) {
                targetChar = "未分类";
            } else {
                targetChar = targetChar.substring(0, 1);
            }
            
            // 使用与字符截图相同的文件夹命名规则
            String charFolderName = CharacterGenerator.getCharFolderName(targetChar);
            File charDir = new File(classifyDir, charFolderName);
            if (!charDir.exists()) {
                charDir.mkdirs();
            }
            
            // 生成带时间戳的文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String fileName = targetChar + "_" + timestamp + ".png";
            File saveFile = new File(charDir, fileName);
            
            // 保存图片
            ImageIO.write(currentImage, "PNG", saveFile);
            
            LogUtil.log("[saveCurrentImage] 图片已保存: " + saveFile.getAbsolutePath());
            showStatus("图片已保存: " + saveFile.getName(), false);
            JOptionPane.showMessageDialog(this, 
                "图片已保存到:\n" + saveFile.getAbsolutePath(), "保存成功", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            LogUtil.log("[saveCurrentImage] 错误: " + e.getMessage());
            showStatus("保存图片失败: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }
}
