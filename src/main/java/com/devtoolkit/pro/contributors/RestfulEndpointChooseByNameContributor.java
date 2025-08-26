package com.devtoolkit.pro.contributors;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.services.RestfulUrlService;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

/**
 * RESTful端点选择器贡献者
 * 为IntelliJ IDEA的"Choose by Name"对话框提供RESTful端点搜索功能
 */
public class RestfulEndpointChooseByNameContributor implements ChooseByNameContributor {
    
    private static final Logger LOG = Logger.getInstance(RestfulEndpointChooseByNameContributor.class);

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
        try {
            LOG.info("Starting RESTful endpoint scan for project: " + project.getName());
            RestfulUrlService urlService = new RestfulUrlService(project);
            List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
            
            LOG.info("Found " + endpoints.size() + " RESTful endpoints");
            
            if (endpoints.isEmpty()) {
                LOG.warn("No RESTful endpoints found in project: " + project.getName());
                return new String[0];
            }
            
            String[] names = endpoints.stream()
                    .map(NavigationItem::getName)
                    .toArray(String[]::new);
                    
            LOG.info("Returning " + names.length + " endpoint names");
            return names;
        } catch (Exception e) {
            LOG.error("Error while scanning RESTful endpoints", e);
            return new String[0];
        }
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(@NotNull String name, @NotNull String pattern, 
                                         @NotNull Project project, boolean includeNonProjectItems) {
        try {
            LOG.debug("Looking for endpoint with name: " + name);
            RestfulUrlService urlService = new RestfulUrlService(project);
            List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
            
            NavigationItem[] items = endpoints.stream()
                    .filter(endpoint -> endpoint.getName().equals(name))
                    .toArray(NavigationItem[]::new);
                    
            LOG.debug("Found " + items.length + " matching items for name: " + name);
            return items;
        } catch (Exception e) {
            LOG.error("Error while finding endpoint by name: " + name, e);
            return new NavigationItem[0];
        }
    }
}