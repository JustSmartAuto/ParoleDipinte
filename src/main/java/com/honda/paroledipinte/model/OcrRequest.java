package com.honda.paroledipinte.model;

import com.honda.paroledipinte.util.LogUtil;
import java.util.Map;

/**
 * OCR请求模型
 */
public class OcrRequest {
    private String imageHash;
    private String imageData;
    private String correctString;
    private String presetName;  // 预设名称，如果指定了预设名称则使用预设中的OCR参数
    
    // OCR参数
    private String service;
    private Integer grayscale;
    private Double gamma;
    private Integer minArea;
    private Integer maxArea;
    private Integer minWidth;
    private Integer minHeight;
    private Integer maxWidth;
    private Integer maxHeight;
    private Integer minRowGap;
    private Integer maxRowGap;
    private Integer minColGap;
    private Integer maxColGap;
    
    // 字符内部空隙参数（用于处理汉字"二"和"川"等多笔画字符）
    private Integer maxInternalVerticalGap;
    private Integer maxInternalHorizontalGap;
    
    // 忽略边界区域参数
    private Integer ignoreBorderWidth;
    
    // 其他参数
    private Map<String, Object> extraParams;
    
    public OcrRequest() {
        LogUtil.log("[OcrRequest] 开始执行");
    }
    
    // Getters and Setters
    public String getImageHash() {
        return imageHash;
    }
    
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }
    
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    public String getCorrectString() {
        return correctString;
    }
    
    public void setCorrectString(String correctString) {
        this.correctString = correctString;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
    
    public Integer getGrayscale() {
        return grayscale;
    }
    
    public void setGrayscale(Integer grayscale) {
        this.grayscale = grayscale;
    }
    
    public Double getGamma() {
        return gamma;
    }
    
    public void setGamma(Double gamma) {
        this.gamma = gamma;
    }
    
    public Integer getMinArea() {
        return minArea;
    }
    
    public void setMinArea(Integer minArea) {
        this.minArea = minArea;
    }
    
    public Integer getMaxArea() {
        return maxArea;
    }
    
    public void setMaxArea(Integer maxArea) {
        this.maxArea = maxArea;
    }
    
    public Integer getMinWidth() {
        return minWidth;
    }
    
    public void setMinWidth(Integer minWidth) {
        this.minWidth = minWidth;
    }
    
    public Integer getMinHeight() {
        return minHeight;
    }
    
    public void setMinHeight(Integer minHeight) {
        this.minHeight = minHeight;
    }
    
    public Integer getMaxWidth() {
        return maxWidth;
    }
    
    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }
    
    public Integer getMaxHeight() {
        return maxHeight;
    }
    
    public void setMaxHeight(Integer maxHeight) {
        this.maxHeight = maxHeight;
    }
    
    public Integer getMinRowGap() {
        return minRowGap;
    }
    
    public void setMinRowGap(Integer minRowGap) {
        this.minRowGap = minRowGap;
    }
    
    public Integer getMaxRowGap() {
        return maxRowGap;
    }
    
    public void setMaxRowGap(Integer maxRowGap) {
        this.maxRowGap = maxRowGap;
    }
    
    public Integer getMinColGap() {
        return minColGap;
    }
    
    public void setMinColGap(Integer minColGap) {
        this.minColGap = minColGap;
    }
    
    public Integer getMaxColGap() {
        return maxColGap;
    }
    
    public void setMaxColGap(Integer maxColGap) {
        this.maxColGap = maxColGap;
    }
    
    public Integer getMaxInternalVerticalGap() {
        return maxInternalVerticalGap;
    }
    
    public void setMaxInternalVerticalGap(Integer maxInternalVerticalGap) {
        this.maxInternalVerticalGap = maxInternalVerticalGap;
    }
    
    public Integer getMaxInternalHorizontalGap() {
        return maxInternalHorizontalGap;
    }
    
    public void setMaxInternalHorizontalGap(Integer maxInternalHorizontalGap) {
        this.maxInternalHorizontalGap = maxInternalHorizontalGap;
    }
    
    public Integer getIgnoreBorderWidth() {
        return ignoreBorderWidth;
    }
    
    public void setIgnoreBorderWidth(Integer ignoreBorderWidth) {
        this.ignoreBorderWidth = ignoreBorderWidth;
    }
    
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }
    
    public void setExtraParams(Map<String, Object> extraParams) {
        this.extraParams = extraParams;
    }
    
    public String getPresetName() {
        return presetName;
    }
    
    public void setPresetName(String presetName) {
        this.presetName = presetName;
    }
}
