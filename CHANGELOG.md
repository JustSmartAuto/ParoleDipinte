# 更新日志 (Changelog)

## [1.0.59] - 2026-04-14

### 修复

#### TextBlobDetector字符相对位置定位算法重构
- **重构`detectByRelativePosition()`的\"情况3\"处理逻辑**
  - 新增`correctByMatchingAndInterpolation()`方法，替代原有的全量重定位策略
  - 核心改进：保留原始分割中已匹配的正确区域，只修正未匹配的部分
  - **预处理阶段**：在匹配前检测并合并相邻的异常窄区域
    - 合并条件：水平间距 ≤ 10像素、高度差异 < 35%、有垂直重叠、原始区域明显偏窄（宽高比 < 0.5 且宽度 ≥ 35）、合并后更接近方形、合并后宽度 ≤ 80
    - 解决汉字\"能\"、\"公\"等被拆分为两个窄区域导致的匹配偏移问题
    - 避免把正常半角字符（宽高比 0.5~0.6）错误合并
  - **匹配阶段**：将原始分割区域与字符预计位置进行匹配评分（距离 + 尺寸差异 + Y差异），贪心分配最佳匹配对
    - 增加Y坐标差异过滤：中心点Y差异超过 `max(预设高度*0.5, 20)` 像素的匹配对被剔除
    - 评分中增加 `yDiff * 2` 的惩罚，避免字符被匹配到下划线/标点等Y坐标异常的区域
  - **缩放计算**：基于所有匹配上的锚点区域重新计算平均缩放比例
  - **插值修正**：对未匹配字符使用左右锚点进行线性插值或相对偏移计算位置
    - 若左右锚点Y差异 > 20像素，不使用Y方向线性插值，改为使用左锚点Y + 相对偏移
    - 避免下划线/标点等异常Y坐标的锚点将普通字符的Y坐标带偏
  - **回退机制**：无任何锚点时回退到`findBestMatchingRegion`
  - 修复了原算法在框数量不匹配时从头重新定位所有字符、导致原本正确的区域也被破坏的问题
  - `findBestMatchingRegion`增加面积过滤（`area >= max(minArea, 10)`），避免将小噪声点当成字符
  - 新增`isRegionSizeReasonable()`检查，修正结果尺寸异常时放弃修正
  - 将`detect(image)`返回的`DetectionResult`正确转换为`List<Region>`，释放临时可视化图像

#### 字符串预设后字符相对位置同步到OCR参数界面
- **使用预设后自动同步字符相对位置参数**
  - `StringPresetPanel.useSelectedPreset()` 现在会将预设中的字符相对位置信息同步到`OCR参数`界面
  - 同步内容包括：启用状态、正确字符串、字符相对位置表格数据
  - 同时同步OCR参数（二值化阈值、Gamma、面积范围、宽高范围）到`ConfigManager`
  - `OcrSettingsPanel` 新增 `setCharRelativePositions()` 公共方法支持外部设置

#### 启用字符相对位置后区域变成点状的问题修复
- **修复`TextBlobDetector.findBestMatchingRegion()`搜索区域过小的问题**
  - 增加最小搜索半径为40像素，避免缩放后搜索区域过小
  - 放宽尺寸匹配限制（最小值减半，最大值翻倍），提高容错性
  - 当未找到匹配区域时，返回基于expectedX/Y的估计区域而非null
  - 避免字符框变成异常小的"点状区域"

### 文档与脚本
- **新增 Lua 调试脚本 `scripts/text_location_detectByRelativePosition.lua`**
  - 完整复现 `TextBlobDetector.detectByRelativePosition()` 算法逻辑
  - 包含：缩放计算、框数量匹配判断、相邻窄区域合并预处理、匹配对构建与贪心分配、锚点插值/偏移修正
  - 不依赖 OpenCV，可在任意 Lua 环境中独立运行调试
  - 提供可直接复制到软件 `文本定位脚本` 面板中运行的 `locate_text` 版本
  - 便于在 Java 端与 Lua 端之间对比验证定位结果

### 构建
- 使用build.sh打包fatjar v1.0.59

---

## [1.0.58] - 2026-04-14

### 新增功能

#### OCR参数界面添加字符相对位置列表显示
- **`OCR参数`标签页新增字符相对位置编辑器**
  - 界面参考`字符串预设`中的字符相对位置编辑器
  - 顶部包含：启用复选框、正确字符串输入框、`从字符串生成`按钮
  - 表格显示：索引、字符、类型、相对X、相对Y、宽度、高度、间距
  - 数值列（相对X/Y、宽度、高度、间距）支持直接编辑
  - 数据持久化到`ConfigManager`，以JSON格式存储在配置文件中
  - 新增配置键：`ocr.charRelativePositions`、`ocr.charRelativePositionEnabled`

#### OCR识别测试显示当前预设
- **在OCR识别测试面板底部添加预设名称标签**
  - 未使用预设时显示：`当前预设: 无`
  - 使用预设后显示：`当前预设: {presetName}`（绿色高亮）
  - `OcrTestPanel.setCurrentPreset()` 自动更新标签状态

#### 参数还原功能
- **菜单栏`参数`菜单新增`还原参数`选项**
  - 在`字符串预设`中点击`使用预设`前，自动备份当前所有OCR参数
  - 备份内容包括：二值化阈值、Gamma、面积范围、宽高范围、字符相对位置等
  - 点击`还原参数`可将ConfigManager恢复为使用预设前的值
  - 还原后自动刷新`OCR参数`界面，并清除OCR识别测试的当前预设
  - 没有备份时菜单项禁用，还原成功后显示确认提示

### 构建
- 使用build.sh打包fatjar v1.0.58

---

## [1.0.57] - 2026-04-13

### 新增功能

#### 字符定位脚本API增强
- **文本定位脚本支持访问当前预设的所有参数**
  - `LuaScriptEngine.executeTextLocation()` 新增完整参数版本，支持传入 `presetParams`、`rawRegions`、`correctedRegions`
  - 在Lua环境中注册 `preset` 全局表（包含：presetName, correctString, OCR参数, charRelativePositions等）
  - 在Lua环境中注册 `rawRegions` 全局变量（OCR参数分割出的原始字符框）
  - 在Lua环境中注册 `correctedRegions` 全局变量（字符相对位置修正后的字符框）
  - `OcrTestPanel.runOcr()` 和 `MqttService.processOcrMessage()` 均注入预设参数到检测器

#### 字符位置推断及修正算法
- **增强 `TextBlobDetector.detectByRelativePosition()` 方法**
  - 新增 `extractAllBlobRegions()` 辅助方法，提取图像中所有有效连通区域
  - **情况1/2（图像正常/拉伸）**：正确字符框数量 = 分割字符框数量，且尺寸成一定比例时，直接使用分割框
  - **情况3（分割错误）**：框数量不匹配时进行修正推断
    - 根据首字符实际尺寸与预设尺寸的比例，修正首字符框的分割（处理"AB"合并/"利"拆分/"A_H"误判）
    - 对准字符串第一个字符位置（头对准）
    - 对准字符串尾部最后一个字符位置（尾对准）
    - 基于首尾实际跨度调整中间字符框间距
    - 对无法匹配的字符使用预计位置填充

#### 文本定位Lua脚本模板更新
- **默认脚本模板集成字符位置推断算法**
  - 新增 `infer_char_positions()` 函数实现字符位置推断及修正
  - 新增 `get_expected_width()` 函数从预设读取预期字符宽度
  - 新增 `adjust_spacing()` 函数调整字符框间距，支持强制宽度分割
  - 脚本可直接访问 `preset`、`rawRegions`、`correctedRegions` 进行自定义处理

### 构建
- 使用build.sh打包fatjar v1.0.57 (593M)

---

## [1.0.56] - 2026-04-13

### 改进

#### MQTT OCR处理流程与OCR识别测试保持一致
- **统一MQTT和OCR识别测试的处理流程**
  - 参考CHANGELOG 1.0.55的完整预设参数使用流程
  - 步骤1: 使用OCR参数进行区域分割（阈值、Gamma、面积范围等）
  - 步骤2: 如果启用了字符相对位置，使用 `TextBlobDetector.detectByRelativePosition()` 修正区域位置
  - 步骤3: 如果启用了文本定位脚本，执行脚本进行区域后处理
  - 步骤4: 使用正确字符串给区域打字符标签
  - 步骤5: 如果启用了字符分类脚本，执行脚本进行字符分类
  - 步骤6: 如果启用了OCR后处理脚本，执行脚本进行最终处理

- **参数优先级修正：用户覆盖为最高优先级**
  - JSON请求中传来的参数（如 `correctString`、`grayscale`、`gamma` 等）具有最高优先级
  - 完整优先级顺序：**请求参数 > 预设参数 > 配置默认值**
  - 特别修正 `correctString` 的处理逻辑：当请求中明确传入 `correctString`（包括空字符串）时，优先使用请求值；仅当请求中未传入（null）时才使用预设值

- **MqttService增强**
  - 新增 `setCharClassifyScriptEngine()` 方法，支持字符分类Lua脚本引擎
  - 新增 `setOcrPostProcessScriptEngine()` 方法，支持OCR后处理Lua脚本引擎
  - `MainFrame` 初始化时将三种Lua脚本引擎（文本定位、字符分类、OCR后处理）全部注入 `MqttService`

### 新增功能

#### 编辑菜单添加删除区域功能
- **在 `编辑` -> `合并区域` 下方添加 `删除区域` 菜单项**
  - 点击菜单进入删除区域模式
  - 鼠标变为手型光标
  - 点击连通域即可删除该区域
  - 删除后自动刷新显示
  - 与拆分/合并区域模式互斥，切换时自动关闭其他编辑模式

### 构建
- 使用build.sh打包fatjar v1.0.56 (593M)

---

## [1.0.55] - 2026-04-12

### 修复

#### 字符串预设参数使用顺序修正
- **修复使用预设后OCR识别测试没有使用字符相对位置的问题**
  - 问题：StringPresetPanel.useSelectedPreset() 只设置了正确字符串，没有使用预设中的其他参数
  - 修复：
    - OcrTestPanel 添加 currentPreset 字段保存当前预设
    - 添加 setCurrentPreset() 方法设置完整的预设信息
    - 修改 runOcr() 方法优先使用预设中的OCR参数
    - 修改 StringPresetPanel.useSelectedPreset() 调用 setCurrentPreset() 而不是 setCorrectString()

### 新增功能

#### 字符串预设参数使用流程
- **完整的预设参数使用顺序**
  1. 使用OCR参数进行区域分割（阈值、Gamma、面积范围等）
  2. 如果启用了字符相对位置，使用 TextBlobDetector.detectByRelativePosition() 修正区域位置
  3. 如果启用了文本定位脚本，执行脚本进行区域后处理
  4. 使用正确字符串给区域打字符标签
  5. 如果启用了字符分类脚本，执行脚本进行字符分类
  6. 如果启用了OCR后处理脚本，执行脚本进行最终处理

### 构建
- 使用build.sh打包fatjar v1.0.55 (592M)

---

## [1.0.54] - 2026-04-12

### 新增功能

#### MQTT OCR请求支持预设名称
- **添加presetName参数支持**
  - OcrRequest添加`presetName`字段，用于指定字符串预设名称
  - 如果请求中指定了预设名称，系统会自动查找对应的字符串预设
  - 优先级：请求中的参数 > 预设中的参数 > 配置默认值
  - 如果找到预设，自动使用预设中的OCR参数（阈值、Gamma、面积范围等）
  - 如果找到预设且请求中未指定correctString，则使用预设中的正确字符串
  - 支持从数据库和XML存储两种方式查找预设

#### 编辑菜单添加更新标签功能
- **添加"更新标签"菜单项**
  - 位于"合并区域"菜单项之后
  - 作用：根据正确字符串更新所有区域的标签
  - 适用场景：合并区域或拆分区域后，更新标签与区域的对应关系
  - 实现：OcrTestPanel.add updateLabels() 方法
  - 使用方法：输入正确字符串后，点击"更新标签"菜单即可更新所有区域标签

### 构建
- 使用build.sh打包fatjar v1.0.54 (592M)

---

## [1.0.53] - 2026-04-12

### 修复

#### OcrTestPanel 添加到预设按钮数据库未初始化问题
- **修复"数据库未初始化"错误提示**
  - 问题：OCR识别测试界面的[添加到预设]按钮报错"数据库未初始化"，但实际上数据库已经初始化完成
  - 原因：OcrTestPanel 通过 setDatabaseService() 注入 dbService，但在某些场景下注入的 dbService 可能为 null
  - 修复：添加 getDbService() 方法，优先从 GlobalKvCore 获取数据库服务
  - 实现：与 StringPresetPanel 的实现方式保持一致，确保代码一致性

#### OcrResultPanel 坐标系统修复
- **修复图像原点（红色圆点）绘制位置不正确的问题**
  - 问题：图像原点红点绘制位置与图像左上角不重合
  - 原因：混淆了面板坐标（Panel）和画布坐标（Canvas）
  - 修复：
    - toggleImageOriginMode() 现在使用 getImageX()/getImageY() 获取画布坐标
    - handleImageOriginClick() 正确转换鼠标坐标到画布坐标系
    - drawImageOrigin() 动态更新原点位置以适应图像平移/缩放
  - 新增坐标转换方法：panelToCanvas(), canvasToPanel(), canvasToImage(), imageToCanvas()

### 文档
- 更新坐标系统详解文档，添加画布坐标空间说明和实际案例

### 构建
- 使用build.sh打包fatjar v1.0.53

---

## [1.0.52] - 2026-04-12

### 修复

#### 坐标系一致性修复
- **修复光标坐标与图像左上角坐标不匹配的问题**
  - 问题：光标(80, 254)与图像左上角(491, 496)显示不一致
  - 原因：坐标计算混用了相对于面板和相对于canvasPanel的坐标
  - 修复：
    - `getImageX/Y()` 返回相对于canvasPanel的坐标（用于绘制）
    - `getImageX/YInPanel()` 返回相对于OcrResultPanel的坐标（用于鼠标事件）
    - 统一所有鼠标事件处理使用相对于面板的坐标
    - 修复`zoomAtPoint`和`resetZoom`使用canvasPanel尺寸

### 新增功能

#### 图像原点校准功能
- **添加`图像原点`按钮**
  - 位于`选择元素`按钮右侧
  - 点击后进入图像原点模式
  
- **图像原点显示**
  - 在图像左上角显示红色圆点（带白色边框和十字线）
  - 显示"原点"标签提示
  
- **原点校准功能**
  - 点击红色原点后，自动调整图像偏移量
  - 使鼠标屏幕坐标与图像屏幕坐标相等
  - 校准完成后自动退出原点模式

### 构建
- 使用build.sh打包fatjar v1.0.52

---

## [1.0.51] - 2026-04-12

### 修复

#### OCR识别测试坐标问题修复
- **修复屏幕坐标系下图像左上角坐标显示不正确的问题**
  - 问题：光标选择"屏幕坐标系"时，图像左上角Y坐标比实际多约50像素
  - 原因：`updateImageScreenPosition()`使用`getLocationOnScreen()`获取的是整个面板位置，而非画布面板位置
  - 修复：添加`canvasPanel`成员变量，使用画布面板的`getLocationOnScreen()`获取准确位置
  - 影响：屏幕坐标系下光标坐标与图像坐标现在能够正确对齐

### 构建
- 使用build.sh打包fatjar v1.0.51

---

## [1.0.50] - 2026-04-12

### 改进

#### OcrResultPanel坐标空间逻辑优化
- **完善面板布局注释**
  - 将无意义的TODO注释改为清晰的说明文字
  - 明确标注状态栏标签的添加顺序

- **优化鼠标范围检查逻辑**
  - 仅在"图像坐标空间"模式下检查鼠标是否在图像范围内
  - 其他坐标空间（屏幕/归一化）下正常显示坐标信息
  - RGB值仅在鼠标位于图像范围内时显示，否则显示N/A
  - 提升用户体验，避免非图像坐标空间下的误报

### 构建
- 使用build.sh打包fatjar v1.0.50

---

## [1.0.49] - 2026-04-12

### 新增功能

#### OcrResultPanel图像屏幕坐标显示
- **添加图像左上角在屏幕上的坐标显示**
  - 新增`imageScreenPosLabel`标签组件
  - 显示格式：`<图像> 坐标: (x, y, "Screen")`
  - 实时更新图像在屏幕上的绝对坐标
  
- **实现`updateImageScreenPosition()`方法**
  - 计算图像左上角相对于屏幕的坐标
  - 使用`getLocationOnScreen()`获取组件屏幕位置
  - 结合图像在画布上的偏移量计算最终坐标

- **在paintComponent中自动更新**
  - 每次重绘时更新图像屏幕坐标显示
  - 确保坐标信息始终准确

### 构建
- 使用build.sh打包fatjar v1.0.49

---

## [1.0.48] - 2026-04-12

### 新增功能

#### [添加到预设]按钮功能
- **OcrTestPanel添加[添加到预设]按钮**
  - 位于[截图保存字符]按钮旁边，海绿色背景
  - 弹出预设选择对话框，显示所有可用预设
  - 如果没有预设，可创建新预设
  - 将当前OCR结果（图片、字符位置、OCR参数）保存到选中的预设
  
- **自动计算字符相对位置**
  - 从当前识别结果生成字符相对位置信息
  - 按X坐标排序（从左到右）
  - 计算每个字符相对于首字符的偏移量
  - 设置默认范围（±30%）
  - 计算与前字符的间距

- **更新预设数据**
  - 更新正确字符串
  - 更新标准图片（当前处理的图片）
  - 更新OCR参数（从ConfigManager获取）
  - 更新字符相对位置列表

#### 使用预设功能完善
- **StringPresetPanel.useSelectedPreset()完善**
  - 将预设字符串填入OCR识别测试的正确字符串输入框
  - 自动切换到OCR识别测试标签页
  - 显示成功提示信息

- **MainFrame添加公共方法**
  - `getOcrTestPanel()`：获取OCR测试面板实例
  - `getTabbedPane()`：获取主标签页面板

- **OcrTestPanel添加公共方法**
  - `setCorrectString(String text)`：设置正确字符串
  - `getCorrectStringField()`：获取正确字符串输入框

### 构建
- 使用build.sh打包fatjar v1.0.48

---

## [1.0.47] - 2026-04-12

### 新增功能

#### 字符串预设字符相对位置功能
- **创建CharRelativePosition模型类**
  - 存储字符索引、相对X/Y偏移量
  - 存储宽度、高度及范围（最小/最大值）
  - 存储与前字符间距及范围
  - 自动检测字符类型（全角/半角/汉字/英文/标点）
  - 支持字符类型：全角、半角大写、半角小写、半角数字、标点、汉字、其他

- **StringPreset模型增强**
  - 添加`List<CharRelativePosition>`字符相对位置列表
  - 添加`charRelativePositionEnabled`启用状态
  - 支持JSON格式存储（用于数据库和XML）
  - 添加`generateDefaultCharRelativePositions()`方法从字符串生成默认位置

- **StringPresetPanel界面增强**
  - 在OCR参数列和文本定位脚本列之间添加"字符相对位置"列
  - 新增"字符相对位置"编辑标签页
  - 表格形式显示字符位置信息（索引、字符、类型、相对X/Y、宽度、高度、间距）
  - 支持从字符串自动生成位置信息
  - 支持手动编辑各字符的位置参数

- **数据存储支持**
  - XmlPresetStorage支持字符相对位置的导入导出（JSON格式）
  - DatabaseService添加`char_relative_position_enabled`和`char_relative_positions_json`字段
  - 更新保存/查询方法支持新字段

- **TextBlobDetector字符定位增强**
  - 添加`detectByRelativePosition()`方法基于相对位置定位所有字符
  - 添加`findBestMatchingRegion()`方法在预计位置附近搜索最佳匹配区域
  - 添加`CharLocationParams`类根据字符类型（全角/半角）提供不同定位参数
  - 支持处理错误联通域（如"AB"被识别为一个连通域的情况）
  - 支持根据首字符实际尺寸与预设尺寸的缩放比例进行自适应定位

### 构建
- 使用build.sh打包fatjar v1.0.47

---

## [1.0.46] - 2026-04-12

### 新增功能

#### 字符串预设图像预览功能
- **添加图像预览区**
  - 在`刷新状态`按钮左侧添加图像预览区
  - 选中表格中的预设项后自动显示预设的图片
  - 图片自动缩放适应预览窗口（保持宽高比）
  - 无图片时显示"无图片"占位符

#### 标准图片Base64存储支持
- **StringPreset模型增强**
  - 添加`getStandardImageBase64()`方法，将标准图片转换为Base64编码
  - 添加`setStandardImageFromBase64()`方法，从Base64编码还原图片
  - 支持XML导出时自动导出Base64图像数据
  - 支持XML导入时自动解析Base64图像数据

#### XML导入导出增强
- **导出XML时包含图片数据**
  - 导出预设时自动将标准图片以Base64格式嵌入XML
  - XML字段：`StandardImageBase64`
  
- **导入XML时解析图片数据**
  - 导入预设时自动解析Base64图片数据并还原为二进制
  - 兼容旧版本XML（无图片数据时正常导入）

### 构建
- 使用build.sh打包fatjar v1.0.46

---

## [1.0.45] - 2026-04-11

### 新增功能

#### 全局数据交换类 GlobalKvCore
- **参考GlobalKVCore.cs实现Java版本**
  - 创建`GlobalKvCore`工具类，提供线程安全的全局键值对存储
  - 支持`get(key)`精准读取和`getMany(pattern)`模糊匹配（支持*和?通配符）
  - 支持`setValue(key, value)`线程安全写入
  - 支持基本类型自动转换（Integer, Long, Double, Float, Boolean, String）

### 修复

#### 数据库服务突然为null的问题
- **使用GlobalKvCore存储数据库服务**
  - MainFrame初始化时将数据库服务存储到GlobalKvCore
  - StringPresetPanel使用时从GlobalKvCore获取数据库服务
  - 避免由于引用丢失导致的数据库服务为null问题

---

## [1.0.44] - 2026-04-11

### 新增功能

#### MainFrame单例模式
- **添加MainFrame.Instance全局访问点**
  - 新增`MainFrame.getInstance()`静态方法，可全局安全访问MainFrame实例
  - 支持从任何位置访问主窗口的成员和方法

### 修复

#### 字符串预设面板初始化时机问题
- **修复stringPresetPanel为null的问题**
  - 调整初始化顺序：`initComponents()`先于`initServices()`执行
  - 使用`MainFrame.getInstance()`访问stringPresetPanel，避免null引用
  - 添加延迟重试机制，确保数据库服务最终能正确注入

---

## [1.0.43] - 2026-04-11

### 新增功能

#### 字符串预设XML存储方式
- **添加存储方式设置**
  - 在软件设置面板中添加"字符串预设存储方式"选项
  - 支持两种方式：XML文件（默认）/ 数据库
  - 实时切换存储方式，无需重启软件

- **实现XML伪数据库存储**
  - 创建XmlPresetStorage类，实现XML文件存储
  - XML文件存储在数据目录/data文件夹（string_presets.xml）
  - 支持完整的CRUD操作（增删改查）
  - 使用结构化数据格式，便于手动编辑

- **字符串预设面板适配**
  - 根据配置自动选择存储方式
  - XML模式下显示"XML文件"状态和"已连接(XML)"
  - 数据库模式下显示重连按钮，XML模式下隐藏
  - 导入/导出功能适配两种存储方式

---

## [1.0.42] - 2026-04-11

### 修复

#### 重连数据库功能修复
- **修复"功能未配置"提示问题**
  - 重新编译并打包，确保setReconnectCallback方法正确包含在jar中
  - 验证类文件包含reconnectCallback字段和相关方法

---

## [1.0.41] - 2026-04-11

### 新增功能

#### 字符串预设界面重连数据库功能
- **添加重连数据库按钮**
  - 在状态栏添加"重连数据库"按钮
  - 按钮仅在数据库未连接或连接异常时启用
  - 连接成功时自动禁用按钮

- **实现重连逻辑**
  - 关闭旧的数据库连接
  - 重新初始化DatabaseService
  - 更新OCR测试面板和字符分类面板的数据库服务
  - 刷新预设列表显示
  - 在后台线程中执行重连，避免界面卡顿

- **添加重连回调机制**
  - StringPresetPanel添加`setReconnectCallback()`方法
  - MainFrame中配置重连逻辑
  - 支持错误处理和用户提示

---

## [1.0.40] - 2026-04-11

### 修复

#### 字符串预设数据库连接问题修复
- **修复数据库服务注入时机问题**
  - 在MainFrame中使用SwingUtilities.invokeLater确保数据库服务注入在EDT线程执行
  - 添加详细的日志记录，便于排查注入过程
  - 修复stringPresetPanel可能为null时的空指针问题

- **改进数据库连接状态检测**
  - 优化updateDbStatus()方法，添加public访问修饰符
  - 添加更详细的连接状态日志
  - 修复状态更新延迟问题

---

## [1.0.39] - 2026-04-11

### 新增功能

#### 字符串预设界面状态显示
- **数据库连接状态显示**
  - 在界面顶部添加状态栏，实时显示数据库连接状态
  - 三种状态：未连接（红色）、连接中（橙色）、已连接（绿色）
  - 状态自动更新，数据库服务注入时刷新

- **数据库初始化状态显示**
  - 显示当前预设记录数量
  - 记录数随增删改操作实时更新

- **手动刷新功能**
  - 添加"刷新状态"按钮，可手动检查数据库连接状态
  - 加载预设后自动更新状态显示

---

## [1.0.38] - 2026-04-11

### 修复

#### 字符串预设保存问题修复
- **修复保存预设时的NullPointerException**
  - 添加脚本编辑区域空值检查，防止用户未打开脚本标签页时保存失败
  - 安全地读取脚本值（如果字段为null则跳过）
  - 修复数据库服务未初始化时的错误提示

#### 改进错误提示
- **更友好的错误信息**
  - 数据库服务未初始化时显示"数据库服务未初始化，请等待数据库初始化完成"
  - 保存失败时保留编辑对话框不关闭，方便用户重试

---

## [1.0.37] - 2026-04-10

### 修复

#### 字符串预设数据库问题修复
- **修复数据库表未创建问题**
  - 移除构造函数中过早的`loadPresets()`调用
  - 添加数据库服务空值检查后再加载预设
  - 添加数据库初始化日志，确认表创建成功

#### 字符串预设保存功能修复
- **修复保存按钮无响应问题**
  - 修复脚本编辑区域引用错误导致的NullPointerException
  - 添加数据库服务空值检查
  - 添加预设名称必填验证
  - 保存失败时显示错误提示，不关闭对话框
  - 保存成功后自动刷新预设列表

### 新增功能

#### 菜单栏编辑选项增强
- **新增菜单项**
  - `显示/隐藏标签`：切换OCR识别测试界面的标签显示
  - `显示/隐藏得分`：切换OCR识别测试界面的得分显示

#### 字符串预设功能
- **菜单栏参数菜单增强**
  - 添加`字符串预设`选项

- **字符串预设界面**
  - 表格形式显示预设列表：序号、预设名称、字符串、有图片、OCR参数、文本定位脚本、字符分类脚本、OCR后处理脚本
  - 支持双击编辑预设

- **预设编辑功能**
  - 基本信息编辑：预设名称（可编辑）、字符串、标准图片
  - OCR参数编辑：二值化阈值、Gamma系数、面积范围、宽高范围
  - 脚本编辑：文本定位脚本、字符分类脚本、OCR后处理脚本（支持启用/禁用）
  - 序号字段只读，其他字段可编辑

- **预设操作按钮**
  - `使用预设`：将预设值应用到各个界面
  - `编辑预设`：打开编辑对话框
  - `保存预设`：保存当前编辑的预设
  - `新增预设`：创建新预设
  - `删除预设`：删除选中的预设
  - `导出XML`：将所有预设导出为XML文件
  - `导入XML`：从XML文件导入预设

- **数据存储**
  - 预设数据存储在H2数据库的`string_presets`表中
  - 支持标准图片的二进制存储

---

## [1.0.36] - 2026-04-10

### 新增功能

#### 区域编辑功能 - 拆分与合并
- **菜单栏编辑菜单增强**
  - 添加`拆分区域`选项
  - 添加`合并区域`选项

- **合并区域功能**
  - 点击菜单进入合并模式
  - 鼠标依次点击两个要合并的区域框
  - 自动合并两个区域为一个新区域（取并集）
  - 第一个选中的区域显示绿色高亮

- **拆分区域功能**
  - 点击菜单进入拆分模式
  - 显示紫色虚线竖线跟随鼠标移动（类似CAD智能修剪工具）
  - 单击后在竖线位置将区域拆分为左右两个区域
  - 仅拆分包含拆分线的区域

---

## [1.0.35] - 2026-04-10

### 新增功能

#### 行基线显示功能
- **4线本风格行基线**
  - 顶线：橙色虚线（使用汉字字符最高点确定）
  - 中线1：绿色虚线（33%位置）
  - 中线2：粉色虚线（66%位置）
  - 基线：黄色虚线（使用非下沉字符最低点确定）
  - 支持倾斜行基线（4条线保持平行）
  - 通过菜单栏`编辑`->`显示/隐藏行基线`切换

#### 坐标空间选择与鼠标信息显示
- **坐标空间选择下拉菜单**
  - 位于控制栏`缩放:`左侧
  - 支持三种坐标空间：
    - `图像坐标空间`：显示图像内的像素坐标 (x, y)
    - `屏幕坐标空间`：显示屏幕绝对坐标 (x, y)
    - `归一化坐标空间`：显示0-1范围的归一化坐标 (0.xxx, 0.xxx)

- **鼠标坐标和RGB值实时显示**
  - 位于底部状态栏左侧
  - 实时显示鼠标在当前坐标空间下的坐标
  - 显示鼠标所在像素的RGB值
  - 灰度图自动检测并显示单一灰度值

---

## [1.0.34] - 2026-04-10

### 新增功能

#### 菜单栏重构
- **添加菜单栏**
  - 新增`设置`菜单：包含`MQTT设置`、`路径设置`、`软件设置`
  - 新增`参数`菜单：包含`OCR参数`（与标签栏中的OCR参数按钮功能相同）
  - 新增`编辑`菜单：包含`显示/隐藏行基线`、`显示/隐藏单元格`（仅在OCR识别测试界面时可用）
  - 新增`脚本`菜单：包含`文本定位脚本`、`字符分类脚本`、`OCR后处理脚本`
  - 新增`更多`菜单：包含`编程`、`关于`

- **标签栏优化**
  - 保留`OCR识别测试`、`字符分类测试`、`图片预处理`、`软件日志`、`OCR参数`标签页
  - 其他设置页面通过菜单栏访问，简化界面布局

---

## [1.0.33] - 2026-04-10

### 新增功能

#### OCR识别测试导出JSON功能
- **添加`[导出JSON]`按钮**
  - 在`截图保存字符`按钮下方新增`[导出JSON]`按钮
  - 使用棕色背景区分于其他按钮
  - 复用`XAnyLabelingJson`模型类作为DTO，与`[打开JSON]`功能格式兼容

- **导出功能实现**
  - 导出路径：`图片文件夹/JSON标注/`
  - 文件名格式：`ocr_result_{yyyyMMddHHmmss}.json`
  - 同时导出配套图片文件：`ocr_result_{yyyyMMddHHmmss}.png`
  - 包含当前识别结果（shapes）和图片base64编码数据
  - 支持XAnyLabeling格式完整字段：version、flags、shapes、imagePath、imageData、imageWidth、imageHeight

---

## [1.0.32] - 2026-04-10

### 新增功能

#### 文本定位脚本增强
- **Lua脚本支持获取所有OCR参数和correctString**
  - 修改`locate_text`函数签名：`locate_text(imageInfo, regions, ocrParams, correctString)`
  - `ocrParams`包含：grayscale, gamma, minArea, maxArea, minWidth, maxWidth, minHeight, maxHeight, maxMergeWidth, isFullWidth
  - `correctString`为用户提供的正确字符串（可能为nil）
  
- **全角/半角智能判断**
  - 根据`correctString`首字符自动判断文本类型
  - ASCII字符（0-127）判定为半角，其他为全角
  - 全角文本使用`maxWidth`作为框合并阈值
  - 半角文本使用`maxWidth/2`作为框合并阈值
  
- **参考Word排版算法的框合并**
  - 按X坐标排序区域
  - 计算相邻区域间距和合并后总宽度
  - 间距小于5像素且合并后宽度不超过阈值时合并
  - 支持Lua脚本自定义合并逻辑

#### 代码改进
- **TextBlobDetector增强**
  - 添加`setLuaScriptEngine()`方法设置Lua脚本引擎
  - 添加`setCorrectString()`方法设置正确字符串
  - 添加`isFullWidthChar()`和`isFullWidthText()`工具方法
  - 添加`getMaxMergeWidth()`根据文本类型返回合并阈值
  - 在`detect()`方法中调用Lua脚本进行后处理
  
- **LuaScriptEngine增强**
  - 修改`executeTextLocation()`支持ocrParams和correctString参数
  - 更新默认脚本模板，添加OCR参数和correctString使用示例
  - 添加`isFullWidthChar()`和`isFullWidthText()`静态工具方法
  
- **OcrTestPanel集成**
  - 添加`setLuaScriptEngine()`方法
  - 在`runOcr()`中传递correctString和Lua脚本引擎给TextBlobDetector
  
- **MqttService集成**
  - 添加`setTextLocationScriptEngine()`方法
  - 在`processOcrMessage()`中传递correctString和Lua脚本引擎
  
- **MainFrame集成**
  - 调整初始化顺序，先创建文本定位脚本面板
  - 将Lua脚本引擎传递给OcrTestPanel和MqttService

---

## [1.0.31] - 2026-04-09

### 改进

#### 飞桨OCR模型文件嵌入
- **将ONNX模型文件打包到fatjar中**
  - det.onnx（检测模型）、rec.onnx（识别模型）、dict.txt（字典文件）现在嵌入到fatjar中
  - 新增`ModelResourceLoader`工具类，自动从classpath提取模型文件到临时目录
  - 修改`OcrTestPanel.runPaddleOcr()`方法，使用嵌入式模型文件
  - 用户无需手动配置models目录，简化部署流程
  - 临时文件在应用退出时自动清理

---

## [1.0.30] - 2026-04-09

### 修复

#### OCR参数保存逻辑
- **修复参数自动保存问题**
  - 移除OCR参数界面中各控件的自动保存监听器
  - 参数修改后不再自动写入配置文件
  - 只有在用户点击"保存设置"按钮后才保存到配置文件
  - 避免软件启动时意外覆盖用户配置

---

## [1.0.29] - 2026-04-09

### 新增功能

#### 飞桨OCR识别
- **集成honda-paddleocr-lib**
  - 添加`[飞桨OCR识别]`按钮到OCR识别测试界面
  - 调用飞桨OCR引擎进行文本识别
  - 自动将识别结果填入`正确字符串`输入框
  - 在结果面板显示识别区域框和置信度
  - 支持多行文本识别和自动排序

#### 依赖更新
- **添加honda-paddleocr-lib**
  - 复制fatjar-lib到libs目录
  - 复制ONNX模型文件(det.onnx, rec.onnx, dict.txt)到models目录

---

## [1.0.28] - 2026-04-09

### 改进

#### OCR参数界面
- **优化参数加载逻辑**
  - 修复界面初始化时滑块标签值不更新的问题
  - 现在二值化阈值和Gamma系数的标签会正确显示配置文件中的值

#### OCR识别测试界面
- **按钮文本修改**
  - `[运行OCR识别]`按钮文本改为`[经典OCR识别]`

---

## [1.0.27] - 2026-04-08

### 修复

#### OCR参数界面
- **修复参数重置问题**
  - 修复OCR参数界面启动时重置为默认参数的问题
  - 现在启动时会正确从配置文件加载已保存的参数
  - 在`ConfigManager.loadDefaults()`中补充了缺失的默认配置项

#### 智能尺寸工具
- **修复鼠标坐标映射问题**
  - 修复鼠标在画布上的坐标无法正确映射到图像坐标系的问题
  - 现在鼠标可以正确选中最临近的线段
  - 修改`SmartDimensionTool.setTransform()`方法，直接接收图像在屏幕上的绝对坐标

---

## [1.0.26] - 2026-04-08

### 新增功能

#### OCR识别测试界面增强
- **添加显示/隐藏得分按钮**
  - 在字符右上角绘制相似度得分（保留2位小数）
  - 兼容JSON中的score字段
  - 支持动态切换显示/隐藏

- **正确字符串输入区优化**
  - 从单行文本框改为5行文本区域
  - 支持多行输入和自动换行
  - 使用等宽字体显示，便于对齐

### 改进

#### OCR识别测试界面简化
- 移除OCR参数设置区域，统一使用"OCR参数"界面的设置
- 减少同步维护量，避免参数不一致问题
- 参数修改后下次运行OCR时自动生效，无需重启软件

---

## [1.0.25] - 2026-04-08

### 新增功能

#### OCR参数增强
- **行间距/列间距参数语义优化**
  - 将"行间距范围"和"列间距范围"修改为更清晰的语义：
    - 行间距最小值 / 行间距最大值
    - 列间距最小值 / 列间距最大值

- **字符内部空隙参数**
  - 新增`字符内部垂直方向空隙最大值`参数，默认为 MaxHeight - MinHeight (197)
  - 新增`字符内部水平方向空隙最大值`参数，默认为 MaxWidth - MinWidth (497)
  - 用于解决汉字"二"和"川"等多笔画字符被分割成多个区域的问题
  - 当两个区域之间的空隙小于阈值时，自动合并为同一个字符

- **忽略图片边界区域参数**
  - 新增`忽略图片边界区域`参数，默认为5像素
  - 用于过滤图片边缘的噪声区域

---

## [1.0.24] - 2026-04-08

### 新增功能

#### 图片保存功能
- **图片预处理界面添加保存图片按钮**
  - 保存处理后的二值化图片到图片目录的`预处理`文件夹
  - 文件名格式：`preprocess_时间戳.png`
  - 保存当前参数设置下的处理结果

- **字符分类测试界面添加保存图片按钮**
  - 保存当前加载的图片到图片目录的`分类测试`文件夹
  - 按目标字符分类存放（使用与字符截图相同的文件夹命名规则）
  - 文件名格式：`字符_时间戳.png`

---

## [1.0.23] - 2026-04-07

### 新增功能

#### MQTT嵌套JSON格式支持
- **兼容应用端的嵌套JSON格式**：应用端在请求JSON外又包了一层JSON
  - 请求格式：`{"_mqtt_id":"xxx","payload":"实际业务JSON"}`
  - 自动检测并解析嵌套格式，提取业务数据和_mqtt_id
  - 响应时按照相同格式嵌套回复，包含相同的_mqtt_id
  - 支持OCR、字符分类、图像二值化三种MQTT服务
  - 新增`MqttPayloadWrapper`类处理嵌套JSON的解析和包装

---

## [1.0.22] - 2026-04-07

### 新增功能

#### 智能尺寸工具增强
- **添加右键菜单选择功能**：右键点击显示半径30像素内的所有候选边线
  - 菜单显示字符标签、边线类型（左/右/上/下）和距离
  - 点击菜单项选择对应边线，解决自动吸附可能选错的问题
  - 候选边线以黄色高亮显示，搜索范围以半透明圆圈显示

#### MQTT服务设置
- **添加imageData字段返回设置**：`mqtt.returnImageData`（默认关闭）
  - 默认不返回imageData以节省网络带宽
  - 可在MQTT设置界面中勾选启用
  - 启用后OCR响应将包含可视化结果图片的base64编码

### 修复

#### OCR结果图片错误
- **修复图像格式转换问题**：`bufferedImageToMat`方法现在正确处理各种图像格式
  - 支持PNG、JPEG等多种格式
  - 自动转换为BGR格式进行处理
  - 确保可视化结果与原始图像一致

---

## [1.0.21] - 2026-04-07

### 修复

#### MQTT响应问题修复
- **添加连接状态检查**：发送响应前检查MQTT客户端是否连接
- **优化日志输出**：明确显示发送成功或失败的状态

#### MQTT主题拼写错误
- **修复OCR主题拼写错误**：`/ampinsepction/ocr/260407/raw` → `/ampinspection/ocr/260407/raw`
- **修复分类主题拼写错误**：`/aminspection/classify/260407/raw` → `/ampinspection/classify/260407/raw`
- 更新配置文件和默认配置值

---

## [1.0.20] - 2026-04-07

### 新增功能

#### 智能尺寸工具（CAD风格）
- **新增"智能尺寸"按钮**，支持类似CAD的智能尺寸功能
- **自动吸附选择**：鼠标靠近字符方框边线时自动吸附高亮显示
- **边线选择**：可选择两个字符方框的左/右/上/下边线
- **距离显示**：
  - 显示水平距离（像素）或垂直距离（像素）
  - 尺寸线带箭头标记
  - 文字显示在尺寸线上方，带黑色背景便于阅读
- **操作方式**：
  1. 点击"智能尺寸"按钮进入尺寸模式
  2. 点击第一个字符方框的边线
  3. 点击第二个字符方框的边线
  4. 自动计算并显示距离
  5. 点击"清除尺寸"清除所有标注

#### 处理时间和参数显示
- **OCR识别测试**：显示处理时间(ms)和已应用的OCR参数
  - 显示：二值化阈值、Gamma、面积范围、宽高范围
- **字符分类测试**：显示处理时间(ms)和参数
  - 显示：灰度值、Gamma系数、相似度百分比
- **图像预处理**：显示处理时间(ms)和参数
  - 显示：阈值、Gamma、偏移量、是否反色

#### MQTT消息日志增强
- **接收消息日志**：打印MQTT接收的消息主题和内容（JSON截断显示）
- **发送消息日志**：打印MQTT发送的响应消息
- 避免日志过长，超过500字符自动截断

### 改进

#### OCR测试结果图布局优化
- **信息区域外扩**：将图例和统计信息绘制在图像上方的外扩区域
- **不遮挡原图**：检测结果框和文字信息不再覆盖在原图上
- **更清晰的信息展示**：
  - 顶部100像素高度的信息区域
  - 显示Text/Blob/Missed数量统计
  - 显示图像尺寸信息
  - 深色背景配彩色图例，更易阅读

---

## [1.0.19] - 2026-04-07

### 改进

#### 字符截图文件夹命名规则优化
- **所有ASCII字符（0-127）统一使用ASCII码作为文件夹名称**
  - 例如：字符 "A" → 文件夹名 `_65_`
  - 例如：字符 "?" → 文件夹名 `_63_`
  - 例如：空格 → 文件夹名 `_32_`
- **非ASCII字符（如中文）保持原样**
  - 例如：字符 "中" → 文件夹名 `中`
  - 例如：字符 "文" → 文件夹名 `文`
- **拖入JSON后截图同样适用新规则**
  - JSON中的label字符通过相同的规则处理
  - ASCII字符转为ASCII码文件夹名，中文保持原样

#### 修复JSON截图label使用问题
- 修复拖入JSON后截图时，没有使用JSON中已有label的问题
- 现在截图时优先使用优先级：
  1. `correctString`输入的字符（如果提供且长度足够）
  2. Shape自带的label（如从JSON加载的标注，且不为"?"）
  3. 如果没有以上，放入`未分类`文件夹

---

## [1.0.18] - 2026-04-06

### 新增功能

#### OCR识别测试截图功能
- 新增`截图保存字符`按钮，支持将识别到的每个字符区域截图保存
- **保存位置**: 数据目录下的 `character_images/` 文件夹
- **文件夹命名规则**:
  - 根据`correctString`输入的字符创建对应名称的文件夹
  - 如果未输入`correctString`或字符数量不足，放入`未分类`文件夹
  - 特殊字符（如 `:`, `/`, `\`, `*`, `?`, `"`, `<`, `>`, `|` 等操作系统保留字符）使用ASCII码表示（如 `_58_` 表示冒号）
- **文件命名规则**: `字符名称_时间戳.png`
- 支持从JSON加载的标注区域截图
- 支持OCR识别后的检测结果截图
- 截图过程有详细日志记录

---

## [1.0.17] - 2026-04-06

### 新增功能

#### OCR识别算法增强
- 新增`强制宽度分割`功能
  - 参考HALCON的PartitionRectangle算法实现
  - 当自动分割的字符数量与预期不符时，使用固定宽度强制分割
  - 可配置分割宽度参数（默认31像素）
  - 在OCR参数设置面板添加启用开关和宽度设置
  - 配置项: `ocr.forceWidthSplit`, `ocr.forceSplitWidth`

- 新增`多行排序`功能
  - 参考HALCON的多行排序策略：先按行排序，再对每行内的字符按列排序
  - 支持多行文本的层次化排序
  - 可配置行间距阈值参数（默认20像素）
  - 在OCR参数设置面板添加启用开关和阈值设置
  - 配置项: `ocr.multiLineSort`, `ocr.rowGapThreshold`

#### OCR后处理Lua脚本
- 新增`OCR后处理脚本`标签页
- 提供Lua脚本实现等价功能：
  - `force_width_split(regions, splitWidth)` - 强制宽度分割函数
  - `sort_multiline(regions, rowGapThreshold)` - 多行排序函数
  - `post_process(imageInfo, regions, options)` - 主处理函数
- 脚本可调试，支持实时验证和测试
- 配置项: `lua.ocrPostProcessScript`, `lua.ocrPostProcessScriptEnabled`

#### OCR测试面板增强
- 在OCR识别测试面板添加新功能控件
  - 强制宽度分割启用/禁用复选框
  - 分割宽度调节（5-200像素）
  - 多行排序启用/禁用复选框
  - 行间距阈值调节（5-100像素）
- 参数自动保存到配置

---

## [1.0.16] - 2026-04-06

### 新增功能

#### 软件设置标签页
- 新增`软件设置`标签页，集中管理软件启动相关配置
- **MQTT自动启动设置**: 控制软件启动时是否自动启动MQTT Broker服务
  - 配置项: `app.autoStartMqtt` (默认: true)
  - 可在界面勾选/取消勾选
- **窗口尺寸设置**: 设置软件启动时的窗口大小
  - 配置项: `app.windowWidth` (默认: 1000), `app.windowHeight` (默认: 750)
  - 保持4:3比例，支持800x600到1920x1440范围
  - 提供快速预设按钮: 小(800x600)、默认(1000x750)、大(1200x900)、超大(1600x1200)
  - 修改后重启软件生效
- 所有设置自动持久化到配置文件

---

## [1.0.15] - 2026-04-06

### 修复

#### 字符分类空指针异常
- 修复字符分类测试时出现的 `Cannot invoke "com.honda.paroledipinte.db.DatabaseService..."` 空指针异常
- 问题原因：`MainFrame` 中 `initComponents()` 在 `initServices()` 之前调用，导致 `OcrTestPanel` 和 `ClassifyTestPanel` 创建时传入的 `dbService` 为 null
- 解决方案：
  - 调整 `MainFrame` 构造函数中的初始化顺序，先调用 `initServices()` 再调用 `initComponents()`
  - 修改 `OcrTestPanel` 和 `ClassifyTestPanel` 构造函数，不再要求传入 `DatabaseService` 参数
  - 新增 `setDatabaseService()` 方法，在数据库服务初始化完成后注入依赖
  - 在 `ClassifyTestPanel.runClassify()` 方法中添加 `dbService` 空值检查，提供友好的错误提示

---

## [1.0.14] - 2026-04-06

### 新增功能

#### 图像处理时间日志
- 为图像处理方法添加处理时间(ms)日志打印
- 覆盖的方法包括：
  - `ImageProcessor.preprocess()` - 图像预处理（动态阈值二值化）
  - `TextBlobDetector.preprocessImage()` - 文本检测预处理
  - `TextBlobDetector.detect()` - 文本区域检测
  - `MqttService.processOcrMessage()` - OCR消息处理（总时间）
  - `MqttService.processClassifyMessage()` - 字符分类处理（总时间）
  - `MqttService.processBinarizeMessage()` - 图像二值化处理（总时间）
- 日志格式：`[方法名] 执行完成，处理时间: Xms`

---

## [1.0.13] - 2026-04-06

### 修复

#### 图片预处理界面图像二值化显示问题
- 修复`图片预处理`->`图像二值化`无法显示处理后图片的问题
- 问题原因：`updateProcessedImage`方法中缺少对`currentMat`状态的详细日志记录，且`displayImage`方法未在EDT线程中执行UI更新
- 解决方案：
  - 添加详细的诊断日志，区分`currentMat`为null和empty的情况
  - 使用`SwingUtilities.invokeLater`确保UI更新在EDT线程中执行
  - 修复`preprocessWithParams`中Mat对象生命周期管理，及时释放中间对象
  - 优化`gammaCorrection`方法，添加完成日志
  - 改进`loadImageFile`方法，使用`Graphics2D`并正确释放资源

#### 补充JavaCV依赖配置
- 在`build.gradle`中补充缺失的JavaCV native包和OpenBLAS依赖
- 新增依赖：
  - `org.bytedeco:javacv:1.5.11` - JavaCV核心库
  - `org.bytedeco:javacpp:1.5.11` - JavaCPP运行时
  - `org.bytedeco:opencv:4.10.0-1.5.11` - OpenCV核心库
  - `org.bytedeco:opencv:4.10.0-1.5.11:macosx-arm64` - macOS ARM64 native库
  - `org.bytedeco:opencv:4.10.0-1.5.11:macosx-x86_64` - macOS x86_64 native库
  - `org.bytedeco:opencv:4.10.0-1.5.11:windows-x86_64` - Windows x86_64 native库
  - `org.bytedeco:opencv:4.10.0-1.5.11:linux-x86_64` - Linux x86_64 native库
  - `org.bytedeco:openblas:0.3.28-1.5.11` - OpenBLAS核心库（OpenCV依赖）
  - `org.bytedeco:openblas:0.3.28-1.5.11:macosx-arm64` - macOS ARM64 OpenBLAS
  - `org.bytedeco:openblas:0.3.28-1.5.11:macosx-x86_64` - macOS x86_64 OpenBLAS
  - `org.bytedeco:openblas:0.3.28-1.5.11:windows-x86_64` - Windows x86_64 OpenBLAS
  - `org.bytedeco:openblas:0.3.28-1.5.11:linux-x86_64` - Linux x86_64 OpenBLAS
- 支持平台：macOS ARM64/x86_64、Windows x86_64、Linux x86_64

#### 依赖管理改进
- 修改`build.gradle`，将所有依赖改为从Maven Central获取
- 原本地flatDir依赖改为Maven坐标：
  - `flatlaf-3.5.4.jar` → `com.formdev:flatlaf:3.5.4`
  - `gson-2.10.1.jar` → `com.google.code.gson:gson:2.10.1`
  - `h2-2.2.224.jar` → `com.h2database:h2:2.2.224`
  - `luaj-jse-3.0.1.jar` → `org.luaj:luaj-jse:3.0.1`
  - `org.eclipse.paho.client.mqttv3-1.2.5.jar` → `org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5`
  - `rsyntaxtextarea-3.3.4.jar` → `com.fifesoft:rsyntaxtextarea:3.3.4`
- 使用`./gradlew copyDependencies`任务下载所有依赖到`libs`目录，支持离线编译
- 打包fatjar（约216MB），包含所有依赖和native库

---

## [1.0.12] - 2026-04-06

### 新增功能

#### 日志界面页码跳转功能
- `软件日志`界面底部控制面板新增页码跳转功能
- 添加页码输入框，支持直接输入页码进行跳转
- 添加"跳转"按钮，点击或按回车键执行跳转
- 输入无效页码时显示友好提示信息

### 修复

#### 图片预处理界面图片显示问题
- 修复`图片预处理`界面打开图片后原图不显示的问题
- 修复调节参数后处理后图片不显示的问题
- 问题原因：`displayImage`方法设置图片后未调用`revalidate()`和`repaint()`刷新界面
- 解决方案：在`displayImage`方法中添加界面刷新调用
- 优化图片缩放逻辑，根据面板实际大小动态计算最大显示尺寸

---

## [1.0.11] - 2026-04-06

### 新增功能

#### LogUtil日志集成到软件日志界面
- `LogUtil`日志现在自动显示在`软件日志`界面中
- 新增`LogUtil.addLogCallback()`方法，支持注册日志回调函数
- 新增`LogUtil.removeLogCallback()`方法，支持移除日志回调
- 所有通过`LogUtil.log()`、`LogUtil.debug()`、`LogUtil.info()`、`LogUtil.warn()`、`LogUtil.error()`输出的日志都会实时显示在UI界面
- 日志格式保持统一：`[时间戳] [类名.方法名:行号] 消息内容`

### 修复

#### UI线程安全问题
- 修复`LogUtil.notifyCallbacks()`可能导致的递归调用问题
- 添加`ThreadLocal`标志防止回调中触发回调导致的死循环
- 修复`MainFrame.log()`方法，确保在UI线程(EDT)中更新日志面板
- 修复`LogPanel.appendLog()`方法，确保UI更新操作在EDT中执行

#### 日志缓冲区优化
- 新增日志缓冲区机制，使用`BlockingQueue`缓冲短时间内（10ms）的日志
- 批量处理日志（每批次最多50条），减少UI更新频率，防止界面卡顿
- 使用独立的`ScheduledExecutorService`定时处理缓冲区日志
- 应用关闭时自动刷新缓冲区，确保所有日志都被处理
- 日志处理逻辑与UI更新分离，提升性能

#### 日志优化
- 注释掉`LogScriptEngine`中的调试日志，减少无意义的刷屏
- 注释掉`LogPanel`中的"开始执行"日志，减少无意义的刷屏
- 保留核心功能日志，提升日志可读性

---

## [1.0.10] - 2026-04-06

### 新增功能

#### 调试日志增强
- 为所有Java类的方法添加`LogUtil.log()`调试信息
- 新增`LogUtil`日志工具类，提供统一的日志输出功能
- 日志格式：`[时间戳] [类名.方法名:行号] 消息内容`
- 支持日志级别：DEBUG、INFO、WARN、ERROR
- 覆盖的类包括：
  - 配置管理：`ConfigManager`
  - 数据库服务：`DatabaseService`
  - 图像处理：`ImageProcessor`、`TextBlobDetector`、`CharacterGenerator`
  - MQTT服务：`MqttService`
  - Lua脚本引擎：`LuaScriptEngine`、`LogScriptEngine`
  - UI面板：`MainFrame`、`LogPanel`、`OcrTestPanel`、`ClassifyTestPanel`等
  - 工具类：`FontLoader`、`XAnyLabelingJsonLoader`
  - 模型类：`OcrRequest`、`OcrResponse`、`Shape`、`XAnyLabelingJson`

---

## [1.0.9] - 2026-04-06

### 修复

#### OCR识别卡住问题
- 修复`OCR识别测试`界面在处理小尺寸图片时可能出现的无限循环问题
- 问题原因：`TextBlobDetector.mergeOverlappingRegions()`方法在特定区域分布下可能陷入死循环
- 解决方案：添加最大迭代次数限制（100次），确保算法总能返回结果

#### 二值化界面图片显示问题
- 修复`图片预处理`标签页打开图片后不显示的问题
- 问题原因：`ImageProcessor.bufferedImageToMat()`方法仅支持灰度图转换
- 解决方案：
  - 扩展`bufferedImageToMat()`方法，支持`TYPE_BYTE_GRAY`和`TYPE_3BYTE_BGR`两种格式
  - 在`ImagePreprocessPanel.loadImageFile()`中先将图片转换为BGR格式再处理

---

## [1.0.8] - 2026-04-06

### 新增功能

#### 图像二值化MQTT服务
- 新增图像二值化MQTT服务 `/ampinspection/binarize/260407/raw`
- MQTT设置面板添加图像二值化主题配置项
- 支持通过MQTT接收图像二值化请求

**请求JSON格式**:
```json
{
    "imageHash": "唯一标识",
    "imageData": "base64编码的图像数据",
    "grayscale": 120,
    "gamma": 0.6
}
```

**响应JSON格式**:
```json
{
    "imageHash": "唯一标识",
    "imageData": "base64编码的二值化图像数据"
}
```

- 支持自定义二值化阈值(grayscale)和Gamma系数(gamma)
- 参数可选，使用默认值(阈值128, Gamma 0.5)

---

## [1.0.7] - 2026-04-06

### 新增功能

#### 字符分类测试增强
- `字符分类测试`标签页添加灰度值参数设置
- 支持调节二值化阈值(0-255)优化预处理效果
- 参数可保存为默认值

#### 图片预处理标签页
- 新增`图片预处理`标签页，支持图像二值化参数调节
- 支持拖入图片或打开图片文件
- 实时调节二值化参数并预览效果:
  - 二值化阈值 (0-255)
  - Gamma系数 (0.1-2.0)
  - 阈值偏移量 (-100~100)
  - 反色处理选项
- 左右分栏显示原始图像和二值化效果
- 支持保存参数为默认值

#### 预处理脚本子标签页
- `图片预处理`标签页添加`脚本`子标签页
- 提供调试控制台，可以覆盖软件内部的二值化逻辑
- 支持Lua脚本自定义预处理流程
- 提供OpenCV API函数到Lua:
  - 二值化: `cv_threshold`, `cv_adaptive_threshold`
  - 滤波: `cv_gaussian_blur`, `cv_median_blur`, `cv_bilateral_filter`
  - 形态学: `cv_erode`, `cv_dilate`, `cv_morphology_ex`
  - 边缘检测: `cv_canny`, `cv_sobel`, `cv_laplacian`
  - 颜色转换: `cv_cvt_color`
  - 位运算: `cv_bitwise_not`, `cv_bitwise_and`, `cv_bitwise_or`
  - 其他: `cv_resize`, `cv_normalize`, `cv_find_contours`等
- 支持脚本验证和测试运行
- 支持加载测试图片预览脚本效果
- 脚本配置持久化到配置文件

---

## [1.0.6] - 2026-04-06

### 新增功能

#### 日志脚本处理
- `软件日志`标签页添加`脚本`子标签页
- 实现脚本编辑和调试控制台界面
- 支持Lua脚本实现日志过滤和自动任务处理
- 新增`LogScriptEngine`日志脚本引擎类

#### 日志过滤功能 (filter函数)
- `filter(log)`函数：返回true保留日志，false过滤掉
- log参数包含：timestamp(时间戳), message(日志内容), level(级别)
- 默认不过滤任何日志
- 支持自定义过滤规则（如按关键词、正则表达式过滤）

#### 日志自动任务功能 (task函数)
- `task(stats)`函数：定期执行的自动任务
- stats参数包含：total(总条数), filtered(已过滤数), current(当前显示数)
- 默认配置：当日志达到10000条时自动清空（防止软件爆内存）
- 支持自定义任务（如按时间清空、按数量归档等）
- 返回值：{action="clear"或"none", message=提示消息}

#### 脚本持久化
- 日志脚本支持保存到配置文件
- 支持启用/禁用脚本
- 提供默认脚本模板
- 脚本配置项：`lua.logScript`, `lua.logScriptEnabled`

---

## [1.0.5] - 2026-04-06

### 新增功能

#### XAnyLabeling JSON支持
- `OCR识别测试`界面增加支持打开/拖入XAnyLabeling JSON文件
- 自动解析JSON中的图片数据（支持base64编码和文件路径）
- 显示JSON中的标注框（shapes）和标签
- 支持从JSON文件所在目录自动查找关联图片
- 新增`XAnyLabelingJson`模型类和`XAnyLabelingJsonLoader`工具类

#### 日志记录增强
- `OCR识别测试`所有操作现在都会在`软件日志`界面有详细记录
- 记录内容包括：文件加载、OCR识别过程、参数设置、错误信息等
- 通过`logCallback`机制实现面板间的日志传递

### 改进

#### 界面主题升级
- 软件主界面风格调整为深蓝色主题（Dark Blue Theme）
- 新增`DarkBlueTheme`主题管理类，统一配色方案
- 主色调：深蓝背景（#0f172a, #1e293b, #334155）
- 强调色：亮蓝色（#3b82f6）
- 文字颜色：白色主文字（#f8fafc），灰色次要文字（#94a3b8）

#### 语法高亮更新
- 更新RSyntaxTextArea的语法高亮为深蓝色主题适配
- Lua脚本编辑器使用新的语法配色方案
- Markdown文档显示使用深蓝色背景
- 语法高亮颜色：
  - 关键字：粉红色（#f92672）
  - 字符串：黄色（#e6db74）
  - 注释：灰色（#75715e）
  - 数字：紫色（#ae81ff）
  - 函数：青色（#66d9ef）

#### 界面组件样式统一
- 所有面板背景色统一为深蓝色主题
- 按钮、输入框、滑块等控件样式统一
- 边框颜色统一为深蓝色调
- 状态标签颜色根据状态变化（成功绿色、错误红色）

---

## [1.0.4] - 2026-04-06

### 新增功能

#### OCR识别测试标签页
- 添加"OCR识别测试"标签页，支持交互式OCR测试
- 支持拖放或打开图片文件进行测试
- 实时调节OCR参数（二值化阈值、Gamma系数、面积/宽高限制）
- 输入正确字符串进行对比验证
- 可视化显示识别结果和标注框
- 支持保存参数为默认值

#### 字符分类测试标签页
- 添加"字符分类测试"标签页，支持单字符分类测试
- 支持拖放或打开单字符图片
- 调节Gamma系数优化预处理效果
- 显示相似度分数和可视化结果
- 根据分数自动着色（绿色/黄色/红色）

#### 结果展示组件 (OcrResultPanel)
- 交互式画布支持缩放（鼠标滚轮）
- 支持平移拖拽查看大图
- 彩色标注框显示识别结果
- 标签显示和隐藏切换
- 缩放比例显示和重置功能
- 图例和统计信息显示

---

## [1.0.3] - 2026-04-06

### 改进

#### 配置文件位置
- 配置文件现在与软件本体（JAR文件）同目录
- 移除了固定路径 `~/ampinspection/config.properties` 的依赖
- 支持从JAR所在目录自动检测和加载配置

---

## [1.0.2] - 2026-04-06

### 新增功能

#### 用户界面改进
- 添加"编程"标签页，显示PROGRAMMING.md内容
- "关于"标签页自动检测README.md文件，存在则显示Markdown格式，否则显示默认信息
- 使用RSyntaxTextArea解析和显示Markdown语法，支持语法高亮

#### MQTT Broker自动启动
- 应用启动时自动检测8907端口
- 如果端口可用，自动启动内置MQTT Broker
- 支持mosquitto和Python paho-mqtt两种broker
- 应用关闭时自动停止broker

#### Skill文档
- 完善 `preview-xanylabeling-skill/SKILL.md` 文档
- 添加详细的使用说明和JSON格式规范

### 改进
- 改进代码字符编码处理
- 修复LuaScriptEngine中的类型转换问题

---

## [1.0.1] - 2026-04-06

### 新增功能

#### 测试工具
- 添加Python测试脚本 `tests/test_ocr.py`
- 支持自动计算图片OCR参数（根据图片尺寸和亮度）
- 支持从同名txt文件读取correctString
- 生成XAnyLabeling格式的JSON结果文件
- 集成可视化工具，支持在浏览器中查看标注结果

#### 测试数据
- 添加3组测试图片和对应的correctString文本
- 测试图片涵盖不同场景和文字密度

### 改进
- 优化OCR参数计算算法，根据图片特性自适应调整
- 添加测试汇总报告生成功能

---

## [1.0.0] - 2026-04-06

### 新增功能

#### OCR核心功能
- 实现经典OCR识别功能，支持检测图像中的文本区域
- 实现字符分类功能，支持单字符相似度判断
- 实现动态阈值二值化，使用Gamma校正增强对比度
- 实现文本区域检测算法，基于连通区域分析
- 支持漏字检测和区域合并

#### MQTT通信
- 实现MQTT客户端，支持连接到MQTT Broker
- 支持订阅经典OCR主题: `/ampinsepction/ocr/260407/raw`
- 支持订阅字符分类主题: `/aminspection/classify/260407/raw`
- 实现自动重连机制
- 支持JSON格式的请求和响应

#### 数据库
- 集成H2嵌入式数据库
- 首次启动自动生成5000个常用字符图片
- 支持动态添加新字符
- 存储OCR识别记录

#### Lua脚本扩展
- 支持使用Lua脚本自定义文本区域定位逻辑
- 支持使用Lua脚本自定义字符分类逻辑
- 提供脚本调试控制台
- 脚本持久化到配置文件

#### 用户界面
- 多标签页界面设计
- 软件日志标签页，每100行自动分页
- OCR参数设置面板
- MQTT配置面板
- 路径设置面板
- Lua脚本编辑面板，支持语法高亮
- 关于页面

#### 配置管理
- 配置文件持久化存储
- 支持自动启动设置
- 支持OCR参数回退默认值
- 支持自定义数据、图片、日志路径

#### 字体与主题
- 集成FlatLaf暗色主题
- 嵌入NotoSans字体作为界面字体
- 嵌入SimSun字体用于字符生成

### 技术栈
- Java 8
- Swing + FlatLaf
- Gradle构建工具
- H2 Database
- JavaCV / OpenCV
- Paho MQTT Client
- LuaJ
- RSyntaxTextArea
- Gson

### 兼容性
- 支持macOS ARM64和x64
- 支持Windows AMD64
- 支持Linux

---

## 版本说明

### 版本号格式
版本号遵循语义化版本控制规范：`主版本号.次版本号.修订号`

- **主版本号**: 重大功能更新，可能包含不兼容的API更改
- **次版本号**: 新功能添加，保持向后兼容
- **修订号**: 问题修复，保持向后兼容

### 构建号格式
FatJar文件名包含时间戳后缀：`yyyyMMddHHmmss`
