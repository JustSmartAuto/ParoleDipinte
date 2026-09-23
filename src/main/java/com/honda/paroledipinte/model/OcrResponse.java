package com.honda.paroledipinte.model;

import com.honda.paroledipinte.util.LogUtil;
import java.util.List;
import java.util.Map;

/**
 * OCR响应模型
 */
public class OcrResponse {
    private String imageHash;
    private String version;
    private Map<String, Object> flags;
    private List<Shape> shapes;
    private String imageData;
    
    public OcrResponse() {
        LogUtil.log("[OcrResponse] 开始执行");
        this.version = "3.3.4";
    }
    
    public OcrResponse(String imageHash) {
        this();
        this.imageHash = imageHash;
    }
    
    // Getters and Setters
    public String getImageHash() {
        return imageHash;
    }
    
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public Map<String, Object> getFlags() {
        return flags;
    }
    
    public void setFlags(Map<String, Object> flags) {
        this.flags = flags;
    }
    
    public List<Shape> getShapes() {
        return shapes;
    }
    
    public void setShapes(List<Shape> shapes) {
        this.shapes = shapes;
    }
    
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
