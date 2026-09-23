package com.honda.paroledipinte.util;

import com.honda.paroledipinte.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.honda.paroledipinte.model.Shape;
import com.honda.paroledipinte.model.XAnyLabelingJson;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.List;

/**
 * XAnyLabeling JSON文件加载器
 */
public class XAnyLabelingJsonLoader {
    
    /**
     * 加载结果
     */
    public static class LoadResult {
        private final boolean success;
        private final String message;
        private final BufferedImage image;
        private final List<Shape> shapes;
        private final XAnyLabelingJson jsonData;
        
        public LoadResult(boolean success, String message, BufferedImage image, 
                         List<Shape> shapes, XAnyLabelingJson jsonData) {
        LogUtil.log("[LoadResult] 开始执行");
            LogUtil.log("[LoadResult] 开始执行");
            this.success = success;
            this.message = message;
            this.image = image;
            this.shapes = shapes;
            this.jsonData = jsonData;
            LogUtil.log("[LoadResult] 执行完成");
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public BufferedImage getImage() {
            return image;
        }
        
        public List<Shape> getShapes() {
            return shapes;
        }
        
        public XAnyLabelingJson getJsonData() {
            return jsonData;
        }
        
        public static LoadResult success(String message, BufferedImage image, 
                                         List<Shape> shapes, XAnyLabelingJson jsonData) {
        LogUtil.log("[success] 开始执行");
            LogUtil.log("[LoadResult.success] 开始执行");
            LogUtil.log("[LoadResult.success] 执行完成");
            return new LoadResult(true, message, image, shapes, jsonData);
        }
        
        public static LoadResult error(String message) {
        LogUtil.log("[error] 开始执行");
            LogUtil.log("[LoadResult.error] 开始执行");
            LogUtil.log("[LoadResult.error] 执行完成");
            return new LoadResult(false, message, null, null, null);
        }
    }
    
    /**
     * 从文件加载XAnyLabeling JSON
     */
    public static LoadResult loadFromFile(File jsonFile) {
        LogUtil.log("[loadFromFile] 开始执行");
        LogUtil.log("[loadFromFile] 开始执行");
        try {
            // 读取JSON文件内容
            String jsonContent = readFileContent(jsonFile);
            if (jsonContent == null) {
                LogUtil.log("[loadFromFile] 执行完成");
                return LoadResult.error("无法读取JSON文件");
            }
            
            // 解析JSON
            XAnyLabelingJson jsonData = XAnyLabelingJson.fromJson(jsonContent);
            
            // 加载图片
            BufferedImage image = loadImage(jsonData, jsonFile.getParentFile());
            if (image == null) {
                LogUtil.log("[loadFromFile] 执行完成");
                return LoadResult.error("无法加载图片: JSON中未找到有效的图片数据");
            }
            
            // 获取shapes
            List<Shape> shapes = jsonData.getShapes();
            
            String message = String.format("成功加载JSON: %s, 图片尺寸: %dx%d, 标注数量: %d",
                    jsonFile.getName(), image.getWidth(), image.getHeight(),
                    shapes != null ? shapes.size() : 0);
            
            LogUtil.log("[loadFromFile] 执行完成");
            return LoadResult.success(message, image, shapes, jsonData);
            
        } catch (Exception e) {
            LogUtil.log("[loadFromFile] 发生错误: " + e.getMessage());
            return LoadResult.error("加载JSON文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取文件内容
     */
    private static String readFileContent(File file) {
        LogUtil.log("[readFileContent] 开始执行");
        LogUtil.log("[readFileContent] 开始执行");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            LogUtil.log("[readFileContent] 执行完成");
            return sb.toString();
        } catch (IOException e) {
            LogUtil.log("[readFileContent] 发生错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 加载图片
     */
    private static BufferedImage loadImage(XAnyLabelingJson jsonData, File parentDir) {
        LogUtil.log("[loadImage] 开始执行");
        LogUtil.log("[loadImage] 开始执行");
        BufferedImage image = null;
        
        // 优先尝试从base64加载
        if (jsonData.getImageData() != null && !jsonData.getImageData().isEmpty()) {
            image = loadImageFromBase64(jsonData.getImageData());
        }
        
        // 如果base64加载失败，尝试从文件路径加载
        if (image == null && jsonData.getImagePath() != null && !jsonData.getImagePath().isEmpty()) {
            image = loadImageFromPath(jsonData.getImagePath(), parentDir);
        }
        
        LogUtil.log("[loadImage] 执行完成");
        return image;
    }
    
    /**
     * 从base64字符串加载图片
     */
    private static BufferedImage loadImageFromBase64(String base64Data) {
        LogUtil.log("[loadImageFromBase64] 开始执行");
        LogUtil.log("[loadImageFromBase64] 开始执行");
        try {
            // 移除可能的data URI前缀
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            LogUtil.log("[loadImageFromBase64] 执行完成");
            return ImageIO.read(bais);
        } catch (Exception e) {
            LogUtil.log("[loadImageFromBase64] 发生错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从文件路径加载图片
     */
    private static BufferedImage loadImageFromPath(String imagePath, File parentDir) {
        LogUtil.log("[loadImageFromPath] 开始执行");
        LogUtil.log("[loadImageFromPath] 开始执行");
        try {
            // 尝试直接作为绝对路径
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                LogUtil.log("[loadImageFromPath] 执行完成");
                return ImageIO.read(imageFile);
            }
            
            // 尝试相对于JSON文件的父目录
            if (parentDir != null) {
        LogUtil.log("[if] 开始执行");
                imageFile = new File(parentDir, imagePath);
                if (imageFile.exists()) {
                    LogUtil.log("[loadImageFromPath] 执行完成");
                    return ImageIO.read(imageFile);
                }
                
                // 尝试只使用文件名
                String fileName = new File(imagePath).getName();
                imageFile = new File(parentDir, fileName);
                if (imageFile.exists()) {
                    LogUtil.log("[loadImageFromPath] 执行完成");
                    return ImageIO.read(imageFile);
                }
            }
            
            LogUtil.log("[loadImageFromPath] 执行完成");
            return null;
        } catch (Exception e) {
            LogUtil.log("[loadImageFromPath] 发生错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查文件是否为XAnyLabeling JSON文件
     */
    public static boolean isXAnyLabelingJson(File file) {
        LogUtil.log("[isXAnyLabelingJson] 开始执行");
        if (file == null || !file.exists() || !file.isFile()) {
            LogUtil.log("[isXAnyLabelingJson] 执行完成");
            return false;
        }
        
        String name = file.getName().toLowerCase();
        if (!name.endsWith(".json")) {
            LogUtil.log("[isXAnyLabelingJson] 执行完成");
            return false;
        }
        
        try {
            String content = readFileContent(file);
            if (content == null) {
                LogUtil.log("[isXAnyLabelingJson] 执行完成");
                return false;
            }
            
            JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
            // 检查是否包含XAnyLabeling特有的字段
            LogUtil.log("[isXAnyLabelingJson] 执行完成");
            return jsonObject.has("shapes") && (jsonObject.has("imageData") || jsonObject.has("imagePath"));
        } catch (Exception e) {
            LogUtil.log("[isXAnyLabelingJson] 发生错误: " + e.getMessage());
            return false;
        }
    }
}
