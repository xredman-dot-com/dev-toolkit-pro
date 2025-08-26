package com.devtoolkit.pro.strategies.impl;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.strategies.RestfulEndpointScanStrategy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FastAPI框架RESTful端点扫描策略
 * 支持PyCharm中的FastAPI项目，包括复杂的路由场景
 * 正确处理include_router的多层嵌套和前缀计算
 */
public class FastApiEndpointScanStrategy implements RestfulEndpointScanStrategy {
    
    private static final String STRATEGY_NAME = "FastAPI";
    private static final int PRIORITY = 2; // 中等优先级
    
    // FastAPI路由装饰器模式（改进的正则，支持多行和复杂格式）
    private static final Pattern ROUTE_DECORATOR_PATTERN = Pattern.compile(
        "@(\\w+)\\.(get|post|put|delete|patch|head|options|trace)\\s*\\(\\s*[\"']([^\"']*)[\"'][^)]*\\)", 
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // 路由器定义模式（支持带参数的APIRouter）
    private static final Pattern ROUTER_DEFINITION_PATTERN = Pattern.compile(
        "(\\w+)\\s*=\\s*APIRouter\\s*\\(([^)]*)\\)",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // include_router模式（更精确的捕获）
    private static final Pattern INCLUDE_ROUTER_PATTERN = Pattern.compile(
        "(\\w+)\\.include_router\\s*\\(\\s*(\\w+)(?:[^,)]*,\\s*prefix\\s*=\\s*[\"\"']([^\"\"']*)[\"\"'])?[^)]*\\)",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // 应用实例定义模式
    private static final Pattern APP_DEFINITION_PATTERN = Pattern.compile(
        "(\\w+)\\s*=\\s*FastAPI\\s*\\([^)]*\\)",
        Pattern.MULTILINE
    );
    
    // 函数定义模式（支持async函数）
    private static final Pattern FUNCTION_DEFINITION_PATTERN = Pattern.compile(
        "(?:async\\s+)?def\\s+(\\w+)\\s*\\([^)]*\\)\\s*:",
        Pattern.MULTILINE
    );
    
    // 路由器定义中的prefix提取模式
    private static final Pattern ROUTER_PREFIX_PATTERN = Pattern.compile(
        "prefix\\s*=\\s*[\"\"']([^\"\"']*)[\"\"']"
    );
    
    private PsiManager psiManager;
    
    // 路由器信息
    private static class RouterInfo {
        String name;
        String prefix = "";
        List<RouteEndpoint> endpoints = new ArrayList<>();
        Set<String> includedRouters = new LinkedHashSet<>();
        
        RouterInfo(String name) {
            this.name = name;
        }
    }
    
    // 路由端点信息
    private static class RouteEndpoint {
        String httpMethod;
        String path;
        String functionName;
        
        RouteEndpoint(String httpMethod, String path, String functionName) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.functionName = functionName;
        }
    }
    
    // include关系
    private static class IncludeRelation {
        String parentRouter;
        String childRouter;
        String prefix;
        
        IncludeRelation(String parentRouter, String childRouter, String prefix) {
            this.parentRouter = parentRouter;
            this.childRouter = childRouter;
            this.prefix = prefix != null ? prefix : "";
        }
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean isApplicable(Project project) {
        try {
            // 检查是否有Python支持
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null || "UNKNOWN".equals(pythonFileType.getName())) {
                return false; // 没有Python支持，不适用
            }
            
            // 检查项目中是否存在FastAPI相关的依赖或导入
            return hasFastApiDependencies(project) || hasFastApiImports(project);
        } catch (Exception e) {
            // 如果出现异常（如Python插件不可用），返回false
            System.err.println("FastAPI strategy not applicable: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean supportsFramework(String frameworkName) {
        return "fastapi".equalsIgnoreCase(frameworkName) || 
               "fast-api".equalsIgnoreCase(frameworkName);
    }
    
    @Override
    public List<RestfulEndpointNavigationItem> scanEndpoints(Project project) {
        this.psiManager = PsiManager.getInstance(project);
        List<RestfulEndpointNavigationItem> endpoints = new ArrayList<>();
        
        try {
            // 全局路由器信息存储
            Map<String, RouterInfo> allRouters = new HashMap<>();
            List<IncludeRelation> includeRelations = new ArrayList<>();
            
            // 第一步：扫描Python文件，收集路由器定义和路由信息
            collectRouterInfo(project, allRouters, includeRelations);
            
            // 第二步：解析include关系，构建路由器树
            buildRouterTree(allRouters, includeRelations);
            
            // 第三步：生成最终的端点列表
            generateEndpoints(allRouters, endpoints, project);
            
            // 去重并排序
            return deduplicateAndSort(endpoints);
            
        } catch (Exception e) {
            System.err.println("FastAPI strategy scan failed: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 第一步：收集所有路由器信息和include关系
     */
    private void collectRouterInfo(Project project, Map<String, RouterInfo> allRouters, 
                                 List<IncludeRelation> includeRelations) {
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) return;
            
            Collection<VirtualFile> pythonFiles = FileTypeIndex.getFiles(pythonFileType, 
                GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : pythonFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    collectFromFile(psiFile, allRouters, includeRelations);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to collect router info: " + e.getMessage());
        }
    }
    
    /**
     * 从单个文件收集路由器信息
     */
    private void collectFromFile(PsiFile pyFile, Map<String, RouterInfo> allRouters, 
                               List<IncludeRelation> includeRelations) {
        try {
            String fileText = pyFile.getText();
            String fileName = pyFile.getName();
            
            // 1. 收集FastAPI应用定义（作为特殊的路由器处理）
            collectAppDefinitions(fileText, allRouters);
            
            // 2. 收集APIRouter定义
            collectRouterDefinitions(fileText, allRouters);
            
            // 3. 收集路由装饰器
            collectRouteDecorators(fileText, fileName, allRouters);
            
            // 4. 收集include_router关系
            collectIncludeRelations(fileText, includeRelations);
            
        } catch (Exception e) {
            System.err.println("Error collecting from file " + pyFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * 收集FastAPI应用定义
     */
    private void collectAppDefinitions(String fileText, Map<String, RouterInfo> allRouters) {
        Matcher appMatcher = APP_DEFINITION_PATTERN.matcher(fileText);
        while (appMatcher.find()) {
            String appName = appMatcher.group(1);
            RouterInfo appRouter = new RouterInfo(appName);
            appRouter.prefix = ""; // app的基本前缀为空
            allRouters.put(appName, appRouter);
        }
    }
    
    /**
     * 收集APIRouter定义
     */
    private void collectRouterDefinitions(String fileText, Map<String, RouterInfo> allRouters) {
        Matcher routerMatcher = ROUTER_DEFINITION_PATTERN.matcher(fileText);
        while (routerMatcher.find()) {
            String routerName = routerMatcher.group(1);
            String routerParams = routerMatcher.group(2);
            
            RouterInfo router = new RouterInfo(routerName);
            
            // 提取路由器定义中的prefix
            if (routerParams != null) {
                Matcher prefixMatcher = ROUTER_PREFIX_PATTERN.matcher(routerParams);
                if (prefixMatcher.find()) {
                    router.prefix = prefixMatcher.group(1);
                }
            }
            
            allRouters.put(routerName, router);
        }
    }
    
    /**
     * 收集路由装饰器
     */
    private void collectRouteDecorators(String fileText, String fileName, Map<String, RouterInfo> allRouters) {
        Matcher routeMatcher = ROUTE_DECORATOR_PATTERN.matcher(fileText);
        
        while (routeMatcher.find()) {
            String instanceName = routeMatcher.group(1);  // app 或 router 名称
            String httpMethod = routeMatcher.group(2).toUpperCase();
            String path = routeMatcher.group(3);
            
            // 查找对应的函数名
            String functionName = findFunctionNameAfterDecorator(fileText, routeMatcher.start());
            if (functionName == null || functionName.isEmpty()) {
                functionName = "unknown_function";
            }
            
            // 将路由添加到对应的路由器
            RouterInfo router = allRouters.get(instanceName);
            if (router == null) {
                // 如果路由器不存在，创建一个默认的
                router = new RouterInfo(instanceName);
                allRouters.put(instanceName, router);
            }
            
            RouteEndpoint endpoint = new RouteEndpoint(httpMethod, path, functionName);
            router.endpoints.add(endpoint);
        }
    }
    
    /**
     * 收集include_router关系
     */
    private void collectIncludeRelations(String fileText, List<IncludeRelation> includeRelations) {
        Matcher includeMatcher = INCLUDE_ROUTER_PATTERN.matcher(fileText);
        while (includeMatcher.find()) {
            String parentRouter = includeMatcher.group(1);
            String childRouter = includeMatcher.group(2);
            String prefix = includeMatcher.group(3); // 可能为null
            
            IncludeRelation relation = new IncludeRelation(parentRouter, childRouter, prefix);
            includeRelations.add(relation);
        }
    }
    
    /**
     * 第二步：构建路由器树，处理include关系
     */
    private void buildRouterTree(Map<String, RouterInfo> allRouters, List<IncludeRelation> includeRelations) {
        // 使用拓扑排序处理嵌套include关系
        Map<String, Set<String>> dependencies = new HashMap<>();
        Map<String, Set<String>> dependents = new HashMap<>();
        
        // 初始化依赖关系图
        for (String routerName : allRouters.keySet()) {
            dependencies.put(routerName, new HashSet<>());
            dependents.put(routerName, new HashSet<>());
        }
        
        // 构建依赖关系
        for (IncludeRelation relation : includeRelations) {
            dependencies.get(relation.parentRouter).add(relation.childRouter);
            dependents.get(relation.childRouter).add(relation.parentRouter);
            
            // 记录include关系
            RouterInfo parentRouter = allRouters.get(relation.parentRouter);
            if (parentRouter != null) {
                parentRouter.includedRouters.add(relation.childRouter);
            }
        }
        
        // 按依赖关系处理路由器（从叶子节点开始）
        Set<String> processed = new HashSet<>();
        for (String routerName : allRouters.keySet()) {
            if (!processed.contains(routerName)) {
                processRouterRecursively(routerName, allRouters, includeRelations, processed);
            }
        }
    }
    
    /**
     * 递归处理路由器及其依赖
     */
    private void processRouterRecursively(String routerName, Map<String, RouterInfo> allRouters, 
                                        List<IncludeRelation> includeRelations, Set<String> processed) {
        if (processed.contains(routerName)) {
            return;
        }
        
        RouterInfo router = allRouters.get(routerName);
        if (router == null) {
            processed.add(routerName);
            return;
        }
        
        // 先处理所有被包含的路由器
        for (String includedRouterName : router.includedRouters) {
            if (!processed.contains(includedRouterName)) {
                processRouterRecursively(includedRouterName, allRouters, includeRelations, processed);
            }
        }
        
        // 处理当前路由器：合并被包含路由器的端点
        for (IncludeRelation relation : includeRelations) {
            if (relation.parentRouter.equals(routerName)) {
                RouterInfo childRouter = allRouters.get(relation.childRouter);
                if (childRouter != null) {
                    // 为子路由器的所有端点添加前缀
                    for (RouteEndpoint endpoint : childRouter.endpoints) {
                        String newPath = combinePathWithPrefix(relation.prefix, endpoint.path);
                        RouteEndpoint newEndpoint = new RouteEndpoint(endpoint.httpMethod, newPath, endpoint.functionName);
                        router.endpoints.add(newEndpoint);
                    }
                }
            }
        }
        
        processed.add(routerName);
    }
    
    /**
     * 第三步：生成最终的端点列表
     */
    private void generateEndpoints(Map<String, RouterInfo> allRouters, 
                                 List<RestfulEndpointNavigationItem> endpoints, 
                                 Project project) {
        for (RouterInfo router : allRouters.values()) {
            // 只从主应用（FastAPI实例）生成端点，避免重复
            if (isMainApp(router, allRouters)) {
                for (RouteEndpoint endpoint : router.endpoints) {
                    String fullPath = combinePathWithPrefix(router.prefix, endpoint.path);
                    
                    RestfulEndpointNavigationItem navigationItem = new RestfulEndpointNavigationItem(
                        endpoint.httpMethod, fullPath, router.name, endpoint.functionName, null, project
                    );
                    endpoints.add(navigationItem);
                }
            }
        }
    }
    
    /**
     * 判断是否为主应用（不被其他路由器include的路由器）
     */
    private boolean isMainApp(RouterInfo router, Map<String, RouterInfo> allRouters) {
        for (RouterInfo otherRouter : allRouters.values()) {
            if (otherRouter.includedRouters.contains(router.name)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 组合路径和前缀
     */
    private String combinePathWithPrefix(String prefix, String path) {
        if (prefix == null || prefix.isEmpty()) {
            return ensureStartsWithSlash(path);
        }
        
        prefix = ensureStartsWithSlash(prefix);
        path = ensureStartsWithSlash(path);
        
        if (prefix.equals("/") && path.equals("/")) {
            return "/";
        }
        
        if (prefix.equals("/")) {
            return path;
        }
        
        if (path.equals("/")) {
            return prefix;
        }
        
        // 移除前缀末尾的/或路径开头的/以避免重复
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (path.startsWith("/")) {
            return prefix + path;
        } else {
            return prefix + "/" + path;
        }
    }
    
    /**
     * 确保路径以/开头
     */
    private String ensureStartsWithSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (path.startsWith("/")) {
            return path;
        }
        return "/" + path;
    }
    
    /**
     * 检查项目是否有FastAPI依赖
     */
    private boolean hasFastApiDependencies(Project project) {
        try {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) return false;
            
            // 检查requirements.txt
            VirtualFile requirementsFile = baseDir.findChild("requirements.txt");
            if (requirementsFile != null) {
                PsiFile psiFile = psiManager.findFile(requirementsFile);
                if (psiFile != null && psiFile.getText().contains("fastapi")) {
                    return true;
                }
            }
            
            // 检查pyproject.toml
            VirtualFile pyprojectFile = baseDir.findChild("pyproject.toml");
            if (pyprojectFile != null) {
                PsiFile psiFile = psiManager.findFile(pyprojectFile);
                if (psiFile != null && psiFile.getText().contains("fastapi")) {
                    return true;
                }
            }
            
            // 检查Pipfile
            VirtualFile pipFile = baseDir.findChild("Pipfile");
            if (pipFile != null) {
                PsiFile psiFile = psiManager.findFile(pipFile);
                if (psiFile != null && psiFile.getText().contains("fastapi")) {
                    return true;
                }
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }
    
    /**
     * 检查项目是否有FastAPI导入
     */
    private boolean hasFastApiImports(Project project) {
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) return false;
            
            Collection<VirtualFile> pythonFiles = FileTypeIndex.getFiles(pythonFileType, 
                GlobalSearchScope.projectScope(project));
            
            for (VirtualFile virtualFile : pythonFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    if (hasFastApiImport(psiFile)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }
    
    /**
     * 检查Python文件是否有FastAPI导入
     */
    private boolean hasFastApiImport(PsiFile pyFile) {
        try {
            // 直接检查文件内容，不依赖Python PSI
            String fileText = pyFile.getText();
            return fileText.contains("from fastapi") || 
                   fileText.contains("import fastapi") ||
                   fileText.contains("FastAPI") ||
                   fileText.contains("APIRouter");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从文本中查找装饰器后面的函数名
     */
    private String findFunctionNameAfterDecorator(String fileText, int decoratorStart) {
        try {
            // 从装饰器位置往后查找 def 关键字
            String afterDecorator = fileText.substring(decoratorStart);
            
            // 查找下一个函数定义
            Matcher functionMatcher = FUNCTION_DEFINITION_PATTERN.matcher(afterDecorator);
            
            if (functionMatcher.find()) {
                return functionMatcher.group(1);
            }
            
        } catch (Exception e) {
            System.err.println("Error finding function name: " + e.getMessage());
        }
        return "unknown_function";
    }
    
    /**
     * 去重并排序
     */
    private List<RestfulEndpointNavigationItem> deduplicateAndSort(List<RestfulEndpointNavigationItem> endpoints) {
        Map<String, RestfulEndpointNavigationItem> uniqueEndpoints = new LinkedHashMap<>();
        
        for (RestfulEndpointNavigationItem endpoint : endpoints) {
            String key = endpoint.getHttpMethod() + ":" + endpoint.getPath() + ":" + 
                        endpoint.getClassName() + "." + endpoint.getMethodName();
            uniqueEndpoints.put(key, endpoint);
        }
        
        List<RestfulEndpointNavigationItem> result = new ArrayList<>(uniqueEndpoints.values());
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }
}