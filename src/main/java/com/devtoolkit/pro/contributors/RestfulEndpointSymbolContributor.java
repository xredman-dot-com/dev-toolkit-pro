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

/**
 * RESTful端点符号贡献者
 * 专门为Symbols标签页提供RESTful端点
 */
public class RestfulEndpointSymbolContributor implements ChooseByNameContributor {
    
    private static final Logger LOG = Logger.getInstance(RestfulEndpointSymbolContributor.class);

    @NotNull
    @Override
    public String[] getNames(@NotNull Project project, boolean includeNonProjectItems) {
        try {
            LOG.info("Symbol scan: Starting RESTful endpoint scan for project: " + project.getName());
            
            RestfulUrlService urlService = new RestfulUrlService(project);
            List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
            
            LOG.info("Symbol scan: Found " + endpoints.size() + " RESTful endpoints");
            
            if (endpoints.isEmpty()) {
                LOG.warn("Symbol scan: No RESTful endpoints found in project: " + project.getName());
                return ArrayUtil.EMPTY_STRING_ARRAY;
            }
            
            String[] names = endpoints.stream()
                    .map(endpoint -> "API: " + endpoint.getName()) // 添加前缀以区分
                    .toArray(String[]::new);
                    
            LOG.info("Symbol scan: Returning " + names.length + " endpoint names");
            return names;
        } catch (Exception e) {
            LOG.error("Symbol scan: Error while scanning RESTful endpoints", e);
            return ArrayUtil.EMPTY_STRING_ARRAY;
        }
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(@NotNull String name, @NotNull String pattern, 
                                         @NotNull Project project, boolean includeNonProjectItems) {
        try {
            LOG.debug("Symbol scan: Looking for endpoint with name: " + name);
            
            // 移除"API: "前缀
            String actualName = name.startsWith("API: ") ? name.substring(5) : name;
            
            RestfulUrlService urlService = new RestfulUrlService(project);
            List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
            
            NavigationItem[] items = endpoints.stream()
                    .filter(endpoint -> endpoint.getName().equals(actualName))
                    .toArray(NavigationItem[]::new);
                    
            LOG.debug("Symbol scan: Found " + items.length + " matching items for name: " + name);
            return items;
        } catch (Exception e) {
            LOG.error("Symbol scan: Error while finding endpoint by name: " + name, e);
            return new NavigationItem[0];
        }
    }
}