package com.honda.paroledipinte.mqtt;

import com.google.gson.Gson;
import com.honda.paroledipinte.util.LogUtil;

/**
 * MQTT Payload包装器
 * 用于处理应用端发送的嵌套JSON格式：{"_mqtt_id":"xxx","payload":"实际业务JSON"}
 */
public class MqttPayloadWrapper {
    private static final Gson gson = new Gson();
    
    private String _mqtt_id;
    private String payload;
    
    public MqttPayloadWrapper() {
    }
    
    public String getMqttId() {
        return _mqtt_id;
    }
    
    public void setMqttId(String mqttId) {
        this._mqtt_id = mqttId;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    /**
     * 检查是否是包装格式（包含_mqtt_id字段）
     */
    public boolean isWrapped() {
        return _mqtt_id != null && !_mqtt_id.isEmpty();
    }
    
    /**
     * 从原始payload解析，尝试解包
     * @param rawPayload 原始payload字符串
     * @return 解析结果，如果不是包装格式则返回null
     */
    public static MqttPayloadWrapper tryParse(String rawPayload) {
        try {
            // 首先检查是否可能是包装格式（简单字符串检查）
            if (rawPayload == null || !rawPayload.contains("_mqtt_id")) {
                return null;
            }
            
            MqttPayloadWrapper wrapper = gson.fromJson(rawPayload, MqttPayloadWrapper.class);
            if (wrapper != null && wrapper.isWrapped()) {
                LogUtil.log("[MqttPayloadWrapper] 检测到嵌套JSON格式，_mqtt_id=" + wrapper.getMqttId());
                return wrapper;
            }
        } catch (Exception e) {
            // 解析失败，不是包装格式
        }
        return null;
    }
    
    /**
     * 将业务响应数据包装成嵌套格式
     * @param mqttId MQTT消息ID
     * @param businessPayload 业务响应JSON字符串
     * @return 包装后的JSON字符串
     */
    public static String wrapResponse(String mqttId, String businessPayload) {
        MqttPayloadWrapper wrapper = new MqttPayloadWrapper();
        wrapper.setMqttId(mqttId);
        wrapper.setPayload(businessPayload);
        return gson.toJson(wrapper);
    }
}
