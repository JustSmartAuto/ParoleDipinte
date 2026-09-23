package com.honda.paroledipinte.image;

import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.lua.LuaScriptEngine;
import com.honda.paroledipinte.model.CharRelativePosition;
import com.honda.paroledipinte.util.LogUtil;

import org.bytedeco.javacpp.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * 文本斑点检测器
 * 基于HALCON OCR算法总结的JavaCV实现
 */
public class TextBlobDetector {
    
    // 参数配置
    private int ocrGray;
    private int minArea;
    private int minWidth;
    private int minHeight;
    private int maxWidth;
    private int maxHeight;
    private boolean invertImage;
    
    // 新增功能开关
    private boolean forceWidthSplitEnabled;  // 强制宽度分割
    private boolean multiLineSortEnabled;    // 多行排序
    private int forceSplitWidth;             // 强制分割宽度
    private int rowGapThreshold;             // 行间距阈值
    
    // 字符内部空隙参数（用于处理汉字"二"和"川"等多笔画字符）
    private int maxInternalVerticalGap;      // 字符内部垂直方向空隙最大值
    private int maxInternalHorizontalGap;    // 字符内部水平方向空隙最大值
    
    // 忽略边界区域参数
    private int ignoreBorderWidth;           // 忽略图片边界区域宽度
    
    // Lua脚本引擎（用于文本定位后处理）
    private LuaScriptEngine luaScriptEngine;
    private String correctString;            // 正确字符串，用于Lua脚本判断全角/半角
    
    // 预设参数和区域信息（用于Lua脚本）
    private java.util.Map<String, Object> presetParams;  // 当前预设参数
    private List<Region> rawRegions;                     // OCR参数分割出的原始字符框
    private List<Region> correctedRegions;               // 字符相对位置修正后的字符框
    
    // 颜色定义 (BGR格式)
    private static final Scalar COLOR_GREEN = new Scalar(0, 255, 0, 0);
    private static final Scalar COLOR_ORANGE = new Scalar(0, 165, 255, 0);
    private static final Scalar COLOR_RED = new Scalar(0, 0, 255, 0);
    private static final Scalar COLOR_WHITE = new Scalar(255, 255, 255, 0);
    private static final Scalar COLOR_BLACK = new Scalar(0, 0, 0, 0);
    
    // 连通区域统计常量
    private static final int CC_STAT_LEFT = 0;
    private static final int CC_STAT_TOP = 1;
    private static final int CC_STAT_WIDTH = 2;
    private static final int CC_STAT_HEIGHT = 3;
    private static final int CC_STAT_AREA = 4;
    
    /**
     * 文本区域
     */
    public static class Region {
        public int x, y, width, height;
        
        public Region(int x, int y, int width, int height) {
            LogUtil.log("[Region] 开始执行");
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            LogUtil.log("[Region] 执行完成");
        }
        
        @Override
        public String toString() {
            return String.format("Region(x=%d, y=%d, w=%d, h=%d)", x, y, width, height);
        }
    }
    
    /**
     * 检测结果
     */
    public static class DetectionResult {
        public List<Region> textRegions;
        public List<Region> blobRegions;
        public List<Region> missedRegions;
        public Mat visualizedImage;
        
        public DetectionResult(List<Region> textRegions, List<Region> blobRegions, 
                              List<Region> missedRegions, Mat visualizedImage) {
            LogUtil.log("[DetectionResult] 开始执行");
            this.textRegions = textRegions;
            this.blobRegions = blobRegions;
            this.missedRegions = missedRegions;
            this.visualizedImage = visualizedImage;
            LogUtil.log("[DetectionResult] 执行完成");
        }
    }
    
    public TextBlobDetector() {
        this(135, 15, 3, 3, 500, 200, false, false, false, 31, 20, 197, 497, 5);
        LogUtil.log("[TextBlobDetector] 执行完成");
    }
    
    public TextBlobDetector(int ocrGray, int minArea, int minWidth, int minHeight,
                           int maxWidth, int maxHeight, boolean invertImage) {
        this(ocrGray, minArea, minWidth, minHeight, maxWidth, maxHeight, invertImage, false, false, 31, 20, 197, 497, 5);
    }
    
    public TextBlobDetector(int ocrGray, int minArea, int minWidth, int minHeight,
                           int maxWidth, int maxHeight, boolean invertImage,
                           boolean forceWidthSplitEnabled, boolean multiLineSortEnabled,
                           int forceSplitWidth, int rowGapThreshold) {
        this(ocrGray, minArea, minWidth, minHeight, maxWidth, maxHeight, invertImage, 
             forceWidthSplitEnabled, multiLineSortEnabled, forceSplitWidth, rowGapThreshold, 197, 497, 5);
    }
    
    public TextBlobDetector(int ocrGray, int minArea, int minWidth, int minHeight,
                           int maxWidth, int maxHeight, boolean invertImage,
                           boolean forceWidthSplitEnabled, boolean multiLineSortEnabled,
                           int forceSplitWidth, int rowGapThreshold,
                           int maxInternalVerticalGap, int maxInternalHorizontalGap, int ignoreBorderWidth) {
        LogUtil.log("[TextBlobDetector] 开始执行");
        this.ocrGray = ocrGray;
        this.minArea = minArea;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.invertImage = invertImage;
        this.forceWidthSplitEnabled = forceWidthSplitEnabled;
        this.multiLineSortEnabled = multiLineSortEnabled;
        this.forceSplitWidth = forceSplitWidth;
        this.rowGapThreshold = rowGapThreshold;
        this.maxInternalVerticalGap = maxInternalVerticalGap;
        this.maxInternalHorizontalGap = maxInternalHorizontalGap;
        this.ignoreBorderWidth = ignoreBorderWidth;
        LogUtil.log("[TextBlobDetector] 执行完成");
    }
    
    /**
     * 设置参数
     */
    public void setParameters(int ocrGray, int minArea, int minWidth, int minHeight,
                             int maxWidth, int maxHeight) {
        LogUtil.log("[setParameters] 开始执行");
        this.ocrGray = ocrGray;
        this.minArea = minArea;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        LogUtil.log("[setParameters] 执行完成");
    }
    
    /**
     * 设置强制宽度分割参数
     */
    public void setForceWidthSplit(boolean enabled, int splitWidth) {
        LogUtil.log("[setForceWidthSplit] 开始执行");
        this.forceWidthSplitEnabled = enabled;
        this.forceSplitWidth = splitWidth;
        LogUtil.log("[setForceWidthSplit] 执行完成");
    }
    
    /**
     * 设置多行排序参数
     */
    public void setMultiLineSort(boolean enabled, int rowGapThreshold) {
        LogUtil.log("[setMultiLineSort] 开始执行");
        this.multiLineSortEnabled = enabled;
        this.rowGapThreshold = rowGapThreshold;
        LogUtil.log("[setMultiLineSort] 执行完成");
    }
    
    /**
     * 设置字符内部空隙参数
     */
    public void setInternalGapParams(int maxInternalVerticalGap, int maxInternalHorizontalGap) {
        LogUtil.log("[setInternalGapParams] 开始执行");
        this.maxInternalVerticalGap = maxInternalVerticalGap;
        this.maxInternalHorizontalGap = maxInternalHorizontalGap;
        LogUtil.log("[setInternalGapParams] 执行完成");
    }
    
    /**
     * 设置忽略边界区域参数
     */
    public void setIgnoreBorderWidth(int ignoreBorderWidth) {
        LogUtil.log("[setIgnoreBorderWidth] 开始执行");
        this.ignoreBorderWidth = ignoreBorderWidth;
        LogUtil.log("[setIgnoreBorderWidth] 执行完成");
    }
    
    /**
     * 设置Lua脚本引擎
     */
    public void setLuaScriptEngine(LuaScriptEngine engine) {
        LogUtil.log("[setLuaScriptEngine] 开始执行");
        this.luaScriptEngine = engine;
        LogUtil.log("[setLuaScriptEngine] 执行完成");
    }
    
    /**
     * 设置正确字符串
     */
    public void setCorrectString(String correctString) {
        LogUtil.log("[setCorrectString] 开始执行");
        this.correctString = correctString;
        LogUtil.log("[setCorrectString] 执行完成");
    }
    
    /**
     * 设置预设参数（供Lua脚本访问）
     */
    public void setPresetParams(Map<String, Object> presetParams) {
        LogUtil.log("[setPresetParams] 开始执行");
        this.presetParams = presetParams;
        LogUtil.log("[setPresetParams] 执行完成");
    }
    
    /**
     * 设置原始字符框（OCR参数分割出的原始字符框）
     */
    public void setRawRegions(List<Region> rawRegions) {
        LogUtil.log("[setRawRegions] 开始执行");
        this.rawRegions = rawRegions;
        LogUtil.log("[setRawRegions] 执行完成");
    }
    
    /**
     * 设置修正后字符框（字符相对位置修正后的字符框）
     */
    public void setCorrectedRegions(List<Region> correctedRegions) {
        LogUtil.log("[setCorrectedRegions] 开始执行");
        this.correctedRegions = correctedRegions;
        LogUtil.log("[setCorrectedRegions] 执行完成");
    }
    
    /**
     * 判断字符是否为全角字符
     * 全角字符：中文字符、全角标点等（Unicode编码大于127）
     * 半角字符：ASCII字符（0-127）
     */
    public static boolean isFullWidthChar(char c) {
        return c > 127;
    }
    
    /**
     * 根据correctString判断文本类型
     * @return true-全角文本, false-半角文本
     */
    public boolean isFullWidthText() {
        if (correctString == null || correctString.isEmpty()) {
            return true; // 默认为全角
        }
        return isFullWidthChar(correctString.charAt(0));
    }
    
    /**
     * 获取用于合并框的最大宽度
     * 全角字符使用maxWidth，半角字符使用maxWidth/2
     */
    public int getMaxMergeWidth() {
        if (isFullWidthText()) {
            return maxWidth;
        } else {
            return maxWidth / 2;
        }
    }
    
    /**
     * 预处理图像
     */
    public Mat preprocessImage(Mat image) {
        LogUtil.log("[preprocessImage] 开始执行");
        long startTime = System.currentTimeMillis();
        Mat gray = new Mat();
        
        if (image.channels() == 3) {
            cvtColor(image, gray, COLOR_BGR2GRAY);
        } else {
            image.copyTo(gray);
        }
        
        if (invertImage) {
            bitwise_not(gray, gray);
        }
        
        // 对比度增强
        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        minMaxLoc(gray, minVal, maxVal, null, null, new Mat());
        double minGray = minVal.get(0);
        double maxGray = maxVal.get(0);
        
        if (maxGray > minGray) {
            double alpha = 255.0 / (maxGray - minGray);
            double beta = -(255.0 * minGray) / (maxGray - minGray);
            gray.convertTo(gray, -1, alpha, beta);
        }
        
        // 锐化
        Mat kernel = new Mat(3, 3, CV_32F);
        FloatPointer kernelPtr = new FloatPointer(kernel.ptr());
        kernelPtr.put(0, -1); kernelPtr.put(1, -1); kernelPtr.put(2, -1);
        kernelPtr.put(3, -1); kernelPtr.put(4,  9); kernelPtr.put(5, -1);
        kernelPtr.put(6, -1); kernelPtr.put(7, -1); kernelPtr.put(8, -1);
        
        Mat sharpened = new Mat();
        filter2D(gray, sharpened, -1, kernel);
        
        gray.release();
        kernel.release();
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        LogUtil.log("[preprocessImage] 执行完成，处理时间: " + elapsedTime + "ms");
        return sharpened;
    }
    
    private int getStat(Mat stats, int label, int statType) {
        IntPointer ptr = new IntPointer(stats.ptr(label));
        return ptr.get(statType);
    }
    
    /**
     * 分割字符
     */
    public Mat[] segmentCharacters(Mat image, List<Region> blobRegions) {
        LogUtil.log("[segmentCharacters] 开始执行");
        Mat binary = new Mat();
        
        if (invertImage) {
            Mat meanImg = new Mat();
            blur(image, meanImg, new Size(35, 35));
            Mat thresholdImg = new Mat();
            Mat offsetMat = new Mat(meanImg.size(), meanImg.type(), new Scalar(7));
            subtract(meanImg, offsetMat, thresholdImg);
            offsetMat.release();
            compare(image, thresholdImg, binary, CMP_LT);
            meanImg.release();
            thresholdImg.release();
        } else {
            threshold(image, binary, ocrGray, 255, THRESH_BINARY_INV);
        }
        
        Mat originalBinary = binary.clone();
        
        // 连通区域分析
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int numLabels = connectedComponentsWithStats(binary, labels, stats, centroids, 8, CV_32S);
        
        // 筛选有效区域
        List<Integer> validLabels = new ArrayList<>();
        for (int i = 1; i < numLabels; i++) {
            int area = getStat(stats, i, CC_STAT_AREA);
            int width = getStat(stats, i, CC_STAT_WIDTH);
            int height = getStat(stats, i, CC_STAT_HEIGHT);
            
            if (minArea <= area && area <= 9000 &&
                minWidth <= width && width <= maxWidth &&
                minHeight <= height && height <= maxHeight) {
                validLabels.add(i);
                int x = getStat(stats, i, CC_STAT_LEFT);
                int y = getStat(stats, i, CC_STAT_TOP);
                blobRegions.add(new Region(x, y, width, height));
            }
        }
        
        // 创建字符掩码
        Mat charMask = new Mat(binary.size(), CV_8UC1, Scalar.ZERO);
        for (int label : validLabels) {
            Mat mask = new Mat();
            Mat labelMat = new Mat(labels.size(), labels.type(), new Scalar(label));
            compare(labels, labelMat, mask, CMP_EQ);
            bitwise_or(charMask, mask, charMask);
            mask.release();
            labelMat.release();
        }
        
        // 水平闭运算连接断裂字符
        Mat kernel = getStructuringElement(MORPH_RECT, new Size(1, 21));
        morphologyEx(charMask, charMask, MORPH_CLOSE, kernel);
        
        labels.release();
        stats.release();
        centroids.release();
        kernel.release();
        
        LogUtil.log("[segmentCharacters] 执行完成");
        return new Mat[]{originalBinary, charMask};
    }
    
    /**
     * 强制宽度分割
     * 当自动分割的字符数量与预期不符时，使用固定宽度强制分割
     * 参考HALCON的PartitionRectangle算法
     */
    public List<Region> forceWidthSplit(List<Region> regions, int expectedCount) {
        LogUtil.log("[forceWidthSplit] 开始执行");
        if (regions == null || regions.isEmpty()) {
            LogUtil.log("[forceWidthSplit] 执行完成 - 无区域");
            return regions;
        }
        
        // 如果区域数量与预期不符，进行强制分割
        if (regions.size() != expectedCount && expectedCount > 0) {
            List<Region> result = new ArrayList<>();
            
            for (Region region : regions) {
                // 计算该区域应该分割成多少个字符
                int charCount = Math.max(1, (int) Math.round((double) region.width / forceSplitWidth));
                
                // 如果实际字符数与预期差距较大，进行分割
                if (charCount > 1) {
                    int charWidth = region.width / charCount;
                    for (int i = 0; i < charCount; i++) {
                        int x = region.x + i * charWidth;
                        int width = (i == charCount - 1) ? (region.x + region.width - x) : charWidth;
                        result.add(new Region(x, region.y, width, region.height));
                    }
                } else {
                    result.add(region);
                }
            }
            
            LogUtil.log("[forceWidthSplit] 强制分割完成: " + regions.size() + " -> " + result.size());
            return result;
        }
        
        LogUtil.log("[forceWidthSplit] 无需分割");
        return regions;
    }
    
    /**
     * 多行字符排序算法
     * 参考HALCON的多行排序策略：先按行排序，再对每行内的字符按列排序
     */
    public List<Region> sortMultiLineRegions(List<Region> regions) {
        LogUtil.log("[sortMultiLineRegions] 开始执行");
        if (regions == null || regions.isEmpty()) {
            LogUtil.log("[sortMultiLineRegions] 执行完成 - 无区域");
            return regions;
        }
        
        if (!multiLineSortEnabled) {
            // 单行模式：只按X坐标排序
            List<Region> sorted = new ArrayList<>(regions);
            sorted.sort(Comparator.comparingInt(r -> r.x));
            LogUtil.log("[sortMultiLineRegions] 单行排序完成: " + sorted.size() + " 个区域");
            return sorted;
        }
        
        // 多行模式
        // 1. 按Y坐标分组（识别行）
        List<List<Region>> rows = groupRegionsByRow(regions);
        
        // 2. 对每行内的字符按X坐标排序
        List<Region> result = new ArrayList<>();
        for (List<Region> row : rows) {
            row.sort(Comparator.comparingInt(r -> r.x));
            result.addAll(row);
        }
        
        LogUtil.log("[sortMultiLineRegions] 多行排序完成: " + rows.size() + " 行, 共 " + result.size() + " 个区域");
        return result;
    }
    
    /**
     * 按行分组区域
     */
    private List<List<Region>> groupRegionsByRow(List<Region> regions) {
        LogUtil.log("[groupRegionsByRow] 开始执行");
        // 先按Y坐标排序
        List<Region> sortedByY = new ArrayList<>(regions);
        sortedByY.sort(Comparator.comparingInt(r -> r.y));
        
        List<List<Region>> rows = new ArrayList<>();
        List<Region> currentRow = new ArrayList<>();
        
        for (Region region : sortedByY) {
            if (currentRow.isEmpty()) {
                currentRow.add(region);
            } else {
                // 检查是否与当前行在同一行（Y坐标差距小于阈值）
                Region lastInRow = currentRow.get(currentRow.size() - 1);
                if (Math.abs(region.y - lastInRow.y) < rowGapThreshold) {
                    currentRow.add(region);
                } else {
                    // 新行
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                    currentRow.add(region);
                }
            }
        }
        
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        
        LogUtil.log("[groupRegionsByRow] 分组完成: " + rows.size() + " 行");
        return rows;
    }
    
    /**
     * 合并重叠区域（传统方式）
     */
    public List<Region> mergeOverlappingRegions(List<Region> regions) {
        LogUtil.log("[mergeOverlappingRegions] 开始执行");
        if (regions == null || regions.isEmpty()) {
            LogUtil.log("[mergeOverlappingRegions] 执行完成");
            return new ArrayList<>();
        }
        
        List<Region> result = new ArrayList<>(regions);
        boolean changed = true;
        int maxIterations = 100; // 防止无限循环
        int iterations = 0;
        
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            List<Region> merged = new ArrayList<>();
            boolean[] used = new boolean[result.size()];
            
            for (int i = 0; i < result.size(); i++) {
                if (used[i]) continue;
                
                Region r1 = result.get(i);
                int x1 = r1.x, y1 = r1.y, x1End = r1.x + r1.width, y1End = r1.y + r1.height;
                int[] current = {x1, y1, x1End, y1End};
                
                for (int j = i + 1; j < result.size(); j++) {
                    if (used[j]) continue;
                    
                    Region r2 = result.get(j);
                    int x2 = r2.x, y2 = r2.y, x2End = r2.x + r2.width, y2End = r2.y + r2.height;
                    
                    int xi1 = Math.max(current[0], x2);
                    int yi1 = Math.max(current[1], y2);
                    int xi2 = Math.min(current[2], x2End);
                    int yi2 = Math.min(current[3], y2End);
                    
                    if (xi2 > xi1 && yi2 > yi1) {
                        current[0] = Math.min(current[0], x2);
                        current[1] = Math.min(current[1], y2);
                        current[2] = Math.max(current[2], x2End);
                        current[3] = Math.max(current[3], y2End);
                        used[j] = true;
                        changed = true;
                    }
                }
                
                merged.add(new Region(current[0], current[1], current[2] - current[0], current[3] - current[1]));
                used[i] = true;
            }
            
            result = merged;
        }
        
        LogUtil.log("[mergeOverlappingRegions] 执行完成");
        return result;
    }
    
    /**
     * 合并重叠区域（考虑字符内部空隙，用于处理汉字"二"和"川"等多笔画字符）
     * 当两个区域之间的空隙小于阈值时，认为它们属于同一个字符
     */
    public List<Region> mergeOverlappingRegionsWithGap(List<Region> regions) {
        LogUtil.log("[mergeOverlappingRegionsWithGap] 开始执行");
        if (regions == null || regions.isEmpty()) {
            LogUtil.log("[mergeOverlappingRegionsWithGap] 执行完成");
            return new ArrayList<>();
        }
        
        List<Region> result = new ArrayList<>(regions);
        boolean changed = true;
        int maxIterations = 100; // 防止无限循环
        int iterations = 0;
        
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            List<Region> merged = new ArrayList<>();
            boolean[] used = new boolean[result.size()];
            
            for (int i = 0; i < result.size(); i++) {
                if (used[i]) continue;
                
                Region r1 = result.get(i);
                int x1 = r1.x, y1 = r1.y, x1End = r1.x + r1.width, y1End = r1.y + r1.height;
                int[] current = {x1, y1, x1End, y1End};
                
                for (int j = i + 1; j < result.size(); j++) {
                    if (used[j]) continue;
                    
                    Region r2 = result.get(j);
                    int x2 = r2.x, y2 = r2.y, x2End = r2.x + r2.width, y2End = r2.y + r2.height;
                    
                    // 计算水平方向和垂直方向的距离
                    int horizontalGap = Math.max(0, Math.max(x2 - x1End, x1 - x2End));
                    int verticalGap = Math.max(0, Math.max(y2 - y1End, y1 - y2End));
                    
                    // 判断是否重叠或空隙在允许范围内
                    boolean shouldMerge = false;
                    
                    // 传统重叠判断
                    int xi1 = Math.max(current[0], x2);
                    int yi1 = Math.max(current[1], y2);
                    int xi2 = Math.min(current[2], x2End);
                    int yi2 = Math.min(current[3], y2End);
                    
                    if (xi2 > xi1 && yi2 > yi1) {
                        // 有重叠
                        shouldMerge = true;
                    } else if (horizontalGap <= maxInternalHorizontalGap && verticalGap <= maxInternalVerticalGap) {
                        // 空隙在允许范围内（处理"二"、"川"等多笔画字符）
                        // 要求两个区域在水平或垂直方向上有一定的投影重叠
                        boolean horizontalOverlap = (x1 < x2End && x1End > x2);
                        boolean verticalOverlap = (y1 < y2End && y1End > y2);
                        
                        if (horizontalOverlap || verticalOverlap) {
                            shouldMerge = true;
                            LogUtil.log("[mergeOverlappingRegionsWithGap] 合并空隙内的区域: (" + 
                                       r1.x + "," + r1.y + ") 和 (" + r2.x + "," + r2.y + ")");
                        }
                    }
                    
                    if (shouldMerge) {
                        current[0] = Math.min(current[0], x2);
                        current[1] = Math.min(current[1], y2);
                        current[2] = Math.max(current[2], x2End);
                        current[3] = Math.max(current[3], y2End);
                        used[j] = true;
                        changed = true;
                    }
                }
                
                merged.add(new Region(current[0], current[1], current[2] - current[0], current[3] - current[1]));
                used[i] = true;
            }
            
            result = merged;
        }
        
        LogUtil.log("[mergeOverlappingRegionsWithGap] 执行完成，合并后区域数: " + result.size());
        return result;
    }
    
    /**
     * 查找漏字区域
     */
    public List<Region> findMissedRegions(Mat binary, List<Region> textRegions, List<Region> blobRegions) {
        LogUtil.log("[findMissedRegions] 开始执行");
        List<Region> allRegions = new ArrayList<>();
        allRegions.addAll(textRegions);
        allRegions.addAll(blobRegions);
        
        Mat detectedMask = new Mat(binary.size(), CV_8UC1, Scalar.ZERO);
        for (Region r : allRegions) {
            int x1 = Math.max(0, r.x - 2);
            int y1 = Math.max(0, r.y - 2);
            int x2 = Math.min(binary.cols(), r.x + r.width + 2);
            int y2 = Math.min(binary.rows(), r.y + r.height + 2);
            rectangle(detectedMask, new Point(x1, y1), new Point(x2, y2), new Scalar(255), -1, LINE_8, 0);
        }
        
        Mat missedMask = new Mat();
        Mat notDetected = new Mat();
        bitwise_not(detectedMask, notDetected);
        bitwise_and(binary, notDetected, missedMask);
        
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int numLabels = connectedComponentsWithStats(missedMask, labels, stats, centroids, 8, CV_32S);
        
        List<Region> missedRegions = new ArrayList<>();
        for (int i = 1; i < numLabels; i++) {
            int area = getStat(stats, i, CC_STAT_AREA);
            int width = getStat(stats, i, CC_STAT_WIDTH);
            int height = getStat(stats, i, CC_STAT_HEIGHT);
            
            if (area >= minArea && 
                width >= minWidth && 
                height >= 2 &&
                width <= maxWidth && 
                height <= maxHeight) {
                
                int x = getStat(stats, i, CC_STAT_LEFT);
                int y = getStat(stats, i, CC_STAT_TOP);
                missedRegions.add(new Region(x, y, width, height));
            }
        }
        
        detectedMask.release();
        missedMask.release();
        notDetected.release();
        labels.release();
        stats.release();
        centroids.release();
        
        LogUtil.log("[findMissedRegions] 执行完成");
        return missedRegions;
    }
    
    /**
     * 使用字符相对位置信息定位所有字符
     * 根据首字符位置和相对偏移量计算其他字符位置
     * 增强版：支持字符位置推断及修正算法
     * 
     * @param image 输入图像
     * @param charPositions 字符相对位置列表
     * @param firstCharRegion 首字符的实际位置（从图像中检测到的）
     * @return 所有字符的定位区域列表
     */
    public List<Region> detectByRelativePosition(Mat image, List<CharRelativePosition> charPositions, Region firstCharRegion) {
        LogUtil.log("[detectByRelativePosition] 开始执行，字符数量: " + (charPositions != null ? charPositions.size() : 0));
        
        if (charPositions == null || charPositions.isEmpty() || firstCharRegion == null) {
            LogUtil.log("[detectByRelativePosition] 参数无效，返回空列表");
            return new ArrayList<>();
        }
        
        List<Region> detectedRegions = new ArrayList<>();
        
        // 首字符作为基准点
        int baseX = firstCharRegion.x;
        int baseY = firstCharRegion.y;
        
        // 计算首字符的实际尺寸与预设尺寸的缩放比例
        CharRelativePosition firstPos = charPositions.get(0);
        double scaleX = 1.0;
        double scaleY = 1.0;
        
        if (firstPos.getWidth() > 0) {
            scaleX = (double) firstCharRegion.width / firstPos.getWidth();
        }
        if (firstPos.getHeight() > 0) {
            scaleY = (double) firstCharRegion.height / firstPos.getHeight();
        }
        
        LogUtil.log("[detectByRelativePosition] 基准点: (" + baseX + "," + baseY + "), 缩放比例: (" + 
                   String.format("%.2f", scaleX) + "," + String.format("%.2f", scaleY) + ")");
        
        // 获取原始分割出的所有连通区域（用于匹配和修正）
        // 使用区域合并和缺失字符查找后的结果作为原始分割
        DetectionResult detectionResult = detect(image);
        List<Region> allBlobRegions = new ArrayList<>();
        allBlobRegions.addAll(detectionResult.textRegions);
        allBlobRegions.addAll(detectionResult.missedRegions);
        allBlobRegions.sort(Comparator.comparingInt(r -> r.x));
        
        // 释放可视化图像以节省内存
        if (detectionResult.visualizedImage != null) {
            detectionResult.visualizedImage.release();
        }

        LogUtil.log("[detectByRelativePosition] 原始连通区域数: " + allBlobRegions.size());
        
        // 阶段1：基于相对位置初步定位每个字符
        List<Region> estimatedRegions = new ArrayList<>();
        for (CharRelativePosition pos : charPositions) {
            int offsetX = (int) (pos.getRelativeX() * scaleX);
            int offsetY = (int) (pos.getRelativeY() * scaleY);
            int expectedX = baseX + offsetX;
            int expectedY = baseY + offsetY;
            int expectedWidth = (int) (pos.getWidth() * scaleX);
            int expectedHeight = (int) (pos.getHeight() * scaleY);
            
            estimatedRegions.add(new Region(expectedX, expectedY, expectedWidth, expectedHeight));
        }
        
        // 阶段2：字符位置推断及修正
        // 情况1和2：正确字符框数量 = 分割字符框数量，且尺寸成一定比例 -> 直接label对应
        if (allBlobRegions.size() == charPositions.size()) {
            LogUtil.log("[detectByRelativePosition] 情况1/2: 框数量匹配(" + allBlobRegions.size() + ")，尝试直接对应");
            boolean sizeRatioValid = true;
            for (int i = 0; i < charPositions.size(); i++) {
                CharRelativePosition pos = charPositions.get(i);
                Region blob = allBlobRegions.get(i);
                double expectedWidth = pos.getWidth() * scaleX;
                double expectedHeight = pos.getHeight() * scaleY;
                
                // 检查尺寸比例是否在合理范围内（0.5 ~ 2.0）
                double widthRatio = expectedWidth > 0 ? blob.width / expectedWidth : 1.0;
                double heightRatio = expectedHeight > 0 ? blob.height / expectedHeight : 1.0;
                
                if (widthRatio < 0.5 || widthRatio > 2.0 || heightRatio < 0.5 || heightRatio > 2.0) {
                    sizeRatioValid = false;
                    LogUtil.log("[detectByRelativePosition] 尺寸比例异常: 字符'" + pos.getCharacter() + "' 宽比=" + 
                               String.format("%.2f", widthRatio) + ", 高比=" + String.format("%.2f", heightRatio));
                    break;
                }
            }
            
            if (sizeRatioValid) {
                // 按X坐标排序后直接使用分割框
                allBlobRegions.sort(Comparator.comparingInt(r -> r.x));
                LogUtil.log("[detectByRelativePosition] 尺寸比例正常，直接使用分割框");
                return allBlobRegions;
            }
        }
        
        // 情况3：分割错误（框数量不匹配）
        LogUtil.log("[detectByRelativePosition] 情况3: 分割错误，进行修正推断");
        
        // 新策略：将原始分割区域与预计位置进行匹配，保留已匹配的正确区域，只修正未匹配的部分
        detectedRegions = correctByMatchingAndInterpolation(image, charPositions, allBlobRegions, estimatedRegions, firstCharRegion);
        
        LogUtil.log("[detectByRelativePosition] 执行完成，定位了 " + detectedRegions.size() + " 个字符");
        return detectedRegions;
    }
    
    /**
     * 基于匹配和插值的字符位置修正算法
     * 核心思想：保留原始分割中已匹配的正确区域，只修正未匹配的部分
     * 改进：在匹配前检测并合并相邻的窄区域，避免拆分导致的匹配偏移
     */
    private List<Region> correctByMatchingAndInterpolation(Mat image,
            List<CharRelativePosition> charPositions,
            List<Region> allBlobRegions,
            List<Region> estimatedRegions,
            Region firstCharRegion) {
        
        int charCount = charPositions.size();
        List<Region> result = new ArrayList<>();
        for (int i = 0; i < charCount; i++) result.add(null);
        boolean[] charMatched = new boolean[charCount];
        boolean[] blobUsed = new boolean[allBlobRegions.size()];
        
        // 步骤0：预处理 - 检测并合并相邻的异常窄区域
        // 当一个字符被拆成两个相邻的窄区域时，合并它们以获得更好的匹配
        List<Region> processedBlobs = new ArrayList<>(allBlobRegions);
        List<List<Integer>> blobGroups = new ArrayList<>();
        for (int j = 0; j < processedBlobs.size(); j++) {
            blobGroups.add(new ArrayList<>(java.util.Collections.singletonList(j)));
        }
        
        boolean merged = true;
        int maxMergeIterations = processedBlobs.size();
        int mergeIteration = 0;
        while (merged && mergeIteration < maxMergeIterations) {
            merged = false;
            mergeIteration++;
            List<Region> newBlobs = new ArrayList<>();
            List<List<Integer>> newGroups = new ArrayList<>();
            boolean[] mergedInThisRound = new boolean[processedBlobs.size()];
            
            for (int j = 0; j < processedBlobs.size(); j++) {
                if (mergedInThisRound[j]) continue;
                
                Region r1 = processedBlobs.get(j);
                int x1 = r1.x, y1 = r1.y, x1End = r1.x + r1.width, y1End = r1.y + r1.height;
                
                // 尝试与右侧相邻区域合并
                if (j + 1 < processedBlobs.size() && !mergedInThisRound[j + 1]) {
                    Region r2 = processedBlobs.get(j + 1);
                    int x2 = r2.x, y2 = r2.y, x2End = r2.x + r2.width, y2End = r2.y + r2.height;
                    
                    // 水平间距
                    int horizontalGap = Math.max(0, x2 - x1End);
                    // 高度差异
                    double heightDiffRatio = Math.abs(r1.height - r2.height) / (double) Math.max(r1.height, r2.height);
                    // 垂直重叠
                    boolean verticalOverlap = y1 < y2End && y1End > y2;
                    
                    // 合并条件：间距小、高度相近、有垂直重叠
                    // 使用固定小阈值（10像素）避免过度合并
                    if (horizontalGap <= 10 && heightDiffRatio < 0.35 && verticalOverlap) {
                        // 检查合并后的尺寸是否更合理（更接近近方形）
                        int mergedWidth = x2End - x1;
                        int mergedHeight = Math.max(y1End, y2End) - Math.min(y1, y2);
                        double mergedRatio = (double) mergedWidth / Math.max(mergedHeight, 1);
                        double r1Ratio = (double) r1.width / Math.max(r1.height, 1);
                        
                        // 如果合并后更接近方形（宽高比更接近1.0），且原始区域明显偏窄
                        // 限制合并条件：主要针对被拆分的汉字（宽度>=35且宽高比<0.5）
                        // 避免把正常的半角字符（宽高比0.5~0.6）错误合并
                        boolean r1IsNarrow = r1Ratio < 0.5 && r1.width >= 35;
                        boolean mergedIsBetter = Math.abs(mergedRatio - 1.0) < Math.abs(r1Ratio - 1.0);
                        boolean mergedSizeReasonable = mergedWidth <= 80;
                        
                        if (r1IsNarrow && mergedIsBetter && mergedSizeReasonable) {
                            Region mergedRegion = new Region(x1, Math.min(y1, y2), mergedWidth, mergedHeight);
                            newBlobs.add(mergedRegion);
                            
                            List<Integer> mergedGroup = new ArrayList<>(blobGroups.get(j));
                            mergedGroup.addAll(blobGroups.get(j + 1));
                            newGroups.add(mergedGroup);
                            
                            mergedInThisRound[j] = true;
                            mergedInThisRound[j + 1] = true;
                            merged = true;
                            LogUtil.log("[correctByMatchingAndInterpolation] 合并相邻窄区域 [" + j + "]" + r1 + " 和 [" + (j+1) + "]" + r2 + " -> " + mergedRegion);
                            continue;
                        }
                    }
                }
                
                newBlobs.add(r1);
                newGroups.add(blobGroups.get(j));
            }
            
            processedBlobs = newBlobs;
            blobGroups = newGroups;
        }
        
        LogUtil.log("[correctByMatchingAndInterpolation] 预处理后区域数: " + processedBlobs.size() + " (原始: " + allBlobRegions.size() + ")");
        
        // 步骤1：构建所有可能的匹配对，并计算匹配分数
        List<MatchPair> matchPairs = new ArrayList<>();
        for (int i = 0; i < charCount; i++) {
            Region est = estimatedRegions.get(i);
            CharRelativePosition pos = charPositions.get(i);
            for (int j = 0; j < processedBlobs.size(); j++) {
                Region blob = processedBlobs.get(j);
                
                // 中心点距离
                double estCenterX = est.x + est.width / 2.0;
                double estCenterY = est.y + est.height / 2.0;
                double blobCenterX = blob.x + blob.width / 2.0;
                double blobCenterY = blob.y + blob.height / 2.0;
                double distance = Math.sqrt(Math.pow(estCenterX - blobCenterX, 2) + Math.pow(estCenterY - blobCenterY, 2));
                
                // 尺寸差异（相对值）
                double widthDiff = Math.abs(est.width - blob.width) / Math.max(est.width, 1.0);
                double heightDiff = Math.abs(est.height - blob.height) / Math.max(est.height, 1.0);
                
                // 过滤明显不匹配的（距离太远、尺寸差异太大、Y坐标差异太大）
                // 距离阈值：预设宽度的1.5倍或50像素
                double distanceThreshold = Math.max(pos.getWidth() * 1.5, 50);
                if (distance > distanceThreshold) continue;
                if (widthDiff > 1.0 || heightDiff > 1.0) continue;
                
                // Y坐标差异过滤：如果中心点Y差异超过20像素或超过预计高度的一半，跳过
                double yDiff = Math.abs(estCenterY - blobCenterY);
                double yDiffThreshold = Math.max(pos.getHeight() * 0.5, 20);
                if (yDiff > yDiffThreshold) continue;
                
                // 综合分数（距离权重更高，Y差异也有较高惩罚）
                double score = distance + (widthDiff + heightDiff) * 20 + yDiff * 2;
                matchPairs.add(new MatchPair(i, j, score, blob));
            }
        }
        
        // 按分数升序排序
        matchPairs.sort(Comparator.comparingDouble(mp -> mp.score));
        
        // 步骤2：贪心分配原始区域
        int matchedCount = 0;
        for (MatchPair pair : matchPairs) {
            if (charMatched[pair.charIndex] || blobUsed[pair.blobIndex]) continue;
            
            result.set(pair.charIndex, pair.blob);
            charMatched[pair.charIndex] = true;
            blobUsed[pair.blobIndex] = true;
            matchedCount++;
            
            LogUtil.log("[correctByMatchingAndInterpolation] 字符[" + pair.charIndex + "] '" + 
                       charPositions.get(pair.charIndex).getCharacter() + "' 匹配原始区域[" + pair.blobIndex + "]: " + pair.blob);
        }
        
        LogUtil.log("[correctByMatchingAndInterpolation] 直接匹配成功: " + matchedCount + "/" + charCount);
        
        // 步骤3：基于锚点重新计算缩放比例（使用所有匹配上的区域）
        double scaleX = 1.0, scaleY = 1.0;
        if (matchedCount > 0) {
            double sumScaleX = 0, sumScaleY = 0;
            int scaleCount = 0;
            for (int i = 0; i < charCount; i++) {
                if (!charMatched[i]) continue;
                Region matched = result.get(i);
                CharRelativePosition pos = charPositions.get(i);
                if (pos.getWidth() > 0 && matched.width > 0) {
                    sumScaleX += (double) matched.width / pos.getWidth();
                    scaleCount++;
                }
                if (pos.getHeight() > 0 && matched.height > 0) {
                    sumScaleY += (double) matched.height / pos.getHeight();
                }
            }
            if (scaleCount > 0) {
                scaleX = sumScaleX / scaleCount;
                scaleY = sumScaleY / scaleCount;
            }
            LogUtil.log("[correctByMatchingAndInterpolation] 基于 " + scaleCount + " 个锚点重新计算缩放: (" + 
                       String.format("%.2f", scaleX) + "," + String.format("%.2f", scaleY) + ")");
        } else {
            // 没有匹配上的区域，使用首字符的缩放
            CharRelativePosition firstPos = charPositions.get(0);
            if (firstPos.getWidth() > 0) scaleX = (double) firstCharRegion.width / firstPos.getWidth();
            if (firstPos.getHeight() > 0) scaleY = (double) firstCharRegion.height / firstPos.getHeight();
        }
        
        // 步骤4：为未匹配的字符计算位置（使用插值或findBestMatchingRegion）
        for (int i = 0; i < charCount; i++) {
            if (charMatched[i]) continue;
            
            CharRelativePosition pos = charPositions.get(i);
            
            // 寻找左右最近的已匹配锚点
            int leftAnchor = -1, rightAnchor = -1;
            for (int j = i - 1; j >= 0; j--) {
                if (charMatched[j]) { leftAnchor = j; break; }
            }
            for (int j = i + 1; j < charCount; j++) {
                if (charMatched[j]) { rightAnchor = j; break; }
            }
            
            Region finalRegion = null;
            
            if (leftAnchor >= 0 && rightAnchor >= 0) {
                // 两侧都有锚点，使用线性插值
                Region leftRegion = result.get(leftAnchor);
                Region rightRegion = result.get(rightAnchor);
                CharRelativePosition leftPos = charPositions.get(leftAnchor);
                CharRelativePosition rightPos = charPositions.get(rightAnchor);
                
                double leftRelX = leftPos.getRelativeX();
                double rightRelX = rightPos.getRelativeX();
                double currentRelX = pos.getRelativeX();
                
                double ratio = (currentRelX - leftRelX) / Math.max(rightRelX - leftRelX, 1.0);
                int expectedX = leftRegion.x + (int) ((rightRegion.x - leftRegion.x) * ratio);
                int expectedWidth = (int) (pos.getWidth() * scaleX);
                int expectedHeight = (int) (pos.getHeight() * scaleY);
                
                // Y坐标插值：如果左右锚点Y差异过大，不使用Y插值，而是使用左侧锚点Y
                int expectedY;
                int yDiff = Math.abs(leftRegion.y - rightRegion.y);
                if (yDiff > 20) {
                    expectedY = leftRegion.y + (int) ((pos.getRelativeY() - leftPos.getRelativeY()) * scaleY);
                    LogUtil.log("[correctByMatchingAndInterpolation] 字符[" + i + "] '" + pos.getCharacter() + 
                               "' 锚点Y差异大(" + yDiff + ")，使用左锚点Y偏移");
                } else {
                    expectedY = leftRegion.y + (int) ((rightRegion.y - leftRegion.y) * ratio);
                }
                
                finalRegion = new Region(expectedX, expectedY, expectedWidth, expectedHeight);
                LogUtil.log("[correctByMatchingAndInterpolation] 字符[" + i + "] '" + pos.getCharacter() + 
                           "' 使用插值: " + finalRegion + " (锚点[" + leftAnchor + "]-[" + rightAnchor + "])");
            } else if (leftAnchor >= 0) {
                // 只有左侧锚点，基于相对偏移计算
                Region leftRegion = result.get(leftAnchor);
                CharRelativePosition leftPos = charPositions.get(leftAnchor);
                int offsetX = (int) ((pos.getRelativeX() - leftPos.getRelativeX()) * scaleX);
                int offsetY = (int) ((pos.getRelativeY() - leftPos.getRelativeY()) * scaleY);
                int expectedX = leftRegion.x + offsetX;
                int expectedY = leftRegion.y + offsetY;
                int expectedWidth = (int) (pos.getWidth() * scaleX);
                int expectedHeight = (int) (pos.getHeight() * scaleY);
                
                finalRegion = new Region(expectedX, expectedY, expectedWidth, expectedHeight);
                LogUtil.log("[correctByMatchingAndInterpolation] 字符[" + i + "] '" + pos.getCharacter() + 
                           "' 使用左锚点偏移: " + finalRegion + " (锚点[" + leftAnchor + "])");
            } else if (rightAnchor >= 0) {
                // 只有右侧锚点，基于相对偏移计算
                Region rightRegion = result.get(rightAnchor);
                CharRelativePosition rightPos = charPositions.get(rightAnchor);
                int offsetX = (int) ((pos.getRelativeX() - rightPos.getRelativeX()) * scaleX);
                int offsetY = (int) ((pos.getRelativeY() - rightPos.getRelativeY()) * scaleY);
                int expectedX = rightRegion.x + offsetX;
                int expectedY = rightRegion.y + offsetY;
                int expectedWidth = (int) (pos.getWidth() * scaleX);
                int expectedHeight = (int) (pos.getHeight() * scaleY);
                
                finalRegion = new Region(expectedX, expectedY, expectedWidth, expectedHeight);
                LogUtil.log("[correctByMatchingAndInterpolation] 字符[" + i + "] '" + pos.getCharacter() + 
                           "' 使用右锚点偏移: " + finalRegion + " (锚点[" + rightAnchor + "])");
            } else {
                // 没有任何锚点，使用findBestMatchingRegion
                int expectedX = firstCharRegion.x + (int) (pos.getRelativeX() * scaleX);
                int expectedY = firstCharRegion.y + (int) (pos.getRelativeY() * scaleY);
                finalRegion = findBestMatchingRegion(image, expectedX, expectedY,
                        (int) (pos.getMinWidth() * scaleX), (int) (pos.getMaxWidth() * scaleX),
                        (int) (pos.getMinHeight() * scaleY), (int) (pos.getMaxHeight() * scaleY),
                        pos.getCharType());
                LogUtil.log("[correctByMatchingAndInterpolation] 字符[" + i + "] '" + pos.getCharacter() + 
                           "' 使用findBestMatchingRegion: " + finalRegion);
            }
            
            result.set(i, finalRegion);
        }
        
        return result;
    }
    
    /**
     * 匹配对数据结构
     */
    private static class MatchPair {
        final int charIndex;
        final int blobIndex;
        final double score;
        final Region blob;
        
        MatchPair(int charIndex, int blobIndex, double score, Region blob) {
            this.charIndex = charIndex;
            this.blobIndex = blobIndex;
            this.score = score;
            this.blob = blob;
        }
    }
    
    /**
     * 检查区域尺寸是否合理
     * 修正后的区域不应比原始区域小太多（避免将小噪声点当成字符）
     */
    private boolean isRegionSizeReasonable(Region corrected, Region original) {
        if (original == null || corrected == null) return false;
        if (original.width <= 0 || original.height <= 0) return false;
        
        double widthRatio = (double) corrected.width / original.width;
        double heightRatio = (double) corrected.height / original.height;
        
        // 修正后的区域宽度/高度不应小于原始区域的30%
        boolean reasonable = widthRatio >= 0.3 && heightRatio >= 0.3;
        if (!reasonable) {
            LogUtil.log("[isRegionSizeReasonable] 尺寸不合理: 修正=" + corrected + ", 原始=" + original +
                       ", 宽比=" + String.format("%.2f", widthRatio) + ", 高比=" + String.format("%.2f", heightRatio));
        }
        return reasonable;
    }
    
    /**
     * 提取图像中所有有效的连通区域（用于字符位置推断）
     */
    private List<Region> extractAllBlobRegions(Mat image) {
        List<Region> blobRegions = new ArrayList<>();
        Mat processed = preprocessImage(image);
        Mat binary = new Mat();
        threshold(processed, binary, ocrGray, 255, THRESH_BINARY_INV);
        
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int numLabels = connectedComponentsWithStats(binary, labels, stats, centroids, 8, CV_32S);
        
        for (int i = 1; i < numLabels; i++) {
            int area = getStat(stats, i, CC_STAT_AREA);
            int width = getStat(stats, i, CC_STAT_WIDTH);
            int height = getStat(stats, i, CC_STAT_HEIGHT);
            
            if (area >= minArea && width >= minWidth && height >= minHeight &&
                width <= maxWidth && height <= maxHeight) {
                int x = getStat(stats, i, CC_STAT_LEFT);
                int y = getStat(stats, i, CC_STAT_TOP);
                blobRegions.add(new Region(x, y, width, height));
            }
        }
        
        // 按X坐标排序
        blobRegions.sort(Comparator.comparingInt(r -> r.x));
        
        processed.release();
        binary.release();
        labels.release();
        stats.release();
        centroids.release();
        
        return blobRegions;
    }
    
    /**
     * 在指定位置附近搜索最佳匹配的字符区域
     * 处理错误联通域的情况（如"AB"被识别为一个连通域）
     */
    private Region findBestMatchingRegion(Mat image, int expectedX, int expectedY,
                                         int minWidth, int maxWidth, int minHeight, int maxHeight,
                                         CharRelativePosition.CharType charType) {
        // 预处理图像
        Mat processed = preprocessImage(image);
        
        // 在预计位置周围创建搜索区域
        // 搜索半径需要足够大，避免只找到很小的点状区域
        int searchRadius = Math.max(Math.max(maxWidth, maxHeight), 40); // 最小搜索半径40像素
        int searchX = Math.max(0, expectedX - searchRadius);
        int searchY = Math.max(0, expectedY - searchRadius);
        int searchWidth = Math.min(image.cols() - searchX, maxWidth + searchRadius * 2);
        int searchHeight = Math.min(image.rows() - searchY, maxHeight + searchRadius * 2);
        
        // 确保搜索区域至少有一定大小
        searchWidth = Math.max(searchWidth, 60);
        searchHeight = Math.max(searchHeight, 60);
        
        // 边界检查
        if (searchX + searchWidth > image.cols()) {
            searchWidth = image.cols() - searchX;
        }
        if (searchY + searchHeight > image.rows()) {
            searchHeight = image.rows() - searchY;
        }
        
        // 提取搜索区域的ROI
        if (searchX + searchWidth > image.cols() || searchY + searchHeight > image.rows() ||
            searchWidth <= 0 || searchHeight <= 0) {
            processed.release();
            // 返回估计位置
            int defaultWidth = Math.max(minWidth, Math.min(maxWidth, 20));
            int defaultHeight = Math.max(minHeight, Math.min(maxHeight, 20));
            return new Region(expectedX, expectedY, defaultWidth, defaultHeight);
        }
        
        Mat roi = new Mat(processed, new Rect(searchX, searchY, searchWidth, searchHeight));
        
        // 连通区域分析
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int numLabels = connectedComponentsWithStats(roi, labels, stats, centroids, 8, CV_32S);
        
        Region bestMatch = null;
        double bestScore = Double.MAX_VALUE;
        
        // 放宽尺寸限制（允许一定误差）
        int relaxedMinWidth = Math.max(3, minWidth / 2);
        int relaxedMaxWidth = maxWidth * 2;
        int relaxedMinHeight = Math.max(3, minHeight / 2);
        int relaxedMaxHeight = maxHeight * 2;
        
        for (int i = 1; i < numLabels; i++) {
            int area = getStat(stats, i, CC_STAT_AREA);
            int width = getStat(stats, i, CC_STAT_WIDTH);
            int height = getStat(stats, i, CC_STAT_HEIGHT);
            
            // 过滤过小的噪声点（面积至少为minArea或10）
            if (area < Math.max(minArea, 10)) {
                continue;
            }
            
            // 检查尺寸是否在放宽后的范围内
            if (width < relaxedMinWidth || width > relaxedMaxWidth || 
                height < relaxedMinHeight || height > relaxedMaxHeight) {
                continue;
            }
            
            int x = getStat(stats, i, CC_STAT_LEFT) + searchX;
            int y = getStat(stats, i, CC_STAT_TOP) + searchY;
            
            // 计算与预计位置的距离分数
            double distanceScore = Math.sqrt(Math.pow(x - expectedX, 2) + Math.pow(y - expectedY, 2));
            
            // 计算尺寸匹配分数
            double expectedWidth = (minWidth + maxWidth) / 2.0;
            double expectedHeight = (minHeight + maxHeight) / 2.0;
            double sizeScore = Math.abs(width - expectedWidth) / Math.max(expectedWidth, 1.0) + 
                              Math.abs(height - expectedHeight) / Math.max(expectedHeight, 1.0);
            
            // 综合分数（距离权重更高）
            double totalScore = distanceScore + sizeScore * 10;
            
            if (totalScore < bestScore) {
                bestScore = totalScore;
                bestMatch = new Region(x, y, width, height);
            }
        }
        
        // 清理
        roi.release();
        labels.release();
        stats.release();
        centroids.release();
        processed.release();
        
        // 如果没有找到匹配区域，返回估计位置
        if (bestMatch == null) {
            int defaultWidth = Math.max(minWidth, Math.min(maxWidth, 20));
            int defaultHeight = Math.max(minHeight, Math.min(maxHeight, 20));
            bestMatch = new Region(expectedX, expectedY, defaultWidth, defaultHeight);
        }
        
        return bestMatch;
    }
    
    /**
     * 根据首字符类型获取定位参数
     * 全角字符和半角字符使用不同的宽度和高度范围
     */
    public static class CharLocationParams {
        public int minWidth, maxWidth;
        public int minHeight, maxHeight;
        public int searchRadius;
        
        public CharLocationParams(CharRelativePosition.CharType charType) {
            switch (charType) {
                case CHINESE:
                case FULL_WIDTH:
                    // 全角字符（汉字、全角标点）
                    minWidth = 20;
                    maxWidth = 50;
                    minHeight = 20;
                    maxHeight = 50;
                    searchRadius = 30;
                    break;
                case HALF_WIDTH_UPPER:
                    // 大写字母
                    minWidth = 12;
                    maxWidth = 25;
                    minHeight = 18;
                    maxHeight = 30;
                    searchRadius = 20;
                    break;
                case HALF_WIDTH_LOWER:
                    // 小写字母
                    minWidth = 10;
                    maxWidth = 22;
                    minHeight = 15;
                    maxHeight = 25;
                    searchRadius = 18;
                    break;
                case HALF_WIDTH_DIGIT:
                    // 数字
                    minWidth = 10;
                    maxWidth = 20;
                    minHeight = 18;
                    maxHeight = 28;
                    searchRadius = 18;
                    break;
                case PUNCTUATION:
                    // 标点符号
                    minWidth = 5;
                    maxWidth = 20;
                    minHeight = 5;
                    maxHeight = 25;
                    searchRadius = 15;
                    break;
                case HALF_WIDTH:
                case OTHER:
                default:
                    // 其他半角字符
                    minWidth = 8;
                    maxWidth = 25;
                    minHeight = 15;
                    maxHeight = 30;
                    searchRadius = 20;
                    break;
            }
        }
    }
    
    /**
     * 检测文本区域
     */
    public DetectionResult detect(Mat image) {
        LogUtil.log("[detect] 开始执行");
        long startTime = System.currentTimeMillis();
        Mat processed = preprocessImage(image);
        
        List<Region> blobRegions = new ArrayList<>();
        Mat[] binaries = segmentCharacters(processed, blobRegions);
        Mat originalBinary = binaries[0];
        Mat charMask = binaries[1];
        
        // 从字符掩码获取文本区域
        Mat finalLabels = new Mat();
        Mat finalStats = new Mat();
        Mat finalCentroids = new Mat();
        int finalNumLabels = connectedComponentsWithStats(charMask, finalLabels, finalStats, finalCentroids, 8, CV_32S);
        
        List<Region> textRegions = new ArrayList<>();
        for (int i = 1; i < finalNumLabels; i++) {
            int area = getStat(finalStats, i, CC_STAT_AREA);
            int width = getStat(finalStats, i, CC_STAT_WIDTH);
            int height = getStat(finalStats, i, CC_STAT_HEIGHT);
            
            if (area >= minArea && width >= minWidth && height >= minHeight) {
                int x = getStat(finalStats, i, CC_STAT_LEFT);
                int y = getStat(finalStats, i, CC_STAT_TOP);
                
                // 忽略边界区域
                if (ignoreBorderWidth > 0) {
                    if (x < ignoreBorderWidth || 
                        y < ignoreBorderWidth || 
                        x + width > image.cols() - ignoreBorderWidth || 
                        y + height > image.rows() - ignoreBorderWidth) {
                        LogUtil.log("[detect] 忽略边界区域: (" + x + "," + y + ")");
                        continue;
                    }
                }
                
                textRegions.add(new Region(x, y, width, height));
            }
        }
        
        // 合并重叠的文本区域（考虑字符内部空隙）
        textRegions = mergeOverlappingRegionsWithGap(textRegions);
        
        // 强制宽度分割（如果启用）
        if (forceWidthSplitEnabled) {
            textRegions = forceWidthSplit(textRegions, 0);  // 0表示根据宽度自动计算
        }
        
        // 多行排序（如果启用）
        textRegions = sortMultiLineRegions(textRegions);
        
        // Lua脚本后处理（如果启用）
        if (luaScriptEngine != null && luaScriptEngine.isEnabled()) {
            textRegions = applyLuaPostProcess(textRegions, image.cols(), image.rows());
        }
        
        // 查找漏字区域
        List<Region> missedRegions = findMissedRegions(originalBinary, textRegions, blobRegions);
        
        // 可视化结果
        Mat result = visualize(image, textRegions, blobRegions, missedRegions);
        
        processed.release();
        originalBinary.release();
        charMask.release();
        finalLabels.release();
        finalStats.release();
        finalCentroids.release();
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        LogUtil.log("[detect] 执行完成，处理时间: " + elapsedTime + "ms");
        return new DetectionResult(textRegions, blobRegions, missedRegions, result);
    }
    
    /**
     * 将Region列表转换为Map列表
     */
    private List<Map<String, Object>> regionsToMapList(List<Region> regions) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (regions == null) return result;
        for (Region r : regions) {
            Map<String, Object> m = new HashMap<>();
            m.put("x", r.x);
            m.put("y", r.y);
            m.put("width", r.width);
            m.put("height", r.height);
            result.add(m);
        }
        return result;
    }
    
    /**
     * 应用Lua脚本后处理
     */
    private List<Region> applyLuaPostProcess(List<Region> regions, int imageWidth, int imageHeight) {
        LogUtil.log("[applyLuaPostProcess] 开始执行");
        try {
            // 构建imageInfo
            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("width", imageWidth);
            imageInfo.put("height", imageHeight);
            
            // 构建regions列表
            List<Map<String, Object>> regionList = regionsToMapList(regions);
            
            // 构建OCR参数表
            Map<String, Object> ocrParams = new HashMap<>();
            ocrParams.put("grayscale", ocrGray);
            ocrParams.put("gamma", 0.5); // 默认值
            ocrParams.put("minArea", minArea);
            ocrParams.put("maxArea", maxWidth * maxHeight);
            ocrParams.put("minWidth", minWidth);
            ocrParams.put("maxWidth", maxWidth);
            ocrParams.put("minHeight", minHeight);
            ocrParams.put("maxHeight", maxHeight);
            ocrParams.put("maxMergeWidth", getMaxMergeWidth()); // 根据全角/半角计算的最大合并宽度
            ocrParams.put("isFullWidth", isFullWidthText()); // 是否为全角文本
            
            // 构建原始区域和修正后区域的Map列表
            List<Map<String, Object>> rawRegionList = regionsToMapList(rawRegions);
            List<Map<String, Object>> correctedRegionList = regionsToMapList(correctedRegions);
            
            // 执行Lua脚本（使用完整参数版本）
            LuaScriptEngine.ScriptResult result = luaScriptEngine.executeTextLocation(
                imageInfo, regionList, ocrParams, correctString, presetParams, rawRegionList, correctedRegionList);
            
            if (result.isSuccess() && result.getData() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> resultRegions = (List<Map<String, Object>>) result.getData();
                
                // 转换回Region列表
                List<Region> processedRegions = new ArrayList<>();
                for (Map<String, Object> m : resultRegions) {
                    int x = ((Number) m.get("x")).intValue();
                    int y = ((Number) m.get("y")).intValue();
                    int w = ((Number) m.get("width")).intValue();
                    int h = ((Number) m.get("height")).intValue();
                    processedRegions.add(new Region(x, y, w, h));
                }
                
                LogUtil.log("[applyLuaPostProcess] Lua脚本处理完成: " + regions.size() + " -> " + processedRegions.size());
                return processedRegions;
            } else {
                LogUtil.log("[applyLuaPostProcess] Lua脚本执行失败: " + result.getError());
            }
        } catch (Exception e) {
            LogUtil.log("[applyLuaPostProcess] 发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return regions;
    }
    
    /**
     * 可视化检测结果
     * 信息区域绘制在图像外扩区域，不遮挡原图
     */
    public Mat visualize(Mat image, List<Region> textRegions, 
                        List<Region> blobRegions, List<Region> missedRegions) {
        LogUtil.log("[visualize] 开始执行");
        
        if (missedRegions == null) {
            missedRegions = new ArrayList<>();
        }
        
        // 创建外扩的图像，顶部留出空间给信息区域
        int infoHeight = 100;  // 信息区域高度
        int padding = 10;      // 内边距
        
        Mat result = new Mat(image.rows() + infoHeight, image.cols(), CV_8UC3, new Scalar(30, 30, 30, 0));
        
        // 复制原图到外扩区域下方
        Mat roi = new Mat(result, new Rect(0, infoHeight, image.cols(), image.rows()));
        if (image.channels() == 1) {
            Mat temp = new Mat();
            cvtColor(image, temp, COLOR_GRAY2BGR);
            temp.copyTo(roi);
            temp.release();
        } else {
            image.copyTo(roi);
        }
        roi.release();
        
        // 在信息区域绘制背景
        rectangle(result, new Point(0, 0), new Point(image.cols(), infoHeight), 
                 new Scalar(20, 20, 20, 0), -1, LINE_8, 0);
        
        // 绘制分隔线
        line(result, new Point(0, infoHeight - 1), new Point(image.cols(), infoHeight - 1), 
             new Scalar(100, 100, 100, 0), 2, LINE_8, 0);
        
        // 在图像上绘制检测结果（注意y坐标要加上infoHeight偏移）
        int offsetY = infoHeight;
        
        // 绘制漏字区域(红色)
        for (Region region : missedRegions) {
            Point pt1 = new Point(region.x, region.y + offsetY);
            Point pt2 = new Point(region.x + region.width, region.y + region.height + offsetY);
            rectangle(result, pt1, pt2, COLOR_RED, 2, LINE_8, 0);
        }
        
        // 绘制斑点区域(橙色)
        for (Region region : blobRegions) {
            Point pt1 = new Point(region.x, region.y + offsetY);
            Point pt2 = new Point(region.x + region.width, region.y + region.height + offsetY);
            rectangle(result, pt1, pt2, COLOR_ORANGE, 1, LINE_8, 0);
        }
        
        // 绘制文本区域(绿色)
        for (Region region : textRegions) {
            Point pt1 = new Point(region.x, region.y + offsetY);
            Point pt2 = new Point(region.x + region.width, region.y + region.height + offsetY);
            rectangle(result, pt1, pt2, COLOR_GREEN, 2, LINE_8, 0);
        }
        
        // 在信息区域绘制图例和统计信息
        int legendY = 35;
        int x = padding;
        
        // 图例背景
        rectangle(result, new Point(5, 5), new Point(450, infoHeight - 10), 
                 new Scalar(40, 40, 40, 0), -1, LINE_8, 0);
        
        // Text图例
        rectangle(result, new Point(x + 10, legendY - 15), new Point(x + 30, legendY + 5), COLOR_GREEN, -1, LINE_8, 0);
        putText(result, "Text(" + textRegions.size() + ")", new Point(x + 35, legendY), 
               FONT_HERSHEY_SIMPLEX, 0.6, COLOR_GREEN, 2, LINE_8, false);
        x += 140;
        
        // Blob图例
        rectangle(result, new Point(x + 10, legendY - 15), new Point(x + 30, legendY + 5), COLOR_ORANGE, -1, LINE_8, 0);
        putText(result, "Blob(" + blobRegions.size() + ")", new Point(x + 35, legendY), 
               FONT_HERSHEY_SIMPLEX, 0.6, COLOR_ORANGE, 2, LINE_8, false);
        x += 140;
        
        // Missed图例
        rectangle(result, new Point(x + 10, legendY - 15), new Point(x + 30, legendY + 5), COLOR_RED, -1, LINE_8, 0);
        putText(result, "Missed(" + missedRegions.size() + ")", new Point(x + 35, legendY), 
               FONT_HERSHEY_SIMPLEX, 0.6, COLOR_RED, 2, LINE_8, false);
        
        // 图像尺寸信息
        String sizeText = String.format("Image: %dx%d", image.cols(), image.rows());
        putText(result, sizeText, new Point(padding, legendY + 40), 
               FONT_HERSHEY_SIMPLEX, 0.6, COLOR_WHITE, 2, LINE_8, false);
        
        LogUtil.log("[visualize] 执行完成");
        return result;
    }
}
