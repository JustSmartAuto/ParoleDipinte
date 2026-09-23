# 识字涂色 (ParoleDipinte) - OCR服务 | OCR Service

## 简介 | Introduction

识字涂色(ParoleDipinte)是一个基于Java的OCR(光学字符识别)服务应用程序，支持通过MQTT协议接收图像识别请求，并提供字符分类和相似度判断功能。

ParoleDipinte is a Java-based OCR (Optical Character Recognition) service application. It receives image recognition requests via the MQTT protocol and provides character classification and similarity scoring.

## 功能特点 | Features

- **经典OCR识别 | Classic OCR**: 检测图像中的文本区域，识别字符并返回带位置信息的结果 / Detect text regions in images, recognize characters, and return results with position information
- **字符分类 | Character Classification**: 对单个字符进行相似度判断 / Similarity scoring for individual characters
- **动态阈值二值化 | Dynamic Threshold Binarization**: 使用Gamma校正和动态阈值进行图像预处理 / Image preprocessing using Gamma correction and dynamic thresholding
- **图像二值化服务 | Image Binarization Service**: 通过MQTT提供图像二值化服务 / Binarization service via MQTT
- **MQTT通信 | MQTT Communication**: 支持通过MQTT协议接收请求和发送响应 / Request/response over MQTT protocol
- **MQTT嵌套JSON格式 | Nested JSON Format**: 支持应用端的嵌套JSON格式，自动解析和响应 / Supports nested JSON payloads with automatic parsing and response
- **Lua脚本扩展 | Lua Scripting**: 支持使用Lua脚本自定义文本区域定位和字符分类逻辑 / Customize text region location and character classification with Lua scripts
- **多标签页界面 | Multi-tab UI**: 现代化的Swing界面，支持多种设置和日志查看 / Modern Swing UI with multiple settings and log tabs
- **字符数据库 | Character Database**: 使用H2数据库存储5000个常用字符的标准图片 / H2 database storing standard images of 5000 common characters
- **页码跳转 | Page Navigation**: 日志界面支持输入页码快速跳转 / Log view supports quick page jump
- **智能尺寸工具 | Smart Measurement Tool**: 支持测量字符方框之间的距离 / Measure distances between character boxes
- **图片保存功能 | Image Saving**: 支持保存预处理和分类测试的图片 / Save preprocessing and classification test images
- **字符内部空隙处理 | Inner-gap Handling**: 支持处理汉字"二"、"川"等多笔画字符 / Handles multi-stroke characters like "二" and "川"
- **字符相对位置定位 | Relative Position Location**: 基于预设的字符相对位置信息，在分割错误时通过匹配+插值算法修正字符位置 / Corrects character positions on segmentation errors via matching + interpolation based on preset relative positions
- **字符串预设 | String Presets**: 支持保存和管理常用字符串的OCR参数、标准图片和字符相对位置 / Save and manage OCR parameters, standard images, and relative positions for common strings
- **相似度得分显示 | Score Display**: OCR识别测试界面支持显示/隐藏字符相似度得分 / Toggle character similarity scores in OCR test UI
- **区域编辑 | Region Editing**: 支持拆分、合并、删除文本区域 / Split, merge, and delete text regions
- **坐标空间切换 | Coordinate Spaces**: 支持图像坐标、屏幕坐标、归一化坐标三种空间显示 / Image, screen, and normalized coordinate display
- **定位算法调试脚本 | Location Debug Script**: 提供独立 Lua 脚本复现 `detectByRelativePosition` 核心算法，便于调试验证 / Standalone Lua script reproducing the `detectByRelativePosition` core algorithm for debugging

## 系统要求 | System Requirements

- Java 8 或更高版本 / Java 8 or higher
- 支持的操作系统 / Supported OS: macOS (ARM64/x64), Windows (AMD64), Linux

## 安装与运行 | Installation & Running

### 1. 解压分发包 | Unpack the Distribution

```bash
unzip ParoleDipinte-1.0.0-{timestamp}.zip
cd ParoleDipinte
```

### 2. 运行程序 | Run the Program

```bash
# Windows
java -jar ParoleDipinte-1.0.38-{timestamp}.jar

# macOS / Linux
java -jar ParoleDipinte-1.0.38-{timestamp}.jar
```

或者双击jar文件运行。/ Alternatively, double-click the jar file.

## 配置说明 | Configuration

### MQTT配置 | MQTT Configuration

- **Broker地址 | Broker Address**: 默认 `tcp://127.0.0.1:8907`
- **经典OCR主题 | Classic OCR Topic**: `/ampinspection/ocr/260407/raw`
- **字符分类主题 | Classification Topic**: `/ampinspection/classify/260407/raw`
- **图像二值化主题 | Binarization Topic**: `/ampinspection/binarize/260407/raw`

### 默认路径 | Default Paths

- **数据库文件夹 | Database Folder**: `$HOME/ampinspection/data`
- **图片文件夹 | Images Folder**: `$HOME/ampinspection/images`
- **日志文件夹 | Logs Folder**: `$HOME/ampinspection/logs`

### OCR参数 | OCR Parameters

- **二值化阈值 | Binarization Threshold**: 默认 128
- **Gamma系数 | Gamma Coefficient**: 默认 0.5
- **最小字符面积 | Min Character Area**: 默认 15
- **最大字符面积 | Max Character Area**: 默认 100000
- **最小字符宽度 | Min Character Width**: 默认 3
- **最小字符高度 | Min Character Height**: 默认 3
- **最大字符宽度 | Max Character Width**: 默认 500
- **最大字符高度 | Max Character Height**: 默认 200
- **字符内部垂直空隙最大值 | Max Inner Vertical Gap**: 默认 197
- **字符内部水平空隙最大值 | Max Inner Horizontal Gap**: 默认 497
- **忽略图片边界区域 | Image Border Ignore Margin**: 默认 5像素

## MQTT消息格式 | MQTT Message Formats

### OCR请求 | OCR Request

```json
{
    "imageHash": "唯一标识 Unique ID",
    "imageData": "base64编码的图像数据 base64-encoded image data",
    "correctString": "正确的字符串(可选) correct string (optional)",
    "presetName": "字符串预设名称(可选) preset name (optional)",
    "grayscale": 128,
    "gamma": 0.5,
    "minArea": 15,
    "maxArea": 100000,
    "minWidth": 3,
    "minHeight": 3,
    "maxWidth": 500,
    "maxHeight": 200
}
```

### OCR响应 | OCR Response

```json
{
    "imageHash": "唯一标识 Unique ID",
    "version": "3.3.4",
    "flags": {},
    "shapes": [
        {
            "label": "字符 character",
            "score": 0.95,
            "points": [[x1, y1], [x2, y2], [x3, y3], [x4, y4]],
            "shapeType": "rotation",
            "direction": 0.0
        }
    ],
    "imageData": "base64编码的结果图像 base64-encoded result image"
}
```

### 字符分类请求 | Character Classification Request

```json
{
    "imageHash": "唯一标识 Unique ID",
    "imageData": "base64编码的图像数据 base64-encoded image data",
    "correctString": "待分类的字符 character to classify"
}
```

### 字符分类响应 | Character Classification Response

```json
{
    "imageHash": "唯一标识 Unique ID",
    "char": "字符 character",
    "score": 0.95,
    "version": "3.3.4"
}
```

### 图像二值化请求 | Image Binarization Request

```json
{
    "imageHash": "唯一标识 Unique ID",
    "imageData": "base64编码的图像数据 base64-encoded image data",
    "grayscale": 120,
    "gamma": 0.6
}
```

**参数说明 | Parameter Description**:
- `imageHash`: 唯一标识（可选）/ Unique ID (optional)
- `imageData`: base64编码的图像数据（必需）/ base64-encoded image data (required)
- `grayscale`: 二值化阈值 0-255（可选，默认128）/ Binarization threshold 0-255 (optional, default 128)
- `gamma`: Gamma系数 0.1-2.0（可选，默认0.5）/ Gamma coefficient 0.1-2.0 (optional, default 0.5)

### 图像二值化响应 | Image Binarization Response

```json
{
    "imageHash": "唯一标识 Unique ID",
    "imageData": "base64编码的二值化图像数据 base64-encoded binarized image"
}
```

## Lua脚本扩展 | Lua Script Extensions

### 文本区域定位脚本 | Text Region Location Script

```lua
-- 文本区域定位脚本 Text region location script
-- imageInfo包含 | contains: width, height
-- regions是检测到的区域列表，每个区域包含 | each region contains: x, y, width, height
-- ocrParams包含 | contains: grayscale, gamma, minArea, maxArea, minWidth, maxWidth, minHeight, maxHeight, maxMergeWidth, isFullWidth
-- correctString为用户提供的正确字符串（可能为nil）| user-provided correct string (may be nil)
-- preset包含当前预设的所有参数（可能为nil）| current preset parameters (may be nil)
-- rawRegions为OCR参数分割出的原始字符框 | raw character boxes from OCR segmentation
-- correctedRegions为字符相对位置修正后的字符框 | character boxes corrected by relative position
-- 返回处理后的区域列表 | returns processed region list

function locate_text(imageInfo, regions, ocrParams, correctString, preset, rawRegions, correctedRegions)
    print("图像尺寸 | image size: " .. imageInfo.width .. "x" .. imageInfo.height)
    print("检测到 | detected: " .. #regions .. " 个区域 regions")

    -- 过滤太小的区域 | filter out regions that are too small
    local filtered = {}
    for i, region in ipairs(regions) do
        if region.width >= 10 and region.height >= 10 then
            table.insert(filtered, region)
        end
    end

    return filtered
end
```

### 字符分类脚本 | Character Classification Script

```lua
-- 字符分类脚本 Character classification script
-- charInfo包含 | contains: x, y, width, height, image_data
-- candidates是候选字符列表，每个包含 | each candidate contains: char, score
-- regionInfo包含 | contains: x, y, width, height
-- presetParams包含当前预设参数（可能为nil）| current preset parameters (may be nil)
-- 返回分类结果 | returns: {label=字符, score=分数}

function classify_char(charInfo, candidates, regionInfo, presetParams)
    print("字符位置 | char position: " .. charInfo.x .. "," .. charInfo.y)
    print("候选字符数 | candidate count: " .. #candidates)

    -- 返回分数最高的候选 | return the highest-scoring candidate
    local best = candidates[1]
    for i, candidate in ipairs(candidates) do
        if candidate.score > best.score then
            best = candidate
        end
    end

    return {label=best.char, score=best.score}
end
```

### OCR后处理脚本 | OCR Post-processing Script

```lua
-- OCR后处理脚本 OCR post-processing script
-- ocrResult包含 | contains: text(识别结果字符串 | recognized text), regions(区域列表 | region list), scores(得分列表 | score list)
-- presetParams包含当前预设参数（可能为nil）| current preset parameters (may be nil)
-- 返回处理后的结果 | returns: {text=字符串, regions=区域列表, scores=得分列表}

function post_process(ocrResult, presetParams)
    print("识别结果 | result: " .. ocrResult.text)
    return ocrResult
end
```

## 构建项目 | Building the Project

### 使用Gradle构建 | Build with Gradle

```bash
# 构建FatJar（使用系统默认Java版本）| Build fat jar (system default Java)
./gradlew jar

# 使用系统Java 8打包FatJar（推荐用于兼容性）| Build with Java 8 (recommended for compatibility)
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) ./gradlew jar

# 复制依赖到libs目录 | Copy dependencies to libs directory
./gradlew copyDependencies

# 创建分发包 | Create distribution package
./gradlew dist
```

### 打包输出 | Build Output

构建完成后，FatJar 位于 `build/libs/` 目录：
After building, the fat jar is located in `build/libs/`:

- 文件名格式 / File name format：`ParoleDipinte-{version}-{timestamp}.jar`
- 文件大小 / Size：约 about 593MB（包含所有依赖、native库和飞桨OCR模型 / includes all dependencies, native libraries, and PaddleOCR models）

### 依赖列表 | Dependencies

- FlatLaf 3.5.4 - 现代Swing外观 / Modern Swing look and feel
- Gson 2.10.1 - JSON处理 / JSON processing
- H2 Database 2.2.224 - 嵌入式数据库 / Embedded database
- JavaCV Platform 1.5.11 - OpenCV Java封装 / OpenCV Java bindings
- LuaJ JSE 3.0.1 - Lua脚本支持 / Lua scripting support
- Paho MQTT Client 1.2.5 - MQTT客户端 / MQTT client
- RSyntaxTextArea 3.3.4 - 代码编辑器组件 / Code editor component
- honda-paddleocr-lib - 飞桨OCR引擎（基于ONNX Runtime）/ PaddleOCR engine (based on ONNX Runtime)

## 许可证 | License

Copyright © 2026 Honda. All rights reserved. 版权所有。

## 技术支持 | Support

如有问题或建议，请联系技术支持团队。/ For questions or suggestions, please contact the technical support team.
