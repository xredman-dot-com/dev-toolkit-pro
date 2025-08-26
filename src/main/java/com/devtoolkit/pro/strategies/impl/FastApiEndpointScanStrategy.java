package com.devtoolkit.pro.strategies.impl;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.strategies.RestfulEndpointScanStrategy;
import com.devtoolkit.pro.strategies.enhancements.FastApiEnhancedStrategy;
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
 * FastAPI框架RESTful端点扫描策略（增强版）
 * 支持PyCharm中的FastAPI项目，包括复杂的路由场景
 * 正确处理include_router的多层嵌套和前缀计算
 * 增强功能：依赖注入分析、中间件检测、标签和元数据提取
 */
public class FastApiEndpointScanStrategy implements RestfulEndpointScanStrategy, FastApiEnhancedStrategy {
    
    private static final String STRATEGY_NAME = "FastAPI";
    private static final int PRIORITY = 2; // 中等优先级
    
    // FastAPI路由装饰器模式（改进的正则，支持多行和复杂格式）
    private static final Pattern ROUTE_DECORATOR_PATTERN = Pattern.compile(
        "@(\\w+)\\.(get|post|put|delete|patch|head|options|trace)\\s*\\(\\s*[\"']([^\"']*)[\"'][^)]*\\)", 
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // 增强的路由装饰器模式（支持更多参数）
    private static final Pattern ENHANCED_ROUTE_PATTERN = Pattern.compile(
        "@(\\w+)\\.(get|post|put|delete|patch|head|options|trace)\\s*\\(\\s*[\"']([^\"']*)[\"']([^)]*)\\)",
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
        "(\\w+)\\s*=\\s*FastAPI\\s*\\(([^)]*)\\)",
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
    
    // === 新增：增强功能的正则模式 ===
    
    // 依赖注入模式（Depends函数）
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "Depends\\s*\\(\\s*(\\w+)\\s*\\)",
        Pattern.MULTILINE
    );
    
    // 中间件模式
    private static final Pattern MIDDLEWARE_PATTERN = Pattern.compile(
        "(\\w+)\\.add_middleware\\s*\\(\\s*(\\w+)(?:[^)]*)\\)",
        Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // 标签提取模式
    private static final Pattern TAGS_PATTERN = Pattern.compile(
        "tags\\s*=\\s*\\[([^\\]]+)\\]"
    );
    
    // 响应模型模式
    private static final Pattern RESPONSE_MODEL_PATTERN = Pattern.compile(
        "response_model\\s*=\\s*(\\w+)"
    );
    
    // Pydantic模型定义模式
    private static final Pattern PYDANTIC_MODEL_PATTERN = Pattern.compile(
        "class\\s+(\\w+)\\s*\\(\\s*BaseModel\\s*\\)\\s*:",
        Pattern.MULTILINE
    );
    
    // 路由描述和摘要模式
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
        "summary\\s*=\\s*[\"\"']([^\"\"']*)[\"\"']"
    );
    
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
        "description\\s*=\\s*[\"\"']([^\"\"']*)[\"\"']"
    );
    
    private PsiManager psiManager;
    
    // === 增强的数据结构 ===
    
    // 路由器信息（增强版）
    private static class RouterInfo {
        String name;
        String prefix = "";
        List<RouteEndpoint> endpoints = new ArrayList<>();
        Set<String> includedRouters = new LinkedHashSet<>();
        Map<String, Object> metadata = new HashMap<>();
        List<String> middleware = new ArrayList<>();
        
        RouterInfo(String name) {
            this.name = name;
        }
    }
    
    // 路由端点信息（增强版）
    private static class RouteEndpoint {
        String httpMethod;
        String path;
        String functionName;
        
        // 增强属性
        String summary;
        String description;
        List<String> tags = new ArrayList<>();
        String responseModel;
        List<String> dependencies = new ArrayList<>();
        boolean deprecated = false;
        Map<String, Object> responses = new HashMap<>();
        
        RouteEndpoint(String httpMethod, String path, String functionName) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.functionName = functionName;
        }
        
        // 获取完整描述（包含元数据）
        public String getFullDescription() {
            StringBuilder desc = new StringBuilder();
            desc.append(httpMethod).append(" ").append(path);
            
            if (summary != null && !summary.isEmpty()) {
                desc.append(" - ").append(summary);
            }
            
            if (!tags.isEmpty()) {
                desc.append(" [").append(String.join(", ", tags)).append("]");
            }
            
            if (deprecated) {
                desc.append(" [DEPRECATED]");
            }
            
            return desc.toString();
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
            // 方法1: 检查是否在PyCharm中
            if (isPyCharmEnvironment()) {
                return hasFastApiDependencies(project) || hasFastApiImports(project);
            }
            
            // 方法2: 在IntelliJ IDEA中检查Python插件是否可用
            if (isPythonPluginAvailable()) {
                return hasFastApiDependencies(project) || hasFastApiImports(project);
            }
            
            // 方法3: 基础文件检查（作为回退方案）
            return hasBasicPythonFiles(project) && (hasFastApiDependencies(project) || hasFastApiImports(project));
            
        } catch (Exception e) {
            // 如果出现异常，使用回退检查
            System.err.println("FastAPI strategy applicability check failed: " + e.getMessage());
            return hasBasicPythonFiles(project);
        }
    }
    
    /**
     * 检查是否在PyCharm环境中
     */
    private boolean isPyCharmEnvironment() {
        try {
            String ideaProduct = System.getProperty("idea.platform.prefix");
            return "PyCharmCore".equals(ideaProduct) || "PyCharm".equals(ideaProduct);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查Python插件是否可用
     */
    private boolean isPythonPluginAvailable() {
        try {
            // 检查是否有Python文件类型支持
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null || "UNKNOWN".equals(pythonFileType.getName())) {
                return false;
            }
            
            // 进一步检查Python PSI支持
            try {
                Class.forName("com.jetbrains.python.psi.PyFile");
                return true;
            } catch (ClassNotFoundException e) {
                // Python插件不可用，但可以使用文本解析
                return true;  // 返回true以支持文本解析模式
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 基础Python文件检查（作为回退方案）
     */
    private boolean hasBasicPythonFiles(Project project) {
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) {
                return false;
            }
            return FileTypeIndex.containsFileOfType(
                pythonFileType, 
                GlobalSearchScope.projectScope(project)
            );
        } catch (Exception e) {
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
     * 收集路由装饰器（增强版）
     */
    private void collectRouteDecorators(String fileText, String fileName, Map<String, RouterInfo> allRouters) {
        // 使用增强的正则模式获取更多信息
        Matcher routeMatcher = ENHANCED_ROUTE_PATTERN.matcher(fileText);
        
        while (routeMatcher.find()) {
            String instanceName = routeMatcher.group(1);  // app 或 router 名称
            String httpMethod = routeMatcher.group(2).toUpperCase();
            String path = routeMatcher.group(3);
            String decoratorParams = routeMatcher.group(4); // 装饰器的其他参数
            
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
            
            // === 新增：提取元数据 ===
            if (decoratorParams != null) {
                // 提取标签
                extractTags(decoratorParams, endpoint);
                
                // 提取摘要和描述
                extractSummaryAndDescription(decoratorParams, endpoint);
                
                // 提取响应模型
                extractResponseModel(decoratorParams, endpoint);
                
                // 检查是否已废弃
                checkDeprecated(decoratorParams, endpoint);
            }
            
            // 提取依赖注入信息
            extractDependencies(fileText, routeMatcher.start(), endpoint);
            
            router.endpoints.add(endpoint);
        }
    }
    
    /**
     * 提取路由标签
     */
    private void extractTags(String decoratorParams, RouteEndpoint endpoint) {
        Matcher tagsMatcher = TAGS_PATTERN.matcher(decoratorParams);
        if (tagsMatcher.find()) {
            String tagsStr = tagsMatcher.group(1);
            // 解析标签列表
            String[] tags = tagsStr.split(",");
            for (String tag : tags) {
                String cleanTag = tag.trim().replaceAll("[\"']", "");
                if (!cleanTag.isEmpty()) {
                    endpoint.tags.add(cleanTag);
                }
            }
        }
    }
    
    /**
     * 提取摘要和描述
     */
    private void extractSummaryAndDescription(String decoratorParams, RouteEndpoint endpoint) {
        // 提取summary
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(decoratorParams);
        if (summaryMatcher.find()) {
            endpoint.summary = summaryMatcher.group(1);
        }
        
        // 提取description
        Matcher descMatcher = DESCRIPTION_PATTERN.matcher(decoratorParams);
        if (descMatcher.find()) {
            endpoint.description = descMatcher.group(1);
        }
    }
    
    /**
     * 提取响应模型
     */
    private void extractResponseModel(String decoratorParams, RouteEndpoint endpoint) {
        Matcher responseMatcher = RESPONSE_MODEL_PATTERN.matcher(decoratorParams);
        if (responseMatcher.find()) {
            endpoint.responseModel = responseMatcher.group(1);
        }
    }
    
    /**
     * 检查是否已废弃
     */
    private void checkDeprecated(String decoratorParams, RouteEndpoint endpoint) {
        if (decoratorParams.contains("deprecated=True") || 
            decoratorParams.contains("deprecated = True")) {
            endpoint.deprecated = true;
        }
    }
    
    /**
     * 提取依赖注入信息
     */
    private void extractDependencies(String fileText, int decoratorStart, RouteEndpoint endpoint) {
        // 查找装饰器后面的函数定义
        String afterDecorator = fileText.substring(decoratorStart);
        Matcher functionMatcher = FUNCTION_DEFINITION_PATTERN.matcher(afterDecorator);
        
        if (functionMatcher.find()) {
            int functionStart = decoratorStart + functionMatcher.start();
            int functionEnd = findFunctionEnd(fileText, functionStart);
            
            if (functionEnd > functionStart) {
                String functionText = fileText.substring(functionStart, functionEnd);
                
                // 查找依赖注入
                Matcher depMatcher = DEPENDENCY_PATTERN.matcher(functionText);
                while (depMatcher.find()) {
                    String dependency = depMatcher.group(1);
                    endpoint.dependencies.add(dependency);
                }
            }
        }
    }
    
    /**
     * 查找函数结束位置（简单实现）
     */
    private int findFunctionEnd(String fileText, int functionStart) {
        // 简单实现：查找下一个不缩进的行或文件末尾
        String[] lines = fileText.substring(functionStart).split("\\n");
        int currentPos = functionStart;
        boolean inFunction = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == 0) {
                inFunction = true;
                currentPos += line.length() + 1;
                continue;
            }
            
            // 如果遇到不缩进的非空行，表示函数结束
            if (inFunction && !line.trim().isEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
                return currentPos;
            }
            currentPos += line.length() + 1;
        }
        
        return Math.min(currentPos, fileText.length());
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
     * 第三步：生成最终的端点列表（增强版）
     */
    private void generateEndpoints(Map<String, RouterInfo> allRouters, 
                                 List<RestfulEndpointNavigationItem> endpoints, 
                                 Project project) {
        for (RouterInfo router : allRouters.values()) {
            // 只从主应用（FastAPI实例）生成端点，避免重复
            if (isMainApp(router, allRouters)) {
                for (RouteEndpoint endpoint : router.endpoints) {
                    String fullPath = combinePathWithPrefix(router.prefix, endpoint.path);
                    
                    // 使用增强的描述信息
                    String enhancedName = createEnhancedEndpointName(endpoint, fullPath);
                    
                    RestfulEndpointNavigationItem navigationItem = new RestfulEndpointNavigationItem(
                        endpoint.httpMethod, fullPath, router.name, endpoint.functionName, null, project
                    );
                    
                    // TODO: 如果RestfulEndpointNavigationItem支持更多属性，可以在这里设置
                    // navigationItem.setTags(endpoint.tags);
                    // navigationItem.setSummary(endpoint.summary);
                    
                    endpoints.add(navigationItem);
                }
            }
        }
    }
    
    /**
     * 创建增强的端点名称（包含元数据）
     */
    private String createEnhancedEndpointName(RouteEndpoint endpoint, String fullPath) {
        StringBuilder name = new StringBuilder();
        name.append(endpoint.httpMethod).append(" ").append(fullPath);
        
        // 添加摘要
        if (endpoint.summary != null && !endpoint.summary.isEmpty()) {
            name.append(" - ").append(endpoint.summary);
        }
        
        // 添加标签
        if (!endpoint.tags.isEmpty()) {
            name.append(" [").append(String.join(", ", endpoint.tags)).append("]");
        }
        
        // 标记已废弃
        if (endpoint.deprecated) {
            name.append(" [DEPRECATED]");
        }
        
        // 添加响应模型信息
        if (endpoint.responseModel != null && !endpoint.responseModel.isEmpty()) {
            name.append(" -> ").append(endpoint.responseModel);
        }
        
        // 添加依赖信息
        if (!endpoint.dependencies.isEmpty()) {
            name.append(" (deps: ").append(String.join(", ", endpoint.dependencies)).append(")");
        }
        
        return name.toString();
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
    
    // === 实现FastApiEnhancedStrategy接口的增强方法 ===
    
    @Override
    public Map<String, List<String>> detectDependencyInjection(Project project) {
        Map<String, List<String>> dependencyMap = new HashMap<>();
        
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) return dependencyMap;
            
            Collection<VirtualFile> pythonFiles = FileTypeIndex.getFiles(pythonFileType, 
                GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : pythonFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    analyzeDependenciesInFile(psiFile, dependencyMap);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to detect dependency injection: " + e.getMessage());
        }
        
        return dependencyMap;
    }
    
    @Override
    public List<MiddlewareInfo> analyzeMiddleware(Project project) {
        List<MiddlewareInfo> middlewareList = new ArrayList<>();
        
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) return middlewareList;
            
            Collection<VirtualFile> pythonFiles = FileTypeIndex.getFiles(pythonFileType, 
                GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : pythonFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    analyzeMiddlewareInFile(psiFile, middlewareList);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to analyze middleware: " + e.getMessage());
        }
        
        return middlewareList;
    }
    
    @Override
    public List<EnhancedEndpointInfo> extractRouteMetadata(Project project) {
        List<EnhancedEndpointInfo> enhancedEndpoints = new ArrayList<>();
        
        try {
            // 使用现有的扫描逻辑，但提取更多元数据
            Map<String, RouterInfo> allRouters = new HashMap<>();
            List<IncludeRelation> includeRelations = new ArrayList<>();
            
            collectRouterInfo(project, allRouters, includeRelations);
            
            // 转换为增强的端点信息
            for (RouterInfo router : allRouters.values()) {
                for (RouteEndpoint endpoint : router.endpoints) {
                    EnhancedEndpointInfo enhancedInfo = new EnhancedEndpointInfo(
                        endpoint.path, endpoint.httpMethod);
                    
                    enhancedInfo.setSummary(endpoint.summary);
                    enhancedInfo.setDescription(endpoint.description);
                    enhancedInfo.setTags(endpoint.tags);
                    enhancedInfo.setDeprecated(endpoint.deprecated);
                    enhancedInfo.setResponses(endpoint.responses);
                    
                    enhancedEndpoints.add(enhancedInfo);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract route metadata: " + e.getMessage());
        }
        
        return enhancedEndpoints;
    }
    
    @Override
    public List<RestfulEndpointNavigationItem> discoverDynamicRoutes(Project project) {
        List<RestfulEndpointNavigationItem> dynamicEndpoints = new ArrayList<>();
        
        try {
            // TODO: 实现动态路由发现逻辑
            // 这里可以分析条件路由、参数化路由等
            System.out.println("Dynamic route discovery not yet implemented");
        } catch (Exception e) {
            System.err.println("Failed to discover dynamic routes: " + e.getMessage());
        }
        
        return dynamicEndpoints;
    }
    
    @Override
    public Map<String, ModelInfo> analyzePydanticModels(Project project) {
        Map<String, ModelInfo> modelMap = new HashMap<>();
        
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) return modelMap;
            
            Collection<VirtualFile> pythonFiles = FileTypeIndex.getFiles(pythonFileType, 
                GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : pythonFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    analyzePydanticModelsInFile(psiFile, modelMap);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to analyze Pydantic models: " + e.getMessage());
        }
        
        return modelMap;
    }
    
    // === 辅助方法 ===
    
    /**
     * 分析单个文件中的依赖注入
     */
    private void analyzeDependenciesInFile(PsiFile pyFile, Map<String, List<String>> dependencyMap) {
        try {
            String fileText = pyFile.getText();
            Matcher depMatcher = DEPENDENCY_PATTERN.matcher(fileText);
            
            List<String> fileDependencies = new ArrayList<>();
            while (depMatcher.find()) {
                String dependency = depMatcher.group(1);
                fileDependencies.add(dependency);
            }
            
            if (!fileDependencies.isEmpty()) {
                dependencyMap.put(pyFile.getName(), fileDependencies);
            }
        } catch (Exception e) {
            System.err.println("Error analyzing dependencies in file " + pyFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * 分析单个文件中的中间件
     */
    private void analyzeMiddlewareInFile(PsiFile pyFile, List<MiddlewareInfo> middlewareList) {
        try {
            String fileText = pyFile.getText();
            Matcher middlewareMatcher = MIDDLEWARE_PATTERN.matcher(fileText);
            
            while (middlewareMatcher.find()) {
                String appName = middlewareMatcher.group(1);
                String middlewareType = middlewareMatcher.group(2);
                
                MiddlewareInfo middlewareInfo = new MiddlewareInfo(
                    middlewareType, "add_middleware", pyFile.getVirtualFile().getPath());
                middlewareList.add(middlewareInfo);
            }
        } catch (Exception e) {
            System.err.println("Error analyzing middleware in file " + pyFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * 分析单个文件中的Pydantic模型
     */
    private void analyzePydanticModelsInFile(PsiFile pyFile, Map<String, ModelInfo> modelMap) {
        try {
            String fileText = pyFile.getText();
            Matcher modelMatcher = PYDANTIC_MODEL_PATTERN.matcher(fileText);
            
            while (modelMatcher.find()) {
                String modelName = modelMatcher.group(1);
                String className = pyFile.getName().replace(".py", "") + "." + modelName;
                
                ModelInfo modelInfo = new ModelInfo(modelName, className);
                modelMap.put(modelName, modelInfo);
            }
        } catch (Exception e) {
            System.err.println("Error analyzing Pydantic models in file " + pyFile.getName() + ": " + e.getMessage());
        }
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