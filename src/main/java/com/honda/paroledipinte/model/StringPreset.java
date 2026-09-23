package com.honda.paroledipinte.model;

import com.honda.paroledipinte.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串预设数据模型
 * 用于存储OCR测试的预设配置
 */
public class StringPreset {
    
    private int id;                          // 序号
    private String presetName;               // 预设名称
    private String correctString;            // 字符串（正确字符串）
    private byte[] standardImage;            // 标准图片（二进制数据）
    private String imagePath;                // 图片路径（如果图片存储为文件）
    
    // OCR参数
    private int grayscale;                   // 二值化阈值
    private double gamma;                    // Gamma系数
    private int minArea;                     // 最小字符面积
    private int maxArea;                     // 最大字符面积
    private int minWidth;                    // 最小字符宽度
    private int maxWidth;                    // 最大字符宽度
    private int minHeight;                   // 最小字符高度
    private int maxHeight;                   // 最大字符高度
    
    // 字符相对位置信息列表
    private List<CharRelativePosition> charRelativePositions;
    
    // 脚本
    private String textLocationScript;       // 文本定位脚本
    private String charClassifyScript;       // 字符分类脚本
    private String ocrPostProcessScript;     // OCR后处理脚本
    
    // 脚本启用状态
    private boolean textLocationScriptEnabled;
    private boolean charClassifyScriptEnabled;
    private boolean ocrPostProcessScriptEnabled;
    
    // 字符相对位置启用状态
    private boolean charRelativePositionEnabled;
    
    public StringPreset() {
        LogUtil.log("[StringPreset] 开始执行");
        // 设置默认值
        this.grayscale = 128;
        this.gamma = 0.5;
        this.minArea = 15;
        this.maxArea = 100000;
        this.minWidth = 3;
        this.maxWidth = 500;
        this.minHeight = 3;
        this.maxHeight = 200;
        this.charRelativePositions = new ArrayList<>();
        this.charRelativePositionEnabled = false;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getPresetName() {
        return presetName;
    }
    
    public void setPresetName(String presetName) {
        this.presetName = presetName;
    }
    
    public String getCorrectString() {
        return correctString;
    }
    
    public void setCorrectString(String correctString) {
        this.correctString = correctString;
    }
    
    public byte[] getStandardImage() {
        return standardImage;
    }
    
    public void setStandardImage(byte[] standardImage) {
        this.standardImage = standardImage;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public int getGrayscale() {
        return grayscale;
    }
    
    public void setGrayscale(int grayscale) {
        this.grayscale = grayscale;
    }
    
    public double getGamma() {
        return gamma;
    }
    
    public void setGamma(double gamma) {
        this.gamma = gamma;
    }
    
    public int getMinArea() {
        return minArea;
    }
    
    public void setMinArea(int minArea) {
        this.minArea = minArea;
    }
    
    public int getMaxArea() {
        return maxArea;
    }
    
    public void setMaxArea(int maxArea) {
        this.maxArea = maxArea;
    }
    
    public int getMinWidth() {
        return minWidth;
    }
    
    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }
    
    public int getMaxWidth() {
        return maxWidth;
    }
    
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }
    
    public int getMinHeight() {
        return minHeight;
    }
    
    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }
    
    public int getMaxHeight() {
        return maxHeight;
    }
    
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }
    
    public String getTextLocationScript() {
        return textLocationScript;
    }
    
    public void setTextLocationScript(String textLocationScript) {
        this.textLocationScript = textLocationScript;
    }
    
    public String getCharClassifyScript() {
        return charClassifyScript;
    }
    
    public void setCharClassifyScript(String charClassifyScript) {
        this.charClassifyScript = charClassifyScript;
    }
    
    public String getOcrPostProcessScript() {
        return ocrPostProcessScript;
    }
    
    public void setOcrPostProcessScript(String ocrPostProcessScript) {
        this.ocrPostProcessScript = ocrPostProcessScript;
    }
    
    public boolean isTextLocationScriptEnabled() {
        return textLocationScriptEnabled;
    }
    
    public void setTextLocationScriptEnabled(boolean textLocationScriptEnabled) {
        this.textLocationScriptEnabled = textLocationScriptEnabled;
    }
    
    public boolean isCharClassifyScriptEnabled() {
        return charClassifyScriptEnabled;
    }
    
    public void setCharClassifyScriptEnabled(boolean charClassifyScriptEnabled) {
        this.charClassifyScriptEnabled = charClassifyScriptEnabled;
    }
    
    public boolean isOcrPostProcessScriptEnabled() {
        return ocrPostProcessScriptEnabled;
    }
    
    public void setOcrPostProcessScriptEnabled(boolean ocrPostProcessScriptEnabled) {
        this.ocrPostProcessScriptEnabled = ocrPostProcessScriptEnabled;
    }
    
    /**
     * 获取字符相对位置信息列表
     */
    public List<CharRelativePosition> getCharRelativePositions() {
        if (charRelativePositions == null) {
            charRelativePositions = new ArrayList<>();
        }
        return charRelativePositions;
    }
    
    /**
     * 设置字符相对位置信息列表
     */
    public void setCharRelativePositions(List<CharRelativePosition> charRelativePositions) {
        this.charRelativePositions = charRelativePositions;
    }
    
    /**
     * 判断是否启用了字符相对位置
     */
    public boolean isCharRelativePositionEnabled() {
        return charRelativePositionEnabled;
    }
    
    /**
     * 设置字符相对位置启用状态
     */
    public void setCharRelativePositionEnabled(boolean charRelativePositionEnabled) {
        this.charRelativePositionEnabled = charRelativePositionEnabled;
    }
    
    /**
     * 获取字符相对位置信息的JSON字符串（用于数据库存储）
     * @return JSON格式的字符串
     */
    public String getCharRelativePositionsJson() {
        if (charRelativePositions == null || charRelativePositions.isEmpty()) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(charRelativePositions);
    }
    
    /**
     * 从JSON字符串设置字符相对位置信息（用于数据库读取）
     * @param json JSON格式的字符串
     */
    public void setCharRelativePositionsFromJson(String json) {
        if (json == null || json.isEmpty()) {
            this.charRelativePositions = new ArrayList<>();
            return;
        }
        try {
            Gson gson = new Gson();
            this.charRelativePositions = gson.fromJson(json, 
                new TypeToken<List<CharRelativePosition>>(){}.getType());
        } catch (Exception e) {
            LogUtil.log("[setCharRelativePositionsFromJson] JSON解析失败: " + e.getMessage());
            this.charRelativePositions = new ArrayList<>();
        }
    }
    
    /**
     * 检查是否有字符相对位置信息
     */
    public boolean hasCharRelativePositions() {
        return charRelativePositions != null && !charRelativePositions.isEmpty();
    }
    
    /**
     * 根据字符串生成默认的字符相对位置信息
     * @param correctString 正确字符串
     */
    public void generateDefaultCharRelativePositions(String correctString) {
        if (correctString == null || correctString.isEmpty()) {
            this.charRelativePositions = new ArrayList<>();
            return;
        }
        
        this.charRelativePositions = new ArrayList<>();
        int currentX = 0;
        
        for (int i = 0; i < correctString.length(); i++) {
            String ch = String.valueOf(correctString.charAt(i));
            CharRelativePosition pos = new CharRelativePosition();
            pos.setCharIndex(i);
            pos.setCharacter(ch);
            pos.setRelativeX(currentX);
            pos.setRelativeY(0);
            
            // 根据字符类型设置默认尺寸
            CharRelativePosition.CharType type = CharRelativePosition.detectCharType(ch);
            pos.setCharType(type);
            
            if (type == CharRelativePosition.CharType.CHINESE || 
                type == CharRelativePosition.CharType.FULL_WIDTH) {
                // 全角字符默认尺寸
                pos.setWidth(30);
                pos.setHeight(30);
                pos.setMinWidth(20);
                pos.setMaxWidth(50);
                pos.setMinHeight(20);
                pos.setMaxHeight(50);
                pos.setSpacing(5);
                pos.setMinSpacing(0);
                pos.setMaxSpacing(15);
                currentX += 35; // 30宽度 + 5间距
            } else {
                // 半角字符默认尺寸
                pos.setWidth(15);
                pos.setHeight(20);
                pos.setMinWidth(8);
                pos.setMaxWidth(25);
                pos.setMinHeight(15);
                pos.setMaxHeight(30);
                pos.setSpacing(3);
                pos.setMinSpacing(0);
                pos.setMaxSpacing(10);
                currentX += 18; // 15宽度 + 3间距
            }
            
            this.charRelativePositions.add(pos);
        }
    }
    
    /**
     * 检查是否有标准图片
     */
    public boolean hasStandardImage() {
        return (standardImage != null && standardImage.length > 0) || 
               (imagePath != null && !imagePath.isEmpty());
    }
    
    /**
     * 获取标准图片的Base64编码
     * @return Base64编码的字符串，如果没有图片则返回null
     */
    public String getStandardImageBase64() {
        if (standardImage == null || standardImage.length == 0) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(standardImage);
    }
    
    /**
     * 从Base64编码设置标准图片
     * @param base64String Base64编码的字符串
     */
    public void setStandardImageFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            this.standardImage = null;
            return;
        }
        try {
            this.standardImage = java.util.Base64.getDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            LogUtil.log("[setStandardImageFromBase64] Base64解码失败: " + e.getMessage());
            this.standardImage = null;
        }
    }
    
    @Override
    public String toString() {
        return "StringPreset{" +
                "id=" + id +
                ", presetName='" + presetName + '\'' +
                ", correctString='" + correctString + '\'' +
                '}';
    }
}
