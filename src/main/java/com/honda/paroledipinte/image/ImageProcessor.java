package com.honda.paroledipinte.image;

import com.honda.paroledipinte.util.LogUtil;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * 图像处理器
 * 提供图像预处理和字符相似度比较功能
 */
public class ImageProcessor {
    public static final int WIDTH = 70;
    public static final int HEIGHT = 76;
    
    // 动态阈值二值化参数
    private static double gammaValue = 0.5;
    private static double thresholdOffset = -20;
    private static int grayscaleValue = 128;
    
    public static void setGammaValue(double gamma) {
        LogUtil.log("[setGammaValue] 开始执行");
        gammaValue = gamma;
        LogUtil.log("[setGammaValue] 执行完成");
    }
    
    public static double getGammaValue() {
        return gammaValue;
    }
    
    public static void setThresholdOffset(double offset) {
        LogUtil.log("[setThresholdOffset] 开始执行");
        thresholdOffset = offset;
        LogUtil.log("[setThresholdOffset] 执行完成");
    }
    
    public static double getThresholdOffset() {
        return thresholdOffset;
    }
    
    public static void setGrayscaleValue(int grayscale) {
        LogUtil.log("[setGrayscaleValue] 开始执行");
        grayscaleValue = grayscale;
        LogUtil.log("[setGrayscaleValue] 执行完成");
    }
    
    public static int getGrayscaleValue() {
        return grayscaleValue;
    }
    
    /**
     * BufferedImage转Mat
     * 支持灰度图和彩色图(BGR格式)
     */
    public static Mat bufferedImageToMat(BufferedImage bi) {
        LogUtil.log("[bufferedImageToMat] 开始执行, 图像类型: " + bi.getType() + ", 尺寸: " + bi.getWidth() + "x" + bi.getHeight());
        
        // 确保图像是BYTE_GRAY或3BYTE_BGR格式
        BufferedImage convertedBi;
        if (bi.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            convertedBi = bi;
        } else if (bi.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            convertedBi = bi;
        } else {
            // 其他格式转换为3BYTE_BGR
            LogUtil.log("[bufferedImageToMat] 转换图像格式: " + bi.getType() + " -> TYPE_3BYTE_BGR");
            convertedBi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = convertedBi.createGraphics();
            g2d.drawImage(bi, 0, 0, null);
            g2d.dispose();
        }
        
        byte[] pixels = ((DataBufferByte) convertedBi.getRaster().getDataBuffer()).getData();
        
        if (convertedBi.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            // 灰度图
            LogUtil.log("[bufferedImageToMat] 执行完成 (灰度图)");
            return new Mat(convertedBi.getHeight(), convertedBi.getWidth(), opencv_core.CV_8UC1, new BytePointer(pixels));
        } else {
            // 彩色图 BGR格式
            LogUtil.log("[bufferedImageToMat] 执行完成 (BGR彩图)");
            return new Mat(convertedBi.getHeight(), convertedBi.getWidth(), opencv_core.CV_8UC3, new BytePointer(pixels));
        }
    }
    
    /**
     * Mat转BufferedImage
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        LogUtil.log("[matToBufferedImage] 开始执行");
        BufferedImage bi = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
        byte[] pixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.data().get(pixels);
        LogUtil.log("[matToBufferedImage] 执行完成");
        return bi;
    }
    
    /**
     * 图像预处理（动态阈值二值化）
     */
    public static Mat preprocess(Mat src) {
        LogUtil.log("[preprocess] 开始执行");
        long startTime = System.currentTimeMillis();
        Mat gray = new Mat();
        if (src.channels() > 1) {
            opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY);
        } else {
            gray = src.clone();
        }
        
        Mat resized = new Mat();
        opencv_imgproc.resize(gray, resized, new Size(WIDTH, HEIGHT), 0, 0, opencv_imgproc.INTER_AREA);
        
        // Gamma校正增强对比度
        Mat gammaCorrected = gammaCorrection(resized, gammaValue);
        
        // 基于Gamma校正后的平均强度计算动态阈值
        Scalar meanScalar = opencv_core.mean(gammaCorrected);
        double threshold = meanScalar.get(0) + thresholdOffset;
        
        Mat binary = new Mat();
        opencv_imgproc.threshold(gammaCorrected, binary, threshold, 255, opencv_imgproc.THRESH_BINARY);
        
        gray.release();
        resized.release();
        gammaCorrected.release();
        meanScalar.deallocate();
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        LogUtil.log("[preprocess] 执行完成，处理时间: " + elapsedTime + "ms");
        return binary;
    }
    
    /**
     * Gamma校正
     */
    private static Mat gammaCorrection(Mat src, double gamma) {
        LogUtil.log("[gammaCorrection] 开始执行");
        Mat lut = new Mat(1, 256, opencv_core.CV_8UC1);
        byte[] lutData = new byte[256];
        for (int i = 0; i < 256; i++) {
            lutData[i] = (byte) Math.round(Math.pow(i / 255.0, gamma) * 255.0);
        }
        lut.data().put(lutData);
        
        Mat dst = new Mat();
        opencv_core.LUT(src, lut, dst);
        lut.release();
        LogUtil.log("[gammaCorrection] 执行完成");
        return dst;
    }
    
    /**
     * 比较两个图像的相似度
     */
    public static double compareSimilarity(Mat ref, Mat input) {
        LogUtil.log("[compareSimilarity] 开始执行");
        Mat procRef = preprocess(ref);
        Mat procInput = preprocess(input);
        double bestScore = 0;
        
        // 尝试小角度旋转来找到最佳匹配
        for (int angle = -3; angle <= 3; angle++) {
            Mat rotated = rotateImage(procInput, angle);
            double score = computeOverlap(procRef, rotated);
            rotated.release();
            if (score > bestScore) {
                bestScore = score;
            }
        }
        
        procRef.release();
        procInput.release();
        LogUtil.log("[compareSimilarity] 执行完成");
        return bestScore;
    }
    
    /**
     * 旋转图像
     */
    private static Mat rotateImage(Mat src, double angle) {
        LogUtil.log("[rotateImage] 开始执行");
        Point2f center = new Point2f((float) (src.cols() / 2.0), (float) (src.rows() / 2.0));
        Mat rotMat = opencv_imgproc.getRotationMatrix2D(center, angle, 1.0);
        Mat dst = new Mat();
        opencv_imgproc.warpAffine(src, dst, rotMat, new Size(src.cols(), src.rows()),
                opencv_imgproc.INTER_LINEAR, opencv_core.BORDER_CONSTANT, new Scalar(255));
        rotMat.release();
        center.deallocate();
        LogUtil.log("[rotateImage] 执行完成");
        return dst;
    }
    
    /**
     * 计算两个二值图像的重叠率
     */
    private static double computeOverlap(Mat a, Mat b) {
        LogUtil.log("[computeOverlap] 开始执行");
        Mat andMat = new Mat();
        Mat orMat = new Mat();
        opencv_core.bitwise_and(a, b, andMat);
        opencv_core.bitwise_or(a, b, orMat);
        
        double andCount = opencv_core.countNonZero(andMat);
        double orCount = opencv_core.countNonZero(orMat);
        
        andMat.release();
        orMat.release();
        
        if (orCount == 0) {
            LogUtil.log("[computeOverlap] 执行完成");
            return 1.0;
        }
        LogUtil.log("[computeOverlap] 执行完成");
        return andCount / orCount;
    }
    
    /**
     * 检查图像是否已经是二值图像
     */
    public static boolean isBinaryImage(Mat mat) {
        LogUtil.log("[isBinaryImage] 开始执行");
        if (mat.channels() > 1) {
            LogUtil.log("[isBinaryImage] 执行完成");
            return false;
        }
        
        // 检查是否只有0和255两个值
        Mat uniqueValues = new Mat();
        opencv_core.findNonZero(mat, uniqueValues);
        
        // 简化检查：如果图像是单通道且类型为CV_8UC1，认为是灰度图
        // 更精确的检查需要遍历所有像素
        LogUtil.log("[isBinaryImage] 执行完成");
        return mat.type() == opencv_core.CV_8UC1;
    }
    
    /**
     * 提取图像区域
     */
    public static Mat extractRegion(Mat src, int x, int y, int width, int height) {
        LogUtil.log("[extractRegion] 开始执行");
        // 确保坐标在有效范围内
        int x1 = Math.max(0, x);
        int y1 = Math.max(0, y);
        int x2 = Math.min(src.cols(), x + width);
        int y2 = Math.min(src.rows(), y + height);
        
        if (x1 >= x2 || y1 >= y2) {
            LogUtil.log("[extractRegion] 执行完成");
            return new Mat();
        }
        
        LogUtil.log("[extractRegion] 执行完成");
        return new Mat(src, new org.bytedeco.opencv.opencv_core.Rect(x1, y1, x2 - x1, y2 - y1));
    }
}
