package com.honda.paroledipinte.model;

import com.honda.paroledipinte.util.LogUtil;

/**
 * 字符相对位置信息
 * 存储字符串中每个字符相对于首字符的位置信息
 */
public class CharRelativePosition {
    
    // 字符索引（在字符串中的位置）
    private int charIndex;
    
    // 字符本身
    private String character;
    
    // 相对首字符的偏移量（像素）
    private int relativeX;  // X方向偏移（水平）
    private int relativeY;  // Y方向偏移（垂直，通常为0表示同一行）
    
    // 字符尺寸
    private int width;      // 实际宽度
    private int height;     // 实际高度
    
    // 宽度范围（用于定位时的容错）
    private int minWidth;   // 最小宽度
    private int maxWidth;   // 最大宽度
    
    // 高度范围
    private int minHeight;  // 最小高度
    private int maxHeight;  // 最大高度
    
    // 与前一个字符的间距
    private int spacing;    // 实际间距
    private int minSpacing; // 最小间距
    private int maxSpacing; // 最大间距
    
    // 字符类型
    private CharType charType;
    
    /**
     * 字符类型枚举
     */
    public enum CharType {
        FULL_WIDTH,     // 全角字符（如汉字、全角标点）
        HALF_WIDTH,     // 半角字符（如英文、数字）
        HALF_WIDTH_UPPER, // 半角大写字母
        HALF_WIDTH_LOWER, // 半角小写字母
        HALF_WIDTH_DIGIT, // 半角数字
        PUNCTUATION,    // 标点符号
        CHINESE,        // 中文字符
        OTHER           // 其他
    }
    
    public CharRelativePosition() {
        LogUtil.log("[CharRelativePosition] 创建新实例");
        // 设置默认值
        this.minWidth = 5;
        this.maxWidth = 100;
        this.minHeight = 5;
        this.maxHeight = 100;
        this.minSpacing = 0;
        this.maxSpacing = 50;
        this.charType = CharType.OTHER;
    }
    
    /**
     * 根据字符自动判断字符类型
     */
    public static CharType detectCharType(String ch) {
        if (ch == null || ch.isEmpty()) {
            return CharType.OTHER;
        }
        char c = ch.charAt(0);
        
        // 判断是否为中文字符
        if (c >= 0x4E00 && c <= 0x9FFF) {
            return CharType.CHINESE;
        }
        
        // 判断是否为全角字符
        if (c >= 0xFF01 && c <= 0xFF5E) {
            return CharType.FULL_WIDTH;
        }
        
        // 判断是否为大写字母
        if (c >= 'A' && c <= 'Z') {
            return CharType.HALF_WIDTH_UPPER;
        }
        
        // 判断是否为小写字母
        if (c >= 'a' && c <= 'z') {
            return CharType.HALF_WIDTH_LOWER;
        }
        
        // 判断是否为数字
        if (c >= '0' && c <= '9') {
            return CharType.HALF_WIDTH_DIGIT;
        }
        
        // 判断是否为标点符号
        if (isPunctuation(c)) {
            return CharType.PUNCTUATION;
        }
        
        // ASCII范围内的为半角字符
        if (c < 128) {
            return CharType.HALF_WIDTH;
        }
        
        return CharType.OTHER;
    }
    
    /**
     * 判断是否为标点符号
     */
    private static boolean isPunctuation(char c) {
        String punctuations = ".,;:!?-'\"()[]{}<>/\\|@#$%^&*~`+-_=";
        return punctuations.indexOf(c) >= 0 || 
               (c >= 0x3000 && c <= 0x303F) || // CJK标点符号
               (c >= 0xFF00 && c <= 0xFF0F) || // 全角ASCII标点
               (c >= 0xFF1A && c <= 0xFF1F) || // 全角ASCII标点
               (c >= 0xFF3B && c <= 0xFF3D) || // 全角方括号
               (c >= 0xFF5B && c <= 0xFF5D) || // 全角花括号
               (c >= 0xFF5F && c <= 0xFF60);   // 全角圆括号
    }
    
    /**
     * 判断是否为首字符（索引为0）
     */
    public boolean isFirstChar() {
        return charIndex == 0;
    }
    
    /**
     * 判断是否为全角字符
     */
    public boolean isFullWidth() {
        return charType == CharType.FULL_WIDTH || charType == CharType.CHINESE;
    }
    
    /**
     * 判断是否为半角字符
     */
    public boolean isHalfWidth() {
        return charType == CharType.HALF_WIDTH || 
               charType == CharType.HALF_WIDTH_UPPER ||
               charType == CharType.HALF_WIDTH_LOWER ||
               charType == CharType.HALF_WIDTH_DIGIT;
    }
    
    // Getters and Setters
    
    public int getCharIndex() {
        return charIndex;
    }
    
    public void setCharIndex(int charIndex) {
        this.charIndex = charIndex;
    }
    
    public String getCharacter() {
        return character;
    }
    
    public void setCharacter(String character) {
        this.character = character;
        // 自动检测字符类型
        this.charType = detectCharType(character);
    }
    
    public int getRelativeX() {
        return relativeX;
    }
    
    public void setRelativeX(int relativeX) {
        this.relativeX = relativeX;
    }
    
    public int getRelativeY() {
        return relativeY;
    }
    
    public void setRelativeY(int relativeY) {
        this.relativeY = relativeY;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
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
    
    public int getSpacing() {
        return spacing;
    }
    
    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }
    
    public int getMinSpacing() {
        return minSpacing;
    }
    
    public void setMinSpacing(int minSpacing) {
        this.minSpacing = minSpacing;
    }
    
    public int getMaxSpacing() {
        return maxSpacing;
    }
    
    public void setMaxSpacing(int maxSpacing) {
        this.maxSpacing = maxSpacing;
    }
    
    public CharType getCharType() {
        return charType;
    }
    
    public void setCharType(CharType charType) {
        this.charType = charType;
    }
    
    /**
     * 获取字符类型的显示名称
     */
    public String getCharTypeDisplayName() {
        if (charType == null) return "其他";
        switch (charType) {
            case FULL_WIDTH: return "全角";
            case HALF_WIDTH: return "半角";
            case HALF_WIDTH_UPPER: return "大写";
            case HALF_WIDTH_LOWER: return "小写";
            case HALF_WIDTH_DIGIT: return "数字";
            case PUNCTUATION: return "标点";
            case CHINESE: return "汉字";
            case OTHER: return "其他";
            default: return "其他";
        }
    }
    
    @Override
    public String toString() {
        return "CharRelativePosition{" +
                "charIndex=" + charIndex +
                ", character='" + character + '\'' +
                ", relativeX=" + relativeX +
                ", width=" + width +
                ", height=" + height +
                ", charType=" + charType +
                '}';
    }
}
