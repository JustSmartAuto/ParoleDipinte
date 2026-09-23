package com.honda.paroledipinte.util;

import com.honda.paroledipinte.util.LogUtil;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;

/**
 * 字体加载器
 * 加载嵌入式字体
 */
public class FontLoader {
    
    private static Font notoSansFont;
    private static Font simSunFont;
    
    /**
     * 加载NotoSans字体(界面字体)
     */
    public static Font loadNotoSans() {
        LogUtil.log("[loadNotoSans] 开始执行");
        LogUtil.log("[loadNotoSans] 开始执行");
        if (notoSansFont != null) {
            LogUtil.log("[loadNotoSans] 执行完成");
            return notoSansFont;
        }
        
        // 尝试从资源加载
        try {
            InputStream is = FontLoader.class.getResourceAsStream("/fonts/NotoSansCJKsc-Regular.ttf");
            if (is != null) {
                notoSansFont = Font.createFont(Font.TRUETYPE_FONT, is);
                is.close();
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(notoSansFont);
                LogUtil.log("[loadNotoSans] 执行完成");
                return notoSansFont;
            }
        } catch (Exception e) {
            LogUtil.log("[loadNotoSans] 发生错误: " + e.getMessage());
        }
        
        // 尝试从文件系统加载
        try {
            File fontFile = new File("fonts/NotoSansCJKsc-Regular.ttf");
            if (fontFile.exists()) {
                notoSansFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(notoSansFont);
                LogUtil.log("[loadNotoSans] 执行完成");
                return notoSansFont;
            }
        } catch (Exception e) {
            LogUtil.log("[loadNotoSans] 发生错误: " + e.getMessage());
        }
        
        // 使用系统默认字体
        LogUtil.log("[loadNotoSans] 执行完成");
        return new Font("SansSerif", Font.PLAIN, 12);
    }
    
    /**
     * 加载SimSun字体(字符生成字体)
     */
    public static Font loadSimSun() {
        LogUtil.log("[loadSimSun] 开始执行");
        LogUtil.log("[loadSimSun] 开始执行");
        if (simSunFont != null) {
            LogUtil.log("[loadSimSun] 执行完成");
            return simSunFont;
        }
        
        // 尝试从资源加载
        try {
            InputStream is = FontLoader.class.getResourceAsStream("/fonts/simsun.ttf");
            if (is != null) {
                simSunFont = Font.createFont(Font.TRUETYPE_FONT, is);
                is.close();
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(simSunFont);
                LogUtil.log("[loadSimSun] 执行完成");
                return simSunFont;
            }
        } catch (Exception e) {
            LogUtil.log("[loadSimSun] 发生错误: " + e.getMessage());
        }
        
        // 尝试从文件系统加载
        try {
            File fontFile = new File("fonts/simsun.ttf");
            if (fontFile.exists()) {
                simSunFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(simSunFont);
                LogUtil.log("[loadSimSun] 执行完成");
                return simSunFont;
            }
        } catch (Exception e) {
            LogUtil.log("[loadSimSun] 发生错误: " + e.getMessage());
        }
        
        // 使用系统字体
        Font sysFont = new Font("SimSun", Font.PLAIN, 12);
        if (sysFont.getFamily().equals("Dialog")) {
            sysFont = new Font("Serif", Font.PLAIN, 12);
        }
        LogUtil.log("[loadSimSun] 执行完成");
        return sysFont;
    }
    
    /**
     * 设置全局字体
     */
    public static void setGlobalFont() {
        LogUtil.log("[setGlobalFont] 开始执行");
        Font font = loadNotoSans().deriveFont(Font.PLAIN, 13);
        
        UIManager.put("Button.font", font);
        UIManager.put("ToggleButton.font", font);
        UIManager.put("RadioButton.font", font);
        UIManager.put("CheckBox.font", font);
        UIManager.put("ColorChooser.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("List.font", font);
        UIManager.put("MenuBar.font", font);
        UIManager.put("MenuItem.font", font);
        UIManager.put("RadioButtonMenuItem.font", font);
        UIManager.put("CheckBoxMenuItem.font", font);
        UIManager.put("Menu.font", font);
        UIManager.put("PopupMenu.font", font);
        UIManager.put("OptionPane.font", font);
        UIManager.put("Panel.font", font);
        UIManager.put("ProgressBar.font", font);
        UIManager.put("ScrollPane.font", font);
        UIManager.put("Viewport.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("PasswordField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("TextPane.font", font);
        UIManager.put("EditorPane.font", font);
        UIManager.put("TitledBorder.font", font);
        UIManager.put("ToolBar.font", font);
        UIManager.put("ToolTip.font", font);
        UIManager.put("Tree.font", font);
        LogUtil.log("[setGlobalFont] 执行完成");
    }
    
    /**
     * 获取指定大小和样式的字体
     */
    public static Font getFont(int style, float size) {
        LogUtil.log("[getFont] 开始执行");
        LogUtil.log("[getFont] 执行完成");
        return loadNotoSans().deriveFont(style, size);
    }
}
