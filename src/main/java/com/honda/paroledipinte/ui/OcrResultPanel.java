package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.util.LogUtil;
import com.honda.paroledipinte.model.Shape;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// 添加 BufferedImage 导入
import java.awt.image.BufferedImage;

/**
 * OCR结果展示面板
 * 支持交互式预览识别结果，包括缩放、平移、标注框显示
 * 参考HTML版本的交互设计实现
 */
public class OcrResultPanel extends JPanel {
    
    private BufferedImage image;
    private List<Shape> shapes;
    private double scaleFactor = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private boolean showLabels = true;
    private boolean showScores = false;  // 是否显示相似度得分
    private boolean isDragging = false;
    private boolean showBaseline = false;  // 是否显示行基线
    private Point lastMousePoint;
    
    // 颜色定义
    private static final Color[] LABEL_COLORS = {
        new Color(255, 99, 71),   // 番茄红
        new Color(50, 205, 50),   // 酸橙绿
        new Color(30, 144, 255),  // 道奇蓝
        new Color(255, 215, 0),   // 金色
        new Color(238, 130, 238), // 紫罗兰
        new Color(0, 255, 255),   // 青色
        new Color(255, 140, 0),   // 深橙色
        new Color(147, 112, 219)  // 中紫色
    };
    
    private JLabel zoomLabel;
    private JLabel infoLabel;
    private JLabel mouseInfoLabel;  // 鼠标坐标和RGB信息显示
    private JLabel imageScreenPosLabel;  // 图像左上角在屏幕上的坐标显示
    private JComboBox<String> coordSpaceCombo;  // 坐标空间选择下拉菜单
    private JPanel canvasPanel;  // 画布面板，用于计算屏幕坐标
    
    // 智能尺寸工具
    private SmartDimensionTool smartDimensionTool;
    private boolean smartDimensionMode = false;
    
    // 元素选择模式
    private boolean elementSelectMode = false;
    private Shape selectedShape = null;
    private Point selectedShapeCenter = null;
    
    // 区域编辑模式
    private boolean splitRegionMode = false;  // 拆分区域模式
    private boolean mergeRegionMode = false;  // 合并区域模式
    private boolean deleteRegionMode = false;  // 删除区域模式
    private Shape firstMergeShape = null;  // 合并模式的第一个选中区域
    private int splitLineX = -1;  // 拆分线的X坐标（图像坐标）
    private Point mousePos = new Point(0, 0);  // 当前鼠标位置
    
    // 图像原点模式
    private boolean imageOriginMode = false;  // 图像原点模式
    private boolean imageOriginSet = false;   // 是否已设置图像原点
    private Point imageOriginPoint = null;    // 图像原点（红色点）的位置
    
    // 坐标空间类型
    public static final String COORD_SPACE_IMAGE = "图像坐标空间";
    public static final String COORD_SPACE_SCREEN = "屏幕坐标空间";
    public static final String COORD_SPACE_NORMALIZED = "归一化坐标空间";
    
    public OcrResultPanel() {
        LogUtil.log("[OcrResultPanel] 开始执行");
        this.shapes = new ArrayList<>();
        initComponents();
        setupEventHandlers();
    }
    
    private void initComponents() {
        LogUtil.log("[initComponents] 开始执行");
        setLayout(new BorderLayout());
        setBackground(DarkBlueTheme.PRIMARY_DARK);
        setBorder(BorderFactory.createLineBorder(DarkBlueTheme.BORDER));
        
        // 创建画布面板
        canvasPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
        LogUtil.log("[paintComponent] 开始执行");
                super.paintComponent(g);
                drawCanvas((Graphics2D) g);
                
                // 更新图像屏幕坐标显示
                if (image != null) {
                    updateImageScreenPosition(getImageX(), getImageY());
                }
            }
        };
        canvasPanel.setBackground(DarkBlueTheme.PRIMARY);
        add(canvasPanel, BorderLayout.CENTER);
        
        // 创建顶部控制栏
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);
        
        // 创建缩放信息显示
        zoomLabel = new JLabel("缩放: 100%");
        zoomLabel.setForeground(DarkBlueTheme.getTextColor());
        zoomLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // 创建信息显示标签（左下角）
        infoLabel = new JLabel(" ");
        infoLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        infoLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        // 创建鼠标信息标签（第一行）
        mouseInfoLabel = new JLabel("<光标> 坐标: (0, 0, \"Image\") | RGB: (0, 0, 0)");
        mouseInfoLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        mouseInfoLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        // 添加图像左上角在Screen的坐标显示（第二行）
        imageScreenPosLabel = new JLabel("<图像> 坐标: (0, 0, \"Screen\")");
        imageScreenPosLabel.setForeground(DarkBlueTheme.TEXT_SECONDARY);
        imageScreenPosLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        // 创建状态栏面板（垂直布局，多行显示）
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(infoLabel, BorderLayout.WEST);
        
        // 创建右侧信息面板（垂直布局，避免标签重叠）
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
        eastPanel.setOpaque(false);
        
        // 第一行：缩放比例和鼠标坐标
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        firstRow.setOpaque(false);
        firstRow.add(zoomLabel);
        firstRow.add(mouseInfoLabel);
        
        // 第二行：图像屏幕坐标
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        secondRow.setOpaque(false);
        secondRow.add(imageScreenPosLabel);
        
        // 添加到垂直面板
        eastPanel.add(firstRow);
        eastPanel.add(secondRow);
        
        southPanel.add(eastPanel, BorderLayout.EAST);
        add(southPanel, BorderLayout.SOUTH);
        
        // 初始化智能尺寸工具
        smartDimensionTool = new SmartDimensionTool(this);
    }
    
    private JPanel createControlPanel() {
        LogUtil.log("[createControlPanel] 开始执行");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(DarkBlueTheme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // 坐标空间选择下拉菜单
        JLabel coordLabel = new JLabel("坐标空间:");
        coordLabel.setForeground(DarkBlueTheme.getTextColor());
        panel.add(coordLabel);
        
        coordSpaceCombo = new JComboBox<>(new String[]{COORD_SPACE_IMAGE, COORD_SPACE_SCREEN});
        coordSpaceCombo.setBackground(DarkBlueTheme.PRIMARY);
        coordSpaceCombo.setForeground(DarkBlueTheme.getTextColor());
        panel.add(coordSpaceCombo);
        
        // 重置缩放按钮
        JButton resetZoomBtn = createStyledButton("重置缩放");
        resetZoomBtn.addActionListener(e -> resetZoom());
        panel.add(resetZoomBtn);
        
        // 显示/隐藏标签按钮(不在这里显示)
        JButton toggleLabelsBtn = createStyledButton("显示/隐藏标签");
        toggleLabelsBtn.addActionListener(e -> toggleLabels());
        // panel.add(toggleLabelsBtn);
        
        // 显示/隐藏得分按钮(不在这里显示)
        JButton toggleScoresBtn = createStyledButton("显示/隐藏得分");
        toggleScoresBtn.addActionListener(e -> toggleScores());
        // panel.add(toggleScoresBtn);
        
        // 清除按钮
        JButton clearBtn = createStyledButton("清除");
        clearBtn.addActionListener(e -> clear());
        panel.add(clearBtn);
        
        // 缩放控制
        JLabel zoomLabel = new JLabel("缩放:");
        zoomLabel.setForeground(DarkBlueTheme.getTextColor());
        panel.add(zoomLabel);
        
        JButton zoomInBtn = createStyledButton("+");
        zoomInBtn.addActionListener(e -> zoom(1.2));
        panel.add(zoomInBtn);
        
        JButton zoomOutBtn = createStyledButton("-");
        zoomOutBtn.addActionListener(e -> zoom(0.8));
        panel.add(zoomOutBtn);
        
        // 适应窗口按钮
        JButton fitBtn = createStyledButton("适应窗口");
        fitBtn.addActionListener(e -> fitToWindow());
        panel.add(fitBtn);
        
        // 智能尺寸按钮
        JButton smartDimBtn = createStyledButton("智能尺寸");
        smartDimBtn.addActionListener(e -> toggleSmartDimension());
        panel.add(smartDimBtn);
        
        // 清除尺寸按钮
        JButton clearDimBtn = createStyledButton("清除尺寸");
        clearDimBtn.addActionListener(e -> clearDimensions());
        panel.add(clearDimBtn);
        
        // 选择元素按钮
        JButton selectElementBtn = createStyledButton("选择元素");
        selectElementBtn.addActionListener(e -> toggleElementSelectMode());
        panel.add(selectElementBtn);
        
        // 图像原点按钮
        JButton imageOriginBtn = createStyledButton("图像原点");
        imageOriginBtn.addActionListener(e -> toggleImageOriginMode());
        panel.add(imageOriginBtn);
        
        return panel;
    }
    
    /**
     * 创建样式化的按钮
     */
    private JButton createStyledButton(String text) {
        LogUtil.log("[createStyledButton] 开始执行");
        JButton button = new JButton(text);
        button.setBackground(DarkBlueTheme.PRIMARY);
        button.setForeground(DarkBlueTheme.getTextColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DarkBlueTheme.BORDER),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 11));
        return button;
    }
    
    private void setupEventHandlers() {
        MouseInputAdapter mouseHandler = new MouseInputAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
        LogUtil.log("[mousePressed] 开始执行");
                if (smartDimensionMode) {
                    smartDimensionTool.mouseClicked(e.getPoint(), e.getButton());
                    return;
                }
                if (splitRegionMode) {
                    handleSplitRegionClick(e.getPoint());
                    return;
                }
                if (mergeRegionMode) {
                    handleMergeRegionClick(e.getPoint());
                    return;
                }
                if (deleteRegionMode) {
                    handleDeleteRegionClick(e.getPoint());
                    return;
                }
                if (elementSelectMode) {
                    selectElementAt(e.getPoint());
                    return;
                }
                if (imageOriginMode) {
                    handleImageOriginClick(e.getPoint());
                    return;
                }
                if (image != null) {
                    isDragging = true;
                    lastMousePoint = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
        LogUtil.log("[mouseReleased] 开始执行");
                if (smartDimensionMode || elementSelectMode || imageOriginMode) return;
                isDragging = false;
                setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
        LogUtil.log("[mouseDragged] 开始执行");
                if (smartDimensionMode || elementSelectMode || imageOriginMode) return;
                if (isDragging && lastMousePoint != null) {
                    Point currentPoint = e.getPoint();
                    offsetX += currentPoint.x - lastMousePoint.x;
                    offsetY += currentPoint.y - lastMousePoint.y;
                    lastMousePoint = currentPoint;
                    repaint();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = e.getPoint();
                if (smartDimensionMode) {
                    smartDimensionTool.mouseMoved(e.getPoint());
                }
                if (splitRegionMode) {
                    updateSplitLinePosition();
                }
                updateMouseInfo(e.getPoint());
            }
            
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
        LogUtil.log("[mouseWheelMoved] 开始执行");
                if (smartDimensionMode || elementSelectMode || imageOriginMode) return;
                if (image != null) {
                    double delta = e.getWheelRotation() > 0 ? 0.9 : 1.1;
                    zoomAtPoint(delta, e.getPoint());
                }
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }
    
    /**
     * 切换智能尺寸模式
     */
    private void toggleSmartDimension() {
        smartDimensionMode = !smartDimensionMode;
        smartDimensionTool.setActive(smartDimensionMode);
        if (smartDimensionMode) {
            updateSmartDimensionTransform();
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            infoLabel.setText("智能尺寸: 点击两个字符方框的边线测量距离");
        } else {
            setCursor(Cursor.getDefaultCursor());
            infoLabel.setText(" ");
        }
        repaint();
    }
    
    /**
     * 清除所有尺寸标注
     */
    private void clearDimensions() {
        smartDimensionTool.clearDimensions();
        infoLabel.setText("已清除所有尺寸标注");
    }
    
    /**
     * 切换元素选择模式
     */
    private void toggleElementSelectMode() {
        elementSelectMode = !elementSelectMode;
        if (elementSelectMode) {
            // 关闭其他模式
            if (smartDimensionMode) {
                smartDimensionMode = false;
                smartDimensionTool.setActive(false);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoLabel.setText("元素选择: 点击方框选择元素，查看CenterX/Y, SideLengthX/Y, Area");
        } else {
            selectedShape = null;
            selectedShapeCenter = null;
            setCursor(Cursor.getDefaultCursor());
            infoLabel.setText(" ");
        }
        repaint();
    }
    
    /**
     * 切换图像原点模式
     */
    private void toggleImageOriginMode() {
        imageOriginMode = !imageOriginMode;
        if (imageOriginMode) {
            // 关闭其他模式
            if (smartDimensionMode) {
                smartDimensionMode = false;
                smartDimensionTool.setActive(false);
            }
            if (elementSelectMode) {
                elementSelectMode = false;
                selectedShape = null;
                selectedShapeCenter = null;
            }
            if (splitRegionMode) {
                splitRegionMode = false;
                splitLineX = -1;
            }
            if (mergeRegionMode) {
                mergeRegionMode = false;
                firstMergeShape = null;
            }
            if (deleteRegionMode) {
                deleteRegionMode = false;
            }
            
            // 计算图像左上角在画布中的位置（相对于canvasPanel的坐标）
            // 注意：绘制是在canvasPanel的paintComponent中进行的，所以必须使用相对于canvasPanel的坐标
            int imgX = getImageX();
            int imgY = getImageY();
            imageOriginPoint = new Point(imgX, imgY);
            imageOriginSet = true;
            
            LogUtil.log("[toggleImageOriginMode] 图像原点设置: canvas坐标=(" + imgX + ", " + imgY + ")");
            
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoLabel.setText("图像原点: 点击红色原点进行校准，使鼠标坐标与图像坐标相等");
        } else {
            imageOriginSet = false;
            imageOriginPoint = null;
            setCursor(Cursor.getDefaultCursor());
            infoLabel.setText(" ");
        }
        repaint();
    }
    
    /**
     * 处理图像原点点击
     * 注意：point是相对于OcrResultPanel的坐标，需要转换为相对于canvasPanel的坐标进行比较
     */
    private void handleImageOriginClick(Point point) {
        if (!imageOriginSet || imageOriginPoint == null) return;
        
        // 将鼠标坐标从OcrResultPanel坐标系转换为canvasPanel坐标系
        Point canvasMousePoint = new Point(point.x - canvasPanel.getX(), point.y - canvasPanel.getY());
        
        // 检查是否点击了原点（允许15像素的误差范围）
        double distance = Math.sqrt(Math.pow(canvasMousePoint.x - imageOriginPoint.x, 2) + 
                                    Math.pow(canvasMousePoint.y - imageOriginPoint.y, 2));
        
        LogUtil.log("[handleImageOriginClick] 鼠标Panel坐标=(" + point.x + ", " + point.y + "), " +
                    "Canvas坐标=(" + canvasMousePoint.x + ", " + canvasMousePoint.y + "), " +
                    "原点坐标=(" + imageOriginPoint.x + ", " + imageOriginPoint.y + "), 距离=" + String.format("%.2f", distance));
        
        if (distance <= 15) {
            // 计算当前鼠标屏幕坐标
            Point panelScreenPos = getLocationOnScreen();
            int mouseScreenX = panelScreenPos.x + point.x;
            int mouseScreenY = panelScreenPos.y + point.y;
            
            // 计算图像左上角的屏幕坐标
            Point canvasScreenPos = canvasPanel.getLocationOnScreen();
            int imgX = getImageX();
            int imgY = getImageY();
            int imageScreenX = canvasScreenPos.x + imgX;
            int imageScreenY = canvasScreenPos.y + imgY;
            
            // 计算偏移量，使鼠标坐标与图像坐标相等
            // 鼠标屏幕坐标应该等于图像屏幕坐标
            int offsetX = imageScreenX - panelScreenPos.x - point.x;
            int offsetY = imageScreenY - panelScreenPos.y - point.y;
            
            // 调整偏移量（将图像移动到鼠标位置）
            this.offsetX += offsetX;
            this.offsetY += offsetY;
            
            infoLabel.setText(String.format("原点校准完成: 鼠标(%d,%d) -> 图像(%d,%d)", 
                mouseScreenX, mouseScreenY, imageScreenX, imageScreenY));
            
            // 退出原点模式
            imageOriginMode = false;
            imageOriginSet = false;
            imageOriginPoint = null;
            setCursor(Cursor.getDefaultCursor());
            
            repaint();
        } else {
            infoLabel.setText("请点击红色原点进行校准");
        }
    }
    
    /**
     * 选择点击位置的元素
     */
    private void selectElementAt(Point point) {
        if (shapes == null || shapes.isEmpty()) return;
        
        // 计算图像在面板上的位置（鼠标坐标是相对于面板的）
        int imgX = getImageXInPanel();
        int imgY = getImageYInPanel();
        
        Shape closestShape = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Shape shape : shapes) {
            Rectangle2D bounds = getShapeBoundsInScreen(shape, imgX, imgY);
            if (bounds == null) continue;
            
            // 计算点到方框中心的距离
            double centerX = bounds.getCenterX();
            double centerY = bounds.getCenterY();
            double distance = Math.sqrt(Math.pow(point.x - centerX, 2) + Math.pow(point.y - centerY, 2));
            
            // 如果点在方框内或距离中心较近
            if (bounds.contains(point) || distance < closestDistance) {
                closestDistance = distance;
                closestShape = shape;
            }
        }
        
        selectedShape = closestShape;
        if (selectedShape != null) {
            selectedShapeCenter = point;
            updateElementInfo();
        }
        repaint();
    }
    
    /**
     * 获取形状在屏幕上的边界矩形
     */
    private Rectangle2D getShapeBoundsInScreen(Shape shape, int imgX, int imgY) {
        if (shape == null || shape.getPoints() == null || shape.getPoints().size() < 4) {
            return null;
        }
        
        List<List<Double>> points = shape.getPoints();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (List<Double> point : points) {
            if (point.size() >= 2) {
                double x = point.get(0);
                double y = point.get(1);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        
        // 转换为屏幕坐标
        int screenMinX = imgX + (int) (minX * scaleFactor);
        int screenMinY = imgY + (int) (minY * scaleFactor);
        int screenMaxX = imgX + (int) (maxX * scaleFactor);
        int screenMaxY = imgY + (int) (maxY * scaleFactor);
        
        return new Rectangle2D.Double(screenMinX, screenMinY, 
            screenMaxX - screenMinX, screenMaxY - screenMinY);
    }
    
    /**
     * 更新元素信息显示
     */
    private void updateElementInfo() {
        if (selectedShape == null) return;
        
        List<List<Double>> points = selectedShape.getPoints();
        if (points == null || points.size() < 4) return;
        
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (List<Double> point : points) {
            if (point.size() >= 2) {
                double x = point.get(0);
                double y = point.get(1);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        double sideLengthX = maxX - minX;
        double sideLengthY = maxY - minY;
        double area = sideLengthX * sideLengthY;
        
        String label = selectedShape.getLabel();
        if (label == null || label.isEmpty()) label = "?";
        
        String info = String.format("字符 '%s' | Center: (%.1f, %.1f) | Size: %.1fx%.1f | Area: %.1f", 
            label, centerX, centerY, sideLengthX, sideLengthY, area);
        infoLabel.setText(info);
        
        LogUtil.log("[OcrResultPanel] 选择元素: " + info);
    }
    
    /**
     * 更新智能尺寸工具的变换参数
     */
    private void updateSmartDimensionTransform() {
        if (image != null) {
            int imgX = getImageX();
            int imgY = getImageY();
            smartDimensionTool.setTransform(scaleFactor, imgX, imgY);
            smartDimensionTool.setImage(image);
            smartDimensionTool.setShapes(shapes);
        }
    }
    
    /**
     * 获取图像在画布上的X坐标（相对于canvasPanel）
     * 坐标系说明：这是画布坐标系，原点在canvasPanel的左上角
     * 用于：在canvasPanel中绘制图像和图形元素
     */
    private int getImageX() {
        if (image == null || canvasPanel == null) return 0;
        int scaledWidth = (int) (image.getWidth() * scaleFactor);
        return canvasPanel.getWidth() / 2 + (int) offsetX - scaledWidth / 2;
    }
    
    /**
     * 获取图像在画布上的Y坐标（相对于canvasPanel）
     * 坐标系说明：这是画布坐标系，原点在canvasPanel的左上角
     * 用于：在canvasPanel中绘制图像和图形元素
     */
    private int getImageY() {
        if (image == null || canvasPanel == null) return 0;
        int scaledHeight = (int) (image.getHeight() * scaleFactor);
        return canvasPanel.getHeight() / 2 + (int) offsetY - scaledHeight / 2;
    }
    
    /**
     * 获取图像在面板上的X坐标（相对于OcrResultPanel，用于鼠标事件处理）
     * 坐标系说明：这是面板坐标系，原点在OcrResultPanel的左上角
     * 转换关系：Panel坐标 = canvasPanel.getX() + 画布坐标
     * 用于：鼠标事件处理（鼠标坐标是相对于OcrResultPanel的）
     */
    private int getImageXInPanel() {
        if (image == null || canvasPanel == null) return 0;
        // canvasPanel在OcrResultPanel中的位置 + 图像在canvasPanel中的位置
        return canvasPanel.getX() + getImageX();
    }
    
    /**
     * 获取图像在面板上的Y坐标（相对于OcrResultPanel，用于鼠标事件处理）
     * 坐标系说明：这是面板坐标系，原点在OcrResultPanel的左上角
     * 转换关系：Panel坐标 = canvasPanel.getY() + 画布坐标
     * 用于：鼠标事件处理（鼠标坐标是相对于OcrResultPanel的）
     */
    private int getImageYInPanel() {
        if (image == null || canvasPanel == null) return 0;
        return canvasPanel.getY() + getImageY();
    }
    
    /**
     * 将面板坐标（相对于OcrResultPanel）转换为画布坐标（相对于canvasPanel）
     * 用于：将鼠标坐标转换为画布坐标进行绘制或击中检测
     */
    private Point panelToCanvas(Point panelPoint) {
        return new Point(panelPoint.x - canvasPanel.getX(), panelPoint.y - canvasPanel.getY());
    }
    
    /**
     * 将画布坐标（相对于canvasPanel）转换为面板坐标（相对于OcrResultPanel）
     * 用于：将画布中的坐标转换为面板坐标进行鼠标事件处理
     */
    private Point canvasToPanel(Point canvasPoint) {
        return new Point(canvasPoint.x + canvasPanel.getX(), canvasPoint.y + canvasPanel.getY());
    }
    
    /**
     * 将画布坐标转换为图像坐标
     * 转换公式：图像坐标 = (画布坐标 - 图像画布偏移) / 缩放比例
     */
    private Point2D canvasToImage(Point canvasPoint) {
        int imgX = getImageX();
        int imgY = getImageY();
        double imageX = (canvasPoint.x - imgX) / scaleFactor;
        double imageY = (canvasPoint.y - imgY) / scaleFactor;
        return new Point2D.Double(imageX, imageY);
    }
    
    /**
     * 将图像坐标转换为画布坐标
     * 转换公式：画布坐标 = 图像坐标 * 缩放比例 + 图像画布偏移
     */
    private Point imageToCanvas(Point2D imagePoint) {
        int imgX = getImageX();
        int imgY = getImageY();
        int canvasX = (int) (imagePoint.getX() * scaleFactor) + imgX;
        int canvasY = (int) (imagePoint.getY() * scaleFactor) + imgY;
        return new Point(canvasX, canvasY);
    }
    
    private void drawCanvas(Graphics2D g2d) {
        LogUtil.log("[drawCanvas] 开始执行");
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 清空背景
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        if (image == null) {
            // 显示提示文字
            g2d.setColor(DarkBlueTheme.TEXT_SECONDARY);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            String message = "请加载图片进行测试";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(message);
            g2d.drawString(message, (getWidth() - textWidth) / 2, getHeight() / 2);
            return;
        }
        
        // 计算图片显示尺寸
        int scaledWidth = (int) (image.getWidth() * scaleFactor);
        int scaledHeight = (int) (image.getHeight() * scaleFactor);
        
        // 计算居中位置（使用canvasPanel的尺寸）
        int centerX = canvasPanel.getWidth() / 2 + (int) offsetX;
        int centerY = canvasPanel.getHeight() / 2 + (int) offsetY;
        int x = centerX - scaledWidth / 2;
        int y = centerY - scaledHeight / 2;
        
        // 绘制图片
        g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
        
        // 绘制标注框
        if (shapes != null && !shapes.isEmpty()) {
            drawShapes(g2d, x, y, scaledWidth, scaledHeight);
        }
        
        // 绘制图例
        drawLegend(g2d);
        
        // 更新智能尺寸工具的变换参数并绘制尺寸标注
        if (smartDimensionMode || !smartDimensionTool.dimensions.isEmpty()) {
            updateSmartDimensionTransform();
            smartDimensionTool.draw(g2d);
        }
        
        // 绘制选中的元素高亮
        if (elementSelectMode && selectedShape != null) {
            drawSelectedShape(g2d, x, y, scaledWidth, scaledHeight);
        }
        
        // 绘制行基线
        if (showBaseline && shapes != null && !shapes.isEmpty()) {
            drawBaseline(g2d, x, y, scaledWidth, scaledHeight);
        }
        
        // 绘制拆分线
        if (splitRegionMode && splitLineX >= 0) {
            drawSplitLine(g2d, x, y, scaledWidth, scaledHeight);
        }
        
        // 绘制合并模式的高亮
        if (mergeRegionMode && firstMergeShape != null) {
            drawMergeHighlight(g2d, x, y, scaledWidth, scaledHeight);
        }
        
        // 绘制图像原点
        if (imageOriginMode && imageOriginSet && imageOriginPoint != null) {
            drawImageOrigin(g2d);
        }
    }
    
    private void drawShapes(Graphics2D g2d, int imgX, int imgY, int imgWidth, int imgHeight) {
        LogUtil.log("[drawShapes] 开始执行");
        double scaleX = (double) imgWidth / image.getWidth();
        double scaleY = (double) imgHeight / image.getHeight();
        
        for (int i = 0; i < shapes.size(); i++) {
            Shape shape = shapes.get(i);
            Color color = getLabelColor(shape.getLabel(), i);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            
            List<List<Double>> points = shape.getPoints();
            if (points != null && points.size() >= 2) {
                // 转换坐标
                int[] xPoints = new int[points.size()];
                int[] yPoints = new int[points.size()];
                
                for (int j = 0; j < points.size(); j++) {
                    List<Double> point = points.get(j);
                    double origX = point.get(0);
                    double origY = point.get(1);
                    
                    // 计算在画布上的位置
                    xPoints[j] = imgX + (int) (origX * scaleX);
                    yPoints[j] = imgY + (int) (origY * scaleY);
                }
                
                // 绘制多边形
                g2d.drawPolygon(xPoints, yPoints, points.size());
                
                // 绘制标签和得分
                if (showLabels || showScores) {
                    String label = shape.getLabel();
                    double score = shape.getScore();
                    
                    // 构建显示文本
                    StringBuilder displayText = new StringBuilder();
                    if (showLabels && label != null && !label.isEmpty()) {
                        displayText.append(label);
                    }
                    if (showScores && score > 0) {
                        if (displayText.length() > 0) {
                            displayText.append(" ");
                        }
                        displayText.append(String.format("%.2f", score));
                    }
                    
                    if (displayText.length() > 0) {
                        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                        FontMetrics fm = g2d.getFontMetrics();
                        String text = displayText.toString();
                        int textWidth = fm.stringWidth(text);
                        int textHeight = fm.getHeight();
                        
                        // 在字符右上角显示
                        int labelX = xPoints[0];
                        int labelY = yPoints[0] - 5;
                        
                        // 标签背景
                        g2d.setColor(new Color(15, 23, 42, 220));
                        g2d.fillRect(labelX - 2, labelY - textHeight + 3, textWidth + 4, textHeight);
                        
                        // 标签文字
                        g2d.setColor(Color.WHITE);
                        g2d.drawString(text, labelX, labelY);
                        g2d.setColor(color);
                    }
                }
            }
        }
    }
    
    private void drawLegend(Graphics2D g2d) {
        LogUtil.log("[drawLegend] 开始执行");
        int legendX = 10;
        int legendY = 30;
        int lineHeight = 20;
        
        // 背景
        g2d.setColor(new Color(15, 23, 42, 200));
        g2d.fillRect(5, 5, 220, 90);
        
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // 统计信息
        g2d.setColor(DarkBlueTheme.getTextColor());
        g2d.drawString("识别结果: " + (shapes != null ? shapes.size() : 0) + " 个字符", legendX, legendY);
        g2d.drawString("图片尺寸: " + (image != null ? image.getWidth() + "x" + image.getHeight() : "N/A"), legendX, legendY + lineHeight);
        g2d.drawString("缩放比例: " + String.format("%.0f%%", scaleFactor * 100), legendX, legendY + lineHeight * 2);
        g2d.drawString("鼠标滚轮: 缩放 | 拖拽: 平移", legendX, legendY + lineHeight * 3);
    }
    
    /**
     * 绘制选中的元素高亮
     */
    private void drawSelectedShape(Graphics2D g2d, int imgX, int imgY, int imgWidth, int imgHeight) {
        if (selectedShape == null) return;
        
        double scaleX = (double) imgWidth / image.getWidth();
        double scaleY = (double) imgHeight / image.getHeight();
        
        List<List<Double>> points = selectedShape.getPoints();
        if (points == null || points.size() < 4) return;
        
        // 转换坐标
        int[] xPoints = new int[points.size()];
        int[] yPoints = new int[points.size()];
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (int j = 0; j < points.size(); j++) {
            List<Double> point = points.get(j);
            double origX = point.get(0);
            double origY = point.get(1);
            
            minX = Math.min(minX, origX);
            minY = Math.min(minY, origY);
            maxX = Math.max(maxX, origX);
            maxY = Math.max(maxY, origY);
            
            xPoints[j] = imgX + (int) (origX * scaleX);
            yPoints[j] = imgY + (int) (origY * scaleY);
        }
        
        // 高亮边框（黄色粗线）
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawPolygon(xPoints, yPoints, points.size());
        
        // 绘制中心点
        int centerX = imgX + (int) (((minX + maxX) / 2) * scaleX);
        int centerY = imgY + (int) (((minY + maxY) / 2) * scaleY);
        g2d.setColor(Color.RED);
        g2d.fillOval(centerX - 5, centerY - 5, 10, 10);
        
        // 绘制中心十字线
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(centerX - 10, centerY, centerX + 10, centerY);
        g2d.drawLine(centerX, centerY - 10, centerX, centerY + 10);
    }
    
    private Color getLabelColor(String label, int index) {
        if (label == null || label.isEmpty()) {
            return LABEL_COLORS[index % LABEL_COLORS.length];
        }
        // 根据标签计算哈希值选择颜色
        int hash = 0;
        for (char c : label.toCharArray()) {
            hash = ((hash << 5) - hash) + c;
            hash = hash & hash;
        }
        return LABEL_COLORS[Math.abs(hash) % LABEL_COLORS.length];
    }
    
    /**
     * 设置图片
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        this.shapes.clear();
        resetZoom();
        repaint();
    }
    
    /**
     * 设置识别结果
     */
    public void setShapes(List<Shape> shapes) {
        this.shapes = shapes != null ? shapes : new ArrayList<>();
        repaint();
    }
    
    /**
     * 设置结果（图片和标注）
     */
    public void setResult(BufferedImage image, List<Shape> shapes) {
        this.image = image;
        this.shapes = shapes != null ? shapes : new ArrayList<>();
        resetZoom();
        // 清除尺寸标注
        if (smartDimensionTool != null) {
            smartDimensionTool.clearDimensions();
        }
        repaint();
    }
    
    /**
     * 设置处理信息（时间和参数）
     */
    public void setProcessInfo(String info) {
        if (infoLabel != null) {
            infoLabel.setText(info);
        }
    }
    
    /**
     * 重置缩放
     */
    public void resetZoom() {
        LogUtil.log("[resetZoom] 开始执行");
        if (image != null && canvasPanel != null) {
            // 计算适应窗口的缩放比例（使用canvasPanel的尺寸）
            double scaleX = (double) (canvasPanel.getWidth() - 40) / image.getWidth();
            double scaleY = (double) (canvasPanel.getHeight() - 40) / image.getHeight();
            scaleFactor = Math.min(Math.min(scaleX, scaleY), 1.0);
        } else {
            scaleFactor = 1.0;
        }
        offsetX = 0;
        offsetY = 0;
        updateZoomLabel();
        repaint();
    }
    
    /**
     * 适应窗口
     */
    public void fitToWindow() {
        LogUtil.log("[fitToWindow] 开始执行");
        resetZoom();
    }
    
    /**
     * 缩放
     */
    public void zoom(double factor) {
        LogUtil.log("[zoom] 开始执行");
        scaleFactor *= factor;
        scaleFactor = Math.max(0.1, Math.min(scaleFactor, 10.0));
        updateZoomLabel();
        repaint();
    }
    
    /**
     * 在指定点缩放
     */
    private void zoomAtPoint(double factor, Point point) {
        LogUtil.log("[zoomAtPoint] 开始执行");
        if (image == null || canvasPanel == null) return;
        
        // 计算缩放前的相对位置（使用canvasPanel的尺寸）
        int centerX = canvasPanel.getWidth() / 2 + (int) offsetX;
        int centerY = canvasPanel.getHeight() / 2 + (int) offsetY;
        int scaledWidth = (int) (image.getWidth() * scaleFactor);
        int scaledHeight = (int) (image.getHeight() * scaleFactor);
        int imgX = centerX - scaledWidth / 2;
        int imgY = centerY - scaledHeight / 2;
        
        // point是相对于OcrResultPanel的坐标，需要转换为相对于canvasPanel的坐标
        int canvasX = point.x - canvasPanel.getX();
        int canvasY = point.y - canvasPanel.getY();
        
        double beforeScaleX = (canvasX - imgX) / scaleFactor;
        double beforeScaleY = (canvasY - imgY) / scaleFactor;
        
        // 应用缩放
        double newScaleFactor = scaleFactor * factor;
        newScaleFactor = Math.max(0.1, Math.min(newScaleFactor, 10.0));
        
        // 调整偏移以保持鼠标位置不变
        double afterScaleX = (canvasX - imgX) / newScaleFactor;
        double afterScaleY = (canvasY - imgY) / newScaleFactor;
        
        offsetX += (afterScaleX - beforeScaleX) * newScaleFactor;
        offsetY += (afterScaleY - beforeScaleY) * newScaleFactor;
        scaleFactor = newScaleFactor;
        
        updateZoomLabel();
        repaint();
    }
    
    /**
     * 切换标签显示
     */
    public void toggleLabels() {
        LogUtil.log("[toggleLabels] 开始执行");
        showLabels = !showLabels;
        repaint();
    }
    
    /**
     * 切换得分显示
     */
    public void toggleScores() {
        LogUtil.log("[toggleScores] 开始执行");
        showScores = !showScores;
        repaint();
    }
    
    /**
     * 清除
     */
    public void clear() {
        LogUtil.log("[clear] 开始执行");
        image = null;
        shapes.clear();
        scaleFactor = 1.0;
        offsetX = 0;
        offsetY = 0;
        updateZoomLabel();
        repaint();
    }
    
    private void updateZoomLabel() {
        LogUtil.log("[updateZoomLabel] 开始执行");
        zoomLabel.setText(String.format("缩放: %.0f%%", scaleFactor * 100));
    }
    
    /**
     * 获取当前图片
     */
    public BufferedImage getImage() {
        return image;
    }
    
    /**
     * 获取识别结果
     */
    public List<Shape> getShapes() {
        return shapes;
    }
    
    /**
     * 切换行基线显示
     */
    public void toggleBaseline() {
        showBaseline = !showBaseline;
        repaint();
    }
    
    /**
     * 更新鼠标信息显示（坐标和RGB值）
     */
    private void updateMouseInfo(Point mousePoint) {
        if (image == null || mouseInfoLabel == null) return;
        
        // 计算图像在画布上的位置（相对于canvasPanel）
        int imgX = getImageX();
        int imgY = getImageY();
        // 计算图像在面板上的位置（相对于OcrResultPanel，用于鼠标事件处理）
        int imgXInPanel = getImageXInPanel();
        int imgYInPanel = getImageYInPanel();
        int scaledWidth = (int) (image.getWidth() * scaleFactor);
        int scaledHeight = (int) (image.getHeight() * scaleFactor);
        
        // 更新图像左上角在屏幕上的坐标
        updateImageScreenPosition(imgX, imgY);
        
        // 获取当前选择的坐标空间
        String coordSpace = (String) coordSpaceCombo.getSelectedItem();
        
        // 检查鼠标是否在图像范围内（使用相对于面板的图像坐标）
        boolean isInImageRange = !(mousePoint.x < imgXInPanel || mousePoint.x >= imgXInPanel + scaledWidth ||
                                   mousePoint.y < imgYInPanel || mousePoint.y >= imgYInPanel + scaledHeight);
        
        if (COORD_SPACE_IMAGE.equals(coordSpace) && !isInImageRange) {
            mouseInfoLabel.setText("<光标> 坐标: (N/A) | RGB: (N/A)");
            return;
        }
        
        // 计算图像坐标（使用相对于面板的坐标差）
        double imageX = (mousePoint.x - imgXInPanel) / scaleFactor;
        double imageY = (mousePoint.y - imgYInPanel) / scaleFactor;
        
        // 确保坐标在有效范围内
        int pixelX = Math.max(0, Math.min(image.getWidth() - 1, (int) imageX));
        int pixelY = Math.max(0, Math.min(image.getHeight() - 1, (int) imageY));
        
        // 获取RGB值（仅在图像范围内时）
        int r = 0, g = 0, b = 0;
        String rgbStr;
        if (isInImageRange) {
            int rgb = image.getRGB(pixelX, pixelY);
            r = (rgb >> 16) & 0xFF;
            g = (rgb >> 8) & 0xFF;
            b = rgb & 0xFF;
            
            // 检查是否为灰度图（R=G=B）
            boolean isGrayscale = (r == g && g == b);
            if (isGrayscale) {
                rgbStr = String.format("(%d)", r);
            } else {
                rgbStr = String.format("(%d, %d, %d)", r, g, b);
            }
        } else {
            rgbStr = "(N/A)";
        }
        
        String coordStr;
        switch (coordSpace) {
            case COORD_SPACE_SCREEN:
                coordStr = String.format("(%d, %d, %s)", mousePoint.x, mousePoint.y, COORD_SPACE_SCREEN);
                break;
            case COORD_SPACE_NORMALIZED:
                double normX = imageX / image.getWidth();
                double normY = imageY / image.getHeight();
                coordStr = String.format("(%.3f, %.3f)", normX, normY);
                break;
            case COORD_SPACE_IMAGE:
            default:
                coordStr = String.format("(%d, %d, %s)", pixelX, pixelY, COORD_SPACE_IMAGE);
                break;
        }
        
        mouseInfoLabel.setText(String.format("<光标>坐标: %s | RGB: %s", coordStr, rgbStr));
    }
    
    /**
     * 更新图像左上角在屏幕上的坐标显示
     */
    private void updateImageScreenPosition(int imgX, int imgY) {
        if (imageScreenPosLabel == null || canvasPanel == null) return;
        
        // 获取画布面板在屏幕上的位置（图像实际绘制在此面板内）
        Point canvasScreenPos = canvasPanel.getLocationOnScreen();
        int screenX = canvasScreenPos.x + imgX;
        int screenY = canvasScreenPos.y + imgY;
        
        imageScreenPosLabel.setText(String.format("<图像> 坐标: (%d, %d, \"Screen\")", screenX, screenY));
    }
    
    /**
     * 绘制行基线（4线本风格）
     * - 顶线：橙色虚线（使用汉字确定）
     * - 中线1：绿色虚线
     * - 中线2：粉色虚线
     * - 基线：黄色虚线（最低线）
     */
    private void drawBaseline(Graphics2D g2d, int imgX, int imgY, int imgWidth, int imgHeight) {
        if (shapes == null || shapes.isEmpty()) return;
        
        double scaleX = (double) imgWidth / image.getWidth();
        double scaleY = (double) imgHeight / image.getHeight();
        
        // 计算所有字符的边界框
        List<Rectangle2D> boundsList = new ArrayList<>();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Shape shape : shapes) {
            Rectangle2D bounds = getShapeBounds(shape);
            if (bounds != null) {
                boundsList.add(bounds);
                minX = Math.min(minX, bounds.getMinX());
                minY = Math.min(minY, bounds.getMinY());
                maxX = Math.max(maxX, bounds.getMaxX());
                maxY = Math.max(maxY, bounds.getMaxY());
            }
        }
        
        if (boundsList.isEmpty()) return;
        
        // 计算行基线（使用非下沉字符的最低点）
        double baselineY = calculateBaseline(boundsList);
        
        // 计算顶线（使用汉字字符的最高点）
        double topLineY = calculateTopLine(boundsList);
        
        // 如果无法确定顶线，使用字符最高点的平均值
        if (topLineY == Double.MAX_VALUE) {
            topLineY = minY;
        }
        
        // 计算4线位置
        double lineHeight = baselineY - topLineY;
        if (lineHeight <= 0) lineHeight = maxY - minY;
        
        double midLine1Y = topLineY + lineHeight * 0.33;
        double midLine2Y = topLineY + lineHeight * 0.66;
        
        // 计算行的倾斜角度（基于字符中心点）
        double angle = calculateRowAngle(boundsList);
        
        // 绘制4条线
        drawDashedLine(g2d, imgX, imgY, scaleX, scaleY, topLineY, minX, maxX, angle, new Color(255, 140, 0), "顶线"); // 橙色
        drawDashedLine(g2d, imgX, imgY, scaleX, scaleY, midLine1Y, minX, maxX, angle, new Color(50, 205, 50), null); // 绿色
        drawDashedLine(g2d, imgX, imgY, scaleX, scaleY, midLine2Y, minX, maxX, angle, new Color(255, 105, 180), null); // 粉色
        drawDashedLine(g2d, imgX, imgY, scaleX, scaleY, baselineY, minX, maxX, angle, Color.YELLOW, "基线"); // 黄色
    }
    
    /**
     * 获取形状的边界框
     */
    private Rectangle2D getShapeBounds(Shape shape) {
        if (shape == null || shape.getPoints() == null || shape.getPoints().size() < 4) {
            return null;
        }
        
        List<List<Double>> points = shape.getPoints();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (List<Double> point : points) {
            if (point.size() >= 2) {
                double x = point.get(0);
                double y = point.get(1);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }
    
    /**
     * 计算行基线（使用非下沉字符的最低点）
     * 下沉字符如g, j, p, q, y等不考虑
     */
    private double calculateBaseline(List<Rectangle2D> boundsList) {
        // 找出所有字符的最低点，取最常见的Y值作为基线
        double maxY = Double.MIN_VALUE;
        for (Rectangle2D bounds : boundsList) {
            maxY = Math.max(maxY, bounds.getMaxY());
        }
        return maxY;
    }
    
    /**
     * 计算顶线（使用汉字字符的最高点）
     */
    private double calculateTopLine(List<Rectangle2D> boundsList) {
        // 使用所有字符最高点的平均值作为顶线
        double minY = Double.MAX_VALUE;
        for (Rectangle2D bounds : boundsList) {
            minY = Math.min(minY, bounds.getMinY());
        }
        return minY;
    }
    
    /**
     * 计算行的倾斜角度
     */
    private double calculateRowAngle(List<Rectangle2D> boundsList) {
        // 简单计算：基于字符中心点的线性回归
        if (boundsList.size() < 2) return 0;
        
        // 计算中心点
        double sumX = 0, sumY = 0;
        for (Rectangle2D bounds : boundsList) {
            sumX += bounds.getCenterX();
            sumY += bounds.getCenterY();
        }
        double avgX = sumX / boundsList.size();
        double avgY = sumY / boundsList.size();
        
        // 计算斜率
        double numerator = 0, denominator = 0;
        for (Rectangle2D bounds : boundsList) {
            double dx = bounds.getCenterX() - avgX;
            double dy = bounds.getCenterY() - avgY;
            numerator += dx * dy;
            denominator += dx * dx;
        }
        
        if (denominator == 0) return 0;
        double slope = numerator / denominator;
        return Math.atan(slope);
    }
    
    /**
     * 绘制虚线
     */
    private void drawDashedLine(Graphics2D g2d, int imgX, int imgY, double scaleX, double scaleY,
                                double lineY, double minX, double maxX, double angle, Color color, String label) {
        g2d.setColor(color);
        
        // 创建虚线样式
        float[] dashPattern = {10, 5};
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));
        
        // 计算线段起点和终点（考虑倾斜角度）
        double startX = minX - 20;
        double endX = maxX + 20;
        
        // 根据角度计算Y坐标偏移
        double startYOffset = Math.tan(angle) * (startX - (minX + maxX) / 2);
        double endYOffset = Math.tan(angle) * (endX - (minX + maxX) / 2);
        
        int x1 = imgX + (int) (startX * scaleX);
        int y1 = imgY + (int) ((lineY + startYOffset) * scaleY);
        int x2 = imgX + (int) (endX * scaleX);
        int y2 = imgY + (int) ((lineY + endYOffset) * scaleY);
        
        g2d.drawLine(x1, y1, x2, y2);
        
        // 绘制标签
        if (label != null) {
            g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2d.drawString(label, x2 + 5, y2);
        }
    }
    
    /**
     * 切换拆分区域模式
     */
    public void toggleSplitRegion() {
        splitRegionMode = !splitRegionMode;
        if (splitRegionMode) {
            // 关闭其他模式
            mergeRegionMode = false;
            firstMergeShape = null;
            deleteRegionMode = false;
            smartDimensionMode = false;
            smartDimensionTool.setActive(false);
            elementSelectMode = false;
            selectedShape = null;
            
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            infoLabel.setText("拆分区域: 移动鼠标选择拆分位置，单击执行拆分");
        } else {
            splitLineX = -1;
            setCursor(Cursor.getDefaultCursor());
            infoLabel.setText(" ");
        }
        repaint();
    }
    
    /**
     * 切换合并区域模式
     */
    public void toggleMergeRegion() {
        mergeRegionMode = !mergeRegionMode;
        if (mergeRegionMode) {
            // 关闭其他模式
            splitRegionMode = false;
            splitLineX = -1;
            deleteRegionMode = false;
            smartDimensionMode = false;
            smartDimensionTool.setActive(false);
            elementSelectMode = false;
            selectedShape = null;
            firstMergeShape = null;
            
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoLabel.setText("合并区域: 依次点击两个要合并的区域");
        } else {
            firstMergeShape = null;
            setCursor(Cursor.getDefaultCursor());
            infoLabel.setText(" ");
        }
        repaint();
    }
    
    /**
     * 切换删除区域模式
     */
    public void toggleDeleteRegion() {
        deleteRegionMode = !deleteRegionMode;
        if (deleteRegionMode) {
            // 关闭其他模式
            splitRegionMode = false;
            splitLineX = -1;
            mergeRegionMode = false;
            firstMergeShape = null;
            smartDimensionMode = false;
            smartDimensionTool.setActive(false);
            elementSelectMode = false;
            selectedShape = null;
            
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoLabel.setText("删除区域: 点击要删除的连通域区域");
        } else {
            setCursor(Cursor.getDefaultCursor());
            infoLabel.setText(" ");
        }
        repaint();
    }
    
    /**
     * 处理删除区域点击
     */
    private void handleDeleteRegionClick(Point point) {
        if (shapes == null || shapes.isEmpty()) return;
        
        // 找到点击的区域
        Shape clickedShape = findShapeAt(point);
        if (clickedShape == null) {
            infoLabel.setText("删除区域: 请点击一个区域");
            return;
        }
        
        // 删除选中的区域
        shapes.remove(clickedShape);
        infoLabel.setText("删除区域: 已删除选中区域");
        repaint();
    }
    
    /**
     * 更新拆分线位置
     */
    private void updateSplitLinePosition() {
        if (image == null) return;
        
        // 计算图像在面板上的位置（mousePos是相对于面板的）
        int imgX = getImageXInPanel();
        int imgY = getImageYInPanel();
        int scaledWidth = (int) (image.getWidth() * scaleFactor);
        int scaledHeight = (int) (image.getHeight() * scaleFactor);
        
        // 检查鼠标是否在图像范围内
        if (mousePos.x < imgX || mousePos.x >= imgX + scaledWidth ||
            mousePos.y < imgY || mousePos.y >= imgY + scaledHeight) {
            splitLineX = -1;
            return;
        }
        
        // 计算图像坐标
        splitLineX = (int) ((mousePos.x - imgX) / scaleFactor);
        repaint();
    }
    
    /**
     * 处理拆分区域点击
     */
    private void handleSplitRegionClick(Point point) {
        if (shapes == null || shapes.isEmpty() || splitLineX < 0) return;
        
        // 找到包含拆分线的区域
        Shape targetShape = null;
        int shapeIndex = -1;
        
        for (int i = 0; i < shapes.size(); i++) {
            Shape shape = shapes.get(i);
            Rectangle2D bounds = getShapeBounds(shape);
            if (bounds != null) {
                // 检查拆分线是否在区域内
                if (splitLineX > bounds.getMinX() && splitLineX < bounds.getMaxX()) {
                    targetShape = shape;
                    shapeIndex = i;
                    break;
                }
            }
        }
        
        if (targetShape == null) {
            infoLabel.setText("拆分区域: 没有找到包含拆分线的区域");
            return;
        }
        
        // 拆分区域
        Rectangle2D bounds = getShapeBounds(targetShape);
        if (bounds == null) return;
        
        // 创建两个新区域（左右拆分）
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();
        
        // 左区域
        Shape leftShape = new Shape();
        leftShape.setLabel(targetShape.getLabel());
        leftShape.setScore(targetShape.getScore());
        List<List<Double>> leftPoints = new ArrayList<>();
        leftPoints.add(createPoint(minX, minY));
        leftPoints.add(createPoint(splitLineX, minY));
        leftPoints.add(createPoint(splitLineX, maxY));
        leftPoints.add(createPoint(minX, maxY));
        leftShape.setPoints(leftPoints);
        
        // 右区域
        Shape rightShape = new Shape();
        rightShape.setLabel(targetShape.getLabel());
        rightShape.setScore(targetShape.getScore());
        List<List<Double>> rightPoints = new ArrayList<>();
        rightPoints.add(createPoint(splitLineX, minY));
        rightPoints.add(createPoint(maxX, minY));
        rightPoints.add(createPoint(maxX, maxY));
        rightPoints.add(createPoint(splitLineX, maxY));
        rightShape.setPoints(rightPoints);
        
        // 替换原区域
        shapes.remove(shapeIndex);
        shapes.add(shapeIndex, rightShape);
        shapes.add(shapeIndex, leftShape);
        
        infoLabel.setText("拆分区域: 已将区域拆分为两个");
        repaint();
    }
    
    /**
     * 处理合并区域点击
     */
    private void handleMergeRegionClick(Point point) {
        if (shapes == null || shapes.isEmpty()) return;
        
        // 找到点击的区域
        Shape clickedShape = findShapeAt(point);
        if (clickedShape == null) {
            infoLabel.setText("合并区域: 请点击一个区域");
            return;
        }
        
        if (firstMergeShape == null) {
            // 选择第一个区域
            firstMergeShape = clickedShape;
            infoLabel.setText("合并区域: 已选择第一个区域，请点击第二个区域");
            repaint();
        } else if (firstMergeShape == clickedShape) {
            // 点击了同一个区域，取消选择
            firstMergeShape = null;
            infoLabel.setText("合并区域: 已取消选择，请重新选择第一个区域");
            repaint();
        } else {
            // 选择第二个区域，执行合并
            mergeShapes(firstMergeShape, clickedShape);
            firstMergeShape = null;
            infoLabel.setText("合并区域: 已合并两个区域");
            repaint();
        }
    }
    
    /**
     * 在指定位置查找区域
     */
    private Shape findShapeAt(Point point) {
        // 使用相对于面板的图像坐标（鼠标坐标是相对于面板的）
        int imgX = getImageXInPanel();
        int imgY = getImageYInPanel();
        
        for (Shape shape : shapes) {
            Rectangle2D bounds = getShapeBoundsInScreen(shape, imgX, imgY);
            if (bounds != null && bounds.contains(point)) {
                return shape;
            }
        }
        return null;
    }
    
    /**
     * 合并两个区域
     */
    private void mergeShapes(Shape shape1, Shape shape2) {
        Rectangle2D bounds1 = getShapeBounds(shape1);
        Rectangle2D bounds2 = getShapeBounds(shape2);
        
        if (bounds1 == null || bounds2 == null) return;
        
        // 计算合并后的边界
        double minX = Math.min(bounds1.getMinX(), bounds2.getMinX());
        double minY = Math.min(bounds1.getMinY(), bounds2.getMinY());
        double maxX = Math.max(bounds1.getMaxX(), bounds2.getMaxX());
        double maxY = Math.max(bounds1.getMaxY(), bounds2.getMaxY());
        
        // 创建合并后的区域
        Shape mergedShape = new Shape();
        mergedShape.setLabel(shape1.getLabel());
        mergedShape.setScore((shape1.getScore() + shape2.getScore()) / 2);
        
        List<List<Double>> points = new ArrayList<>();
        points.add(createPoint(minX, minY));
        points.add(createPoint(maxX, minY));
        points.add(createPoint(maxX, maxY));
        points.add(createPoint(minX, maxY));
        mergedShape.setPoints(points);
        
        // 替换原区域
        int index1 = shapes.indexOf(shape1);
        int index2 = shapes.indexOf(shape2);
        
        // 移除索引较大的先
        if (index1 > index2) {
            shapes.remove(index1);
            shapes.remove(index2);
        } else {
            shapes.remove(index2);
            shapes.remove(index1);
        }
        
        // 在较小索引位置插入合并后的区域
        shapes.add(Math.min(index1, index2), mergedShape);
    }
    
    /**
     * 创建点
     */
    private List<Double> createPoint(double x, double y) {
        List<Double> point = new ArrayList<>();
        point.add(x);
        point.add(y);
        return point;
    }
    
    /**
     * 绘制拆分线
     */
    private void drawSplitLine(Graphics2D g2d, int imgX, int imgY, int imgWidth, int imgHeight) {
        if (splitLineX < 0 || image == null) return;
        
        double scaleX = (double) imgWidth / image.getWidth();
        int screenX = imgX + (int) (splitLineX * scaleX);
        
        // 绘制紫色虚线
        g2d.setColor(new Color(128, 0, 128)); // 紫色
        float[] dashPattern = {5, 5};
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dashPattern, 0));
        
        // 绘制贯穿整个画布的竖线
        g2d.drawLine(screenX, 0, screenX, getHeight());
        
        // 绘制标签
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g2d.drawString("拆分线", screenX + 5, 20);
    }
    
    /**
     * 绘制合并模式的高亮
     */
    private void drawMergeHighlight(Graphics2D g2d, int imgX, int imgY, int imgWidth, int imgHeight) {
        if (firstMergeShape == null) return;
        
        double scaleX = (double) imgWidth / image.getWidth();
        double scaleY = (double) imgHeight / image.getHeight();
        
        // 绘制第一个选中区域的高亮（绿色闪烁效果）
        g2d.setColor(new Color(0, 255, 0, 128));
        g2d.setStroke(new BasicStroke(3));
        
        List<List<Double>> points = firstMergeShape.getPoints();
        if (points != null && points.size() >= 4) {
            int[] xPoints = new int[points.size()];
            int[] yPoints = new int[points.size()];
            
            for (int j = 0; j < points.size(); j++) {
                List<Double> point = points.get(j);
                xPoints[j] = imgX + (int) (point.get(0) * scaleX);
                yPoints[j] = imgY + (int) (point.get(1) * scaleY);
            }
            
            g2d.drawPolygon(xPoints, yPoints, points.size());
            
            // 填充半透明绿色
            g2d.fillPolygon(xPoints, yPoints, points.size());
        }
    }
    
    /**
     * 绘制图像原点（红色点）
     * 注意：此方法在canvasPanel的paintComponent中调用，所以imageOriginPoint必须是相对于canvasPanel的坐标
     * 坐标转换关系：
     * - 屏幕坐标 = canvasPanel屏幕位置 + 画布坐标
     * - 画布坐标 = 屏幕坐标 - canvasPanel屏幕位置
     * - 图像坐标 = (画布坐标 - 图像画布偏移) / 缩放比例
     */
    private void drawImageOrigin(Graphics2D g2d) {
        if (imageOriginPoint == null) return;
        
        // 重新计算当前图像左上角在画布中的位置（因为图像可能被平移/缩放）
        int currentImgX = getImageX();
        int currentImgY = getImageY();
        
        // 更新原点位置为当前图像左上角位置
        imageOriginPoint = new Point(currentImgX, currentImgY);
        
        // 绘制红色圆点
        g2d.setColor(Color.RED);
        g2d.fillOval(imageOriginPoint.x - 8, imageOriginPoint.y - 8, 16, 16);
        
        // 绘制白色边框
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(imageOriginPoint.x - 8, imageOriginPoint.y - 8, 16, 16);
        
        // 绘制十字线
        g2d.drawLine(imageOriginPoint.x - 12, imageOriginPoint.y, imageOriginPoint.x + 12, imageOriginPoint.y);
        g2d.drawLine(imageOriginPoint.x, imageOriginPoint.y - 12, imageOriginPoint.x, imageOriginPoint.y + 12);
        
        // 绘制标签
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g2d.setColor(Color.RED);
        g2d.drawString("原点", imageOriginPoint.x + 15, imageOriginPoint.y - 10);
        
        LogUtil.log("[drawImageOrigin] 绘制原点于画布坐标=(" + imageOriginPoint.x + ", " + imageOriginPoint.y + ")");
    }
}
