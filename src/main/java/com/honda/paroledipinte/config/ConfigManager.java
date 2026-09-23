package com.honda.paroledipinte.config;

import com.honda.paroledipinte.util.LogUtil;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * 配置文件管理类
 * 用于存储和加载应用程序配置参数
 * 配置文件与JAR文件同目录
 */
public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static String configFilePath = null;
    private static final Properties properties = new Properties();
    private static boolean initialized = false;
    
    /**
     * 获取JAR文件所在目录
     */
    private static String getJarDirectory() {
        LogUtil.log("[getJarDirectory] 开始执行");
        try {
            String jarPath = ConfigManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            LogUtil.log("[getJarDirectory] 执行完成");
            return jarFile.getParent();
        } catch (URISyntaxException e) {
            LogUtil.log("[getJarDirectory] 发生错误: " + e.getMessage());
            // 如果无法获取JAR目录，使用当前工作目录
            return System.getProperty("user.dir");
        }
    }
    
    /**
     * 获取配置文件完整路径
     */
    private static String getConfigFilePath() {
        LogUtil.log("[getConfigFilePath] 开始执行");
        if (configFilePath == null) {
            configFilePath = getJarDirectory() + File.separator + CONFIG_FILE_NAME;
        }
        LogUtil.log("[getConfigFilePath] 执行完成");
        return configFilePath;
    }
    
    // 默认配置值
    private static final String DEFAULT_BROKER = "tcp://127.0.0.1:8907";
    private static final String DEFAULT_OCR_TOPIC = "/ampinspection/ocr/260407/raw";
    private static final String DEFAULT_CLASSIFY_TOPIC = "/ampinspection/classify/260407/raw";
    private static final String DEFAULT_BINARIZE_TOPIC = "/ampinspection/binarize/260407/raw";
    private static final String DEFAULT_DATA_DIR = System.getProperty("user.home") + "/ampinspection/data";
    private static final String DEFAULT_IMAGE_DIR = System.getProperty("user.home") + "/ampinspection/images";
    private static final String DEFAULT_LOG_DIR = System.getProperty("user.home") + "/ampinspection/logs";
    private static final String DEFAULT_AUTO_START = "true";
    private static final String DEFAULT_AUTO_START_MQTT = "true";
    private static final String DEFAULT_WINDOW_WIDTH = "1000";
    private static final String DEFAULT_WINDOW_HEIGHT = "750";
    private static final String DEFAULT_MQTT_RETURN_IMAGE = "false";  // 默认不返回imageData以节省带宽
    
    // OCR参数默认值
    private static final String DEFAULT_SERVICE = "";
    private static final String DEFAULT_GRAYSCALE = "128";
    private static final String DEFAULT_GAMMA = "0.5";
    private static final String DEFAULT_MIN_AREA = "15";
    private static final String DEFAULT_MAX_AREA = "100000";
    private static final String DEFAULT_MIN_WIDTH = "3";
    private static final String DEFAULT_MIN_HEIGHT = "3";
    private static final String DEFAULT_MAX_WIDTH = "500";
    private static final String DEFAULT_MAX_HEIGHT = "200";
    private static final String DEFAULT_MIN_ROW_GAP = "2";
    private static final String DEFAULT_MAX_ROW_GAP = "50";
    private static final String DEFAULT_MIN_COL_GAP = "2";
    private static final String DEFAULT_MAX_COL_GAP = "50";
    
    // 配置键名
    public static final String KEY_BROKER = "mqtt.broker";
    public static final String KEY_OCR_TOPIC = "mqtt.topic.ocr";
    public static final String KEY_CLASSIFY_TOPIC = "mqtt.topic.classify";
    public static final String KEY_BINARIZE_TOPIC = "mqtt.topic.binarize";
    public static final String KEY_DATA_DIR = "path.data";
    public static final String KEY_IMAGE_DIR = "path.images";
    public static final String KEY_LOG_DIR = "path.logs";
    public static final String KEY_AUTO_START = "app.autoStart";
    public static final String KEY_AUTO_START_MQTT = "app.autoStartMqtt";
    public static final String KEY_WINDOW_WIDTH = "app.windowWidth";
    public static final String KEY_WINDOW_HEIGHT = "app.windowHeight";
    public static final String KEY_MQTT_RETURN_IMAGE = "mqtt.returnImageData";  // 是否返回imageData字段
    
    // OCR参数键名
    public static final String KEY_SERVICE = "ocr.service";
    public static final String KEY_GRAYSCALE = "ocr.grayscale";
    public static final String KEY_GAMMA = "ocr.gamma";
    public static final String KEY_MIN_AREA = "ocr.minArea";
    public static final String KEY_MAX_AREA = "ocr.maxArea";
    public static final String KEY_MIN_WIDTH = "ocr.minWidth";
    public static final String KEY_MIN_HEIGHT = "ocr.minHeight";
    public static final String KEY_MAX_WIDTH = "ocr.maxWidth";
    public static final String KEY_MAX_HEIGHT = "ocr.maxHeight";
    public static final String KEY_MIN_ROW_GAP = "ocr.minRowGap";
    public static final String KEY_MAX_ROW_GAP = "ocr.maxRowGap";
    public static final String KEY_MIN_COL_GAP = "ocr.minColGap";
    public static final String KEY_MAX_COL_GAP = "ocr.maxColGap";
    
    // Lua脚本键名
    public static final String KEY_TEXT_LOCATION_SCRIPT = "lua.textLocationScript";
    public static final String KEY_CHAR_CLASSIFY_SCRIPT = "lua.charClassifyScript";
    public static final String KEY_TEXT_LOCATION_ENABLED = "lua.textLocationEnabled";
    public static final String KEY_CHAR_CLASSIFY_ENABLED = "lua.charClassifyEnabled";
    
    // 日志脚本键名
    public static final String KEY_LOG_SCRIPT = "lua.logScript";
    public static final String KEY_LOG_SCRIPT_ENABLED = "lua.logScriptEnabled";
    
    // 预处理脚本键名
    public static final String KEY_PREPROCESS_SCRIPT = "lua.preprocessScript";
    public static final String KEY_PREPROCESS_SCRIPT_ENABLED = "lua.preprocessScriptEnabled";
    
    // OCR后处理脚本键名
    public static final String KEY_OCR_POST_PROCESS_SCRIPT = "lua.ocrPostProcessScript";
    public static final String KEY_OCR_POST_PROCESS_SCRIPT_ENABLED = "lua.ocrPostProcessScriptEnabled";
    
    // 新增OCR功能配置键名
    public static final String KEY_FORCE_WIDTH_SPLIT = "ocr.forceWidthSplit";
    public static final String KEY_FORCE_SPLIT_WIDTH = "ocr.forceSplitWidth";
    public static final String KEY_MULTI_LINE_SORT = "ocr.multiLineSort";
    public static final String KEY_ROW_GAP_THRESHOLD = "ocr.rowGapThreshold";
    
    // 字符内部空隙参数键名
    public static final String KEY_MAX_INTERNAL_VERTICAL_GAP = "ocr.maxInternalVerticalGap";
    public static final String KEY_MAX_INTERNAL_HORIZONTAL_GAP = "ocr.maxInternalHorizontalGap";
    
    // 忽略边界区域参数键名
    public static final String KEY_IGNORE_BORDER_WIDTH = "ocr.ignoreBorderWidth";
    
    // 字符串预设存储方式键名
    public static final String KEY_STRING_PRESET_STORAGE_MODE = "stringPreset.storageMode";
    public static final String DEFAULT_STRING_PRESET_STORAGE_MODE = "xml"; // 默认XML方式
    
    // 字符相对位置键名
    public static final String KEY_CHAR_RELATIVE_POSITIONS = "ocr.charRelativePositions";
    public static final String KEY_CHAR_RELATIVE_POSITION_ENABLED = "ocr.charRelativePositionEnabled";
    
    // 阈值偏移量默认值
    private static final String DEFAULT_THRESHOLD_OFFSET = "-20";
    
    // 新增OCR功能默认值
    private static final String DEFAULT_FORCE_WIDTH_SPLIT = "false";
    private static final String DEFAULT_FORCE_SPLIT_WIDTH = "31";
    private static final String DEFAULT_MULTI_LINE_SORT = "false";
    private static final String DEFAULT_ROW_GAP_THRESHOLD = "20";
    
    // 字符内部空隙默认值（MaxHeight - MinHeight = 200-3=197, MaxWidth - MinWidth = 500-3=497）
    private static final String DEFAULT_MAX_INTERNAL_VERTICAL_GAP = "197";
    private static final String DEFAULT_MAX_INTERNAL_HORIZONTAL_GAP = "497";
    
    // 忽略边界区域默认值
    private static final String DEFAULT_IGNORE_BORDER_WIDTH = "5";
    
    /**
     * 初始化配置管理器
     */
    public static synchronized void init() {
        LogUtil.log("[init] 开始执行");
        if (initialized) {
            LogUtil.log("[init] 执行完成");
            return;
        }
        
        // 确保配置目录存在（JAR所在目录）
        String jarDir = getJarDirectory();
        File configDir = new File(jarDir);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        loadConfig();
        initialized = true;
        LogUtil.log("[init] 执行完成");
    }
    
    /**
     * 加载配置文件
     */
    public static void loadConfig() {
        LogUtil.log("[loadConfig] 开始执行");
        File configFile = new File(getConfigFilePath());
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
                LogUtil.log("[loadConfig] 配置文件已加载: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                LogUtil.log("[loadConfig] 发生错误: " + e.getMessage());
                loadDefaults();
                saveConfig();
            }
        } else {
            LogUtil.log("[loadConfig] 配置文件不存在，使用默认配置: " + configFile.getAbsolutePath());
            loadDefaults();
            saveConfig();
        }
        LogUtil.log("[loadConfig] 执行完成");
    }
    
    /**
     * 加载默认配置
     */
    private static void loadDefaults() {
        LogUtil.log("[loadDefaults] 开始执行");
        // MQTT配置
        properties.setProperty(KEY_BROKER, DEFAULT_BROKER);
        properties.setProperty(KEY_OCR_TOPIC, DEFAULT_OCR_TOPIC);
        properties.setProperty(KEY_CLASSIFY_TOPIC, DEFAULT_CLASSIFY_TOPIC);
        properties.setProperty(KEY_BINARIZE_TOPIC, DEFAULT_BINARIZE_TOPIC);
        
        // 路径配置
        properties.setProperty(KEY_DATA_DIR, DEFAULT_DATA_DIR);
        properties.setProperty(KEY_IMAGE_DIR, DEFAULT_IMAGE_DIR);
        properties.setProperty(KEY_LOG_DIR, DEFAULT_LOG_DIR);
        
        // 应用配置
        properties.setProperty(KEY_AUTO_START, DEFAULT_AUTO_START);
        properties.setProperty(KEY_AUTO_START_MQTT, DEFAULT_AUTO_START_MQTT);
        properties.setProperty(KEY_WINDOW_WIDTH, DEFAULT_WINDOW_WIDTH);
        properties.setProperty(KEY_WINDOW_HEIGHT, DEFAULT_WINDOW_HEIGHT);
        
        // OCR参数
        properties.setProperty(KEY_SERVICE, DEFAULT_SERVICE);
        properties.setProperty(KEY_GRAYSCALE, DEFAULT_GRAYSCALE);
        properties.setProperty(KEY_GAMMA, DEFAULT_GAMMA);
        properties.setProperty(KEY_MIN_AREA, DEFAULT_MIN_AREA);
        properties.setProperty(KEY_MAX_AREA, DEFAULT_MAX_AREA);
        properties.setProperty(KEY_MIN_WIDTH, DEFAULT_MIN_WIDTH);
        properties.setProperty(KEY_MIN_HEIGHT, DEFAULT_MIN_HEIGHT);
        properties.setProperty(KEY_MAX_WIDTH, DEFAULT_MAX_WIDTH);
        properties.setProperty(KEY_MAX_HEIGHT, DEFAULT_MAX_HEIGHT);
        properties.setProperty(KEY_MIN_ROW_GAP, DEFAULT_MIN_ROW_GAP);
        properties.setProperty(KEY_MAX_ROW_GAP, DEFAULT_MAX_ROW_GAP);
        properties.setProperty(KEY_MIN_COL_GAP, DEFAULT_MIN_COL_GAP);
        properties.setProperty(KEY_MAX_COL_GAP, DEFAULT_MAX_COL_GAP);
        
        // Lua脚本配置
        properties.setProperty(KEY_TEXT_LOCATION_ENABLED, "false");
        properties.setProperty(KEY_CHAR_CLASSIFY_ENABLED, "false");
        properties.setProperty(KEY_LOG_SCRIPT_ENABLED, "false");
        properties.setProperty(KEY_LOG_SCRIPT, "");
        properties.setProperty(KEY_PREPROCESS_SCRIPT_ENABLED, "false");
        properties.setProperty(KEY_PREPROCESS_SCRIPT, "");
        
        // MQTT配置
        properties.setProperty(KEY_MQTT_RETURN_IMAGE, DEFAULT_MQTT_RETURN_IMAGE);
        
        // OCR后处理配置
        properties.setProperty(KEY_OCR_POST_PROCESS_SCRIPT_ENABLED, "false");
        properties.setProperty(KEY_OCR_POST_PROCESS_SCRIPT, "");
        
        // 新增OCR功能配置
        properties.setProperty(KEY_FORCE_WIDTH_SPLIT, DEFAULT_FORCE_WIDTH_SPLIT);
        properties.setProperty(KEY_FORCE_SPLIT_WIDTH, DEFAULT_FORCE_SPLIT_WIDTH);
        properties.setProperty(KEY_MULTI_LINE_SORT, DEFAULT_MULTI_LINE_SORT);
        properties.setProperty(KEY_ROW_GAP_THRESHOLD, DEFAULT_ROW_GAP_THRESHOLD);
        
        // 字符内部空隙参数
        properties.setProperty(KEY_MAX_INTERNAL_VERTICAL_GAP, DEFAULT_MAX_INTERNAL_VERTICAL_GAP);
        properties.setProperty(KEY_MAX_INTERNAL_HORIZONTAL_GAP, DEFAULT_MAX_INTERNAL_HORIZONTAL_GAP);
        
        // 忽略边界区域参数
        properties.setProperty(KEY_IGNORE_BORDER_WIDTH, DEFAULT_IGNORE_BORDER_WIDTH);
        
        // 字符串预设存储方式
        properties.setProperty(KEY_STRING_PRESET_STORAGE_MODE, DEFAULT_STRING_PRESET_STORAGE_MODE);
        
        // 字符相对位置默认值
        properties.setProperty(KEY_CHAR_RELATIVE_POSITION_ENABLED, "false");
        properties.setProperty(KEY_CHAR_RELATIVE_POSITIONS, "");
        
        LogUtil.log("[loadDefaults] 执行完成");
    }
    
    /**
     * 保存配置到文件
     */
    public static void saveConfig() {
        LogUtil.log("[saveConfig] 开始执行");
        String configPath = getConfigFilePath();
        try (OutputStream output = new FileOutputStream(configPath)) {
            properties.store(output, "识字涂色(ParoleDipinte) 配置文件");
            LogUtil.log("[saveConfig] 配置已保存到: " + configPath);
        } catch (IOException e) {
            LogUtil.log("[saveConfig] 发生错误: " + e.getMessage());
        }
        LogUtil.log("[saveConfig] 执行完成");
    }
    
    /**
     * 获取配置值
     */
    public static String getProperty(String key) {
        LogUtil.log("[getProperty] 开始执行");
        init();
        LogUtil.log("[getProperty] 执行完成");
        return properties.getProperty(key);
    }
    
    /**
     * 获取配置值，带默认值
     */
    public static String getProperty(String key, String defaultValue) {
        LogUtil.log("[getProperty] 开始执行");
        init();
        LogUtil.log("[getProperty] 执行完成");
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * 设置配置值
     */
    public static void setProperty(String key, String value) {
        LogUtil.log("[setProperty] 开始执行");
        init();
        properties.setProperty(key, value);
        LogUtil.log("[setProperty] 执行完成");
    }
    
    // MQTT配置 getter/setter
    public static String getBroker() {
        return getProperty(KEY_BROKER, DEFAULT_BROKER);
    }
    
    public static void setBroker(String broker) {
        setProperty(KEY_BROKER, broker);
    }
    
    public static String getOcrTopic() {
        return getProperty(KEY_OCR_TOPIC, DEFAULT_OCR_TOPIC);
    }
    
    public static void setOcrTopic(String topic) {
        setProperty(KEY_OCR_TOPIC, topic);
    }
    
    public static String getClassifyTopic() {
        return getProperty(KEY_CLASSIFY_TOPIC, DEFAULT_CLASSIFY_TOPIC);
    }
    
    public static void setClassifyTopic(String topic) {
        setProperty(KEY_CLASSIFY_TOPIC, topic);
    }
    
    public static String getBinarizeTopic() {
        return getProperty(KEY_BINARIZE_TOPIC, DEFAULT_BINARIZE_TOPIC);
    }
    
    public static void setBinarizeTopic(String topic) {
        setProperty(KEY_BINARIZE_TOPIC, topic);
    }
    
    // 路径配置 getter/setter
    public static String getDataDir() {
        return getProperty(KEY_DATA_DIR, DEFAULT_DATA_DIR);
    }
    
    public static void setDataDir(String dir) {
        setProperty(KEY_DATA_DIR, dir);
    }
    
    public static String getImageDir() {
        return getProperty(KEY_IMAGE_DIR, DEFAULT_IMAGE_DIR);
    }
    
    public static void setImageDir(String dir) {
        setProperty(KEY_IMAGE_DIR, dir);
    }
    
    public static String getLogDir() {
        return getProperty(KEY_LOG_DIR, DEFAULT_LOG_DIR);
    }
    
    public static void setLogDir(String dir) {
        setProperty(KEY_LOG_DIR, dir);
    }
    
    // 应用配置 getter/setter
    public static boolean isAutoStart() {
        return Boolean.parseBoolean(getProperty(KEY_AUTO_START, DEFAULT_AUTO_START));
    }
    
    public static void setAutoStart(boolean autoStart) {
        setProperty(KEY_AUTO_START, String.valueOf(autoStart));
    }
    
    // 软件设置 getter/setter
    public static boolean isAutoStartMqtt() {
        return Boolean.parseBoolean(getProperty(KEY_AUTO_START_MQTT, DEFAULT_AUTO_START_MQTT));
    }
    
    public static void setAutoStartMqtt(boolean autoStartMqtt) {
        setProperty(KEY_AUTO_START_MQTT, String.valueOf(autoStartMqtt));
    }
    
    public static int getWindowWidth() {
        try {
            return Integer.parseInt(getProperty(KEY_WINDOW_WIDTH, DEFAULT_WINDOW_WIDTH));
        } catch (NumberFormatException e) {
            return 1000;
        }
    }
    
    public static void setWindowWidth(int width) {
        setProperty(KEY_WINDOW_WIDTH, String.valueOf(width));
    }
    
    public static int getWindowHeight() {
        try {
            return Integer.parseInt(getProperty(KEY_WINDOW_HEIGHT, DEFAULT_WINDOW_HEIGHT));
        } catch (NumberFormatException e) {
            return 750;
        }
    }
    
    public static void setWindowHeight(int height) {
        setProperty(KEY_WINDOW_HEIGHT, String.valueOf(height));
    }
    
    // MQTT设置 getter/setter
    public static boolean isMqttReturnImageData() {
        return Boolean.parseBoolean(getProperty(KEY_MQTT_RETURN_IMAGE, DEFAULT_MQTT_RETURN_IMAGE));
    }
    
    public static void setMqttReturnImageData(boolean returnImage) {
        setProperty(KEY_MQTT_RETURN_IMAGE, String.valueOf(returnImage));
    }
    
    // OCR参数 getter/setter
    public static String getService() {
        return getProperty(KEY_SERVICE, DEFAULT_SERVICE);
    }
    
    public static void setService(String service) {
        setProperty(KEY_SERVICE, service);
    }
    
    public static int getGrayscale() {
        try {
            return Integer.parseInt(getProperty(KEY_GRAYSCALE, DEFAULT_GRAYSCALE));
        } catch (NumberFormatException e) {
            return 128;
        }
    }
    
    public static void setGrayscale(int grayscale) {
        setProperty(KEY_GRAYSCALE, String.valueOf(grayscale));
    }
    
    public static double getGamma() {
        try {
            return Double.parseDouble(getProperty(KEY_GAMMA, DEFAULT_GAMMA));
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }
    
    public static void setGamma(double gamma) {
        setProperty(KEY_GAMMA, String.valueOf(gamma));
    }
    
    public static int getMinArea() {
        try {
            return Integer.parseInt(getProperty(KEY_MIN_AREA, DEFAULT_MIN_AREA));
        } catch (NumberFormatException e) {
            return 15;
        }
    }
    
    public static void setMinArea(int minArea) {
        setProperty(KEY_MIN_AREA, String.valueOf(minArea));
    }
    
    public static int getMaxArea() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_AREA, DEFAULT_MAX_AREA));
        } catch (NumberFormatException e) {
            return 100000;
        }
    }
    
    public static void setMaxArea(int maxArea) {
        setProperty(KEY_MAX_AREA, String.valueOf(maxArea));
    }
    
    public static int getMinWidth() {
        try {
            return Integer.parseInt(getProperty(KEY_MIN_WIDTH, DEFAULT_MIN_WIDTH));
        } catch (NumberFormatException e) {
            return 3;
        }
    }
    
    public static void setMinWidth(int minWidth) {
        setProperty(KEY_MIN_WIDTH, String.valueOf(minWidth));
    }
    
    public static int getMinHeight() {
        try {
            return Integer.parseInt(getProperty(KEY_MIN_HEIGHT, DEFAULT_MIN_HEIGHT));
        } catch (NumberFormatException e) {
            return 3;
        }
    }
    
    public static void setMinHeight(int minHeight) {
        setProperty(KEY_MIN_HEIGHT, String.valueOf(minHeight));
    }
    
    public static int getMaxWidth() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_WIDTH, DEFAULT_MAX_WIDTH));
        } catch (NumberFormatException e) {
            return 500;
        }
    }
    
    public static void setMaxWidth(int maxWidth) {
        setProperty(KEY_MAX_WIDTH, String.valueOf(maxWidth));
    }
    
    public static int getMaxHeight() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_HEIGHT, DEFAULT_MAX_HEIGHT));
        } catch (NumberFormatException e) {
            return 200;
        }
    }
    
    public static void setMaxHeight(int maxHeight) {
        setProperty(KEY_MAX_HEIGHT, String.valueOf(maxHeight));
    }
    
    public static int getMinRowGap() {
        try {
            return Integer.parseInt(getProperty(KEY_MIN_ROW_GAP, DEFAULT_MIN_ROW_GAP));
        } catch (NumberFormatException e) {
            return 2;
        }
    }
    
    public static void setMinRowGap(int minRowGap) {
        setProperty(KEY_MIN_ROW_GAP, String.valueOf(minRowGap));
    }
    
    public static int getMaxRowGap() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_ROW_GAP, DEFAULT_MAX_ROW_GAP));
        } catch (NumberFormatException e) {
            return 50;
        }
    }
    
    public static void setMaxRowGap(int maxRowGap) {
        setProperty(KEY_MAX_ROW_GAP, String.valueOf(maxRowGap));
    }
    
    public static int getMinColGap() {
        try {
            return Integer.parseInt(getProperty(KEY_MIN_COL_GAP, DEFAULT_MIN_COL_GAP));
        } catch (NumberFormatException e) {
            return 2;
        }
    }
    
    public static void setMinColGap(int minColGap) {
        setProperty(KEY_MIN_COL_GAP, String.valueOf(minColGap));
    }
    
    public static int getMaxColGap() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_COL_GAP, DEFAULT_MAX_COL_GAP));
        } catch (NumberFormatException e) {
            return 50;
        }
    }
    
    public static void setMaxColGap(int maxColGap) {
        setProperty(KEY_MAX_COL_GAP, String.valueOf(maxColGap));
    }
    
    // Lua脚本配置 getter/setter
    public static String getTextLocationScript() {
        return getProperty(KEY_TEXT_LOCATION_SCRIPT, "");
    }
    
    public static void setTextLocationScript(String script) {
        setProperty(KEY_TEXT_LOCATION_SCRIPT, script);
    }
    
    public static String getCharClassifyScript() {
        return getProperty(KEY_CHAR_CLASSIFY_SCRIPT, "");
    }
    
    public static void setCharClassifyScript(String script) {
        setProperty(KEY_CHAR_CLASSIFY_SCRIPT, script);
    }
    
    public static boolean isTextLocationEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_TEXT_LOCATION_ENABLED, "false"));
    }
    
    public static void setTextLocationEnabled(boolean enabled) {
        setProperty(KEY_TEXT_LOCATION_ENABLED, String.valueOf(enabled));
    }
    
    public static boolean isCharClassifyEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_CHAR_CLASSIFY_ENABLED, "false"));
    }
    
    public static void setCharClassifyEnabled(boolean enabled) {
        setProperty(KEY_CHAR_CLASSIFY_ENABLED, String.valueOf(enabled));
    }
    
    // 日志脚本配置 getter/setter
    public static String getLogScript() {
        return getProperty(KEY_LOG_SCRIPT, "");
    }
    
    public static void setLogScript(String script) {
        setProperty(KEY_LOG_SCRIPT, script);
    }
    
    public static boolean isLogScriptEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_LOG_SCRIPT_ENABLED, "false"));
    }
    
    public static void setLogScriptEnabled(boolean enabled) {
        setProperty(KEY_LOG_SCRIPT_ENABLED, String.valueOf(enabled));
    }
    
    // 预处理脚本配置 getter/setter
    public static String getPreprocessScript() {
        return getProperty(KEY_PREPROCESS_SCRIPT, "");
    }
    
    public static void setPreprocessScript(String script) {
        setProperty(KEY_PREPROCESS_SCRIPT, script);
    }
    
    public static boolean isPreprocessScriptEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_PREPROCESS_SCRIPT_ENABLED, "false"));
    }
    
    public static void setPreprocessScriptEnabled(boolean enabled) {
        setProperty(KEY_PREPROCESS_SCRIPT_ENABLED, String.valueOf(enabled));
    }
    
    // 阈值偏移量 getter/setter
    public static double getThresholdOffset() {
        try {
            return Double.parseDouble(getProperty("ocr.thresholdOffset", DEFAULT_THRESHOLD_OFFSET));
        } catch (NumberFormatException e) {
            return -20;
        }
    }
    
    public static void setThresholdOffset(double offset) {
        setProperty("ocr.thresholdOffset", String.valueOf(offset));
    }
    
    // 新增OCR功能 getter/setter
    public static boolean isForceWidthSplitEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_FORCE_WIDTH_SPLIT, DEFAULT_FORCE_WIDTH_SPLIT));
    }
    
    public static void setForceWidthSplitEnabled(boolean enabled) {
        setProperty(KEY_FORCE_WIDTH_SPLIT, String.valueOf(enabled));
    }
    
    public static int getForceSplitWidth() {
        try {
            return Integer.parseInt(getProperty(KEY_FORCE_SPLIT_WIDTH, DEFAULT_FORCE_SPLIT_WIDTH));
        } catch (NumberFormatException e) {
            return 31;
        }
    }
    
    public static void setForceSplitWidth(int width) {
        setProperty(KEY_FORCE_SPLIT_WIDTH, String.valueOf(width));
    }
    
    public static boolean isMultiLineSortEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_MULTI_LINE_SORT, DEFAULT_MULTI_LINE_SORT));
    }
    
    public static void setMultiLineSortEnabled(boolean enabled) {
        setProperty(KEY_MULTI_LINE_SORT, String.valueOf(enabled));
    }
    
    public static int getRowGapThreshold() {
        try {
            return Integer.parseInt(getProperty(KEY_ROW_GAP_THRESHOLD, DEFAULT_ROW_GAP_THRESHOLD));
        } catch (NumberFormatException e) {
            return 20;
        }
    }
    
    public static void setRowGapThreshold(int threshold) {
        setProperty(KEY_ROW_GAP_THRESHOLD, String.valueOf(threshold));
    }
    
    // 字符内部空隙参数 getter/setter
    public static int getMaxInternalVerticalGap() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_INTERNAL_VERTICAL_GAP, DEFAULT_MAX_INTERNAL_VERTICAL_GAP));
        } catch (NumberFormatException e) {
            return 197;
        }
    }
    
    public static void setMaxInternalVerticalGap(int gap) {
        setProperty(KEY_MAX_INTERNAL_VERTICAL_GAP, String.valueOf(gap));
    }
    
    public static int getMaxInternalHorizontalGap() {
        try {
            return Integer.parseInt(getProperty(KEY_MAX_INTERNAL_HORIZONTAL_GAP, DEFAULT_MAX_INTERNAL_HORIZONTAL_GAP));
        } catch (NumberFormatException e) {
            return 497;
        }
    }
    
    public static void setMaxInternalHorizontalGap(int gap) {
        setProperty(KEY_MAX_INTERNAL_HORIZONTAL_GAP, String.valueOf(gap));
    }
    
    // 忽略边界区域参数 getter/setter
    public static int getIgnoreBorderWidth() {
        try {
            return Integer.parseInt(getProperty(KEY_IGNORE_BORDER_WIDTH, DEFAULT_IGNORE_BORDER_WIDTH));
        } catch (NumberFormatException e) {
            return 5;
        }
    }
    
    public static void setIgnoreBorderWidth(int width) {
        setProperty(KEY_IGNORE_BORDER_WIDTH, String.valueOf(width));
    }
    
    // OCR后处理脚本配置 getter/setter
    public static String getOcrPostProcessScript() {
        return getProperty(KEY_OCR_POST_PROCESS_SCRIPT, "");
    }
    
    public static void setOcrPostProcessScript(String script) {
        setProperty(KEY_OCR_POST_PROCESS_SCRIPT, script);
    }
    
    public static boolean isOcrPostProcessEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_OCR_POST_PROCESS_SCRIPT_ENABLED, "false"));
    }
    
    public static void setOcrPostProcessEnabled(boolean enabled) {
        setProperty(KEY_OCR_POST_PROCESS_SCRIPT_ENABLED, String.valueOf(enabled));
    }
    
    // 字符串预设存储方式 getter/setter
    public static String getStringPresetStorageMode() {
        return getProperty(KEY_STRING_PRESET_STORAGE_MODE, DEFAULT_STRING_PRESET_STORAGE_MODE);
    }
    
    public static void setStringPresetStorageMode(String mode) {
        setProperty(KEY_STRING_PRESET_STORAGE_MODE, mode);
    }
    
    // 字符相对位置 getter/setter
    public static String getCharRelativePositions() {
        return getProperty(KEY_CHAR_RELATIVE_POSITIONS, "");
    }
    
    public static void setCharRelativePositions(String json) {
        setProperty(KEY_CHAR_RELATIVE_POSITIONS, json != null ? json : "");
    }
    
    public static boolean isCharRelativePositionEnabled() {
        return Boolean.parseBoolean(getProperty(KEY_CHAR_RELATIVE_POSITION_ENABLED, "false"));
    }
    
    public static void setCharRelativePositionEnabled(boolean enabled) {
        setProperty(KEY_CHAR_RELATIVE_POSITION_ENABLED, String.valueOf(enabled));
    }
}
