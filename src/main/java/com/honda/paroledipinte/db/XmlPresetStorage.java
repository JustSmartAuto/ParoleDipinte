package com.honda.paroledipinte.db;

import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.model.CharRelativePosition;
import com.honda.paroledipinte.model.StringPreset;
import com.honda.paroledipinte.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.xml.stream.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * XML预设存储服务
 * 使用XML文件实现伪数据库存储字符串预设
 */
public class XmlPresetStorage {
    
    private static final String XML_FILE_NAME = "string_presets.xml";
    private File xmlFile;
    private AtomicInteger idGenerator;
    
    public XmlPresetStorage() {
        LogUtil.log("[XmlPresetStorage] 开始执行");
        String dataDir = ConfigManager.getDataDir();
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.xmlFile = new File(dir, XML_FILE_NAME);
        this.idGenerator = new AtomicInteger(1);
        init();
        LogUtil.log("[XmlPresetStorage] 执行完成");
    }
    
    /**
     * 初始化XML文件
     */
    private void init() {
        if (!xmlFile.exists()) {
            try {
                createEmptyXml();
                LogUtil.log("[init] 创建新的XML文件: " + xmlFile.getAbsolutePath());
            } catch (Exception e) {
                LogUtil.log("[init] 创建XML文件失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // 加载现有数据，确定最大ID
            try {
                List<StringPreset> presets = loadAllPresets();
                int maxId = 0;
                for (StringPreset preset : presets) {
                    if (preset.getId() > maxId) {
                        maxId = preset.getId();
                    }
                }
                idGenerator.set(maxId + 1);
                LogUtil.log("[init] 加载现有XML文件，最大ID: " + maxId);
            } catch (Exception e) {
                LogUtil.log("[init] 加载XML文件失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 创建空XML文件
     */
    private void createEmptyXml() throws XMLStreamException, IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(new FileWriter(xmlFile));
        
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("StringPresets");
        writer.writeAttribute("version", "1.0");
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
    }
    
    /**
     * 添加预设
     */
    public int addPreset(StringPreset preset) throws Exception {
        LogUtil.log("[addPreset] 开始执行");
        List<StringPreset> presets = loadAllPresets();
        
        // 生成新ID
        int newId = idGenerator.getAndIncrement();
        preset.setId(newId);
        
        presets.add(preset);
        saveAllPresets(presets);
        
        LogUtil.log("[addPreset] 执行完成，ID=" + newId);
        return newId;
    }
    
    /**
     * 更新预设
     */
    public void updatePreset(StringPreset preset) throws Exception {
        LogUtil.log("[updatePreset] 开始执行");
        List<StringPreset> presets = loadAllPresets();
        
        boolean found = false;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).getId() == preset.getId()) {
                presets.set(i, preset);
                found = true;
                break;
            }
        }
        
        if (found) {
            saveAllPresets(presets);
            LogUtil.log("[updatePreset] 执行完成");
        } else {
            throw new Exception("预设不存在，ID=" + preset.getId());
        }
    }
    
    /**
     * 删除预设
     */
    public void deletePreset(int id) throws Exception {
        LogUtil.log("[deletePreset] 开始执行");
        List<StringPreset> presets = loadAllPresets();
        
        boolean removed = presets.removeIf(p -> p.getId() == id);
        
        if (removed) {
            saveAllPresets(presets);
            LogUtil.log("[deletePreset] 执行完成");
        } else {
            throw new Exception("预设不存在，ID=" + id);
        }
    }
    
    /**
     * 根据ID获取预设
     */
    public StringPreset getPreset(int id) throws Exception {
        LogUtil.log("[getPreset] 开始执行");
        List<StringPreset> presets = loadAllPresets();
        
        for (StringPreset preset : presets) {
            if (preset.getId() == id) {
                LogUtil.log("[getPreset] 执行完成");
                return preset;
            }
        }
        
        LogUtil.log("[getPreset] 预设不存在，ID=" + id);
        return null;
    }
    
    /**
     * 获取所有预设
     */
    public List<StringPreset> getAllPresets() throws Exception {
        LogUtil.log("[getAllPresets] 开始执行");
        List<StringPreset> presets = loadAllPresets();
        LogUtil.log("[getAllPresets] 执行完成，共" + presets.size() + "条");
        return presets;
    }
    
    /**
     * 从XML加载所有预设
     */
    private List<StringPreset> loadAllPresets() throws Exception {
        List<StringPreset> presets = new ArrayList<>();
        
        if (!xmlFile.exists() || xmlFile.length() == 0) {
            return presets;
        }
        
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(xmlFile));
        
        StringPreset currentPreset = null;
        String currentElement = null;
        
        while (reader.hasNext()) {
            int event = reader.next();
            
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if ("Preset".equals(elementName)) {
                        currentPreset = new StringPreset();
                    } else if (currentPreset != null) {
                        currentElement = elementName;
                    }
                    break;
                    
                case XMLStreamConstants.CHARACTERS:
                    if (currentPreset != null && currentElement != null) {
                        String text = reader.getText().trim();
                        if (!text.isEmpty()) {
                            parsePresetField(currentPreset, currentElement, text);
                        }
                    }
                    break;
                    
                case XMLStreamConstants.END_ELEMENT:
                    if ("Preset".equals(reader.getLocalName()) && currentPreset != null) {
                        presets.add(currentPreset);
                        currentPreset = null;
                    }
                    currentElement = null;
                    break;
            }
        }
        
        reader.close();
        return presets;
    }
    
    /**
     * 解析预设字段
     */
    private void parsePresetField(StringPreset preset, String field, String value) {
        try {
            switch (field) {
                case "Id": preset.setId(Integer.parseInt(value)); break;
                case "PresetName": preset.setPresetName(value); break;
                case "CorrectString": preset.setCorrectString(value); break;
                case "Grayscale": preset.setGrayscale(Integer.parseInt(value)); break;
                case "Gamma": preset.setGamma(Double.parseDouble(value)); break;
                case "MinArea": preset.setMinArea(Integer.parseInt(value)); break;
                case "MaxArea": preset.setMaxArea(Integer.parseInt(value)); break;
                case "MinWidth": preset.setMinWidth(Integer.parseInt(value)); break;
                case "MaxWidth": preset.setMaxWidth(Integer.parseInt(value)); break;
                case "MinHeight": preset.setMinHeight(Integer.parseInt(value)); break;
                case "MaxHeight": preset.setMaxHeight(Integer.parseInt(value)); break;
                case "TextLocationScript": preset.setTextLocationScript(value); break;
                case "CharClassifyScript": preset.setCharClassifyScript(value); break;
                case "OcrPostProcessScript": preset.setOcrPostProcessScript(value); break;
                case "TextLocationScriptEnabled": preset.setTextLocationScriptEnabled(Boolean.parseBoolean(value)); break;
                case "CharClassifyScriptEnabled": preset.setCharClassifyScriptEnabled(Boolean.parseBoolean(value)); break;
                case "OcrPostProcessScriptEnabled": preset.setOcrPostProcessScriptEnabled(Boolean.parseBoolean(value)); break;
                case "CharRelativePositionEnabled": preset.setCharRelativePositionEnabled(Boolean.parseBoolean(value)); break;
                case "CharRelativePositionsJson": 
                    if (!value.isEmpty()) {
                        preset.setCharRelativePositionsFromJson(value);
                    }
                    break;
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }
    
    /**
     * 保存所有预设到XML
     */
    private void saveAllPresets(List<StringPreset> presets) throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(new FileWriter(xmlFile));
        
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("StringPresets");
        writer.writeAttribute("version", "1.0");
        
        for (StringPreset preset : presets) {
            writer.writeStartElement("Preset");
            
            writeXmlElement(writer, "Id", String.valueOf(preset.getId()));
            writeXmlElement(writer, "PresetName", preset.getPresetName());
            writeXmlElement(writer, "CorrectString", preset.getCorrectString());
            writeXmlElement(writer, "Grayscale", String.valueOf(preset.getGrayscale()));
            writeXmlElement(writer, "Gamma", String.valueOf(preset.getGamma()));
            writeXmlElement(writer, "MinArea", String.valueOf(preset.getMinArea()));
            writeXmlElement(writer, "MaxArea", String.valueOf(preset.getMaxArea()));
            writeXmlElement(writer, "MinWidth", String.valueOf(preset.getMinWidth()));
            writeXmlElement(writer, "MaxWidth", String.valueOf(preset.getMaxWidth()));
            writeXmlElement(writer, "MinHeight", String.valueOf(preset.getMinHeight()));
            writeXmlElement(writer, "MaxHeight", String.valueOf(preset.getMaxHeight()));
            writeXmlElement(writer, "TextLocationScript", preset.getTextLocationScript());
            writeXmlElement(writer, "CharClassifyScript", preset.getCharClassifyScript());
            writeXmlElement(writer, "OcrPostProcessScript", preset.getOcrPostProcessScript());
            writeXmlElement(writer, "TextLocationScriptEnabled", String.valueOf(preset.isTextLocationScriptEnabled()));
            writeXmlElement(writer, "CharClassifyScriptEnabled", String.valueOf(preset.isCharClassifyScriptEnabled()));
            writeXmlElement(writer, "OcrPostProcessScriptEnabled", String.valueOf(preset.isOcrPostProcessScriptEnabled()));
            writeXmlElement(writer, "CharRelativePositionEnabled", String.valueOf(preset.isCharRelativePositionEnabled()));
            writeXmlElement(writer, "CharRelativePositionsJson", preset.getCharRelativePositionsJson());
            
            writer.writeEndElement();
        }
        
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();
    }
    
    private void writeXmlElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(value != null ? value : "");
        writer.writeEndElement();
    }
}
