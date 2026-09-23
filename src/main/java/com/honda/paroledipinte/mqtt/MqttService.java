package com.honda.paroledipinte.mqtt;

import com.google.gson.Gson;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.db.DatabaseService;
import com.honda.paroledipinte.db.XmlPresetStorage;
import com.honda.paroledipinte.image.*;
import com.honda.paroledipinte.lua.LuaScriptEngine;
import com.honda.paroledipinte.model.CharRelativePosition;
import com.honda.paroledipinte.model.OcrRequest;
import com.honda.paroledipinte.model.OcrResponse;
import com.honda.paroledipinte.model.Shape;
import com.honda.paroledipinte.model.StringPreset;
import com.honda.paroledipinte.util.GlobalKvCore;
import com.honda.paroledipinte.util.LogUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * MQTT服务类
 * 处理OCR和字符分类的MQTT消息
 */
public class MqttService {
    private static final String CLIENT_ID_PREFIX = "paroledipinte-";
    private static final String VERSION = "3.3.4";
    
    private MqttClient client;
    private final DatabaseService dbService;
    private final Consumer<String> logger;
    private final Gson gson = new Gson();
    private volatile boolean running = false;
    
    // 存储每个请求对应的_mqtt_id，用于响应时回传
    private final ConcurrentHashMap<String, String> requestIdMap = new ConcurrentHashMap<>();
    
    private String broker;
    private String ocrTopic;
    private String classifyTopic;
    private String binarizeTopic;
    
    // Lua脚本引擎（用于文本定位后处理）
    private LuaScriptEngine textLocationScriptEngine;
    
    // Lua脚本引擎（用于字符分类）
    private LuaScriptEngine charClassifyScriptEngine;
    
    // Lua脚本引擎（用于OCR后处理）
    private LuaScriptEngine ocrPostProcessScriptEngine;
    
    public MqttService(DatabaseService dbService, Consumer<String> logger) {
        LogUtil.log("[MqttService] 开始执行");
        this.dbService = dbService;
        this.logger = logger;
        this.broker = ConfigManager.getBroker();
        this.ocrTopic = ConfigManager.getOcrTopic();
        this.classifyTopic = ConfigManager.getClassifyTopic();
        this.binarizeTopic = ConfigManager.getBinarizeTopic();
        LogUtil.log("[MqttService] 执行完成");
    }
    
    public void setBroker(String broker) {
        LogUtil.log("[setBroker] 开始执行");
        this.broker = broker;
        LogUtil.log("[setBroker] 执行完成");
    }
    
    public void setOcrTopic(String topic) {
        LogUtil.log("[setOcrTopic] 开始执行");
        this.ocrTopic = topic;
        LogUtil.log("[setOcrTopic] 执行完成");
    }
    
    public void setClassifyTopic(String topic) {
        LogUtil.log("[setClassifyTopic] 开始执行");
        this.classifyTopic = topic;
        LogUtil.log("[setClassifyTopic] 执行完成");
    }
    
    public void setBinarizeTopic(String topic) {
        LogUtil.log("[setBinarizeTopic] 开始执行");
        this.binarizeTopic = topic;
        LogUtil.log("[setBinarizeTopic] 执行完成");
    }
    
    /**
     * 设置文本定位Lua脚本引擎
     */
    public void setTextLocationScriptEngine(LuaScriptEngine engine) {
        LogUtil.log("[setTextLocationScriptEngine] 开始执行");
        this.textLocationScriptEngine = engine;
        LogUtil.log("[setTextLocationScriptEngine] 执行完成");
    }
    
    /**
     * 设置字符分类Lua脚本引擎
     */
    public void setCharClassifyScriptEngine(LuaScriptEngine engine) {
        LogUtil.log("[setCharClassifyScriptEngine] 开始执行");
        this.charClassifyScriptEngine = engine;
        LogUtil.log("[setCharClassifyScriptEngine] 执行完成");
    }
    
    /**
     * 设置OCR后处理Lua脚本引擎
     */
    public void setOcrPostProcessScriptEngine(LuaScriptEngine engine) {
        LogUtil.log("[setOcrPostProcessScriptEngine] 开始执行");
        this.ocrPostProcessScriptEngine = engine;
        LogUtil.log("[setOcrPostProcessScriptEngine] 执行完成");
    }
    
    public String getBroker() {
        return broker;
    }
    
    public String getOcrTopic() {
        return ocrTopic;
    }
    
    public String getClassifyTopic() {
        return classifyTopic;
    }
    
    public String getBinarizeTopic() {
        return binarizeTopic;
    }
    
    /**
     * 启动MQTT服务
     */
    public void start() {
        LogUtil.log("[start] 开始执行");
        try {
            if (running && client != null && client.isConnected()) {
                logger.accept("MQTT服务已在运行");
                LogUtil.log("[start] 执行完成");
                return;
            }
            
            String clientId = CLIENT_ID_PREFIX + System.nanoTime();
            client = new MqttClient(broker, clientId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.accept("MQTT连接断开: " + cause.getMessage());
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload(), "UTF-8");
                    // 打印接收到的消息
                    logger.accept("[MQTT接收] 主题: " + topic);
                    logger.accept("[MQTT接收] 内容: " + truncateJson(payload, 500));
                    processMessage(topic, payload);
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            
            client.connect(options);
            
            // 订阅主题
            client.subscribe(ocrTopic);
            client.subscribe(classifyTopic);
            client.subscribe(binarizeTopic);
            
            running = true;
            logger.accept("MQTT服务已启动");
            logger.accept("  Broker: " + broker);
            logger.accept("  OCR主题: " + ocrTopic);
            logger.accept("  分类主题: " + classifyTopic);
            logger.accept("  二值化主题: " + binarizeTopic);
            
        } catch (Exception e) {
            LogUtil.log("[start] 发生错误: " + e.getMessage());
            logger.accept("MQTT启动失败: " + e.getMessage());
            e.printStackTrace();
        }
        LogUtil.log("[start] 执行完成");
    }
    
    /**
     * 停止MQTT服务
     */
    public void stop() {
        LogUtil.log("[stop] 开始执行");
        try {
            running = false;
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                logger.accept("MQTT服务已停止");
            }
        } catch (Exception e) {
            LogUtil.log("[stop] 发生错误: " + e.getMessage());
            logger.accept("MQTT停止失败: " + e.getMessage());
        }
        LogUtil.log("[stop] 执行完成");
    }
    
    /**
     * 检查服务是否运行中
     */
    public boolean isRunning() {
        return running && client != null && client.isConnected();
    }
    
    /**
     * 处理接收到的消息
     */
    private void processMessage(String topic, String payload) {
        LogUtil.log("[processMessage] 开始执行");
        try {
            // 检查是否是响应消息（包含shapeType字段），避免递归处理自己发送的响应
            if (isResponseMessage(payload)) {
                logger.accept("[MQTT接收] 忽略自己发送的响应消息");
                LogUtil.log("[processMessage] 忽略响应消息");
                return;
            }
            
            // 尝试解析嵌套JSON格式
            String businessPayload = payload;
            String mqttId = null;
            MqttPayloadWrapper wrapper = MqttPayloadWrapper.tryParse(payload);
            if (wrapper != null) {
                mqttId = wrapper.getMqttId();
                businessPayload = wrapper.getPayload();
                logger.accept("[MQTT接收] 检测到嵌套JSON格式，_mqtt_id=" + mqttId);
            }
            
            if (topic.equals(ocrTopic)) {
                processOcrMessage(businessPayload, mqttId);
            } else if (topic.equals(classifyTopic)) {
                processClassifyMessage(businessPayload, mqttId);
            } else if (topic.equals(binarizeTopic)) {
                processBinarizeMessage(businessPayload, mqttId);
            }
        } catch (Exception e) {
            LogUtil.log("[processMessage] 发生错误: " + e.getMessage());
            logger.accept("处理消息失败: " + e.getMessage());
            e.printStackTrace();
        }
        LogUtil.log("[processMessage] 执行完成");
    }
    
    /**
     * 检查消息是否是响应消息（包含shapes或shapeType字段）
     */
    private boolean isResponseMessage(String payload) {
        try {
            // 简单检查是否包含响应特有的字段
            return payload.contains("\"shapes\"") || payload.contains("\"shapeType\"");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 处理OCR消息
     */
    private void processOcrMessage(String payload) {
        processOcrMessage(payload, null);
    }
    
    /**
     * 处理OCR消息（带_mqtt_id）
     * @param payload 业务payload
     * @param mqttId MQTT消息ID（可能为null）
     */
    private void processOcrMessage(String payload, String mqttId) {
        LogUtil.log("[processOcrMessage] 开始执行");
        long startTime = System.currentTimeMillis();
        logger.accept("收到OCR请求");
        
        try {
            OcrRequest request = gson.fromJson(payload, OcrRequest.class);
            
            // 记录imageHash
            String imageHash = request.getImageHash();
            if (imageHash == null || imageHash.isEmpty()) {
                imageHash = UUID.randomUUID().toString();
            }
            
            // 保存mqttId用于后续响应
            if (mqttId != null) {
                requestIdMap.put(imageHash, mqttId);
            }
            
            // 解析图像
            String base64 = request.getImageData();
            if (base64 == null || base64.isEmpty()) {
                logger.accept("无效的图像数据");
                LogUtil.log("[processOcrMessage] 执行完成");
                return;
            }
            
            // 移除data URI前缀
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            if (inputImage == null) {
                logger.accept("无法解析图像");
                LogUtil.log("[processOcrMessage] 执行完成");
                return;
            }
            
            logger.accept("处理图像: " + imageHash + " (" + inputImage.getWidth() + "x" + inputImage.getHeight() + ")");
            
            // 检查是否指定了预设名称
            String presetName = request.getPresetName();
            StringPreset preset = null;
            if (presetName != null && !presetName.isEmpty()) {
                preset = findPresetByName(presetName);
                if (preset != null) {
                    logger.accept("使用预设: " + presetName + " (字符串: " + preset.getCorrectString() + ")");
                } else {
                    logger.accept("未找到预设: " + presetName + "，使用请求参数或默认值");
                }
            }
            
            // 获取OCR参数（优先级：请求中的参数 > 预设中的参数 > 配置默认值）
            int ocrGray = request.getGrayscale() != null ? request.getGrayscale() : 
                (preset != null ? preset.getGrayscale() : ConfigManager.getGrayscale());
            double gamma = request.getGamma() != null ? request.getGamma() : 
                (preset != null ? preset.getGamma() : ConfigManager.getGamma());
            int minArea = request.getMinArea() != null ? request.getMinArea() : 
                (preset != null ? preset.getMinArea() : ConfigManager.getMinArea());
            int maxArea = request.getMaxArea() != null ? request.getMaxArea() : 
                (preset != null ? preset.getMaxArea() : ConfigManager.getMaxArea());
            int minWidth = request.getMinWidth() != null ? request.getMinWidth() : 
                (preset != null ? preset.getMinWidth() : ConfigManager.getMinWidth());
            int minHeight = request.getMinHeight() != null ? request.getMinHeight() : 
                (preset != null ? preset.getMinHeight() : ConfigManager.getMinHeight());
            int maxWidth = request.getMaxWidth() != null ? request.getMaxWidth() : 
                (preset != null ? preset.getMaxWidth() : ConfigManager.getMaxWidth());
            int maxHeight = request.getMaxHeight() != null ? request.getMaxHeight() : 
                (preset != null ? preset.getMaxHeight() : ConfigManager.getMaxHeight());
            
            // 获取字符内部空隙参数（优先级：请求 > 配置默认值）
            int maxInternalVerticalGap = request.getMaxInternalVerticalGap() != null ? 
                request.getMaxInternalVerticalGap() : ConfigManager.getMaxInternalVerticalGap();
            int maxInternalHorizontalGap = request.getMaxInternalHorizontalGap() != null ? 
                request.getMaxInternalHorizontalGap() : ConfigManager.getMaxInternalHorizontalGap();
            
            // 获取忽略边界区域参数（优先级：请求 > 配置默认值）
            int ignoreBorderWidth = request.getIgnoreBorderWidth() != null ? 
                request.getIgnoreBorderWidth() : ConfigManager.getIgnoreBorderWidth();
            
            // 获取correctString（优先级：请求中的值（非null）> 预设中的值 > null）
            String correctString = request.getCorrectString();
            if (correctString == null && preset != null) {
                correctString = preset.getCorrectString();
                logger.accept("使用预设中的正确字符串: " + correctString);
            }
            
            // 设置图像处理参数
            ImageProcessor.setGammaValue(gamma);
            
            // 转换为Mat进行处理
            Mat inputMat = ImageProcessor.bufferedImageToMat(inputImage);
            
            // 文本区域检测
            TextBlobDetector detector = new TextBlobDetector(
                ocrGray, minArea, minWidth, minHeight, maxWidth, maxHeight, false,
                false, false, 31, 20, maxInternalVerticalGap, maxInternalHorizontalGap, ignoreBorderWidth
            );
            
            // 设置Lua脚本引擎和correctString（如果可用）
            if (textLocationScriptEngine != null) {
                detector.setLuaScriptEngine(textLocationScriptEngine);
                detector.setCorrectString(correctString != null ? correctString : "");
                logger.accept("OCR处理 - 已启用文本定位Lua脚本引擎");
            }
            
            // 设置预设参数到检测器（供Lua脚本访问）
            if (preset != null) {
                Map<String, Object> presetParams = new HashMap<>();
                presetParams.put("presetName", preset.getPresetName());
                presetParams.put("correctString", preset.getCorrectString());
                presetParams.put("grayscale", preset.getGrayscale());
                presetParams.put("gamma", preset.getGamma());
                presetParams.put("minArea", preset.getMinArea());
                presetParams.put("maxArea", preset.getMaxArea());
                presetParams.put("minWidth", preset.getMinWidth());
                presetParams.put("maxWidth", preset.getMaxWidth());
                presetParams.put("minHeight", preset.getMinHeight());
                presetParams.put("maxHeight", preset.getMaxHeight());
                presetParams.put("charRelativePositionEnabled", preset.isCharRelativePositionEnabled());
                
                // 字符相对位置信息转换为List<Map>
                if (preset.hasCharRelativePositions()) {
                    List<Map<String, Object>> charPosList = new ArrayList<>();
                    for (CharRelativePosition pos : preset.getCharRelativePositions()) {
                        Map<String, Object> posMap = new HashMap<>();
                        posMap.put("charIndex", pos.getCharIndex());
                        posMap.put("character", pos.getCharacter());
                        posMap.put("relativeX", pos.getRelativeX());
                        posMap.put("relativeY", pos.getRelativeY());
                        posMap.put("width", pos.getWidth());
                        posMap.put("height", pos.getHeight());
                        posMap.put("minWidth", pos.getMinWidth());
                        posMap.put("maxWidth", pos.getMaxWidth());
                        posMap.put("minHeight", pos.getMinHeight());
                        posMap.put("maxHeight", pos.getMaxHeight());
                        posMap.put("spacing", pos.getSpacing());
                        posMap.put("minSpacing", pos.getMinSpacing());
                        posMap.put("maxSpacing", pos.getMaxSpacing());
                        posMap.put("charType", pos.getCharTypeDisplayName());
                        charPosList.add(posMap);
                    }
                    presetParams.put("charRelativePositions", charPosList);
                }
                
                detector.setPresetParams(presetParams);
            }
            
            TextBlobDetector.DetectionResult result = detector.detect(inputMat);
            
            // 保存原始字符框
            List<TextBlobDetector.Region> rawRegions = new ArrayList<>(result.textRegions);
            detector.setRawRegions(rawRegions);
            
            logger.accept("OCR请求 - correctString: " + (correctString != null ? "'" + correctString + "'" : "null"));
            logger.accept("OCR请求 - 检测到区域数: " + result.textRegions.size());
            
            // 步骤2: 如果使用了预设且启用了字符相对位置，使用字符相对位置修正区域位置
            List<TextBlobDetector.Region> finalRegions = result.textRegions;
            if (preset != null && preset.isCharRelativePositionEnabled() && 
                preset.hasCharRelativePositions() && !result.textRegions.isEmpty()) {
                
                logger.accept("使用字符相对位置修正区域...");
                List<CharRelativePosition> charPositions = preset.getCharRelativePositions();
                
                // 使用首个检测到的区域作为基准点
                TextBlobDetector.Region firstRegion = result.textRegions.get(0);
                finalRegions = detector.detectByRelativePosition(inputMat, charPositions, firstRegion);
                
                // 保存修正后字符框
                detector.setCorrectedRegions(new ArrayList<>(finalRegions));
                
                logger.accept("字符相对位置修正完成: " + finalRegions.size() + " 个区域");
            } else {
                detector.setCorrectedRegions(new ArrayList<>(finalRegions));
            }
            
            // 步骤3: 如果使用了预设且启用了文本定位脚本，执行脚本进行区域后处理
            // 注意：脚本已在detect()方法中作为后处理执行
            if (preset != null && preset.isTextLocationScriptEnabled() && 
                preset.getTextLocationScript() != null && !preset.getTextLocationScript().isEmpty()) {
                logger.accept("文本定位脚本已在detect()中执行");
            }
            
            // 处理每个检测到的区域
            List<Shape> shapes = new ArrayList<>();
            String correctStringCleaned = correctString != null ? correctString.replaceAll("\\s+", "") : "";
            char[] correctChars = correctStringCleaned.isEmpty() ? new char[0] : correctStringCleaned.toCharArray();
            
            // 确保图片保存目录存在
            String imageDir = ConfigManager.getImageDir();
            File imgDir = new File(imageDir);
            if (!imgDir.exists()) {
                imgDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
            String timestamp = sdf.format(new Date());
            
            for (int i = 0; i < finalRegions.size(); i++) {
                TextBlobDetector.Region region = finalRegions.get(i);
                
                // 提取字符区域
                Mat charMat = ImageProcessor.extractRegion(inputMat, region.x, region.y, region.width, region.height);
                
                // 确定字符标签
                String label;
                double score;
                
                if (i < correctChars.length) {
                    // 使用correctString中的对应字符
                    label = String.valueOf(correctChars[i]);
                    logger.accept("  区域 " + i + ": 使用字符 '" + label + "' 来自correctString");
                    
                    // 计算相似度分数
                    BufferedImage refImage = dbService.loadCharacterImage(label);
                    if (refImage != null) {
                        Mat refMat = ImageProcessor.bufferedImageToMat(refImage);
                        score = ImageProcessor.compareSimilarity(refMat, charMat);
                        refMat.release();
                    } else {
                        // 如果数据库中没有该字符，生成并保存
                        CharacterGenerator generator = new CharacterGenerator();
                        BufferedImage newCharImage = generator.generateCharacterImage(label);
                        dbService.saveCharacterImage(label, newCharImage);
                        score = 0.5; // 默认分数
                    }
                } else {
                    // correctString为空或长度不够
                    label = "?";
                    score = 0.1;
                    logger.accept("  区域 " + i + ": 使用字符 '?' (correctString长度不足: " + correctChars.length + " < " + (i+1) + ")");
                }
                
                // 保存字符截图
                String charFolderName = CharacterGenerator.getCharFolderName(label);
                File charDir = new File(imageDir, charFolderName);
                if (!charDir.exists()) {
                    charDir.mkdirs();
                }
                
                String charFileName = timestamp + "_" + i + ".png";
                File charFile = new File(charDir, charFileName);
                
                // 保存字符图像
                BufferedImage charImage = ImageProcessor.matToBufferedImage(charMat);
                ImageIO.write(charImage, "PNG", charFile);
                
                // 创建Shape对象
                Shape shape = new Shape();
                shape.setLabel(label);
                shape.setScore(score);
                
                // 设置四个角点坐标
                List<List<Double>> points = new ArrayList<>();
                points.add(Arrays.asList((double) region.x, (double) region.y));
                points.add(Arrays.asList((double) (region.x + region.width), (double) region.y));
                points.add(Arrays.asList((double) (region.x + region.width), (double) (region.y + region.height)));
                points.add(Arrays.asList((double) region.x, (double) (region.y + region.height)));
                shape.setPoints(points);
                
                shapes.add(shape);
                
                charMat.release();
            }
            
            // 步骤5: 如果使用了预设且启用了字符分类脚本，执行脚本
            if (preset != null && preset.isCharClassifyScriptEnabled() && 
                preset.getCharClassifyScript() != null && !preset.getCharClassifyScript().isEmpty()) {
                logger.accept("执行字符分类脚本...");
                // 脚本执行逻辑待实现
                // TODO: 执行Lua脚本进行字符分类
            }
            
            // 步骤6: 如果使用了预设且启用了OCR后处理脚本，执行脚本
            if (preset != null && preset.isOcrPostProcessScriptEnabled() && 
                preset.getOcrPostProcessScript() != null && !preset.getOcrPostProcessScript().isEmpty()) {
                logger.accept("执行OCR后处理脚本...");
                // 脚本执行逻辑待实现
                // TODO: 执行Lua脚本进行OCR后处理
            }
            
            // 创建响应
            OcrResponse response = new OcrResponse(imageHash);
            response.setShapes(shapes);
            response.setVersion(VERSION);
            
            // 保存可视化结果
            Mat visualized = result.visualizedImage;
            File visFile = new File(imageDir, "ocr_result_" + timestamp + ".png");
            opencv_imgcodecs.imwrite(visFile.getAbsolutePath(), visualized);
            
            // 根据配置决定是否返回imageData字段（默认关闭以节省带宽）
            if (ConfigManager.isMqttReturnImageData()) {
                // 直接返回原始发送的imageData
                response.setImageData(request.getImageData());
                logger.accept("OCR响应包含imageData字段（已启用，返回原始图像）");
            } else {
                logger.accept("OCR响应不包含imageData字段（已禁用，节省带宽）");
            }
            
            // 发送响应
            String responseJson = gson.toJson(response);
            
            // 如果原始请求有_mqtt_id，则包装响应
            String finalResponse = responseJson;
            if (mqttId != null) {
                finalResponse = MqttPayloadWrapper.wrapResponse(mqttId, responseJson);
                logger.accept("[MQTT发送] 使用嵌套格式响应，_mqtt_id=" + mqttId);
            }
            
            publishResponse(ocrTopic, finalResponse);
            
            // 保存到数据库
            dbService.saveOcrRecord(imageHash, payload, responseJson);
            
            // 清理requestIdMap
            if (imageHash != null) {
                requestIdMap.remove(imageHash);
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.accept("OCR处理完成: 检测到 " + shapes.size() + " 个字符，总处理时间: " + elapsedTime + "ms");
            
            // 释放资源
            inputMat.release();
            result.visualizedImage.release();
            
        } catch (Exception e) {
            LogUtil.log("[processOcrMessage] 发生错误: " + e.getMessage());
            logger.accept("OCR处理失败: " + e.getMessage());
            e.printStackTrace();
        }
        LogUtil.log("[processOcrMessage] 执行完成");
    }
    
    /**
     * 处理字符分类消息
     */
    private void processClassifyMessage(String payload) {
        processClassifyMessage(payload, null);
    }
    
    /**
     * 处理字符分类消息（带_mqtt_id）
     * @param payload 业务payload
     * @param mqttId MQTT消息ID（可能为null）
     */
    private void processClassifyMessage(String payload, String mqttId) {
        LogUtil.log("[processClassifyMessage] 开始执行");
        long startTime = System.currentTimeMillis();
        logger.accept("收到字符分类请求");
        
        try {
            // 解析请求
            OcrRequest request = gson.fromJson(payload, OcrRequest.class);
            
            // 保存mqttId用于后续响应
            String imageHash = request.getImageHash();
            if (imageHash != null && !imageHash.isEmpty() && mqttId != null) {
                requestIdMap.put(imageHash, mqttId);
            }
            
            String base64 = request.getImageData();
            if (base64 == null || base64.isEmpty()) {
                logger.accept("无效的图像数据");
                LogUtil.log("[processClassifyMessage] 执行完成");
                return;
            }
            
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            if (inputImage == null) {
                logger.accept("无法解析图像");
                LogUtil.log("[processClassifyMessage] 执行完成");
                return;
            }
            
            String correctString = request.getCorrectString();
            if (correctString == null || correctString.isEmpty()) {
                logger.accept("未提供正确字符串");
                LogUtil.log("[processClassifyMessage] 执行完成");
                return;
            }
            
            // 只处理第一个字符
            String targetChar = correctString.substring(0, 1);
            
            Mat inputMat = ImageProcessor.bufferedImageToMat(inputImage);
            
            // 加载或生成参考图像
            BufferedImage refImage = dbService.loadCharacterImage(targetChar);
            if (refImage == null) {
                CharacterGenerator generator = new CharacterGenerator();
                refImage = generator.generateCharacterImage(targetChar);
                dbService.saveCharacterImage(targetChar, refImage);
            }
            
            Mat refMat = ImageProcessor.bufferedImageToMat(refImage);
            
            // 计算相似度
            double score = ImageProcessor.compareSimilarity(refMat, inputMat);
            
            // 创建响应
            Map<String, Object> response = new HashMap<>();
            response.put("imageHash", request.getImageHash());
            response.put("char", targetChar);
            response.put("score", score);
            response.put("version", VERSION);
            
            String responseJson = gson.toJson(response);
            
            // 如果原始请求有_mqtt_id，则包装响应
            String finalResponse = responseJson;
            if (mqttId != null) {
                finalResponse = MqttPayloadWrapper.wrapResponse(mqttId, responseJson);
                logger.accept("[MQTT发送] 使用嵌套格式响应，_mqtt_id=" + mqttId);
            }
            
            publishResponse(classifyTopic, finalResponse);
            
            // 清理requestIdMap
            if (imageHash != null) {
                requestIdMap.remove(imageHash);
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.accept(String.format("字符分类完成: [%s] 相似度: %.4f，总处理时间: %dms", targetChar, score, elapsedTime));
            
            // 释放资源
            inputMat.release();
            refMat.release();
            
        } catch (Exception e) {
            LogUtil.log("[processClassifyMessage] 发生错误: " + e.getMessage());
            logger.accept("字符分类失败: " + e.getMessage());
            e.printStackTrace();
        }
        LogUtil.log("[processClassifyMessage] 执行完成");
    }
    
    /**
     * 处理图像二值化消息
     * 请求格式: { "imageHash":"", "imageData":"", "grayscale":120, "gamma":0.6 }
     * 响应格式: { "imageHash":"", "imageData":"base64encoded..."}
     */
    private void processBinarizeMessage(String payload) {
        processBinarizeMessage(payload, null);
    }
    
    /**
     * 处理图像二值化消息（带_mqtt_id）
     * @param payload 业务payload
     * @param mqttId MQTT消息ID（可能为null）
     */
    private void processBinarizeMessage(String payload, String mqttId) {
        LogUtil.log("[processBinarizeMessage] 开始执行");
        long startTime = System.currentTimeMillis();
        logger.accept("收到图像二值化请求");
        
        try {
            // 解析请求
            Map<String, Object> request = gson.fromJson(payload, Map.class);
            
            // 记录imageHash
            String imageHash = (String) request.get("imageHash");
            if (imageHash == null || imageHash.isEmpty()) {
                imageHash = UUID.randomUUID().toString();
            }
            
            // 保存mqttId用于后续响应
            if (mqttId != null) {
                requestIdMap.put(imageHash, mqttId);
            }
            
            String base64 = (String) request.get("imageData");
            if (base64 == null || base64.isEmpty()) {
                logger.accept("无效的图像数据");
                LogUtil.log("[processBinarizeMessage] 执行完成");
                return;
            }
            
            // 移除data URI前缀
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            if (inputImage == null) {
                logger.accept("无法解析图像");
                LogUtil.log("[processBinarizeMessage] 执行完成");
                return;
            }
            
            logger.accept("处理图像二值化: " + imageHash + " (" + inputImage.getWidth() + "x" + inputImage.getHeight() + ")");
            
            // 获取二值化参数（优先使用请求中的参数，否则使用配置默认值）
            int grayscale = 128;
            double gamma = 0.5;
            
            if (request.containsKey("grayscale")) {
                Object gs = request.get("grayscale");
                if (gs instanceof Number) {
                    grayscale = ((Number) gs).intValue();
                }
            }
            
            if (request.containsKey("gamma")) {
                Object g = request.get("gamma");
                if (g instanceof Number) {
                    gamma = ((Number) g).doubleValue();
                }
            }
            
            logger.accept("二值化参数 - 阈值:" + grayscale + ", Gamma:" + gamma);
            
            // 转换为Mat进行处理
            Mat inputMat = ImageProcessor.bufferedImageToMat(inputImage);
            
            // 执行二值化处理
            Mat binaryMat = binarizeImage(inputMat, grayscale, gamma);
            
            // 将二值化结果转为BufferedImage
            BufferedImage binaryImage = ImageProcessor.matToBufferedImage(binaryMat);
            
            // 转为base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(binaryImage, "PNG", baos);
            String binaryBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            
            // 创建响应
            Map<String, Object> response = new HashMap<>();
            response.put("imageHash", imageHash);
            response.put("imageData", "data:image/png;base64," + binaryBase64);
            
            String responseJson = gson.toJson(response);
            
            // 如果原始请求有_mqtt_id，则包装响应
            String finalResponse = responseJson;
            if (mqttId != null) {
                finalResponse = MqttPayloadWrapper.wrapResponse(mqttId, responseJson);
                logger.accept("[MQTT发送] 使用嵌套格式响应，_mqtt_id=" + mqttId);
            }
            
            publishResponse(binarizeTopic, finalResponse);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.accept("图像二值化完成: " + imageHash + "，总处理时间: " + elapsedTime + "ms");
            
            // 清理requestIdMap
            if (imageHash != null) {
                requestIdMap.remove(imageHash);
            }
            
            // 释放资源
            inputMat.release();
            binaryMat.release();
            
        } catch (Exception e) {
            LogUtil.log("[processBinarizeMessage] 发生错误: " + e.getMessage());
            logger.accept("图像二值化失败: " + e.getMessage());
            e.printStackTrace();
        }
        LogUtil.log("[processBinarizeMessage] 执行完成");
    }
    
    /**
     * 图像二值化处理
     */
    private Mat binarizeImage(Mat src, int grayscale, double gamma) {
        LogUtil.log("[binarizeImage] 开始执行");
        Mat gray = new Mat();
        if (src.channels() > 1) {
            opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY);
        } else {
            gray = src.clone();
        }
        
        // Gamma校正
        Mat gammaCorrected = gammaCorrection(gray, gamma);
        
        // 二值化
        Mat binary = new Mat();
        opencv_imgproc.threshold(gammaCorrected, binary, grayscale, 255, opencv_imgproc.THRESH_BINARY);
        
        gray.release();
        gammaCorrected.release();
        
        LogUtil.log("[binarizeImage] 执行完成");
        return binary;
    }
    
    /**
     * Gamma校正
     */
    private Mat gammaCorrection(Mat src, double gamma) {
        LogUtil.log("[gammaCorrection] 开始执行");
        Mat lut = new Mat(1, 256, opencv_core.CV_8UC1);
        byte[] lutData = new byte[256];
        for (int i = 0; i < 256; i++) {
            lutData[i] = (byte) Math.round(Math.pow(i / 255.0, gamma) * 255.0);
        }
        lut.data().put(lutData);
        
        Mat dst = new Mat();
        opencv_core.LUT(src, lut, dst);
        lut.release();
        LogUtil.log("[gammaCorrection] 执行完成");
        return dst;
    }
    
    /**
     * 根据预设名称查找预设
     * 优先从数据库查找，如果未找到则从XML存储查找
     * @param presetName 预设名称
     * @return 找到的预设，未找到返回null
     */
    private StringPreset findPresetByName(String presetName) {
        if (presetName == null || presetName.isEmpty()) {
            return null;
        }
        
        try {
            // 优先尝试从数据库查找
            if (dbService != null) {
                List<StringPreset> presets = dbService.getAllStringPresets();
                for (StringPreset preset : presets) {
                    if (presetName.equals(preset.getPresetName())) {
                        logger.accept("从数据库找到预设: " + presetName);
                        return preset;
                    }
                }
            }
            
            // 如果数据库中未找到，尝试从XML存储查找
            XmlPresetStorage xmlStorage = new XmlPresetStorage();
            List<StringPreset> xmlPresets = xmlStorage.getAllPresets();
            for (StringPreset preset : xmlPresets) {
                if (presetName.equals(preset.getPresetName())) {
                    logger.accept("从XML存储找到预设: " + presetName);
                    return preset;
                }
            }
            
            logger.accept("未找到预设: " + presetName);
            return null;
        } catch (Exception e) {
            logger.accept("查找预设失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 发布响应消息
     */
    private void publishResponse(String topic, String json) {
        LogUtil.log("[publishResponse] 开始执行");
        try {
            // 检查客户端是否连接
            if (client == null || !client.isConnected()) {
                logger.accept("[MQTT发送] 错误: MQTT客户端未连接，无法发送响应");
                LogUtil.log("[publishResponse] MQTT客户端未连接");
                return;
            }
            
            // 打印发送的消息
            logger.accept("[MQTT发送] 主题: " + topic);
            logger.accept("[MQTT发送] 内容: " + truncateJson(json, 500));
            
            MqttMessage msg = new MqttMessage(json.getBytes("UTF-8"));
            msg.setQos(1);
            client.publish(topic, msg);
            logger.accept("[MQTT发送] 成功发送响应");
        } catch (Exception e) {
            LogUtil.log("[publishResponse] 发生错误: " + e.getMessage());
            logger.accept("发送响应失败: " + e.getMessage());
        }
        LogUtil.log("[publishResponse] 执行完成");
    }
    
    /**
     * 截断JSON字符串，避免日志过长
     */
    private String truncateJson(String json, int maxLength) {
        if (json == null) return "null";
        if (json.length() <= maxLength) return json;
        return json.substring(0, maxLength) + "... (共 " + json.length() + " 字符)";
    }
}
