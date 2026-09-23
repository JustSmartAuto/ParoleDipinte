package com.honda.paroledipinte.ui.theme;

import com.honda.paroledipinte.util.LogUtil;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 深蓝色主题管理类
 * 提供统一的深蓝色配色方案和样式设置
 */
public class DarkBlueTheme {
    
    // 主色调 - 深蓝色
    public static final Color PRIMARY_DARK = new Color(15, 23, 42);      // 最深的蓝色背景
    public static final Color PRIMARY = new Color(30, 41, 59);          // 主背景色
    public static final Color PRIMARY_LIGHT = new Color(51, 65, 85);    // 稍浅的蓝色
    public static final Color PRIMARY_LIGHTER = new Color(71, 85, 105); // 更浅的蓝色
    
    // 强调色
    public static final Color ACCENT = new Color(59, 130, 246);         // 亮蓝色强调
    public static final Color ACCENT_LIGHT = new Color(96, 165, 250);   // 浅蓝色
    public static final Color ACCENT_DARK = new Color(37, 99, 235);     // 深蓝色
    
    // 功能色
    public static final Color SUCCESS = new Color(34, 197, 94);         // 成功绿色
    public static final Color WARNING = new Color(234, 179, 8);         // 警告黄色
    public static final Color ERROR = new Color(239, 68, 68);           // 错误红色
    public static final Color INFO = new Color(56, 189, 248);           // 信息蓝色
    
    // 文字颜色
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);  // 主要文字（白色）
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184); // 次要文字（灰色）
    public static final Color TEXT_MUTED = new Color(100, 116, 139);    // 淡化文字
    
    // 边框颜色
    public static final Color BORDER = new Color(51, 65, 85);           // 边框颜色
    public static final Color BORDER_LIGHT = new Color(71, 85, 105);    // 浅色边框
    
    // 语法高亮颜色 - 深蓝色主题
    public static final Color SYNTAX_KEYWORD = new Color(249, 38, 114);     // 关键字 - 粉红
    public static final Color SYNTAX_STRING = new Color(230, 219, 116);     // 字符串 - 黄色
    public static final Color SYNTAX_COMMENT = new Color(117, 113, 94);     // 注释 - 灰色
    public static final Color SYNTAX_NUMBER = new Color(174, 129, 255);     // 数字 - 紫色
    public static final Color SYNTAX_FUNCTION = new Color(102, 217, 239);   // 函数 - 青色
    public static final Color SYNTAX_VARIABLE = new Color(248, 248, 242);   // 变量 - 白色
    public static final Color SYNTAX_TYPE = new Color(102, 217, 239);       // 类型 - 青色
    public static final Color SYNTAX_OPERATOR = new Color(249, 38, 114);    // 运算符 - 粉红
    
    /**
     * 应用深蓝色主题到FlatLaf
     */
    public static void apply() {
        LogUtil.log("[apply] 开始执行");
        try {
            // 设置FlatLaf属性
            UIManager.put("Panel.background", PRIMARY);
            UIManager.put("Panel.foreground", TEXT_PRIMARY);
            
            UIManager.put("Label.foreground", TEXT_PRIMARY);
            UIManager.put("Label.background", PRIMARY);
            
            UIManager.put("Button.background", PRIMARY_LIGHT);
            UIManager.put("Button.foreground", TEXT_PRIMARY);
            UIManager.put("Button.hoverBackground", PRIMARY_LIGHTER);
            UIManager.put("Button.pressedBackground", ACCENT);
            
            UIManager.put("TextField.background", PRIMARY_DARK);
            UIManager.put("TextField.foreground", TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
            
            UIManager.put("TextArea.background", PRIMARY_DARK);
            UIManager.put("TextArea.foreground", TEXT_PRIMARY);
            UIManager.put("TextArea.caretForeground", TEXT_PRIMARY);
            
            UIManager.put("ComboBox.background", PRIMARY_LIGHT);
            UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
            UIManager.put("ComboBox.selectionBackground", ACCENT);
            UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
            
            UIManager.put("List.background", PRIMARY_DARK);
            UIManager.put("List.foreground", TEXT_PRIMARY);
            UIManager.put("List.selectionBackground", ACCENT);
            UIManager.put("List.selectionForeground", TEXT_PRIMARY);
            
            UIManager.put("Table.background", PRIMARY_DARK);
            UIManager.put("Table.foreground", TEXT_PRIMARY);
            UIManager.put("Table.selectionBackground", ACCENT);
            UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
            UIManager.put("Table.gridColor", BORDER);
            
            UIManager.put("Tree.background", PRIMARY_DARK);
            UIManager.put("Tree.foreground", TEXT_PRIMARY);
            UIManager.put("Tree.selectionBackground", ACCENT);
            UIManager.put("Tree.selectionForeground", TEXT_PRIMARY);
            
            UIManager.put("TabbedPane.background", PRIMARY);
            UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
            UIManager.put("TabbedPane.selectedBackground", PRIMARY_LIGHT);
            UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
            UIManager.put("TabbedPane.underlineColor", ACCENT);
            UIManager.put("TabbedPane.inactiveUnderlineColor", BORDER);
            
            UIManager.put("ScrollPane.background", PRIMARY);
            UIManager.put("ScrollBar.background", PRIMARY);
            UIManager.put("ScrollBar.thumb", PRIMARY_LIGHTER);
            UIManager.put("ScrollBar.thumbHover", ACCENT);
            
            UIManager.put("Slider.background", PRIMARY);
            UIManager.put("Slider.foreground", ACCENT);
            UIManager.put("Slider.tickColor", TEXT_SECONDARY);
            
            UIManager.put("Spinner.background", PRIMARY_DARK);
            UIManager.put("Spinner.foreground", TEXT_PRIMARY);
            
            UIManager.put("ProgressBar.background", PRIMARY_LIGHT);
            UIManager.put("ProgressBar.foreground", ACCENT);
            
            UIManager.put("MenuBar.background", PRIMARY);
            UIManager.put("MenuBar.foreground", TEXT_PRIMARY);
            UIManager.put("Menu.background", PRIMARY);
            UIManager.put("Menu.foreground", TEXT_PRIMARY);
            UIManager.put("MenuItem.background", PRIMARY);
            UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
            UIManager.put("MenuItem.selectionBackground", ACCENT);
            UIManager.put("MenuItem.selectionForeground", TEXT_PRIMARY);
            
            UIManager.put("ToolBar.background", PRIMARY);
            UIManager.put("ToolBar.foreground", TEXT_PRIMARY);
            
            UIManager.put("StatusBar.background", PRIMARY_DARK);
            
            UIManager.put("SplitPane.background", PRIMARY);
            UIManager.put("SplitPaneDivider.background", BORDER);
            UIManager.put("SplitPaneDivider.draggingColor", ACCENT);
            
            UIManager.put("TitledBorder.titleColor", TEXT_SECONDARY);
            
            UIManager.put("OptionPane.background", PRIMARY);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
            
            UIManager.put("CheckBox.background", PRIMARY);
            UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
            UIManager.put("RadioButton.background", PRIMARY);
            UIManager.put("RadioButton.foreground", TEXT_PRIMARY);
            
            // 设置FlatLaf主题
            FlatDarkLaf.setup();
            
        } catch (Exception e) {
            System.err.println("应用深蓝色主题失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建标准边框
     */
    public static Border createBorder() {
        LogUtil.log("[createBorder] 开始执行");
        return BorderFactory.createLineBorder(BORDER);
    }
    
    /**
     * 创建标题边框
     */
    public static Border createTitledBorder(String title) {
        LogUtil.log("[createTitledBorder] 开始执行");
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER),
            title,
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            TEXT_SECONDARY
        );
    }
    
    /**
     * 创建内边距边框
     */
    public static Border createPaddingBorder(int padding) {
        LogUtil.log("[createPaddingBorder] 开始执行");
        return BorderFactory.createEmptyBorder(padding, padding, padding, padding);
    }
    
    /**
     * 创建内边距边框
     */
    public static Border createPaddingBorder(int top, int left, int bottom, int right) {
        LogUtil.log("[createPaddingBorder] 开始执行");
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
    
    /**
     * 获取主背景色
     */
    public static Color getBackgroundColor() {
        return PRIMARY;
    }
    
    /**
     * 获取深色背景色
     */
    public static Color getDarkBackgroundColor() {
        return PRIMARY_DARK;
    }
    
    /**
     * 获取浅色背景色
     */
    public static Color getLightBackgroundColor() {
        return PRIMARY_LIGHT;
    }
    
    /**
     * 获取强调色
     */
    public static Color getAccentColor() {
        return ACCENT;
    }
    
    /**
     * 获取主要文字颜色
     */
    public static Color getTextColor() {
        return TEXT_PRIMARY;
    }
    
    /**
     * 获取次要文字颜色
     */
    public static Color getSecondaryTextColor() {
        return TEXT_SECONDARY;
    }
}
