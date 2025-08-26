package com.devtoolkit.pro.contributors;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.services.RestfulUrlService;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * RESTful端点选择器贡献者
 * 为IntelliJ IDEA的"Choose by Name"对话框提供RESTful端点搜索功能
 */
public class RestfulEndpointChooseByNameContributor implements ChooseByNameContributor {

    /**
     * 获取显示名称 - 用于自定义标签页名称
     */
    public String getQualifiedName() {
        return "Dev Toolkit Pro Endpoints";
    }

    /**
     * 获取元素类型 - 用于自定义标签页名称
     */
    public String getElementKind() {
        return "Dev Toolkit Pro Endpoints";
    }

    @NotNull
    @Override
    public String[] getNames(@NotNull Project project, boolean includeNonProjectItems) {
        RestfulUrlService urlService = new RestfulUrlService(project);
        List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
        
        return endpoints.stream()
                .map(NavigationItem::getName)
                .toArray(String[]::new);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(@NotNull String name, @NotNull String pattern, 
                                         @NotNull Project project, boolean includeNonProjectItems) {
        RestfulUrlService urlService = new RestfulUrlService(project);
        List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
        
        return endpoints.stream()
                .filter(endpoint -> endpoint.getName().equals(name))
                .toArray(NavigationItem[]::new);
    }
}