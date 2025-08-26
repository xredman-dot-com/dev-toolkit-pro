package com.devtoolkit.pro.strategies;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.intellij.openapi.project.Project;

import java.util.List;

/**
 * RESTful端点扫描策略接口
 * 定义了不同框架和IDE环境下的端点扫描策略
 */
public interface RestfulEndpointScanStrategy {
    
    /**
     * 策略名称
     * @return 策略的唯一标识名称
     */
    String getStrategyName();
    
    /**
     * 检查当前策略是否适用于当前项目
     * @param project 当前项目
     * @return 如果策略适用返回true，否则返回false
     */
    boolean isApplicable(Project project);
    
    /**
     * 获取策略的优先级
     * 数值越小优先级越高
     * @return 优先级数值
     */
    int getPriority();
    
    /**
     * 扫描RESTful端点
     * @param project 当前项目
     * @return 找到的RESTful端点列表
     */
    List<RestfulEndpointNavigationItem> scanEndpoints(Project project);
    
    /**
     * 检查策略是否支持指定的框架
     * @param frameworkName 框架名称（如"spring", "fastapi", "flask"等）
     * @return 是否支持
     */
    boolean supportsFramework(String frameworkName);
}