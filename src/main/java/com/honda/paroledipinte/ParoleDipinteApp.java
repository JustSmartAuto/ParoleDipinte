package com.honda.paroledipinte;

import com.formdev.flatlaf.FlatDarkLaf;
import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.ui.MainFrame;
import com.honda.paroledipinte.ui.theme.DarkBlueTheme;
import com.honda.paroledipinte.util.FontLoader;
import com.honda.paroledipinte.util.LogUtil;

import javax.swing.*;

/**
 * 识字涂色(ParoleDipinte) OCR服务主应用程序
 */
public class ParoleDipinteApp {
    
    public static void main(String[] args) {
        LogUtil.log("[main] 开始执行");
        // 设置系统属性
        System.setProperty("file.encoding", "UTF-8");
        
        // 在事件调度线程中启动GUI
        SwingUtilities.invokeLater(() -> {
            try {
                // 加载配置
                ConfigManager.init();
                
                // 应用深蓝色主题
                DarkBlueTheme.apply();
                
                // 设置全局字体
                FontLoader.setGlobalFont();
                
                // 创建并显示主窗口
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                
            } catch (Exception e) {
                LogUtil.log("[main] 发生错误: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "启动失败: " + e.getMessage(), 
                    "错误", 
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
        LogUtil.log("[main] 执行完成");
    }
}
