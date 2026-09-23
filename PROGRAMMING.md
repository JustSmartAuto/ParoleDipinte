# 编程手册 (Programming Guide)

## 概述

本文档面向开发人员，介绍识字涂色(ParoleDipinte) OCR服务的架构设计和扩展开发指南。

## 版本更新

### v1.0.59 更新内容
- 重构 `TextBlobDetector.detectByRelativePosition()` 的"情况3"处理逻辑
- 新增 `correctByMatchingAndInterpolation()` 方法，保留已匹配正确区域、仅修正未匹配部分
- 预处理阶段检测并合并相邻异常窄区域，解决汉字被拆分导致的匹配偏移
- 匹配阶段增加Y坐标差异过滤和惩罚，避免匹配到异常Y坐标的区域
- 提供独立 Lua 调试脚本 `scripts/text_location_detectByRelativePosition.lua`，完整复现算法逻辑
- 脚本包含可直接在软件 `文本定位脚本` 面板中运行的 `locate_text` 版本

### v1.0.32 更新内容
- 文本定位脚本增强，支持获取所有OCR参数和correctString
- Lua脚本可以根据correctString首字符判断全角/半角文本
- 全角文本使用maxWidth合并框，半角文本使用maxWidth/2合并框
- 参考Word等排版引擎的排版算法实现智能框合并

### v1.0.17 更新内容
- 新增OCR强制宽度分割功能，参考HALCON的PartitionRectangle算法
- 新增OCR多行排序功能，支持多行文本的层次化排序
- 新增OCR后处理Lua脚本标签页，提供字符区域查找、强制宽度分割、多行排序的Lua实现
- OCR参数设置面板添加新功能开关和参数调节

### v1.0.16 更新内容
- 新增软件设置标签页，支持MQTT自动启动和窗口尺寸设置
- 配置持久化到config.properties

### v1.0.7 更新内容
- 字符分类测试添加灰度值参数设置
- 新增图片预处理标签页，支持图像二值化参数调节和实时预览
- 新增预处理脚本功能，提供OpenCV API到Lua
- 支持自定义二值化逻辑覆盖软件内部实现

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        用户界面层                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 日志面板  │ │OCR设置   │ │MQTT设置  │ │Lua脚本   │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                        业务逻辑层                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  MQTT服务    │  │  OCR处理器   │  │ Lua脚本引擎  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                        数据访问层                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 数据库服务   │  │ 图像处理器   │  │ 配置管理器   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 核心模块

#### 1. 配置管理 (ConfigManager)

负责应用程序配置的加载、保存和管理。

**配置文件位置**: 与JAR文件同目录的`config.properties`

**主要功能**:
- MQTT配置 (broker, topic)
- OCR参数 (阈值、Gamma、面积等)
- 路径配置 (数据、图片、日志)
- Lua脚本配置 (文本定位、字符分类、日志处理)

**使用示例**:
```java
// 初始化配置
ConfigManager.init();

// 读取配置
String broker = ConfigManager.getBroker();
int grayscale = ConfigManager.getGrayscale();

// 设置配置
ConfigManager.setBroker("tcp://127.0.0.1:8907");
ConfigManager.setGrayscale(128);
ConfigManager.saveConfig();
```

#### 2. 数据库服务 (DatabaseService)

使用H2嵌入式数据库存储字符图片和OCR记录。

**数据库位置**: 配置的数据目录下的`character_db`

**主要表结构**:
```sql
-- 字符图片表
CREATE TABLE character_images (
    char_name VARCHAR(10) PRIMARY KEY,
    image_data BLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- OCR记录表
CREATE TABLE ocr_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_hash VARCHAR(64),
    request_json CLOB,
    response_json CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**使用示例**:
```java
DatabaseService dbService = new DatabaseService();
dbService.init();

// 保存字符图片
dbService.saveCharacterImage("中", characterImage);

// 加载字符图片
BufferedImage image = dbService.loadCharacterImage("中");

// 检查字符是否存在
boolean exists = dbService.characterExists("中");
```

#### 3. 图像处理器 (ImageProcessor)

提供图像预处理和字符相似度比较功能。

**主要功能**:
- BufferedImage与OpenCV Mat转换
- 动态阈值二值化
- Gamma校正
- 字符相似度比较
- 灰度值参数设置

**使用示例**:
```java
// 设置处理参数
ImageProcessor.setGrayscaleValue(128);
ImageProcessor.setGammaValue(0.5);
ImageProcessor.setThresholdOffset(-20);

// 预处理图像
Mat inputMat = ImageProcessor.bufferedImageToMat(image);
Mat binary = ImageProcessor.preprocess(inputMat);

// 比较相似度
Mat refMat = ImageProcessor.bufferedImageToMat(refImage);
double score = ImageProcessor.compareSimilarity(refMat, inputMat);
```

#### 4. 文本区域检测器 (TextBlobDetector)

基于连通区域分析的文本检测算法，参考HALCON OCR算法实现。

**主要功能**:
- 图像预处理(对比度增强、锐化)
- 连通区域分析
- 字符分割
- 漏字检测
- 区域合并
- **强制宽度分割** (v1.0.17新增)
- **多行排序** (v1.0.17新增)
- **字符相对位置定位** (v1.0.47+新增，v1.0.59重构)
  - 基于预设的字符相对位置信息，在分割错误时通过匹配+插值算法修正字符位置
  - 核心方法：`detectByRelativePosition(image, charPositions, firstCharRegion)`
  - 当框数量匹配且尺寸比例正常时，直接返回分割框
  - 当框数量不匹配时，调用 `correctByMatchingAndInterpolation()` 进行修正
    - 预处理合并相邻异常窄区域（解决汉字被拆分问题）
    - 匹配对构建与贪心分配
    - 基于锚点重新计算缩放比例
    - 对未匹配字符使用线性插值或相对偏移计算位置

**使用示例**:
```java
// 基础用法
TextBlobDetector detector = new TextBlobDetector(
    135,  // ocrGray
    15,   // minArea
    3,    // minWidth
    3,    // minHeight
    500,  // maxWidth
    200,  // maxHeight
    false // invertImage
);

// 完整用法（带新功能）
TextBlobDetector detector = new TextBlobDetector(
    135,    // ocrGray
    15,     // minArea
    3,      // minWidth
    3,      // minHeight
    500,    // maxWidth
    200,    // maxHeight
    false,  // invertImage
    true,   // forceWidthSplitEnabled - 启用强制宽度分割
    true,   // multiLineSortEnabled - 启用多行排序
    31,     // forceSplitWidth - 分割宽度
    20      // rowGapThreshold - 行间距阈值
);

TextBlobDetector.DetectionResult result = detector.detect(image);
List<TextBlobDetector.Region> regions = result.textRegions;
```

**强制宽度分割**:
参考HALCON的PartitionRectangle算法，当自动分割的字符数量与预期不符时，使用固定宽度强制分割。
```java
// 单独使用强制宽度分割
detector.setForceWidthSplit(true, 31);
List<Region> splitRegions = detector.forceWidthSplit(regions, expectedCount);
```

**多行排序**:
参考HALCON的多行排序策略：先按行排序（从上到下），再对每行内的字符按列排序（从左到右）。
```java
// 单独使用多行排序
detector.setMultiLineSort(true, 20);
List<Region> sortedRegions = detector.sortMultiLineRegions(regions);
```

**字符相对位置定位使用示例**:
```java
// 准备字符相对位置列表和首字符区域
List<CharRelativePosition> charPositions = preset.getCharRelativePositions();
TextBlobDetector.Region firstCharRegion = detectedRegions.get(0);

// 使用相对位置修正所有字符位置
List<TextBlobDetector.Region> correctedRegions = detector.detectByRelativePosition(
    image, charPositions, firstCharRegion
);
```

**Lua 调试脚本**:
项目提供独立的 Lua 脚本 `scripts/text_location_detectByRelativePosition.lua`，完整复现了 `detectByRelativePosition` 的算法逻辑：
- 不依赖 OpenCV，可在任意 Lua 环境中运行
- 支持传入 `charPositions`、`firstCharRegion`、`allBlobRegions` 进行调试
- 输出日志与 Java 端保持一致，便于对比验证

```lua
local script = dofile("scripts/text_location_detectByRelativePosition.lua")
local detected = script.detectByRelativePosition(charPositions, firstCharRegion, allBlobRegions)
for i, r in ipairs(detected) do
    print(script.formatRegion(r))
end
```

**在软件 `文本定位脚本` 中直接使用**：
将 `scripts/text_location_detectByRelativePosition.lua` 的完整内容复制到软件的 `文本定位脚本` 编辑器中即可。脚本已包含标准的 `locate_text(imageInfo, regions, ocrParams, correctString)` 入口函数，会自动读取 `preset.charRelativePositions` 和 `rawRegions` 等全局变量进行计算。

#### 5. MQTT服务 (MqttService)

处理MQTT消息的接收和响应。

**主要功能**:
- 连接到MQTT Broker
- 订阅OCR和分类主题
- 解析请求JSON
- 执行OCR处理
- 发送响应JSON

**使用示例**:
```java
MqttService mqttService = new MqttService(dbService, logger);
mqttService.setBroker("tcp://127.0.0.1:8907");
mqttService.setOcrTopic("/ampinsepction/ocr/260407/raw");
mqttService.setClassifyTopic("/aminspection/classify/260407/raw");
mqttService.start();
```

#### 6. Lua脚本引擎 (LuaScriptEngine)

支持使用Lua脚本扩展OCR逻辑。

**脚本类型**:
- `TEXT_LOCATION`: 文本区域定位
- `CHAR_CLASSIFY`: 字符分类
- `OCR_POST_PROCESS`: OCR后处理（强制宽度分割、多行排序）

**使用示例**:
```java
LuaScriptEngine engine = new LuaScriptEngine(LuaScriptEngine.ScriptType.TEXT_LOCATION);
engine.setScript(luaScript);
engine.setEnabled(true);

// 验证脚本
ValidationResult result = engine.validateScript(script);

// 执行文本定位脚本
ScriptResult result = engine.executeTextLocation(imageInfo, regions);

// 执行OCR后处理脚本 (v1.0.17新增)
Map<String, Object> options = new HashMap<>();
options.put("forceWidthSplit", true);
options.put("splitWidth", 31);
options.put("multiLineSort", true);
options.put("rowGapThreshold", 20);
ScriptResult result = engine.executeOcrPostProcess(imageInfo, regions, options);
```

#### 7. 日志脚本引擎 (LogScriptEngine)

支持使用Lua脚本处理日志过滤和自动任务。

**主要功能**:
- 日志过滤 (filter函数)
- 自动任务 (task函数)
- 脚本验证和测试

**使用示例**:
```java
LogScriptEngine engine = new LogScriptEngine();
engine.setScript(luaScript);
engine.setEnabled(true);

// 验证脚本
ValidationResult result = engine.validateScript(script);

// 执行过滤
boolean keep = engine.executeFilter(timestamp, message, level);

// 执行任务
TaskResult taskResult = engine.executeTask(total, filtered, current);
```

## Lua脚本开发指南

### 预处理脚本

**函数签名**:
```lua
function preprocess(image_data, params)
    -- image_data: 图像数据表 {width=宽度, height=高度, channels=通道数}
    -- params: 参数表 {grayscale=阈值, gamma=伽马值, threshold_offset=偏移量, invert=是否反色}
    -- 返回: 处理后的图像数据表
end
```

**可用的OpenCV API函数**:

| 函数 | 参数 | 说明 |
|------|------|------|
| `cv_threshold` | image, thresh, maxval | 二值化 |
| `cv_adaptive_threshold` | image, maxval, method, block_size, c | 自适应二值化 |
| `cv_gaussian_blur` | image, kw, kh | 高斯模糊 |
| `cv_median_blur` | image, ksize | 中值滤波 |
| `cv_bilateral_filter` | image, d, sigma_color, sigma_space | 双边滤波 |
| `cv_erode` | image, ksize | 腐蚀 |
| `cv_dilate` | image, ksize | 膨胀 |
| `cv_morphology_ex` | image, op, ksize | 形态学操作(open/close/gradient/tophat/blackhat) |
| `cv_cvt_color` | image, code | 颜色空间转换(gray/bgr2gray/rgb2gray) |
| `cv_resize` | image, width, height | 缩放 |
| `cv_normalize` | image, alpha | 归一化 |
| `cv_bitwise_not` | image | 反色 |
| `cv_bitwise_and` | img1, img2 | 按位与 |
| `cv_bitwise_or` | img1, img2 | 按位或 |
| `cv_add_weighted` | img1, alpha, img2, beta | 加权叠加 |
| `cv_laplacian` | image | 拉普拉斯边缘检测 |
| `cv_sobel` | image, dx, dy | Sobel边缘检测 |
| `cv_canny` | image, thresh1, thresh2 | Canny边缘检测 |
| `cv_find_contours` | image | 查找轮廓 |
| `cv_contour_area` | contour | 计算轮廓面积 |
| `cv_bounding_rect` | contour | 计算边界矩形 |
| `cv_mean` | image | 计算均值 |
| `cv_min_max_loc` | image | 查找最小最大值 |

**示例脚本**:
```lua
-- 自定义预处理脚本
function preprocess(image_data, params)
    print("图像尺寸: " .. image_data.width .. "x" .. image_data.height)
    
    -- 高斯模糊去噪
    local blurred = cv_gaussian_blur(image_data, 5, 5)
    
    -- 自适应二值化
    local binary = cv_adaptive_threshold(
        blurred, 
        255, 
        "gaussian", 
        11, 
        2
    )
    
    -- 形态学操作去除噪点
    local cleaned = cv_morphology_ex(binary, "open", 3)
    
    return cleaned
end
```

### 文本区域定位脚本

**函数签名** (v1.0.32增强):
```lua
function locate_text(imageInfo, regions, ocrParams, correctString)
    -- imageInfo: 图像信息表 {width=宽度, height=高度}
    -- regions: 区域列表，每个元素为 {x=, y=, width=, height=}
    -- ocrParams: OCR参数表 {grayscale, gamma, minArea, maxArea, minWidth, maxWidth, minHeight, maxHeight, maxMergeWidth, isFullWidth}
    -- correctString: 用户提供的正确字符串（可能为nil）
    -- 返回: 处理后的区域列表
end
```

**ocrParams参数说明**:
| 参数名 | 类型 | 说明 |
|--------|------|------|
| `grayscale` | number | 二值化阈值 |
| `gamma` | number | Gamma系数 |
| `minArea` | number | 最小字符面积 |
| `maxArea` | number | 最大字符面积 |
| `minWidth` | number | 最小字符宽度 |
| `maxWidth` | number | 最大字符宽度 |
| `minHeight` | number | 最小字符高度 |
| `maxHeight` | number | 最大字符高度 |
| `maxMergeWidth` | number | 最大合并宽度（根据全角/半角自动计算）|
| `isFullWidth` | boolean | 是否为全角文本 |

**全角/半角判断**:
- 全角字符：中文字符、全角标点等（Unicode > 127）
- 半角字符：ASCII字符（0-127）
- 根据`correctString`的第一个字符自动判断
- 全角文本使用`maxWidth`作为合并阈值
- 半角文本使用`maxWidth/2`作为合并阈值

**可用函数**:
- `print(...)`: 输出到控制台
- `contains(str, substr)`: 检查字符串是否包含子串
- `starts_with(str, prefix)`: 检查字符串前缀
- `ends_with(str, suffix)`: 检查字符串后缀
- `matches(str, pattern)`: 正则表达式匹配
- `abs(x)`: 绝对值
- `min(a, b)`: 最小值
- `max(a, b)`: 最大值
- `sqrt(x)`: 平方根

**示例脚本** (基础过滤和排序):
```lua
-- 过滤太小的区域并按x坐标排序
function locate_text(imageInfo, regions, ocrParams, correctString)
    local filtered = {}
    
    -- 过滤
    for i, region in ipairs(regions) do
        if region.width >= 10 and region.height >= 10 then
            table.insert(filtered, region)
        end
    end
    
    -- 按x坐标排序
    table.sort(filtered, function(a, b)
        return a.x < b.x
    end)
    
    print("过滤后剩余 " .. #filtered .. " 个区域")
    
    return filtered
end
```

**示例脚本** (智能框合并，参考Word排版算法):
```lua
-- 根据全角/半角智能合并文本框
function locate_text(imageInfo, regions, ocrParams, correctString)
    print("图像尺寸: " .. imageInfo.width .. "x" .. imageInfo.height)
    print("检测到 " .. #regions .. " 个区域")
    
    -- 打印OCR参数
    if ocrParams then
        print("OCR参数 - 阈值:" .. ocrParams.grayscale .. " Gamma:" .. ocrParams.gamma)
        print("尺寸限制 - 宽:" .. ocrParams.minWidth .. "-" .. ocrParams.maxWidth ..
              " 高:" .. ocrParams.minHeight .. "-" .. ocrParams.maxHeight)
    end
    
    -- 根据correctString判断全角/半角并调整合并策略
    local maxMergeWidth = ocrParams and ocrParams.maxWidth or 500
    if correctString and #correctString > 0 then
        local firstChar = correctString:sub(1, 1)
        local charCode = utf8.codepoint(firstChar) or firstChar:byte(1)
        
        -- 判断全角/半角：ASCII字符(0-127)为半角，其他为全角
        if charCode <= 127 then
            -- 半角字符：使用一半的maxWidth作为合并阈值
            maxMergeWidth = math.floor(maxMergeWidth / 2)
            print("检测到半角文本，合并阈值调整为:" .. maxMergeWidth)
        else
            -- 全角字符：使用完整maxWidth
            print("检测到全角文本，合并阈值:" .. maxMergeWidth)
        end
    end
    
    -- 根据maxMergeWidth合并相邻区域（类似Word排版算法）
    local merged = {}
    table.sort(regions, function(a, b) return a.x < b.x end)
    
    for i, region in ipairs(regions) do
        if #merged == 0 then
            table.insert(merged, {x=region.x, y=region.y, width=region.width, height=region.height})
        else
            local last = merged[#merged]
            local gap = region.x - (last.x + last.width)
            local combinedWidth = region.x + region.width - last.x
            
            -- 如果间距小且合并后不超过maxMergeWidth，则合并
            if gap <= 5 and combinedWidth <= maxMergeWidth then
                last.width = combinedWidth
                last.height = math.max(last.height, region.height)
            else
                table.insert(merged, {x=region.x, y=region.y, width=region.width, height=region.height})
            end
        end
    end
    
    print("合并后 " .. #merged .. " 个区域")
    return merged
end
```

### 字符分类脚本

**函数签名**:
```lua
function classify_char(charInfo, candidates)
    -- charInfo: 字符信息表 {x=, y=, width=, height=}
    -- candidates: 候选列表，每个元素为 {char=字符, score=分数}
    -- 返回: {label=字符, score=分数}
end
```

**示例脚本**:
```lua
-- 选择分数最高的候选，但分数必须大于0.5
function classify_char(charInfo, candidates)
    local best = nil
    local bestScore = 0.5
    
    for i, candidate in ipairs(candidates) do
        if candidate.score > bestScore then
            best = candidate
            bestScore = candidate.score
        end
    end
    
    if best then
        return {label=best.char, score=best.score}
    else
        return {label="?", score=0.1}
    end
end
```

### OCR后处理脚本 (v1.0.17新增)

**函数签名**:
```lua
function post_process(imageInfo, regions, options)
    -- imageInfo: 图像信息表 {width=宽度, height=高度}
    -- regions: 区域列表，每个元素为 {x=, y=, width=, height=}
    -- options: 选项表 {forceWidthSplit=是否强制分割, splitWidth=分割宽度, multiLineSort=是否多行排序, rowGapThreshold=行间距阈值}
    -- 返回: 处理后的区域列表
end
```

**可用函数**:
- `force_width_split(regions, splitWidth)`: 强制宽度分割
- `sort_multiline(regions, rowGapThreshold)`: 多行排序
- 所有文本区域定位脚本的可用函数

**默认脚本**:
```lua
-- OCR后处理脚本
-- 实现字符区域查找、强制宽度分割、多行排序功能

function post_process(imageInfo, regions, options)
    print("图像尺寸: " .. imageInfo.width .. "x" .. imageInfo.height)
    print("检测到 " .. #regions .. " 个区域")
    
    local result = regions
    
    -- 1. 强制宽度分割
    if options.forceWidthSplit then
        result = force_width_split(result, options.splitWidth)
        print("强制分割后: " .. #result .. " 个区域")
    end
    
    -- 2. 多行排序
    if options.multiLineSort then
        result = sort_multiline(result, options.rowGapThreshold)
        print("多行排序完成")
    else
        -- 单行模式：按X坐标排序
        table.sort(result, function(a, b) return a.x < b.x end)
        print("单行排序完成")
    end
    
    return result
end

-- 强制宽度分割函数
function force_width_split(regions, splitWidth)
    local result = {}
    for i, region in ipairs(regions) do
        local charCount = math.max(1, math.floor(region.width / splitWidth + 0.5))
        if charCount > 1 then
            local charWidth = math.floor(region.width / charCount)
            for j = 0, charCount - 1 do
                local x = region.x + j * charWidth
                local width = charWidth
                if j == charCount - 1 then
                    width = region.x + region.width - x
                end
                table.insert(result, {x=x, y=region.y, width=width, height=region.height})
            end
        else
            table.insert(result, region)
        end
    end
    return result
end

-- 多行排序函数
function sort_multiline(regions, rowGapThreshold)
    -- 按Y坐标排序
    local sorted = {}
    for i, r in ipairs(regions) do sorted[i] = r end
    table.sort(sorted, function(a, b) return a.y < b.y end)
    
    -- 按行分组
    local rows = {}
    local currentRow = {}
    for i, region in ipairs(sorted) do
        if #currentRow == 0 then
            table.insert(currentRow, region)
        else
            local last = currentRow[#currentRow]
            if math.abs(region.y - last.y) < rowGapThreshold then
                table.insert(currentRow, region)
            else
                table.insert(rows, currentRow)
                currentRow = {region}
            end
        end
    end
    if #currentRow > 0 then
        table.insert(rows, currentRow)
    end
    
    -- 对每行按X坐标排序，然后合并
    local result = {}
    for i, row in ipairs(rows) do
        table.sort(row, function(a, b) return a.x < b.x end)
        for j, region in ipairs(row) do
            table.insert(result, region)
        end
    end
    return result
end
```

**使用示例**:
```lua
-- 自定义后处理：强制分割后按面积过滤
function post_process(imageInfo, regions, options)
    -- 先执行默认的强制宽度分割
    local result = regions
    if options.forceWidthSplit then
        result = force_width_split(result, options.splitWidth)
    end
    
    -- 过滤太小的区域
    local filtered = {}
    for i, region in ipairs(result) do
        local area = region.width * region.height
        if area >= 100 then
            table.insert(filtered, region)
        end
    end
    
    -- 多行排序
    if options.multiLineSort then
        result = sort_multiline(filtered, options.rowGapThreshold)
    else
        result = filtered
        table.sort(result, function(a, b) return a.x < b.x end)
    end
    
    print("过滤后剩余 " .. #result .. " 个区域")
    return result
end
```

### 日志处理脚本

**函数签名**:
```lua
-- filter函数: 返回true保留日志，false过滤掉
function filter(log)
    -- log: 日志表 {timestamp=时间戳, message=内容, level=级别}
    -- 返回: true或false
end

-- task函数: 定期执行的自动任务
function task(stats)
    -- stats: 统计表 {total=总条数, filtered=已过滤数, current=当前显示数}
    -- 返回: {action="clear"或"none", message=提示消息}
end
```

**默认脚本**:
```lua
-- 日志处理脚本
-- filter函数: 返回true表示保留日志，false表示过滤掉
-- log参数: {timestamp=时间戳, message=日志内容, level=级别}

function filter(log)
    -- 默认不过滤任何日志
    return true
end

-- task函数: 定期执行的任务
-- stats参数: {total=总条数, filtered=已过滤数, current=当前显示数}
-- 返回值: {action=动作, message=消息}
-- action可选: "clear"(清空), "none"(无操作)

function task(stats)
    -- 当日志达到10000条时自动清空
    if stats.total >= 10000 then
        return {
            action = "clear",
            message = "日志达到10000条，自动清空"
        }
    end
    return {action = "none"}
end
```

**过滤示例**:
```lua
-- 只保留包含"错误"或"失败"的日志
function filter(log)
    return contains(log.message, "错误") or contains(log.message, "失败")
end
```

**自动清空示例**:
```lua
-- 每5000条清空一次
function task(stats)
    if stats.total >= 5000 then
        return {
            action = "clear",
            message = "定期清空日志"
        }
    end
    return {action = "none"}
end
```

## XAnyLabeling JSON支持

### XAnyLabelingJson模型

用于解析XAnyLabeling标注工具生成的JSON文件。

**JSON格式**:
```json
{
    "version": "3.3.4",
    "flags": {},
    "shapes": [
        {
            "label": "本",
            "score": 0.8,
            "points": [[45.75, 11.17], [124.92, 11.17], [124.92, 102.83], [45.75, 102.83]],
            "shape_type": "rotation",
            "direction": 0.0
        }
    ],
    "imagePath": "image.jpg",
    "imageData": "base64encoded...",
    "imageHeight": 1080,
    "imageWidth": 1920
}
```

**使用示例**:
```java
// 从文件加载
XAnyLabelingJsonLoader.LoadResult result = XAnyLabelingJsonLoader.loadFromFile(jsonFile);

if (result.isSuccess()) {
    BufferedImage image = result.getImage();
    List<Shape> shapes = result.getShapes();
    XAnyLabelingJson jsonData = result.getJsonData();
    
    // 显示图片和标注
    resultPanel.setResult(image, shapes);
}
```

## 扩展开发

### 添加新的OCR参数

1. 在 `ConfigManager` 中添加配置键和默认值:
```java
public static final String KEY_NEW_PARAM = "ocr.newParam";
private static final String DEFAULT_NEW_PARAM = "100";
```

2. 添加getter和setter方法:
```java
public static int getNewParam() {
    try {
        return Integer.parseInt(getProperty(KEY_NEW_PARAM, DEFAULT_NEW_PARAM));
    } catch (NumberFormatException e) {
        return 100;
    }
}

public static void setNewParam(int value) {
    setProperty(KEY_NEW_PARAM, String.valueOf(value));
}
```

3. 在 `OcrSettingsPanel` 中添加UI控件

### 强制宽度分割和多行排序配置 (v1.0.17)

**配置项**:
```java
// 强制宽度分割
public static final String KEY_FORCE_WIDTH_SPLIT = "ocr.forceWidthSplit";
public static final String KEY_FORCE_SPLIT_WIDTH = "ocr.forceSplitWidth";

// 多行排序
public static final String KEY_MULTI_LINE_SORT = "ocr.multiLineSort";
public static final String KEY_ROW_GAP_THRESHOLD = "ocr.rowGapThreshold";
```

**使用示例**:
```java
// 读取配置
boolean forceSplit = ConfigManager.isForceWidthSplitEnabled();
int splitWidth = ConfigManager.getForceSplitWidth();
boolean multiLineSort = ConfigManager.isMultiLineSortEnabled();
int rowGapThreshold = ConfigManager.getRowGapThreshold();

// 设置配置
ConfigManager.setForceWidthSplitEnabled(true);
ConfigManager.setForceSplitWidth(31);
ConfigManager.setMultiLineSortEnabled(true);
ConfigManager.setRowGapThreshold(20);
ConfigManager.saveConfig();
```

### 添加新的MQTT主题

1. 在 `ConfigManager` 中添加主题配置:
```java
public static final String KEY_NEW_TOPIC = "mqtt.topic.new";
```

2. 在 `MqttService` 中添加主题处理和订阅逻辑

### 自定义图像处理算法

1. 创建新的处理器类:
```java
public class CustomImageProcessor {
    public Mat process(Mat input) {
        // 自定义处理逻辑
        return output;
    }
}
```

2. 在 `MqttService` 中集成新的处理器

## 调试技巧

### 启用详细日志

在启动参数中添加:
```bash
java -Djava.util.logging.SimpleFormatter.format='%4$s: %5$s%n' -jar ParoleDipinte.jar
```

### 测试MQTT连接

使用 `mosquitto_pub` 和 `mosquitto_sub` 测试:
```bash
# 订阅响应
mosquitto_sub -h 127.0.0.1 -p 8907 -t "/ampinsepction/ocr/260407/raw"

# 发送请求
mosquitto_pub -h 127.0.0.1 -p 8907 -t "/ampinsepction/ocr/260407/raw" -f request.json
```

### 数据库查看

使用H2 Console查看数据库:
```bash
java -cp libs/h2-2.2.224.jar org.h2.tools.Console
```

### 测试日志脚本

在软件界面的`软件日志`->`脚本`标签页中:
1. 编辑Lua脚本
2. 点击"验证脚本"检查语法
3. 点击"测试运行"测试功能
4. 查看调试控制台输出

## 性能优化

### 图像处理优化
- 使用适当大小的图像(建议不超过2000x2000)
- 调整OCR参数以平衡精度和速度
- 使用GPU加速(如果JavaCV支持)

### 数据库优化
- 定期清理旧的OCR记录
- 对常用查询添加索引

### 内存优化
- 及时释放OpenCV Mat对象
- 使用日志脚本自动清空过多日志（默认10000条）
- 控制日志缓冲区大小

## 图片预处理面板

### 功能说明

图片预处理面板提供交互式的图像二值化参数调节和预览功能。

**主要功能**:
1. **图像加载**: 支持拖入图片或打开图片文件
2. **参数调节**:
   - 二值化阈值 (0-255): 控制黑白转换的阈值
   - Gamma系数 (0.1-2.0): 调整图像对比度
   - 阈值偏移量 (-100~100): 动态阈值的偏移调整
   - 反色处理: 交换黑白颜色
3. **实时预览**: 左右分栏显示原始图像和二值化效果
4. **脚本扩展**: 支持Lua脚本自定义预处理逻辑

### 使用OpenCV API

在预处理脚本中，可以直接调用OpenCV API函数:

```lua
function preprocess(image_data, params)
    -- 使用高斯模糊去噪
    local blurred = cv_gaussian_blur(image_data, 5, 5)
    
    -- 使用Canny边缘检测
    local edges = cv_canny(blurred, 50, 150)
    
    -- 查找轮廓
    local contours = cv_find_contours(edges)
    
    print("找到 " .. #contours .. " 个轮廓")
    
    -- 遍历轮廓
    for i, contour in ipairs(contours) do
        local area = cv_contour_area(contour)
        local rect = cv_bounding_rect(contour)
        
        print("轮廓 " .. i .. ": 面积=" .. area .. 
              ", 位置=(" .. rect.x .. "," .. rect.y .. ")")
    end
    
    return edges
end
```

## 常见问题

### Q: 字符相似度始终为0?
A: 检查数据库中是否已生成字符图片，或尝试调整Gamma值和阈值偏移量。

### Q: MQTT连接失败?
A: 检查Broker地址和端口，确保网络连通，检查防火墙设置。

### Q: Lua脚本不生效?
A: 确保脚本已启用，检查脚本语法，查看调试控制台输出。

### Q: 界面显示异常?
A: 确保使用Java 8或更高版本，检查字体文件是否存在。

### Q: 日志过多导致内存不足?
A: 在`软件日志`->`脚本`中启用默认脚本，或自定义task函数设置合适的清空阈值。

## 参考资源

- [OpenCV Java Documentation](https://docs.opencv.org/)
- [Lua 5.1 Reference Manual](https://www.lua.org/manual/5.1/)
- [MQTT Version 3.1.1 Specification](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/)
- [FlatLaf Documentation](https://www.formdev.com/flatlaf/)
- [XAnyLabeling Documentation](https://github.com/CVHub520/XAnyLabeling)
