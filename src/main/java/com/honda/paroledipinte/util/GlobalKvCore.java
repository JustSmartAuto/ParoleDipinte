package com.honda.paroledipinte.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局键值对数据交换类
 * 参考 GlobalKvCore.cs 实现的Java版本
 * 提供线程安全的全局数据存储和访问
 */
public class GlobalKvCore {
    
    // 全局线程安全字典（忽略大小写）
    private static final ConcurrentHashMap<String, Object> _kv = new ConcurrentHashMap<>();
    
    /**
     * 精准读取
     * @param key 键名
     * @param <T> 返回类型
     * @return 值，不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Object v = _kv.get(key.toLowerCase());
        if (v == null) return null;
        return (T) convert(v, null);
    }
    
    /**
     * 精准读取（带默认值）
     * @param key 键名
     * @param defaultValue 默认值
     * @param <T> 返回类型
     * @return 值，不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        Object v = _kv.get(key.toLowerCase());
        if (v == null) return defaultValue;
        return (T) convert(v, defaultValue);
    }
    
    /**
     * 模糊读取（支持 * 或 ?）
     * @param pattern 匹配模式，支持 * 和 ? 通配符
     * @param <T> 返回类型
     * @return 匹配的键值对
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getMany(String pattern) {
        String regex = "^" + Pattern.quote(pattern)
                .replace("\\*", ".*")
                .replace("\\?", ".") + "$";
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        
        return _kv.entrySet().stream()
                .filter(e -> p.matcher(e.getKey()).matches())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> (T) convert(e.getValue(), null)
                ));
    }
    
    /**
     * 万能类型转换
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T convert(Object v, T defaultValue) {
        if (v == null) return defaultValue;
        
        // 如果目标类型就是Object，直接返回
        if (defaultValue == null) {
            return (T) v;
        }
        
        // 如果已经是目标类型，直接返回
        if (defaultValue.getClass().isInstance(v)) {
            return (T) v;
        }
        
        // 特殊处理基本类型
        Class<?> targetClass = defaultValue.getClass();
        
        // 处理包装类型
        if (targetClass == Integer.class || targetClass == int.class) {
            if (v instanceof Number) return (T) Integer.valueOf(((Number) v).intValue());
            return (T) Integer.valueOf(v.toString());
        }
        if (targetClass == Long.class || targetClass == long.class) {
            if (v instanceof Number) return (T) Long.valueOf(((Number) v).longValue());
            return (T) Long.valueOf(v.toString());
        }
        if (targetClass == Double.class || targetClass == double.class) {
            if (v instanceof Number) return (T) Double.valueOf(((Number) v).doubleValue());
            return (T) Double.valueOf(v.toString());
        }
        if (targetClass == Float.class || targetClass == float.class) {
            if (v instanceof Number) return (T) Float.valueOf(((Number) v).floatValue());
            return (T) Float.valueOf(v.toString());
        }
        if (targetClass == Boolean.class || targetClass == boolean.class) {
            if (v instanceof Boolean) return (T) v;
            return (T) Boolean.valueOf(v.toString());
        }
        if (targetClass == String.class) {
            return (T) v.toString();
        }
        
        // 其他类型尝试强制转换
        try {
            return (T) v;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * 线程安全读取，无值返回 null，不抛异常
     * @param key 键名
     * @return 值，不存在时返回null
     */
    public static Object getValue(String key) {
        return _kv.get(key.toLowerCase());
    }
    
    /**
     * 线程安全写入（覆盖或新增）
     * @param key 键名
     * @param value 值
     */
    public static void setValue(String key, Object value) {
        _kv.put(key.toLowerCase(), value);
    }
    
    /**
     * 删除键值对
     * @param key 键名
     * @return 被删除的值，不存在则返回null
     */
    public static Object remove(String key) {
        return _kv.remove(key.toLowerCase());
    }
    
    /**
     * 检查键是否存在
     * @param key 键名
     * @return 是否存在
     */
    public static boolean containsKey(String key) {
        return _kv.containsKey(key.toLowerCase());
    }
    
    /**
     * 获取所有键
     * @return 键集合
     */
    public static java.util.Set<String> keySet() {
        return _kv.keySet();
    }
    
    /**
     * 清空所有数据
     */
    public static void clear() {
        _kv.clear();
    }
    
    /**
     * 获取键值对数量
     * @return 数量
     */
    public static int size() {
        return _kv.size();
    }
}
