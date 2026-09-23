package com.honda.paroledipinte.db;

import com.honda.paroledipinte.config.ConfigManager;
import com.honda.paroledipinte.model.StringPreset;
import com.honda.paroledipinte.util.LogUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 数据库服务类
 * 使用H2数据库存储字符图片
 */
public class DatabaseService {
    private static final String DB_NAME = "character_db";
    private Connection connection;
    private String dbPath;
    
    public DatabaseService() {
        LogUtil.log("[DatabaseService] 开始执行");
        this.dbPath = ConfigManager.getDataDir();
        LogUtil.log("[DatabaseService] 执行完成");
    }
    
    /**
     * 初始化数据库
     */
    public void init() throws Exception {
        LogUtil.log("[init] 开始执行");
        // 确保数据目录存在
        java.io.File dataDir = new java.io.File(dbPath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        String dbUrl = "jdbc:h2:" + dbPath + "/" + DB_NAME;
        LogUtil.log("[init] 连接数据库: " + dbUrl);
        connection = DriverManager.getConnection(dbUrl, "sa", "");
        
        // 创建字符图片表
        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS character_images (" +
                "char_name VARCHAR(10) PRIMARY KEY, " +
                "image_data BLOB NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        
        // 创建OCR记录表
        stmt.execute("CREATE TABLE IF NOT EXISTS ocr_records (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "image_hash VARCHAR(64), " +
                "request_json CLOB, " +
                "response_json CLOB, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        
        // 创建字符串预设表
        stmt.execute("CREATE TABLE IF NOT EXISTS string_presets (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "preset_name VARCHAR(255) NOT NULL, " +
                "correct_string VARCHAR(1000), " +
                "standard_image BLOB, " +
                "image_path VARCHAR(500), " +
                "grayscale INT DEFAULT 128, " +
                "gamma DOUBLE DEFAULT 0.5, " +
                "min_area INT DEFAULT 15, " +
                "max_area INT DEFAULT 100000, " +
                "min_width INT DEFAULT 3, " +
                "max_width INT DEFAULT 500, " +
                "min_height INT DEFAULT 3, " +
                "max_height INT DEFAULT 200, " +
                "text_location_script CLOB, " +
                "char_classify_script CLOB, " +
                "ocr_post_process_script CLOB, " +
                "text_location_script_enabled BOOLEAN DEFAULT FALSE, " +
                "char_classify_script_enabled BOOLEAN DEFAULT FALSE, " +
                "ocr_post_process_script_enabled BOOLEAN DEFAULT FALSE, " +
                "char_relative_position_enabled BOOLEAN DEFAULT FALSE, " +
                "char_relative_positions_json CLOB, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        
        stmt.close();
        LogUtil.log("[init] 数据库表创建完成: character_images, ocr_records, string_presets");
        LogUtil.log("[init] 执行完成");
    }
    
    /**
     * 检查数据库是否为空
     */
    public boolean isEmpty() throws SQLException {
        LogUtil.log("[isEmpty] 开始执行");
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM character_images");
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();
        LogUtil.log("[isEmpty] 执行完成");
        return count == 0;
    }
    
    /**
     * 获取字符数量
     */
    public int getCharacterCount() throws SQLException {
        LogUtil.log("[getCharacterCount] 开始执行");
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM character_images");
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();
        LogUtil.log("[getCharacterCount] 执行完成");
        return count;
    }
    
    /**
     * 保存字符图片
     */
    public void saveCharacterImage(String charName, BufferedImage image) throws Exception {
        LogUtil.log("[saveCharacterImage] 开始执行");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] bytes = baos.toByteArray();
        
        PreparedStatement ps = connection.prepareStatement(
                "MERGE INTO character_images(char_name, image_data) VALUES(?, ?)");
        ps.setString(1, charName);
        ps.setBytes(2, bytes);
        ps.executeUpdate();
        ps.close();
        LogUtil.log("[saveCharacterImage] 执行完成");
    }
    
    /**
     * 加载字符图片
     */
    public BufferedImage loadCharacterImage(String charName) throws Exception {
        LogUtil.log("[loadCharacterImage] 开始执行");
        PreparedStatement ps = connection.prepareStatement(
                "SELECT image_data FROM character_images WHERE char_name = ?");
        ps.setString(1, charName);
        ResultSet rs = ps.executeQuery();
        BufferedImage image = null;
        if (rs.next()) {
            byte[] bytes = rs.getBytes(1);
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        }
        rs.close();
        ps.close();
        LogUtil.log("[loadCharacterImage] 执行完成");
        return image;
    }
    
    /**
     * 检查字符是否存在
     */
    public boolean characterExists(String charName) throws SQLException {
        LogUtil.log("[characterExists] 开始执行");
        PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM character_images WHERE char_name = ?");
        ps.setString(1, charName);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        LogUtil.log("[characterExists] 执行完成");
        return exists;
    }
    
    /**
     * 删除字符图片
     */
    public void deleteCharacterImage(String charName) throws SQLException {
        LogUtil.log("[deleteCharacterImage] 开始执行");
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM character_images WHERE char_name = ?");
        ps.setString(1, charName);
        ps.executeUpdate();
        ps.close();
        LogUtil.log("[deleteCharacterImage] 执行完成");
    }
    
    /**
     * 保存OCR记录
     */
    public void saveOcrRecord(String imageHash, String requestJson, String responseJson) throws SQLException {
        LogUtil.log("[saveOcrRecord] 开始执行");
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ocr_records(image_hash, request_json, response_json) VALUES(?, ?, ?)");
        ps.setString(1, imageHash);
        ps.setString(2, requestJson);
        ps.setString(3, responseJson);
        ps.executeUpdate();
        ps.close();
        LogUtil.log("[saveOcrRecord] 执行完成");
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() throws SQLException {
        LogUtil.log("[close] 开始执行");
        if (connection != null) {
            connection.close();
        }
        LogUtil.log("[close] 执行完成");
    }
    
    // ==================== 字符串预设操作 ====================
    
    /**
     * 保存字符串预设
     */
    public void saveStringPreset(StringPreset preset) throws SQLException {
        LogUtil.log("[saveStringPreset] 开始执行");
        String sql = "MERGE INTO string_presets(" +
                "id, preset_name, correct_string, standard_image, image_path, " +
                "grayscale, gamma, min_area, max_area, min_width, max_width, min_height, max_height, " +
                "text_location_script, char_classify_script, ocr_post_process_script, " +
                "text_location_script_enabled, char_classify_script_enabled, ocr_post_process_script_enabled) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, preset.getId());
        ps.setString(2, preset.getPresetName());
        ps.setString(3, preset.getCorrectString());
        ps.setBytes(4, preset.getStandardImage());
        ps.setString(5, preset.getImagePath());
        ps.setInt(6, preset.getGrayscale());
        ps.setDouble(7, preset.getGamma());
        ps.setInt(8, preset.getMinArea());
        ps.setInt(9, preset.getMaxArea());
        ps.setInt(10, preset.getMinWidth());
        ps.setInt(11, preset.getMaxWidth());
        ps.setInt(12, preset.getMinHeight());
        ps.setInt(13, preset.getMaxHeight());
        ps.setString(14, preset.getTextLocationScript());
        ps.setString(15, preset.getCharClassifyScript());
        ps.setString(16, preset.getOcrPostProcessScript());
        ps.setBoolean(17, preset.isTextLocationScriptEnabled());
        ps.setBoolean(18, preset.isCharClassifyScriptEnabled());
        ps.setBoolean(19, preset.isOcrPostProcessScriptEnabled());
        
        ps.executeUpdate();
        ps.close();
        LogUtil.log("[saveStringPreset] 执行完成");
    }
    
    /**
     * 新增字符串预设（自动生成ID）
     */
    public int addStringPreset(StringPreset preset) throws SQLException {
        LogUtil.log("[addStringPreset] 开始执行");
        String sql = "INSERT INTO string_presets(" +
                "preset_name, correct_string, standard_image, image_path, " +
                "grayscale, gamma, min_area, max_area, min_width, max_width, min_height, max_height, " +
                "text_location_script, char_classify_script, ocr_post_process_script, " +
                "text_location_script_enabled, char_classify_script_enabled, ocr_post_process_script_enabled, " +
                "char_relative_position_enabled, char_relative_positions_json) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, preset.getPresetName());
        ps.setString(2, preset.getCorrectString());
        ps.setBytes(3, preset.getStandardImage());
        ps.setString(4, preset.getImagePath());
        ps.setInt(5, preset.getGrayscale());
        ps.setDouble(6, preset.getGamma());
        ps.setInt(7, preset.getMinArea());
        ps.setInt(8, preset.getMaxArea());
        ps.setInt(9, preset.getMinWidth());
        ps.setInt(10, preset.getMaxWidth());
        ps.setInt(11, preset.getMinHeight());
        ps.setInt(12, preset.getMaxHeight());
        ps.setString(13, preset.getTextLocationScript());
        ps.setString(14, preset.getCharClassifyScript());
        ps.setString(15, preset.getOcrPostProcessScript());
        ps.setBoolean(16, preset.isTextLocationScriptEnabled());
        ps.setBoolean(17, preset.isCharClassifyScriptEnabled());
        ps.setBoolean(18, preset.isOcrPostProcessScriptEnabled());
        ps.setBoolean(19, preset.isCharRelativePositionEnabled());
        ps.setString(20, preset.getCharRelativePositionsJson());
        
        ps.executeUpdate();
        
        ResultSet rs = ps.getGeneratedKeys();
        int id = -1;
        if (rs.next()) {
            id = rs.getInt(1);
        }
        rs.close();
        ps.close();
        LogUtil.log("[addStringPreset] 执行完成，ID=" + id);
        return id;
    }
    
    /**
     * 更新字符串预设
     */
    public void updateStringPreset(StringPreset preset) throws SQLException {
        LogUtil.log("[updateStringPreset] 开始执行");
        String sql = "UPDATE string_presets SET " +
                "preset_name=?, correct_string=?, standard_image=?, image_path=?, " +
                "grayscale=?, gamma=?, min_area=?, max_area=?, min_width=?, max_width=?, min_height=?, max_height=?, " +
                "text_location_script=?, char_classify_script=?, ocr_post_process_script=?, " +
                "text_location_script_enabled=?, char_classify_script_enabled=?, ocr_post_process_script_enabled=?, " +
                "char_relative_position_enabled=?, char_relative_positions_json=?, " +
                "updated_at=CURRENT_TIMESTAMP WHERE id=?";
        
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, preset.getPresetName());
        ps.setString(2, preset.getCorrectString());
        ps.setBytes(3, preset.getStandardImage());
        ps.setString(4, preset.getImagePath());
        ps.setInt(5, preset.getGrayscale());
        ps.setDouble(6, preset.getGamma());
        ps.setInt(7, preset.getMinArea());
        ps.setInt(8, preset.getMaxArea());
        ps.setInt(9, preset.getMinWidth());
        ps.setInt(10, preset.getMaxWidth());
        ps.setInt(11, preset.getMinHeight());
        ps.setInt(12, preset.getMaxHeight());
        ps.setString(13, preset.getTextLocationScript());
        ps.setString(14, preset.getCharClassifyScript());
        ps.setString(15, preset.getOcrPostProcessScript());
        ps.setBoolean(16, preset.isTextLocationScriptEnabled());
        ps.setBoolean(17, preset.isCharClassifyScriptEnabled());
        ps.setBoolean(18, preset.isOcrPostProcessScriptEnabled());
        ps.setBoolean(19, preset.isCharRelativePositionEnabled());
        ps.setString(20, preset.getCharRelativePositionsJson());
        ps.setInt(21, preset.getId());
        
        ps.executeUpdate();
        ps.close();
        LogUtil.log("[updateStringPreset] 执行完成");
    }
    
    /**
     * 删除字符串预设
     */
    public void deleteStringPreset(int id) throws SQLException {
        LogUtil.log("[deleteStringPreset] 开始执行");
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM string_presets WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        LogUtil.log("[deleteStringPreset] 执行完成");
    }
    
    /**
     * 根据ID获取字符串预设
     */
    public StringPreset getStringPreset(int id) throws SQLException {
        LogUtil.log("[getStringPreset] 开始执行");
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM string_presets WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        
        StringPreset preset = null;
        if (rs.next()) {
            preset = resultSetToPreset(rs);
        }
        
        rs.close();
        ps.close();
        LogUtil.log("[getStringPreset] 执行完成");
        return preset;
    }
    
    /**
     * 获取所有字符串预设
     */
    public List<StringPreset> getAllStringPresets() throws SQLException {
        LogUtil.log("[getAllStringPresets] 开始执行");
        List<StringPreset> presets = new ArrayList<>();
        
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM string_presets ORDER BY id");
        
        while (rs.next()) {
            presets.add(resultSetToPreset(rs));
        }
        
        rs.close();
        stmt.close();
        LogUtil.log("[getAllStringPresets] 执行完成，共" + presets.size() + "条");
        return presets;
    }
    
    /**
     * 将ResultSet转换为StringPreset
     */
    private StringPreset resultSetToPreset(ResultSet rs) throws SQLException {
        StringPreset preset = new StringPreset();
        preset.setId(rs.getInt("id"));
        preset.setPresetName(rs.getString("preset_name"));
        preset.setCorrectString(rs.getString("correct_string"));
        preset.setStandardImage(rs.getBytes("standard_image"));
        preset.setImagePath(rs.getString("image_path"));
        preset.setGrayscale(rs.getInt("grayscale"));
        preset.setGamma(rs.getDouble("gamma"));
        preset.setMinArea(rs.getInt("min_area"));
        preset.setMaxArea(rs.getInt("max_area"));
        preset.setMinWidth(rs.getInt("min_width"));
        preset.setMaxWidth(rs.getInt("max_width"));
        preset.setMinHeight(rs.getInt("min_height"));
        preset.setMaxHeight(rs.getInt("max_height"));
        preset.setTextLocationScript(rs.getString("text_location_script"));
        preset.setCharClassifyScript(rs.getString("char_classify_script"));
        preset.setOcrPostProcessScript(rs.getString("ocr_post_process_script"));
        preset.setTextLocationScriptEnabled(rs.getBoolean("text_location_script_enabled"));
        preset.setCharClassifyScriptEnabled(rs.getBoolean("char_classify_script_enabled"));
        preset.setOcrPostProcessScriptEnabled(rs.getBoolean("ocr_post_process_script_enabled"));
        preset.setCharRelativePositionEnabled(rs.getBoolean("char_relative_position_enabled"));
        String charPosJson = rs.getString("char_relative_positions_json");
        if (charPosJson != null && !charPosJson.isEmpty()) {
            preset.setCharRelativePositionsFromJson(charPosJson);
        }
        return preset;
    }
}
