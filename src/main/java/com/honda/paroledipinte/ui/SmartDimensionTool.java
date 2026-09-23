package com.honda.paroledipinte.ui;

import com.honda.paroledipinte.model.Shape;
import com.honda.paroledipinte.util.LogUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能尺寸工具类
 * 类似CAD的智能尺寸功能，自动吸附选择字符方框的边线并显示距离
 */
public class SmartDimensionTool {
    
    // 吸附阈值（像素）
    private static final int SNAP_THRESHOLD = 10;
    // 右键菜单搜索半径（像素）
    private static final int CONTEXT_MENU_RADIUS = 30;
    // 尺寸线颜色
    private static final Color DIMENSION_COLOR = new Color(0, 255, 255);
    // 尺寸文字颜色
    private static final Color TEXT_COLOR = new Color(255, 255, 0);
    // 吸附点颜色
    private static final Color SNAP_COLOR = new Color(255, 0, 255);
    
    private List<Shape> shapes;
    private double scaleFactor = 1.0;
    private int imageX = 0;
    private int imageY = 0;
    private BufferedImage image;
    
    // 尺寸测量状态
    private boolean isActive = false;
    private Point firstPoint = null;
    private Point secondPoint = null;
    private Shape firstShape = null;
    private Shape secondShape = null;
    private int firstEdgeType = -1; // 0:左, 1:右, 2:上, 3:下
    private int secondEdgeType = -1;
    private Point currentMousePoint = null;
    
    // 边线类型
    public static final int EDGE_LEFT = 0;
    public static final int EDGE_RIGHT = 1;
    public static final int EDGE_TOP = 2;
    public static final int EDGE_BOTTOM = 3;
    
    // 尺寸结果
    public List<DimensionResult> dimensions = new ArrayList<>();
    
    private JComponent parentComponent;
    
    // 右键菜单
    private JPopupMenu contextMenu;
    private Point rightClickPoint = null;
    private List<EdgeCandidate> contextMenuCandidates = new ArrayList<>();
    
    public SmartDimensionTool(JComponent parent) {
        this.parentComponent = parent;
        initContextMenu();
    }
    
    /**
     * 初始化右键菜单
     */
    private void initContextMenu() {
        contextMenu = new JPopupMenu();
        contextMenu.setBackground(new Color(40, 40, 40));
        contextMenu.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
    }
    
    /**
     * 设置形状列表
     */
    public void setShapes(List<Shape> shapes) {
        this.shapes = shapes;
    }
    
    /**
     * 设置变换参数
     * @param scaleFactor 缩放因子
     * @param imageX 图像在画布上的X坐标（屏幕坐标）
     * @param imageY 图像在画布上的Y坐标（屏幕坐标）
     */
    public void setTransform(double scaleFactor, int imageX, int imageY) {
        this.scaleFactor = scaleFactor;
        this.imageX = imageX;
        this.imageY = imageY;
    }
    
    /**
     * 设置图像
     */
    public void setImage(BufferedImage image) {
        this.image = image;
    }
    
    /**
     * 激活/停用智能尺寸工具
     */
    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            reset();
            hideContextMenu();
        }
        LogUtil.log("[SmartDimensionTool] " + (active ? "激活" : "停用"));
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 重置状态
     */
    public void reset() {
        firstPoint = null;
        secondPoint = null;
        firstShape = null;
        secondShape = null;
        firstEdgeType = -1;
        secondEdgeType = -1;
        currentMousePoint = null;
        rightClickPoint = null;
        contextMenuCandidates.clear();
    }
    
    /**
     * 清除所有尺寸
     */
    public void clearDimensions() {
        dimensions.clear();
        parentComponent.repaint();
    }
    
    /**
     * 隐藏右键菜单
     */
    public void hideContextMenu() {
        if (contextMenu != null && contextMenu.isVisible()) {
            contextMenu.setVisible(false);
        }
    }
    
    /**
     * 处理鼠标移动事件
     */
    public void mouseMoved(Point point) {
        if (!isActive) return;
        currentMousePoint = point;
        parentComponent.repaint();
    }
    
    /**
     * 处理鼠标点击事件
     */
    public void mouseClicked(Point point, int button) {
        if (!isActive) return;
        
        // 右键点击 - 显示上下文菜单
        if (button == MouseEvent.BUTTON3) {
            showContextMenu(point);
            return;
        }
        
        // 左键点击 - 正常选择
        if (button == MouseEvent.BUTTON1) {
            // 如果菜单正在显示，先隐藏
            hideContextMenu();
            
            SnapResult snap = findNearestEdge(point);
            if (snap == null) return;
            
            selectEdge(snap);
        }
    }
    
    /**
     * 选择边线
     */
    private void selectEdge(SnapResult snap) {
        if (firstPoint == null) {
            // 选择第一个点
            firstPoint = snap.snapPoint;
            firstShape = snap.shape;
            firstEdgeType = snap.edgeType;
            LogUtil.log("[SmartDimensionTool] 选择第一个点: shape=" + firstShape.getLabel() + ", edge=" + getEdgeName(firstEdgeType));
        } else {
            // 选择第二个点
            secondPoint = snap.snapPoint;
            secondShape = snap.shape;
            secondEdgeType = snap.edgeType;
            
            // 计算距离
            double distance = calculateDistance();
            boolean isHorizontal = isHorizontalMeasurement();
            
            // 保存尺寸结果
            DimensionResult dim = new DimensionResult(
                firstPoint, secondPoint, 
                firstShape, secondShape,
                firstEdgeType, secondEdgeType,
                distance, isHorizontal
            );
            dimensions.add(dim);
            
            LogUtil.log("[SmartDimensionTool] 添加尺寸: " + 
                String.format("%.1f像素 %s", distance, isHorizontal ? "水平" : "垂直"));
            
            // 重置选择
            reset();
        }
        
        parentComponent.repaint();
    }
    
    /**
     * 显示右键上下文菜单
     */
    private void showContextMenu(Point point) {
        rightClickPoint = point;
        contextMenuCandidates = findEdgesInRadius(point, CONTEXT_MENU_RADIUS);
        
        if (contextMenuCandidates.isEmpty()) {
            LogUtil.log("[SmartDimensionTool] 右键位置附近没有边线");
            return;
        }
        
        // 清空菜单
        contextMenu.removeAll();
        
        // 添加标题
        JMenuItem titleItem = new JMenuItem("选择边线 (" + contextMenuCandidates.size() + "个)");
        titleItem.setEnabled(false);
        titleItem.setForeground(Color.WHITE);
        titleItem.setBackground(new Color(40, 40, 40));
        contextMenu.add(titleItem);
        contextMenu.addSeparator();
        
        // 添加候选边线菜单项
        for (int i = 0; i < contextMenuCandidates.size(); i++) {
            EdgeCandidate candidate = contextMenuCandidates.get(i);
            String label = candidate.shape.getLabel();
            if (label == null || label.isEmpty()) label = "?";
            
            String edgeName = getEdgeName(candidate.edgeType);
            double distance = candidate.distance;
            
            String menuText = String.format("[%d] 字符 '%s' - %s边 (距离: %.1fpx)", 
                i + 1, label, edgeName, distance);
            
            JMenuItem menuItem = new JMenuItem(menuText);
            menuItem.setForeground(Color.WHITE);
            menuItem.setBackground(new Color(40, 40, 40));
            
            final int index = i;
            menuItem.addActionListener(e -> {
                EdgeCandidate selected = contextMenuCandidates.get(index);
                SnapResult snap = new SnapResult(selected.shape, selected.edgeType, selected.snapPoint);
                selectEdge(snap);
                contextMenu.setVisible(false);
            });
            
            contextMenu.add(menuItem);
        }
        
        // 显示菜单
        contextMenu.show(parentComponent, point.x, point.y);
        LogUtil.log("[SmartDimensionTool] 显示右键菜单，找到 " + contextMenuCandidates.size() + " 个候选边线");
    }
    
    /**
     * 查找半径内的所有边线
     */
    private List<EdgeCandidate> findEdgesInRadius(Point point, int radius) {
        List<EdgeCandidate> candidates = new ArrayList<>();
        if (shapes == null || shapes.isEmpty()) return candidates;
        
        for (Shape shape : shapes) {
            Rectangle2D rect = getShapeBounds(shape);
            if (rect == null) continue;
            
            // 检查四条边
            checkEdgeCandidate(candidates, shape, EDGE_LEFT, point, radius, 
                rect.getMinX(), rect.getMinY(), rect.getMaxY(), true);
            checkEdgeCandidate(candidates, shape, EDGE_RIGHT, point, radius, 
                rect.getMaxX(), rect.getMinY(), rect.getMaxY(), true);
            checkEdgeCandidate(candidates, shape, EDGE_TOP, point, radius, 
                rect.getMinY(), rect.getMinX(), rect.getMaxX(), false);
            checkEdgeCandidate(candidates, shape, EDGE_BOTTOM, point, radius, 
                rect.getMaxY(), rect.getMinX(), rect.getMaxX(), false);
        }
        
        // 按距离排序
        candidates.sort((a, b) -> Double.compare(a.distance, b.distance));
        
        return candidates;
    }
    
    /**
     * 检查边线候选
     */
    private void checkEdgeCandidate(List<EdgeCandidate> candidates, Shape shape, int edgeType,
                                    Point point, int radius, double edgePos, 
                                    double rangeStart, double rangeEnd, boolean isVertical) {
        // 计算点到边线的距离
        double distToEdge = isVertical ? Math.abs(point.x - edgePos) : Math.abs(point.y - edgePos);
        
        // 检查点是否在边的范围内（考虑半径）
        double pointPos = isVertical ? point.y : point.x;
        if (pointPos < rangeStart - radius || pointPos > rangeEnd + radius) {
            return;
        }
        
        // 计算点到边线最近点的距离
        double closestPos = Math.max(rangeStart, Math.min(pointPos, rangeEnd));
        double dx = isVertical ? point.x - edgePos : point.x - closestPos;
        double dy = isVertical ? point.y - closestPos : point.y - edgePos;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance <= radius) {
            Point snapPoint = isVertical ? 
                new Point((int)edgePos, (int)closestPos) :
                new Point((int)closestPos, (int)edgePos);
            candidates.add(new EdgeCandidate(shape, edgeType, snapPoint, distance));
        }
    }
    
    /**
     * 绘制尺寸标注
     */
    public void draw(Graphics2D g2d) {
        if (!isActive && dimensions.isEmpty()) return;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制已保存的尺寸
        for (DimensionResult dim : dimensions) {
            drawDimension(g2d, dim);
        }
        
        // 绘制当前选择状态
        if (isActive) {
            // 绘制右键菜单候选边线（高亮显示）
            if (contextMenu.isVisible() && rightClickPoint != null) {
                for (EdgeCandidate candidate : contextMenuCandidates) {
                    drawCandidateEdge(g2d, candidate);
                }
                // 绘制搜索范围圆
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.drawOval(rightClickPoint.x - CONTEXT_MENU_RADIUS, 
                            rightClickPoint.y - CONTEXT_MENU_RADIUS,
                            CONTEXT_MENU_RADIUS * 2, CONTEXT_MENU_RADIUS * 2);
            }
            
            // 绘制吸附提示
            if (currentMousePoint != null && !contextMenu.isVisible()) {
                SnapResult snap = findNearestEdge(currentMousePoint);
                if (snap != null) {
                    // 高亮吸附的边
                    drawHighlightedEdge(g2d, snap);
                }
            }
            
            // 绘制第一个点和连线
            if (firstPoint != null) {
                g2d.setColor(SNAP_COLOR);
                g2d.fillOval(firstPoint.x - 5, firstPoint.y - 5, 10, 10);
                
                if (currentMousePoint != null && !contextMenu.isVisible()) {
                    // 绘制临时连线
                    g2d.setColor(DIMENSION_COLOR);
                    g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                        0, new float[]{5, 5}, 0));
                    g2d.drawLine(firstPoint.x, firstPoint.y, currentMousePoint.x, currentMousePoint.y);
                    
                    // 预览距离
                    SnapResult snap = findNearestEdge(currentMousePoint);
                    if (snap != null) {
                        double distance = calculatePreviewDistance(firstPoint, snap.snapPoint);
                        boolean isHorizontal = Math.abs(snap.snapPoint.x - firstPoint.x) > 
                                               Math.abs(snap.snapPoint.y - firstPoint.y);
                        
                        g2d.setStroke(new BasicStroke(1));
                        String text = String.format("%.1fpx %s", distance, isHorizontal ? "H" : "V");
                        
                        int midX = (firstPoint.x + snap.snapPoint.x) / 2;
                        int midY = (firstPoint.y + snap.snapPoint.y) / 2;
                        
                        // 文字背景
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(text);
                        int textHeight = fm.getHeight();
                        g2d.setColor(new Color(0, 0, 0, 180));
                        g2d.fillRect(midX - textWidth/2 - 3, midY - textHeight/2 - 2, 
                                     textWidth + 6, textHeight + 4);
                        
                        // 文字
                        g2d.setColor(TEXT_COLOR);
                        g2d.drawString(text, midX - textWidth/2, midY + textHeight/4);
                    }
                }
            }
        }
    }
    
    /**
     * 绘制候选边线（用于右键菜单）
     */
    private void drawCandidateEdge(Graphics2D g2d, EdgeCandidate candidate) {
        Rectangle2D rect = getShapeBounds(candidate.shape);
        if (rect == null) return;
        
        // 根据距离设置透明度
        float alpha = (float)(1.0 - candidate.distance / CONTEXT_MENU_RADIUS);
        alpha = Math.max(0.3f, Math.min(1.0f, alpha));
        
        g2d.setColor(new Color(255, 255, 0, (int)(255 * alpha)));
        g2d.setStroke(new BasicStroke(2));
        
        switch (candidate.edgeType) {
            case EDGE_LEFT:
                g2d.drawLine((int)rect.getMinX(), (int)rect.getMinY(), 
                            (int)rect.getMinX(), (int)rect.getMaxY());
                break;
            case EDGE_RIGHT:
                g2d.drawLine((int)rect.getMaxX(), (int)rect.getMinY(), 
                            (int)rect.getMaxX(), (int)rect.getMaxY());
                break;
            case EDGE_TOP:
                g2d.drawLine((int)rect.getMinX(), (int)rect.getMinY(), 
                            (int)rect.getMaxX(), (int)rect.getMinY());
                break;
            case EDGE_BOTTOM:
                g2d.drawLine((int)rect.getMinX(), (int)rect.getMaxY(), 
                            (int)rect.getMaxX(), (int)rect.getMaxY());
                break;
        }
        
        // 绘制吸附点
        g2d.fillOval(candidate.snapPoint.x - 4, candidate.snapPoint.y - 4, 8, 8);
    }
    
    /**
     * 绘制单个尺寸标注
     */
    private void drawDimension(Graphics2D g2d, DimensionResult dim) {
        Point p1 = dim.point1;
        Point p2 = dim.point2;
        
        // 绘制尺寸线
        g2d.setColor(DIMENSION_COLOR);
        g2d.setStroke(new BasicStroke(2));
        
        // 绘制延长线（从边线延伸出来）
        int extendLength = 15;
        if (dim.isHorizontal) {
            // 水平尺寸，延长线垂直
            int y = Math.min(p1.y, p2.y) - extendLength;
            g2d.drawLine(p1.x, p1.y, p1.x, y);
            g2d.drawLine(p2.x, p2.y, p2.x, y);
            // 尺寸线
            g2d.drawLine(p1.x, y, p2.x, y);
            // 箭头
            drawArrow(g2d, p1.x, y, true, true);
            drawArrow(g2d, p2.x, y, true, false);
        } else {
            // 垂直尺寸，延长线水平
            int x = Math.min(p1.x, p2.x) - extendLength;
            g2d.drawLine(p1.x, p1.y, x, p1.y);
            g2d.drawLine(p2.x, p2.y, x, p2.y);
            // 尺寸线
            g2d.drawLine(x, p1.y, x, p2.y);
            // 箭头
            drawArrow(g2d, x, p1.y, false, true);
            drawArrow(g2d, x, p2.y, false, false);
        }
        
        // 绘制文字
        String text = String.format("%.1fpx", dim.distance);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int midX, midY;
        if (dim.isHorizontal) {
            midX = (p1.x + p2.x) / 2;
            midY = Math.min(p1.y, p2.y) - extendLength;
        } else {
            midX = Math.min(p1.x, p2.x) - extendLength;
            midY = (p1.y + p2.y) / 2;
        }
        
        // 文字背景
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(midX - textWidth/2 - 4, midY - textHeight/2 - 2, textWidth + 8, textHeight + 4);
        
        // 文字
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        g2d.drawString(text, midX - textWidth/2, midY + textHeight/4);
        
        // 绘制端点标记
        g2d.setColor(SNAP_COLOR);
        g2d.fillOval(p1.x - 4, p1.y - 4, 8, 8);
        g2d.fillOval(p2.x - 4, p2.y - 4, 8, 8);
    }
    
    /**
     * 绘制箭头
     */
    private void drawArrow(Graphics2D g2d, int x, int y, boolean horizontal, boolean leftOrUp) {
        int size = 6;
        int[] xPoints, yPoints;
        
        if (horizontal) {
            if (leftOrUp) {
                xPoints = new int[]{x, x + size, x + size};
                yPoints = new int[]{y, y - size/2, y + size/2};
            } else {
                xPoints = new int[]{x, x - size, x - size};
                yPoints = new int[]{y, y - size/2, y + size/2};
            }
        } else {
            if (leftOrUp) {
                xPoints = new int[]{x, x - size/2, x + size/2};
                yPoints = new int[]{y, y + size, y + size};
            } else {
                xPoints = new int[]{x, x - size/2, x + size/2};
                yPoints = new int[]{y, y - size, y - size};
            }
        }
        
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
    
    /**
     * 高亮显示吸附的边
     */
    private void drawHighlightedEdge(Graphics2D g2d, SnapResult snap) {
        Rectangle2D rect = getShapeBounds(snap.shape);
        if (rect == null) return;
        
        g2d.setColor(SNAP_COLOR);
        g2d.setStroke(new BasicStroke(3));
        
        switch (snap.edgeType) {
            case EDGE_LEFT:
                g2d.drawLine((int)rect.getMinX(), (int)rect.getMinY(), 
                            (int)rect.getMinX(), (int)rect.getMaxY());
                break;
            case EDGE_RIGHT:
                g2d.drawLine((int)rect.getMaxX(), (int)rect.getMinY(), 
                            (int)rect.getMaxX(), (int)rect.getMaxY());
                break;
            case EDGE_TOP:
                g2d.drawLine((int)rect.getMinX(), (int)rect.getMinY(), 
                            (int)rect.getMaxX(), (int)rect.getMinY());
                break;
            case EDGE_BOTTOM:
                g2d.drawLine((int)rect.getMinX(), (int)rect.getMaxY(), 
                            (int)rect.getMaxX(), (int)rect.getMaxY());
                break;
        }
        
        // 绘制吸附点
        g2d.fillOval(snap.snapPoint.x - 5, snap.snapPoint.y - 5, 10, 10);
    }
    
    /**
     * 查找最近的边线
     */
    private SnapResult findNearestEdge(Point point) {
        if (shapes == null || shapes.isEmpty()) return null;
        
        SnapResult bestSnap = null;
        double bestDistance = SNAP_THRESHOLD;
        
        for (Shape shape : shapes) {
            Rectangle2D rect = getShapeBounds(shape);
            if (rect == null) continue;
            
            // 检查四条边
            double distLeft = Math.abs(point.x - rect.getMinX());
            double distRight = Math.abs(point.x - rect.getMaxX());
            double distTop = Math.abs(point.y - rect.getMinY());
            double distBottom = Math.abs(point.y - rect.getMaxY());
            
            // 检查是否在边的范围内
            boolean inYRange = point.y >= rect.getMinY() - SNAP_THRESHOLD && 
                              point.y <= rect.getMaxY() + SNAP_THRESHOLD;
            boolean inXRange = point.x >= rect.getMinX() - SNAP_THRESHOLD && 
                              point.x <= rect.getMaxX() + SNAP_THRESHOLD;
            
            // 左边
            if (distLeft < bestDistance && inYRange) {
                bestDistance = distLeft;
                bestSnap = new SnapResult(shape, EDGE_LEFT, 
                    new Point((int)rect.getMinX(), point.y));
            }
            // 右边
            if (distRight < bestDistance && inYRange) {
                bestDistance = distRight;
                bestSnap = new SnapResult(shape, EDGE_RIGHT, 
                    new Point((int)rect.getMaxX(), point.y));
            }
            // 上边
            if (distTop < bestDistance && inXRange) {
                bestDistance = distTop;
                bestSnap = new SnapResult(shape, EDGE_TOP, 
                    new Point(point.x, (int)rect.getMinY()));
            }
            // 下边
            if (distBottom < bestDistance && inXRange) {
                bestDistance = distBottom;
                bestSnap = new SnapResult(shape, EDGE_BOTTOM, 
                    new Point(point.x, (int)rect.getMaxY()));
            }
        }
        
        return bestSnap;
    }
    
    /**
     * 获取形状的边界矩形（屏幕坐标）
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
        
        // 转换为屏幕坐标
        int imgX = imageX;
        int imgY = imageY;
        
        int screenMinX = imgX + (int) (minX * scaleFactor);
        int screenMinY = imgY + (int) (minY * scaleFactor);
        int screenMaxX = imgX + (int) (maxX * scaleFactor);
        int screenMaxY = imgY + (int) (maxY * scaleFactor);
        
        return new Rectangle2D.Double(screenMinX, screenMinY, 
            screenMaxX - screenMinX, screenMaxY - screenMinY);
    }
    

    
    /**
     * 计算两点之间的距离（转换回原始图像坐标）
     */
    private double calculateDistance() {
        if (firstPoint == null || secondPoint == null) return 0;
        
        double dx = Math.abs(secondPoint.x - firstPoint.x) / scaleFactor;
        double dy = Math.abs(secondPoint.y - firstPoint.y) / scaleFactor;
        
        // 根据边线类型确定是水平还是垂直距离
        if (firstEdgeType == EDGE_LEFT || firstEdgeType == EDGE_RIGHT ||
            secondEdgeType == EDGE_LEFT || secondEdgeType == EDGE_RIGHT) {
            // 水平边参与，返回水平距离
            if (firstEdgeType == EDGE_TOP || firstEdgeType == EDGE_BOTTOM ||
                secondEdgeType == EDGE_TOP || secondEdgeType == EDGE_BOTTOM) {
                // 垂直边也参与，返回直线距离
                return Math.sqrt(dx * dx + dy * dy);
            }
            return dx;
        }
        return dy;
    }
    
    /**
     * 计算预览距离
     */
    private double calculatePreviewDistance(Point p1, Point p2) {
        double dx = Math.abs(p2.x - p1.x) / scaleFactor;
        double dy = Math.abs(p2.y - p1.y) / scaleFactor;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 判断是否为水平测量
     */
    private boolean isHorizontalMeasurement() {
        if (firstPoint == null || secondPoint == null) return true;
        return Math.abs(secondPoint.x - firstPoint.x) > Math.abs(secondPoint.y - firstPoint.y);
    }
    
    /**
     * 获取边线名称
     */
    private String getEdgeName(int edgeType) {
        switch (edgeType) {
            case EDGE_LEFT: return "左";
            case EDGE_RIGHT: return "右";
            case EDGE_TOP: return "上";
            case EDGE_BOTTOM: return "下";
            default: return "未知";
        }
    }
    
    /**
     * 吸附结果类
     */
    private static class SnapResult {
        Shape shape;
        int edgeType;
        Point snapPoint;
        
        SnapResult(Shape shape, int edgeType, Point snapPoint) {
            this.shape = shape;
            this.edgeType = edgeType;
            this.snapPoint = snapPoint;
        }
    }
    
    /**
     * 边线候选类（用于右键菜单）
     */
    private static class EdgeCandidate {
        Shape shape;
        int edgeType;
        Point snapPoint;
        double distance;
        
        EdgeCandidate(Shape shape, int edgeType, Point snapPoint, double distance) {
            this.shape = shape;
            this.edgeType = edgeType;
            this.snapPoint = snapPoint;
            this.distance = distance;
        }
    }
    
    /**
     * 尺寸结果类
     */
    public static class DimensionResult {
        Point point1, point2;
        Shape shape1, shape2;
        int edgeType1, edgeType2;
        double distance;
        boolean isHorizontal;
        
        public DimensionResult(Point point1, Point point2, 
                              Shape shape1, Shape shape2,
                              int edgeType1, int edgeType2,
                              double distance, boolean isHorizontal) {
            this.point1 = point1;
            this.point2 = point2;
            this.shape1 = shape1;
            this.shape2 = shape2;
            this.edgeType1 = edgeType1;
            this.edgeType2 = edgeType2;
            this.distance = distance;
            this.isHorizontal = isHorizontal;
        }
    }
}
