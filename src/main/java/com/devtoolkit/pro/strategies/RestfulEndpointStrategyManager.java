package com.devtoolkit.pro.strategies;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.strategies.impl.SpringEndpointScanStrategy;
import com.devtoolkit.pro.strategies.impl.FastApiEndpointScanStrategy;
import com.devtoolkit.pro.strategies.impl.JaxRsEndpointScanStrategy;
import com.intellij.openapi.project.Project;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;

import java.util.*;

/**
 * RESTful端点扫描策略管理器
 * 负责管理和调度不同的扫描策略
 */
public class RestfulEndpointStrategyManager {
    
    private final List<RestfulEndpointScanStrategy> strategies;
    private final Project project;
    
    public RestfulEndpointStrategyManager(Project project) {
        this.project = project;
        this.strategies = initializeStrategies();
    }
    
    /**
     * 初始化所有可用的策略
     */
    private List<RestfulEndpointScanStrategy> initializeStrategies() {
        List<RestfulEndpointScanStrategy> strategyList = new ArrayList<>();
        
        // 检查IDE环境
        String ideEnvironment = detectIDEEnvironment();
        
        // 始终添加FastAPI策略（支持所有IDE）
        strategyList.add(new FastApiEndpointScanStrategy());
        
        // 只在Java模块可用时添加Java相关策略
        if (isJavaModuleAvailable()) {
            strategyList.add(new SpringEndpointScanStrategy());
            strategyList.add(new JaxRsEndpointScanStrategy());

        } else {

        }
        
        // 按优先级排序（数值越小优先级越高）
        strategyList.sort(Comparator.comparingInt(RestfulEndpointScanStrategy::getPriority));
        
        return strategyList;
    }
    
    /**
     * 检查Java模块是否可用
     */
    private boolean isJavaModuleAvailable() {
        try {
            // 检查Java模块是否被加载
            IdeaPluginDescriptor javaPlugin = PluginManager.getPlugin(PluginId.getId("com.intellij.java"));
            if (javaPlugin != null && javaPlugin.isEnabled()) {
                return true;
            }
            
            // 如果上面失败，尝试检查核心Java类是否可用
            Class.forName("com.intellij.psi.PsiJavaFile");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测IDE环境
     */
    private String detectIDEEnvironment() {
        try {
            String ideaProduct = System.getProperty("idea.platform.prefix");
            if ("PyCharmCore".equals(ideaProduct) || "PyCharm".equals(ideaProduct)) {
                return "PyCharm";
            } else if ("Idea".equals(ideaProduct) || ideaProduct == null) {
                return "IntelliJ IDEA";
            } else {
                return ideaProduct != null ? ideaProduct : "Unknown";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * 获取所有适用的策略
     */
    public List<RestfulEndpointScanStrategy> getApplicableStrategies() {
        List<RestfulEndpointScanStrategy> applicableStrategies = new ArrayList<>();
        
        for (RestfulEndpointScanStrategy strategy : strategies) {
            try {
                if (strategy.isApplicable(project)) {
                    applicableStrategies.add(strategy);

                }
            } catch (Exception e) {
                // Strategy applicability check failed, continue with next
            }
        }
        
        return applicableStrategies;
    }
    
    /**
     * 使用最佳策略扫描端点
     * 选择第一个适用的策略进行扫描
     */
    public List<RestfulEndpointNavigationItem> scanWithBestStrategy() {
        List<RestfulEndpointScanStrategy> applicableStrategies = getApplicableStrategies();
        
        if (applicableStrategies.isEmpty()) {

            return scanWithAllStrategies();
        }
        
        // 使用优先级最高的策略
        RestfulEndpointScanStrategy bestStrategy = applicableStrategies.get(0);

        
        try {
            List<RestfulEndpointNavigationItem> endpoints = bestStrategy.scanEndpoints(project);
            
            // 如果最佳策略找到的端点较少，尝试其他策略补充
            if (endpoints.size() < 3 && applicableStrategies.size() > 1) {

                return scanWithMultipleStrategies(applicableStrategies);
            }
            
            return endpoints;
        } catch (Exception e) {
            return scanWithAllStrategies();
        }
    }
    
    /**
     * 使用多个策略进行扫描
     */
    public List<RestfulEndpointNavigationItem> scanWithMultipleStrategies(List<RestfulEndpointScanStrategy> strategiesToUse) {
        List<RestfulEndpointNavigationItem> allEndpoints = new ArrayList<>();
        
        for (RestfulEndpointScanStrategy strategy : strategiesToUse) {
            try {
                List<RestfulEndpointNavigationItem> endpoints = strategy.scanEndpoints(project);
                allEndpoints.addAll(endpoints);
            } catch (Exception e) {
                // Strategy failed, continue with next
            }
        }
        
        return deduplicateEndpoints(allEndpoints);
    }
    
    /**
     * 使用所有策略进行扫描（回退方案）
     */
    public List<RestfulEndpointNavigationItem> scanWithAllStrategies() {
        return scanWithMultipleStrategies(strategies);
    }
    
    /**
     * 根据框架名称获取策略
     */
    public List<RestfulEndpointScanStrategy> getStrategiesByFramework(String frameworkName) {
        List<RestfulEndpointScanStrategy> matchingStrategies = new ArrayList<>();
        
        for (RestfulEndpointScanStrategy strategy : strategies) {
            if (strategy.supportsFramework(frameworkName)) {
                matchingStrategies.add(strategy);
            }
        }
        
        return matchingStrategies;
    }
    
    /**
     * 获取所有可用的策略信息
     */
    public List<StrategyInfo> getAllStrategyInfo() {
        List<StrategyInfo> strategyInfos = new ArrayList<>();
        
        for (RestfulEndpointScanStrategy strategy : strategies) {
            StrategyInfo info = new StrategyInfo(
                strategy.getStrategyName(),
                strategy.getPriority(),
                strategy.isApplicable(project)
            );
            strategyInfos.add(info);
        }
        
        return strategyInfos;
    }
    
    /**
     * 去重端点列表
     */
    private List<RestfulEndpointNavigationItem> deduplicateEndpoints(List<RestfulEndpointNavigationItem> endpoints) {
        Map<String, RestfulEndpointNavigationItem> uniqueEndpoints = new LinkedHashMap<>();
        
        for (RestfulEndpointNavigationItem endpoint : endpoints) {
            String key = endpoint.getHttpMethod() + ":" + endpoint.getPath() + ":" + 
                        endpoint.getClassName() + "." + endpoint.getMethodName();
            
            // 如果已存在相同的端点，保留第一个（优先级高的策略找到的）
            if (!uniqueEndpoints.containsKey(key)) {
                uniqueEndpoints.put(key, endpoint);
            }
        }
        
        List<RestfulEndpointNavigationItem> result = new ArrayList<>(uniqueEndpoints.values());
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }
    
    /**
     * 策略信息内部类
     */
    public static class StrategyInfo {
        private final String name;
        private final int priority;
        private final boolean applicable;
        
        public StrategyInfo(String name, int priority, boolean applicable) {
            this.name = name;
            this.priority = priority;
            this.applicable = applicable;
        }
        
        public String getName() {
            return name;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public boolean isApplicable() {
            return applicable;
        }
        
        @Override
        public String toString() {
            return String.format("Strategy{name='%s', priority=%d, applicable=%s}", 
                               name, priority, applicable);
        }
    }
}