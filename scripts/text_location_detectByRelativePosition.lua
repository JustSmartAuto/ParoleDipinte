--[[
    文本定位脚本 - 在软件中复现 TextBlobDetector.detectByRelativePosition 完整算法
    
    使用方式：将此脚本完整复制到软件的 `文本定位脚本` 编辑器中并启用。
    
    说明：
    - 本脚本在 Lua 脚本引擎中运行，接收 Java 端传入的数据：
      imageInfo, regions, ocrParams, correctString, preset, rawRegions, correctedRegions
    - 从 preset.charRelativePositions 读取字符相对位置配置
    - 从 regions（或 rawRegions / correctedRegions）获取实际分割框作为首字符基准和连通区域
    - 完整复现 Java 端的 detectByRelativePosition + correctByMatchingAndInterpolation 逻辑
    - 不依赖 OpenCV，纯 Lua 实现，便于调试和验证
--]]

-- ==================== 工具函数 ====================

local function cloneRegion(r)
    if not r then return nil end
    return { x = r.x, y = r.y, width = r.width, height = r.height }
end

local function cloneRegions(regions)
    if not regions then return {} end
    local t = {}
    for i, r in ipairs(regions) do
        t[i] = cloneRegion(r)
    end
    return t
end

local function sortByX(regions)
    table.sort(regions, function(a, b) return a.x < b.x end)
    return regions
end

local function formatRegion(r)
    if not r then return "nil" end
    return string.format("Region(x=%d, y=%d, w=%d, h=%d)", r.x, r.y, r.width, r.height)
end

local function log(msg)
    print("[detectByRelativePosition] " .. msg)
end

-- ==================== 核心算法 ====================

--[[
    步骤0：预处理 - 检测并合并相邻的异常窄区域
--]]
local function preprocessMergeNarrowBlobs(allBlobRegions)
    local processedBlobs = cloneRegions(allBlobRegions)
    local blobGroups = {}
    for j = 1, #processedBlobs do
        blobGroups[j] = { j - 1 }
    end

    local merged = true
    local maxMergeIterations = #processedBlobs
    local mergeIteration = 0

    while merged and mergeIteration < maxMergeIterations do
        merged = false
        mergeIteration = mergeIteration + 1

        local newBlobs = {}
        local newGroups = {}
        local mergedInThisRound = {}
        for j = 1, #processedBlobs do
            mergedInThisRound[j] = false
        end

        for j = 1, #processedBlobs do
            if mergedInThisRound[j] then goto continue end

            local r1 = processedBlobs[j]
            local x1 = r1.x
            local y1 = r1.y
            local x1End = r1.x + r1.width
            local y1End = r1.y + r1.height

            if j + 1 <= #processedBlobs and not mergedInThisRound[j + 1] then
                local r2 = processedBlobs[j + 1]
                local x2 = r2.x
                local y2 = r2.y
                local x2End = r2.x + r2.width
                local y2End = r2.y + r2.height

                local horizontalGap = math.max(0, x2 - x1End)
                local heightDiffRatio = math.abs(r1.height - r2.height) / math.max(r1.height, r2.height)
                local verticalOverlap = (y1 < y2End) and (y1End > y2)

                if horizontalGap <= 10 and heightDiffRatio < 0.35 and verticalOverlap then
                    local mergedWidth = x2End - x1
                    local mergedHeight = math.max(y1End, y2End) - math.min(y1, y2)
                    local mergedRatio = mergedWidth / math.max(mergedHeight, 1)
                    local r1Ratio = r1.width / math.max(r1.height, 1)

                    local r1IsNarrow = (r1Ratio < 0.5) and (r1.width >= 35)
                    local mergedIsBetter = math.abs(mergedRatio - 1.0) < math.abs(r1Ratio - 1.0)
                    local mergedSizeReasonable = mergedWidth <= 80

                    if r1IsNarrow and mergedIsBetter and mergedSizeReasonable then
                        local mergedRegion = {
                            x = x1,
                            y = math.min(y1, y2),
                            width = mergedWidth,
                            height = mergedHeight
                        }
                        table.insert(newBlobs, mergedRegion)

                        local mg = {}
                        for _, v in ipairs(blobGroups[j]) do table.insert(mg, v) end
                        for _, v in ipairs(blobGroups[j + 1]) do table.insert(mg, v) end
                        table.insert(newGroups, mg)

                        mergedInThisRound[j] = true
                        mergedInThisRound[j + 1] = true
                        merged = true

                        log(string.format("合并相邻窄区域 [%d]%s 和 [%d]%s -> %s",
                            j - 1, formatRegion(r1), j, formatRegion(r2), formatRegion(mergedRegion)))
                        goto continue
                    end
                end
            end

            table.insert(newBlobs, cloneRegion(r1))
            local g = {}
            for _, v in ipairs(blobGroups[j]) do table.insert(g, v) end
            table.insert(newGroups, g)

            ::continue::
        end

        processedBlobs = newBlobs
        blobGroups = newGroups
    end

    return processedBlobs, blobGroups
end

--[[
    步骤1-2：构建匹配对并贪心分配
--]]
local function buildMatchPairs(charCount, charPositions, estimatedRegions, processedBlobs)
    local matchPairs = {}

    for i = 1, charCount do
        local est = estimatedRegions[i]
        local pos = charPositions[i]
        for j = 1, #processedBlobs do
            local blob = processedBlobs[j]

            local estCenterX = est.x + est.width / 2.0
            local estCenterY = est.y + est.height / 2.0
            local blobCenterX = blob.x + blob.width / 2.0
            local blobCenterY = blob.y + blob.height / 2.0

            local distance = math.sqrt((estCenterX - blobCenterX) ^ 2 + (estCenterY - blobCenterY) ^ 2)
            local widthDiff = math.abs(est.width - blob.width) / math.max(est.width, 1.0)
            local heightDiff = math.abs(est.height - blob.height) / math.max(est.height, 1.0)

            local distanceThreshold = math.max(pos.width * 1.5, 50)
            if distance > distanceThreshold then goto skip end
            if widthDiff > 1.0 or heightDiff > 1.0 then goto skip end

            local yDiff = math.abs(estCenterY - blobCenterY)
            local yDiffThreshold = math.max(pos.height * 0.5, 20)
            if yDiff > yDiffThreshold then goto skip end

            local score = distance + (widthDiff + heightDiff) * 20 + yDiff * 2
            table.insert(matchPairs, {
                charIndex = i - 1,
                blobIndex = j - 1,
                score = score,
                blob = cloneRegion(blob)
            })

            ::skip::
        end
    end

    table.sort(matchPairs, function(a, b) return a.score < b.score end)
    return matchPairs
end

local function greedyAssign(matchPairs, charCount, blobCount)
    local result = {}
    for i = 1, charCount do result[i] = nil end

    local charMatched = {}
    local blobUsed = {}
    for i = 1, charCount do charMatched[i] = false end
    for j = 1, blobCount do blobUsed[j] = false end

    local matchedCount = 0
    for _, pair in ipairs(matchPairs) do
        local ci = pair.charIndex + 1
        local bj = pair.blobIndex + 1
        if not charMatched[ci] and not blobUsed[bj] then
            result[ci] = cloneRegion(pair.blob)
            charMatched[ci] = true
            blobUsed[bj] = true
            matchedCount = matchedCount + 1
        end
    end

    return result, charMatched, matchedCount
end

--[[
    步骤3：基于锚点重新计算缩放比例
--]]
local function recalcScale(charCount, charPositions, result, charMatched)
    local scaleX, scaleY = 1.0, 1.0
    local sumScaleX, sumScaleY = 0.0, 0.0
    local scaleCount = 0

    for i = 1, charCount do
        if not charMatched[i] then goto continue end
        local matched = result[i]
        local pos = charPositions[i]
        if pos.width > 0 and matched.width > 0 then
            sumScaleX = sumScaleX + (matched.width / pos.width)
            scaleCount = scaleCount + 1
        end
        if pos.height > 0 and matched.height > 0 then
            sumScaleY = sumScaleY + (matched.height / pos.height)
        end
        ::continue::
    end

    if scaleCount > 0 then
        scaleX = sumScaleX / scaleCount
        scaleY = sumScaleY / scaleCount
    end

    log(string.format("基于 %d 个锚点重新计算缩放: (%.2f, %.2f)", scaleCount, scaleX, scaleY))
    return scaleX, scaleY
end

--[[
    步骤4：为未匹配的字符计算位置（插值或偏移）
--]]
local function interpolateUnmatched(charCount, charPositions, result, charMatched, firstCharRegion)
    local scaleX, scaleY = recalcScale(charCount, charPositions, result, charMatched)

    -- 检查是否有任何匹配
    local hasAnyMatch = false
    for i = 1, charCount do
        if charMatched[i] then hasAnyMatch = true; break end
    end

    if not hasAnyMatch then
        local firstPos = charPositions[1]
        if firstPos.width > 0 then scaleX = firstCharRegion.width / firstPos.width end
        if firstPos.height > 0 then scaleY = firstCharRegion.height / firstPos.height end
    end

    for i = 1, charCount do
        if charMatched[i] then goto continue end

        local pos = charPositions[i]

        local leftAnchor = -1
        local rightAnchor = -1
        for j = i - 1, 1, -1 do
            if charMatched[j] then leftAnchor = j; break end
        end
        for j = i + 1, charCount do
            if charMatched[j] then rightAnchor = j; break end
        end

        local finalRegion = nil

        if leftAnchor >= 1 and rightAnchor >= 1 then
            local leftRegion = result[leftAnchor]
            local rightRegion = result[rightAnchor]
            local leftPos = charPositions[leftAnchor]
            local rightPos = charPositions[rightAnchor]

            local leftRelX = leftPos.relativeX
            local rightRelX = rightPos.relativeX
            local currentRelX = pos.relativeX

            local ratio = (currentRelX - leftRelX) / math.max(rightRelX - leftRelX, 1.0)
            local expectedX = math.floor(leftRegion.x + (rightRegion.x - leftRegion.x) * ratio + 0.5)
            local expectedWidth = math.floor(pos.width * scaleX + 0.5)
            local expectedHeight = math.floor(pos.height * scaleY + 0.5)

            local expectedY
            local yDiff = math.abs(leftRegion.y - rightRegion.y)
            if yDiff > 20 then
                expectedY = math.floor(leftRegion.y + (pos.relativeY - leftPos.relativeY) * scaleY + 0.5)
                log(string.format("字符[%d] '%s' 锚点Y差异大(%d)，使用左锚点Y偏移",
                    i - 1, pos.character or "", yDiff))
            else
                expectedY = math.floor(leftRegion.y + (rightRegion.y - leftRegion.y) * ratio + 0.5)
            end

            finalRegion = { x = expectedX, y = expectedY, width = expectedWidth, height = expectedHeight }
            log(string.format("字符[%d] '%s' 使用插值: %s (锚点[%d]-[%d])",
                i - 1, pos.character or "", formatRegion(finalRegion), leftAnchor - 1, rightAnchor - 1))

        elseif leftAnchor >= 1 then
            local leftRegion = result[leftAnchor]
            local leftPos = charPositions[leftAnchor]
            local offsetX = math.floor((pos.relativeX - leftPos.relativeX) * scaleX + 0.5)
            local offsetY = math.floor((pos.relativeY - leftPos.relativeY) * scaleY + 0.5)
            local expectedX = leftRegion.x + offsetX
            local expectedY = leftRegion.y + offsetY
            local expectedWidth = math.floor(pos.width * scaleX + 0.5)
            local expectedHeight = math.floor(pos.height * scaleY + 0.5)

            finalRegion = { x = expectedX, y = expectedY, width = expectedWidth, height = expectedHeight }
            log(string.format("字符[%d] '%s' 使用左锚点偏移: %s (锚点[%d])",
                i - 1, pos.character or "", formatRegion(finalRegion), leftAnchor - 1))

        elseif rightAnchor >= 1 then
            local rightRegion = result[rightAnchor]
            local rightPos = charPositions[rightAnchor]
            local offsetX = math.floor((pos.relativeX - rightPos.relativeX) * scaleX + 0.5)
            local offsetY = math.floor((pos.relativeY - rightPos.relativeY) * scaleY + 0.5)
            local expectedX = rightRegion.x + offsetX
            local expectedY = rightRegion.y + offsetY
            local expectedWidth = math.floor(pos.width * scaleX + 0.5)
            local expectedHeight = math.floor(pos.height * scaleY + 0.5)

            finalRegion = { x = expectedX, y = expectedY, width = expectedWidth, height = expectedHeight }
            log(string.format("字符[%d] '%s' 使用右锚点偏移: %s (锚点[%d])",
                i - 1, pos.character or "", formatRegion(finalRegion), rightAnchor - 1))

        else
            local expectedX = math.floor(firstCharRegion.x + pos.relativeX * scaleX + 0.5)
            local expectedY = math.floor(firstCharRegion.y + pos.relativeY * scaleY + 0.5)
            local expectedWidth = math.floor(pos.width * scaleX + 0.5)
            local expectedHeight = math.floor(pos.height * scaleY + 0.5)

            finalRegion = { x = expectedX, y = expectedY, width = expectedWidth, height = expectedHeight }
            log(string.format("字符[%d] '%s' 无锚点，使用估计位置: %s",
                i - 1, pos.character or "", formatRegion(finalRegion)))
        end

        result[i] = finalRegion
        ::continue::
    end

    return result
end

--[[
    主算法：复现 TextBlobDetector.detectByRelativePosition
    
    参数：
    - charPositions: 从 preset.charRelativePositions 获取的字符相对位置列表
    - firstCharRegion: 首字符实际位置（使用 regions[1] 或 correctedRegions[1]）
    - allBlobRegions: 原始连通区域列表（textRegions + missedRegions，已按 x 排序）
    
    返回：定位后的区域列表
--]]
local function detectByRelativePosition(charPositions, firstCharRegion, allBlobRegions)
    if not charPositions or #charPositions == 0 or not firstCharRegion then
        log("参数无效，返回空列表")
        return {}
    end

    log(string.format("开始执行，字符数量: %d", #charPositions))

    local baseX = firstCharRegion.x
    local baseY = firstCharRegion.y

    local firstPos = charPositions[1]
    local scaleX = 1.0
    local scaleY = 1.0
    if firstPos.width > 0 then
        scaleX = firstCharRegion.width / firstPos.width
    end
    if firstPos.height > 0 then
        scaleY = firstCharRegion.height / firstPos.height
    end

    log(string.format("基准点: (%d,%d), 缩放比例: (%.2f, %.2f)", baseX, baseY, scaleX, scaleY))
    log(string.format("原始连通区域数: %d", #allBlobRegions))

    -- 阶段1：基于相对位置初步定位每个字符
    local estimatedRegions = {}
    for i, pos in ipairs(charPositions) do
        local offsetX = math.floor(pos.relativeX * scaleX + 0.5)
        local offsetY = math.floor(pos.relativeY * scaleY + 0.5)
        local expectedX = baseX + offsetX
        local expectedY = baseY + offsetY
        local expectedWidth = math.floor(pos.width * scaleX + 0.5)
        local expectedHeight = math.floor(pos.height * scaleY + 0.5)
        estimatedRegions[i] = { x = expectedX, y = expectedY, width = expectedWidth, height = expectedHeight }
    end

    -- 阶段2：框数量匹配且尺寸比例正常时，直接返回 allBlobRegions
    local blobCount = #allBlobRegions
    local charCount = #charPositions
    if blobCount == charCount then
        log(string.format("情况1/2: 框数量匹配(%d)，尝试直接对应", blobCount))
        local sizeRatioValid = true
        for i = 1, charCount do
            local pos = charPositions[i]
            local blob = allBlobRegions[i]
            local expectedWidth = pos.width * scaleX
            local expectedHeight = pos.height * scaleY

            local widthRatio = expectedWidth > 0 and (blob.width / expectedWidth) or 1.0
            local heightRatio = expectedHeight > 0 and (blob.height / expectedHeight) or 1.0

            if widthRatio < 0.5 or widthRatio > 2.0 or heightRatio < 0.5 or heightRatio > 2.0 then
                sizeRatioValid = false
                log(string.format("尺寸比例异常: 字符'%s' 宽比=%.2f, 高比=%.2f",
                    pos.character or "", widthRatio, heightRatio))
                break
            end
        end

        if sizeRatioValid then
            log("尺寸比例正常，直接使用分割框")
            return cloneRegions(allBlobRegions)
        end
    end

    -- 阶段3：分割错误，进行修正推断
    log("情况3: 分割错误，进行修正推断")

    local processedBlobs, blobGroups = preprocessMergeNarrowBlobs(allBlobRegions)
    log(string.format("预处理后区域数: %d (原始: %d)", #processedBlobs, #allBlobRegions))

    local matchPairs = buildMatchPairs(charCount, charPositions, estimatedRegions, processedBlobs)
    local result, charMatched, matchedCount = greedyAssign(matchPairs, charCount, #processedBlobs)
    log(string.format("直接匹配成功: %d/%d", matchedCount, charCount))

    result = interpolateUnmatched(charCount, charPositions, result, charMatched, firstCharRegion)

    log(string.format("执行完成，定位了 %d 个字符", #result))
    return result
end

-- ==================== 软件接口：locate_text ====================

--[[
    文本定位脚本入口函数
    
    参数：
    - imageInfo: {width, height}
    - regions: 检测到的区域列表（已修正后），每个元素 {x, y, width, height}
    - ocrParams: OCR参数表
    - correctString: 正确字符串（可能为nil）
    
    全局变量（由Java端注入）：
    - preset: 当前预设参数表（可能为nil）
    - rawRegions: OCR参数分割出的原始字符框（可能为nil）
    - correctedRegions: 字符相对位置修正后的字符框（可能为nil）
    
    返回：处理后的区域列表
--]]
function locate_text(imageInfo, regions, ocrParams, correctString)
    print("图像尺寸: " .. imageInfo.width .. "x" .. imageInfo.height)
    print("检测到 " .. #regions .. " 个区域")

    -- 检查是否有预设和字符相对位置信息
    if not preset or not preset.charRelativePositions or #preset.charRelativePositions == 0 then
        print("无字符相对位置信息，跳过 detectByRelativePosition 处理")
        return regions
    end

    -- 获取字符相对位置列表
    local charPositions = preset.charRelativePositions

    -- 确定参考区域和首字符基准
    -- 优先使用 correctedRegions，其次 rawRegions，最后 regions
    local refRegions = correctedRegions or rawRegions or regions
    if not refRegions or #refRegions == 0 then
        print("无参考区域，跳过处理")
        return regions
    end

    -- 按X坐标排序参考区域，取第一个作为首字符基准
    local sortedRef = cloneRegions(refRegions)
    sortByX(sortedRef)
    local firstCharRegion = sortedRef[1]

    print("首字符基准: " .. formatRegion(firstCharRegion))

    -- 构建 allBlobRegions：合并 textRegions 和 missedRegions
    -- 在 Lua 环境中，我们没有直接的 missedRegions，使用 rawRegions + regions 的并集近似
    local allBlobRegions = cloneRegions(rawRegions or regions)
    -- 去重：将 regions 中不在 allBlobRegions 中的区域加入
    local function regionKey(r)
        return string.format("%d,%d,%d,%d", r.x, r.y, r.width, r.height)
    end
    local seen = {}
    for _, r in ipairs(allBlobRegions) do
        seen[regionKey(r)] = true
    end
    if regions then
        for _, r in ipairs(regions) do
            local k = regionKey(r)
            if not seen[k] then
                seen[k] = true
                table.insert(allBlobRegions, cloneRegion(r))
            end
        end
    end
    sortByX(allBlobRegions)

    -- 执行核心算法
    local detected = detectByRelativePosition(charPositions, firstCharRegion, allBlobRegions)

    print("\n========== 最终结果 ==========")
    for i, r in ipairs(detected) do
        print(string.format("[%d] %s", i - 1, formatRegion(r)))
    end

    return detected
end
