package com.devtoolkit.pro.strategies.enhancements;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.Map;

/**
 * FastAPI增强策略接口
 * 提供高级功能如依赖注入分析、中间件检测、OpenAPI集成等
 */
public interface FastApiEnhancedStrategy {
    
    /**
     * 检测FastAPI应用的依赖注入
     * @param project 项目实例
     * @return 依赖注入信息映射
     */
    Map<String, List<String>> detectDependencyInjection(Project project);
    
    /**
     * 分析中间件配置
     * @param project 项目实例
     * @return 中间件信息列表
     */
    List<MiddlewareInfo> analyzeMiddleware(Project project);
    
    /**
     * 提取路由标签和元数据
     * @param project 项目实例
     * @return 端点增强信息列表
     */
    List<EnhancedEndpointInfo> extractRouteMetadata(Project project);
    
    /**
     * 支持动态路由发现（基于条件路由）
     * @param project 项目实例
     * @return 动态端点列表
     */
    List<RestfulEndpointNavigationItem> discoverDynamicRoutes(Project project);
    
    /**
     * 分析Pydantic模型和数据验证
     * @param project 项目实例
     * @return 数据模型信息
     */
    Map<String, ModelInfo> analyzePydanticModels(Project project);
    
    /**
     * 中间件信息类
     */
    class MiddlewareInfo {
        private String name;
        private String type;
        private String path;
        private Map<String, Object> config;
        
        public MiddlewareInfo(String name, String type, String path) {
            this.name = name;
            this.type = type;
            this.path = path;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
    
    /**
     * 增强端点信息类
     */
    class EnhancedEndpointInfo {
        private String path;
        private String method;
        private String summary;
        private String description;
        private List<String> tags;
        private String operationId;
        private boolean deprecated;
        private Map<String, Object> responses;
        
        public EnhancedEndpointInfo(String path, String method) {
            this.path = path;
            this.method = method;
        }
        
        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public String getOperationId() { return operationId; }
        public void setOperationId(String operationId) { this.operationId = operationId; }
        
        public boolean isDeprecated() { return deprecated; }
        public void setDeprecated(boolean deprecated) { this.deprecated = deprecated; }
        
        public Map<String, Object> getResponses() { return responses; }
        public void setResponses(Map<String, Object> responses) { this.responses = responses; }
    }
    
    /**
     * 数据模型信息类
     */
    class ModelInfo {
        private String name;
        private String className;
        private Map<String, String> fields;
        private List<String> validators;
        
        public ModelInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public Map<String, String> getFields() { return fields; }
        public void setFields(Map<String, String> fields) { this.fields = fields; }
        
        public List<String> getValidators() { return validators; }
        public void setValidators(List<String> validators) { this.validators = validators; }
    }
}