package com.honda.paroledipinte.util;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 模型资源加载工具类
 * 用于从classpath（fatjar内部）加载ONNX模型文件和字典文件
 * 自动将嵌入式资源提取到临时文件供OCR引擎使用
 */
public class ModelResourceLoader {
    
    private static final String MODELS_DIR = "models";
    private static final String DET_MODEL = "det.onnx";
    private static final String REC_MODEL = "rec.onnx";
    private static final String DICT_FILE = "dict.txt";
    
    private static File tempDetModel;
    private static File tempRecModel;
    private static File tempDictFile;
    private static boolean initialized = false;
    
    /**
     * 初始化模型资源
     * 将嵌入式模型文件提取到临时目录
     * 
     * @return true if initialization successful
     */
    public static synchronized boolean initialize() {
        if (initialized) {
            return true;
        }
        
        try {
            LogUtil.log("[ModelResourceLoader] 开始初始化模型资源");
            
            // 创建临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            File modelTempDir = new File(tempDir, "paroledipinte_models_" + timestamp);
            if (!modelTempDir.exists()) {
                modelTempDir.mkdirs();
            }
            
            // 提取检测模型
            tempDetModel = extractResource(MODELS_DIR + "/" + DET_MODEL, 
                    new File(modelTempDir, DET_MODEL));
            LogUtil.log("[ModelResourceLoader] 检测模型已提取: " + tempDetModel.getAbsolutePath());
            
            // 提取识别模型
            tempRecModel = extractResource(MODELS_DIR + "/" + REC_MODEL, 
                    new File(modelTempDir, REC_MODEL));
            LogUtil.log("[ModelResourceLoader] 识别模型已提取: " + tempRecModel.getAbsolutePath());
            
            // 提取字典文件
            tempDictFile = extractResource(MODELS_DIR + "/" + DICT_FILE, 
                    new File(modelTempDir, DICT_FILE));
            LogUtil.log("[ModelResourceLoader] 字典文件已提取: " + tempDictFile.getAbsolutePath());
            
            initialized = true;
            LogUtil.log("[ModelResourceLoader] 模型资源初始化完成");
            return true;
            
        } catch (Exception e) {
            LogUtil.error("[ModelResourceLoader] 模型资源初始化失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从classpath提取资源到临时文件
     */
    private static File extractResource(String resourcePath, File targetFile) throws IOException {
        // 首先尝试从classpath加载（fatjar内部）
        InputStream is = ModelResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        
        // 如果找不到，尝试使用绝对路径（开发环境）
        if (is == null) {
            File externalFile = new File(resourcePath);
            if (externalFile.exists()) {
                is = new FileInputStream(externalFile);
                LogUtil.log("[ModelResourceLoader] 从外部文件加载: " + externalFile.getAbsolutePath());
            }
        } else {
            LogUtil.log("[ModelResourceLoader] 从classpath加载: " + resourcePath);
        }
        
        if (is == null) {
            throw new IOException("找不到资源文件: " + resourcePath);
        }
        
        // 复制到临时文件
        try (OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        is.close();
        
        // 设置删除钩子
        targetFile.deleteOnExit();
        
        return targetFile;
    }
    
    /**
     * 获取检测模型文件路径
     * @return 检测模型文件的绝对路径
     */
    public static String getDetModelPath() {
        if (!initialized) {
            initialize();
        }
        return tempDetModel != null ? tempDetModel.getAbsolutePath() : null;
    }
    
    /**
     * 获取识别模型文件路径
     * @return 识别模型文件的绝对路径
     */
    public static String getRecModelPath() {
        if (!initialized) {
            initialize();
        }
        return tempRecModel != null ? tempRecModel.getAbsolutePath() : null;
    }
    
    /**
     * 获取字典文件路径
     * @return 字典文件的绝对路径
     */
    public static String getDictFilePath() {
        if (!initialized) {
            initialize();
        }
        return tempDictFile != null ? tempDictFile.getAbsolutePath() : null;
    }
    
    /**
     * 检查模型资源是否可用
     * @return true if all model files are available
     */
    public static boolean isAvailable() {
        if (!initialized) {
            return initialize();
        }
        return tempDetModel != null && tempDetModel.exists() &&
               tempRecModel != null && tempRecModel.exists() &&
               tempDictFile != null && tempDictFile.exists();
    }
    
    /**
     * 清理临时文件（应用退出时调用）
     */
    public static void cleanup() {
        if (tempDetModel != null && tempDetModel.exists()) {
            tempDetModel.delete();
        }
        if (tempRecModel != null && tempRecModel.exists()) {
            tempRecModel.delete();
        }
        if (tempDictFile != null && tempDictFile.exists()) {
            tempDictFile.delete();
        }
        // 尝试删除父目录
        if (tempDetModel != null) {
            File parentDir = tempDetModel.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                parentDir.delete();
            }
        }
        initialized = false;
        LogUtil.log("[ModelResourceLoader] 临时模型文件已清理");
    }
}
