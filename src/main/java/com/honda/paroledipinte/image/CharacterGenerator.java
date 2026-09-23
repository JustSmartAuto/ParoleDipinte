package com.honda.paroledipinte.image;

import com.honda.paroledipinte.util.LogUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 字符图片生成器
 * 使用SimSun字体生成标准字符图片
 */
public class CharacterGenerator {
    public static final int WIDTH = 70;
    public static final int HEIGHT = 76;
    public static final float FONT_SIZE = 56f;
    
    private Font font;
    
    public CharacterGenerator() {
        LogUtil.log("[CharacterGenerator] 开始执行");
        this.font = loadFont();
        LogUtil.log("[CharacterGenerator] 执行完成");
    }
    
    /**
     * 加载字体
     */
    public static Font loadFont() {
        LogUtil.log("[loadFont] 开始执行");
        // 首先尝试从资源目录加载
        try {
            InputStream is = CharacterGenerator.class.getResourceAsStream("/fonts/simsun.ttf");
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                is.close();
                LogUtil.log("[loadFont] 执行完成");
                return font.deriveFont(Font.PLAIN, FONT_SIZE);
            }
        } catch (Exception e) {
            LogUtil.log("[loadFont] 发生错误: " + e.getMessage());
        }
        
        // 尝试从文件系统加载
        try {
            File fontFile = new File("fonts/simsun.ttf");
            if (fontFile.exists()) {
                LogUtil.log("[loadFont] 执行完成");
                return Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, FONT_SIZE);
            }
        } catch (Exception e) {
            LogUtil.log("[loadFont] 发生错误: " + e.getMessage());
        }
        
        // 使用系统字体
        Font sysFont = new Font("SimSun", Font.PLAIN, (int) FONT_SIZE);
        if (sysFont.getFamily().equals("Dialog")) {
            sysFont = new Font("Serif", Font.PLAIN, (int) FONT_SIZE);
        }
        LogUtil.log("[loadFont] 执行完成");
        return sysFont;
    }
    
    /**
     * 生成单个字符图片
     */
    public BufferedImage generateCharacterImage(String ch) {
        LogUtil.log("[generateCharacterImage] 开始执行");
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        
        // 白色背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(ch, frc);
        
        // 居中显示
        int x = (int) ((WIDTH - bounds.getWidth()) / 2 - bounds.getX());
        int y = (int) ((HEIGHT - bounds.getHeight()) / 2 - bounds.getY());
        
        g2d.drawString(ch, x, y);
        g2d.dispose();
        
        LogUtil.log("[generateCharacterImage] 执行完成");
        return image;
    }
    
    /**
     * 获取5000个常用字符列表
     */
    public static List<String> getCommonCharacters() {
        LogUtil.log("[getCommonCharacters] 开始执行");
        List<String> list = new ArrayList<>();
        
        // 常用汉字 (从Unicode CJK Unified Ideographs)
        // 使用最常用的5000个汉字范围
        String[] commonChars = {
            // 最常用的100个汉字
            "的", "一", "是", "在", "不", "了", "有", "和", "人", "这",
            "中", "大", "为", "上", "个", "国", "我", "以", "要", "他",
            "时", "来", "用", "们", "生", "到", "作", "地", "于", "出",
            "就", "分", "对", "成", "会", "可", "主", "发", "年", "动",
            "同", "工", "也", "能", "下", "过", "子", "说", "产", "种",
            "面", "而", "方", "后", "多", "定", "行", "学", "法", "所",
            "民", "得", "经", "十", "三", "之", "进", "着", "等", "部",
            "度", "家", "电", "力", "里", "如", "水", "化", "高", "自",
            "二", "理", "起", "小", "物", "现", "实", "加", "量", "都",
            "两", "体", "制", "机", "当", "使", "点", "从", "业", "本"
        };
        
        for (String ch : commonChars) {
            list.add(ch);
        }
        
        // 添加更多常用汉字 (Unicode 4E00-9FA5 范围内的常用字)
        int start = 0x4E00;
        int end = Math.min(start + 4900, 0x9FA5 + 1);
        for (int codePoint = start; codePoint < end; codePoint++) {
            String ch = String.valueOf((char) codePoint);
            if (!list.contains(ch)) {
                list.add(ch);
            }
        }
        
        // 英文字母
        for (char c = 'A'; c <= 'Z'; c++) {
            list.add(String.valueOf(c));
        }
        for (char c = 'a'; c <= 'z'; c++) {
            list.add(String.valueOf(c));
        }
        
        // 数字
        for (char c = '0'; c <= '9'; c++) {
            list.add(String.valueOf(c));
        }
        
        // 常用标点符号
        String punctuations = "。，、；：？！\"\"''（）《》【】—…～·｜'";
        for (char c : punctuations.toCharArray()) {
            if (!list.contains(String.valueOf(c))) {
                list.add(String.valueOf(c));
            }
        }
        
        LogUtil.log("[getCommonCharacters] 执行完成");
        return list;
    }
    
    /**
     * 获取ASCII字符的文件夹名称
     * 对于非ASCII字符，使用Unicode编码
     */
    public static String getCharFolderName(String ch) {
        LogUtil.log("[getCharFolderName] 开始执行");
        if (ch.length() == 1) {
            char c = ch.charAt(0);
            if (c < 128) {
                // ASCII字符直接使用
                LogUtil.log("[getCharFolderName] 执行完成");
                return String.valueOf((int) c);
            }
        }
        // 非ASCII字符使用Unicode编码
        StringBuilder sb = new StringBuilder();
        for (char c : ch.toCharArray()) {
            sb.append(String.format("U+%04X", (int) c));
        }
        LogUtil.log("[getCharFolderName] 执行完成");
        return sb.toString();
    }
}
