package com.honda.paroledipinte.model;

import com.honda.paroledipinte.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * XAnyLabeling JSON格式模型
 */
public class XAnyLabelingJson {
    
    private String version;
    private Map<String, Object> flags;
    private List<Shape> shapes;
    
    @SerializedName("imagePath")
    private String imagePath;
    
    @SerializedName("imageData")
    private String imageData;
    
    @SerializedName("imageHeight")
    private int imageHeight;
    
    @SerializedName("imageWidth")
    private int imageWidth;
    
    public XAnyLabelingJson() {
        LogUtil.log("[XAnyLabelingJson] 开始执行");
    }
    
    // Getters and Setters
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
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    public int getImageHeight() {
        return imageHeight;
    }
    
    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }
    
    public int getImageWidth() {
        return imageWidth;
    }
    
    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }
    
    /**
     * 从JSON字符串解析
     */
    public static XAnyLabelingJson fromJson(String json) {
        LogUtil.log("[fromJson] 开始执行");
        Gson gson = new Gson();
        return gson.fromJson(json, XAnyLabelingJson.class);
    }
    
    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        LogUtil.log("[toJson] 开始执行");
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
