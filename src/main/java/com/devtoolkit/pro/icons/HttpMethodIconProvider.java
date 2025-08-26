package com.devtoolkit.pro.icons;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP方法图标提供者
 * 为不同的HTTP方法提供对应的彩色图标
 */
public class HttpMethodIconProvider {
    
    private static final Map<String, Icon> ICON_CACHE = new HashMap<>();
    
    // 默认图标（用于未知HTTP方法）
    private static final Icon DEFAULT_ICON = IconLoader.getIcon("/icons/pluginIcon.svg", HttpMethodIconProvider.class);
    
    static {
        // 预加载所有HTTP方法图标
        loadIcon("GET", "/icons/http_get.svg");
        loadIcon("POST", "/icons/http_post.svg");
        loadIcon("PUT", "/icons/http_put.svg");
        loadIcon("DELETE", "/icons/http_delete.svg");
        loadIcon("PATCH", "/icons/http_patch.svg");
        loadIcon("HEAD", "/icons/http_head.svg");
        loadIcon("OPTIONS", "/icons/http_options.svg");
    }
    
    /**
     * 加载图标到缓存
     */
    private static void loadIcon(String method, String iconPath) {
        try {
            Icon icon = IconLoader.getIcon(iconPath, HttpMethodIconProvider.class);
            ICON_CACHE.put(method.toUpperCase(), icon);
        } catch (Exception e) {
            // 如果加载失败，使用默认图标
            ICON_CACHE.put(method.toUpperCase(), DEFAULT_ICON);
        }
    }
    
    /**
     * 根据HTTP方法获取对应的图标
     * 
     * @param httpMethod HTTP方法名称（如：GET, POST, PUT等）
     * @return 对应的图标，如果找不到则返回默认图标
     */
    public static Icon getIcon(String httpMethod) {
        if (httpMethod == null || httpMethod.trim().isEmpty()) {
            return DEFAULT_ICON;
        }
        
        String method = httpMethod.trim().toUpperCase();
        return ICON_CACHE.getOrDefault(method, DEFAULT_ICON);
    }
    
    /**
     * 检查是否支持指定的HTTP方法
     */
    public static boolean isSupported(String httpMethod) {
        if (httpMethod == null || httpMethod.trim().isEmpty()) {
            return false;
        }
        return ICON_CACHE.containsKey(httpMethod.trim().toUpperCase());
    }
    
    /**
     * 获取所有支持的HTTP方法
     */
    public static String[] getSupportedMethods() {
        return ICON_CACHE.keySet().toArray(new String[0]);
    }
}