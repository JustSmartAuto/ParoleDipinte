package com.honda.paroledipinte.model;

import com.honda.paroledipinte.util.LogUtil;
import java.util.List;

/**
 * OCR结果中的形状(字符区域)
 */
public class Shape {
    private String label;
    private double score;
    private List<List<Double>> points;
    private Integer groupId;
    private String description;
    private boolean difficult;
    private String shapeType;
    private double direction;
    
    public Shape() {
        LogUtil.log("[Shape] 开始执行");
        this.groupId = null;
        this.description = "";
        this.difficult = false;
        this.shapeType = "rotation";
        this.direction = 0.0;
    }
    
    public Shape(String label, double score, List<List<Double>> points) {
        this();
        this.label = label;
        this.score = score;
        this.points = points;
    }
    
    // Getters and Setters
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public List<List<Double>> getPoints() {
        return points;
    }
    
    public void setPoints(List<List<Double>> points) {
        this.points = points;
    }
    
    public Integer getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isDifficult() {
        return difficult;
    }
    
    public void setDifficult(boolean difficult) {
        this.difficult = difficult;
    }
    
    public String getShapeType() {
        return shapeType;
    }
    
    public void setShapeType(String shapeType) {
        this.shapeType = shapeType;
    }
    
    public double getDirection() {
        return direction;
    }
    
    public void setDirection(double direction) {
        this.direction = direction;
    }
}
