package com.devtoolkit.pro.services;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.strategies.RestfulEndpointStrategyManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiConstantEvaluationHelper;
import com.intellij.psi.JavaPsiFacade;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RESTful URL服务类
 * 负责扫描项目中的RESTful API端点
 */
public class RestfulUrlService {
    private final Project project;
    private final PsiManager psiManager;
    private final RestfulEndpointStrategyManager strategyManager;

    // Spring注解模式
    private static final String[] SPRING_ANNOTATIONS = {
            "@RequestMapping", "@GetMapping", "@PostMapping",
            "@PutMapping", "@DeleteMapping", "@PatchMapping"
    };

    // JAX-RS注解模式
    private static final String[] JAXRS_ANNOTATIONS = {
            "@Path", "@GET", "@POST", "@PUT", "@DELETE"
    };

    // URL路径提取正则
    private static final Pattern URL_PATTERN = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("value\s*=\s*\"([^\"]*)\"");
    private static final Pattern PATH_PATTERN = Pattern.compile("path\s*=\s*\"([^\"]*)\"");

    public RestfulUrlService(Project project) {
        this.project = project;
        this.psiManager = PsiManager.getInstance(project);
        this.strategyManager = new RestfulEndpointStrategyManager(project);
    }

    /**
     * 查找项目中所有的RESTful URL
     */
    public List<String> findAllRestfulUrls() {
        Set<String> urls = new HashSet<>();

        // 扫描Java文件
        scanJavaFiles(urls);

        // 转换为列表并排序
        List<String> result = new ArrayList<>(urls);
        result.sort(String::compareTo);

        return result;
    }

    /**
     * 扫描Java文件中的RESTful注解
     */
    private void scanJavaFiles(Set<String> urls) {
        // 检查Java支持是否可用
        if (!isJavaModuleAvailable()) {

            return;
        }

        try {
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(javaFileType,
                    GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : javaFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (isJavaFile(psiFile)) {
                    scanJavaFileWithReflection(psiFile, urls);
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning Java files: " + e.getMessage());
        }
    }

    /**
     * 检查Java模块是否可用
     */
    private boolean isJavaModuleAvailable() {
        try {
            // 尝试加载PsiJavaFile类
            Class.forName("com.intellij.psi.PsiJavaFile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查文件是否是Java文件（不使用instanceof PsiJavaFile）
     */
    private boolean isJavaFile(PsiFile psiFile) {
        if (psiFile == null)
            return false;
        return psiFile.getClass().getSimpleName().equals("PsiJavaFileImpl") ||
                psiFile.getFileType().getName().equals("JAVA");
    }

    /**
     * 使用反射方式扫描Java文件
     */
    private void scanJavaFileWithReflection(PsiFile javaFile, Set<String> urls) {
        try {
            // 通过反射获取classes
            Object[] classes = (Object[]) javaFile.getClass().getMethod("getClasses").invoke(javaFile);

            for (Object classObj : classes) {
                PsiClass psiClass = (PsiClass) classObj;
                String classLevelPath = extractClassLevelPath(psiClass);
                scanMethods(psiClass, classLevelPath, urls);
            }
        } catch (Exception e) {
            System.err.println("Error scanning Java file with reflection: " + e.getMessage());
        }
    }

    /**
     * 扫描单个Java文件（已弃用，保留向后兼容性）
     */
    @Deprecated
    private void scanJavaFile(Object javaFile, Set<String> urls) {
        // 此方法保留向后兼容性，实际功能已移至scanJavaFileWithReflection
    }

    /**
     * 提取类级别的路径
     */
    private String extractClassLevelPath(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();

        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null &&
                    (qualifiedName.endsWith("RequestMapping") || qualifiedName.endsWith("Path"))) {
                return extractPathFromAnnotation(annotation);
            }
        }

        return "";
    }

    /**
     * 扫描类中的方法
     */
    private void scanMethods(PsiClass psiClass, String classLevelPath, Set<String> urls) {
        PsiMethod[] methods = psiClass.getMethods();

        for (PsiMethod method : methods) {
            scanMethod(method, classLevelPath, urls);
        }
    }

    /**
     * 扫描单个方法
     */
    private void scanMethod(PsiMethod method, String classLevelPath, Set<String> urls) {
        PsiAnnotation[] annotations = method.getAnnotations();

        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null)
                continue;

            String httpMethod = getHttpMethod(qualifiedName);
            if (httpMethod != null) {
                String methodPath = extractPathFromAnnotation(annotation);
                String fullPath = combinePaths(classLevelPath, methodPath);

                if (!fullPath.isEmpty()) {
                    String urlInfo = String.format("%s %s (%s.%s)",
                            httpMethod, fullPath, method.getContainingClass().getName(), method.getName());
                    urls.add(urlInfo);
                }
            }
        }
    }

    /**
     * 从注解中提取路径，支持常量解析
     * 这是统一的常量解析方法，供所有组件使用
     */
    public String extractPathFromAnnotation(PsiAnnotation annotation) {
        if (annotation == null) {
            return "";
        }

        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue("path");
        }

        if (value != null) {
            // 尝试使用PSI常量求值来解析表达式
            Object constantValue = evaluateConstantExpression(value);
            if (constantValue != null) {
                return constantValue.toString();
            }

            // 回退到直接字面量处理
            if (value instanceof PsiLiteralExpression) {
                Object literalValue = ((PsiLiteralExpression) value).getValue();
                return literalValue != null ? literalValue.toString() : null;
            }
        }

        // 回退到原有的正则表达式方法
        String annotationText = annotation.getText();

        // 尝试提取value属性
        Matcher valueMatcher = VALUE_PATTERN.matcher(annotationText);
        if (valueMatcher.find()) {
            return valueMatcher.group(1);
        }

        // 尝试提取path属性
        Matcher pathMatcher = PATH_PATTERN.matcher(annotationText);
        if (pathMatcher.find()) {
            return pathMatcher.group(1);
        }

        // 尝试提取直接的字符串值
        Matcher urlMatcher = URL_PATTERN.matcher(annotationText);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }

        return "";
    }

    /**
     * 使用PSI常量求值来解析常量表达式，如 API.API_V1_PREFIX + "/fetch"
     * 这是统一的常量解析方法，供所有组件使用
     */
    public Object evaluateConstantExpression(PsiElement element) {
        try {
            // 获取PSI常量求值助手
            PsiConstantEvaluationHelper evaluationHelper = JavaPsiFacade.getInstance(element.getProject())
                    .getConstantEvaluationHelper();

            // 尝试计算常量表达式的值
            Object result = evaluationHelper.computeConstantExpression(element, false);

            // 如果标准求值失败，尝试手动解析常量引用
            if (result == null && element instanceof PsiExpression) {
                result = resolveConstantReference((PsiExpression) element);
            }

            return result;
        } catch (Exception e) {
            // 如果求值失败，尝试手动解析
            if (element instanceof PsiExpression) {
                return resolveConstantReference((PsiExpression) element);
            }
            return null;
        }
    }

    /**
     * 手动解析常量引用，特别是第三方jar包中的常量
     * 这是统一的常量解析方法，供所有组件使用
     */
    public Object resolveConstantReference(PsiExpression expression) {
        try {
            // 处理二元表达式（如 API.API_V1_PREFIX + "/fetch"）
            if (expression instanceof PsiBinaryExpression) {
                PsiBinaryExpression binaryExpr = (PsiBinaryExpression) expression;
                if (binaryExpr.getOperationTokenType() == JavaTokenType.PLUS) {
                    Object left = resolveConstantReference(binaryExpr.getLOperand());
                    Object right = resolveConstantReference(binaryExpr.getROperand());
                    if (left != null && right != null) {
                        return left.toString() + right.toString();
                    }
                }
            }

            // 处理引用表达式（如 API.API_V1_PREFIX）
            if (expression instanceof PsiReferenceExpression) {
                PsiReferenceExpression refExpr = (PsiReferenceExpression) expression;
                PsiElement resolved = refExpr.resolve();

                if (resolved instanceof PsiField) {
                    PsiField field = (PsiField) resolved;
                    // 检查是否是常量字段（static final）
                    if (field.hasModifierProperty(PsiModifier.STATIC) &&
                            field.hasModifierProperty(PsiModifier.FINAL)) {

                        PsiExpression initializer = field.getInitializer();
                        if (initializer instanceof PsiLiteralExpression) {
                            return ((PsiLiteralExpression) initializer).getValue();
                        } else if (initializer != null) {
                            // 递归解析初始化表达式
                            return resolveConstantReference(initializer);
                        }
                    }
                } else if (resolved == null) {
                    // 如果标准resolve失败，尝试使用JavaPsiFacade查找第三方jar包中的类和字段
                    Object result = resolveExternalConstant(refExpr);
                    if (result != null) {
                        return result;
                    }
                }
            }

            // 处理字面量表达式
            if (expression instanceof PsiLiteralExpression) {
                return ((PsiLiteralExpression) expression).getValue();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据注解名称获取HTTP方法
     */
    private String getHttpMethod(String qualifiedName) {
        if (qualifiedName.endsWith("GetMapping") || qualifiedName.endsWith("GET")) {
            return "GET";
        } else if (qualifiedName.endsWith("PostMapping") || qualifiedName.endsWith("POST")) {
            return "POST";
        } else if (qualifiedName.endsWith("PutMapping") || qualifiedName.endsWith("PUT")) {
            return "PUT";
        } else if (qualifiedName.endsWith("DeleteMapping") || qualifiedName.endsWith("DELETE")) {
            return "DELETE";
        } else if (qualifiedName.endsWith("PatchMapping")) {
            return "PATCH";
        } else if (qualifiedName.endsWith("RequestMapping")) {
            return "GET"; // 默认为GET
        }
        return null;
    }

    /**
     * 组合类级别路径和方法级别路径
     */
    private String combinePaths(String classPath, String methodPath) {
        if (classPath.isEmpty()) {
            return methodPath;
        }
        if (methodPath.isEmpty()) {
            return classPath;
        }

        // 确保路径以/开头
        if (!classPath.startsWith("/")) {
            classPath = "/" + classPath;
        }
        if (!methodPath.startsWith("/")) {
            methodPath = "/" + methodPath;
        }

        // 移除classPath末尾的/
        if (classPath.endsWith("/")) {
            classPath = classPath.substring(0, classPath.length() - 1);
        }

        return classPath + methodPath;
    }

    /**
     * 查找项目中所有的RESTful端点并返回NavigationItem列表
     * 使用策略模式选择最佳的扫描策略
     */
    public List<RestfulEndpointNavigationItem> findAllRestfulEndpoints() {
        try {
            // 使用策略模式扫描端点
            List<RestfulEndpointNavigationItem> endpoints = strategyManager.scanWithBestStrategy();

            // 如果策略模式没有找到端点，回退到传统扫描方式
            if (endpoints.isEmpty()) {

                endpoints = fallbackToLegacyScan();
            }

            return endpoints;

        } catch (Exception e) {
            System.err.println("Strategy-based scan failed: " + e.getMessage());
            // 如果策略扫描失败，回退到传统扫描
            return fallbackToLegacyScan();
        }
    }

    /**
     * 获取策略管理器（用于调试和扩展）
     */
    public RestfulEndpointStrategyManager getStrategyManager() {
        return strategyManager;
    }

    /**
     * 传统扫描方式（回退方案）
     */
    private List<RestfulEndpointNavigationItem> fallbackToLegacyScan() {
        List<RestfulEndpointNavigationItem> endpoints = new ArrayList<>();

        try {
            // 第一步：尝试从注解获取Controller
            boolean foundFromAnnotations = scanFromSpringContainer(endpoints);

            // 第二步：如果注解扫描结果不理想，则通过文件解析补充
            if (!foundFromAnnotations || endpoints.size() < 3) {
                scanJavaFilesForEndpoints(endpoints);
            }

            // 去重并按名称排序
            endpoints = deduplicateEndpoints(endpoints);
            endpoints.sort((a, b) -> a.getName().compareTo(b.getName()));

        } catch (Exception e) {
            System.err.println("Legacy scan also failed: " + e.getMessage());
        }

        return endpoints;
    }

    /**
     * 构建完整的URL路径，包括类级别和方法级别的路径
     */
    public String buildFullUrl(PsiAnnotation annotation, String path) {


        // 查找类级别的@RequestMapping
        PsiClass containingClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class);
        String basePath = "";

        if (containingClass != null) {

            PsiAnnotation classMapping = containingClass
                    .getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
            if (classMapping != null) {
                String classPath = extractPathFromAnnotation(classMapping);

                if (classPath != null && !classPath.isEmpty()) {
                    basePath = classPath;
                }
            } else {

            }
        }

        // 确保路径以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!basePath.isEmpty() && !basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }

        // 合并基础路径和方法路径
        String fullPath = basePath + path;

        // 清理重复的斜杠
        fullPath = fullPath.replaceAll("/+", "/");

        // 只返回路径部分，不包含host:port
        return fullPath;
    }

    /**
     * 解析第三方jar包中的常量，使用JavaPsiFacade主动查找
     */
    private Object resolveExternalConstant(PsiReferenceExpression refExpr) {
        try {
            String referenceName = refExpr.getReferenceName();
            if (referenceName == null) {
                return null;
            }

            // 获取限定符表达式（如 API.API_V1_PREFIX 中的 API）
            PsiExpression qualifierExpression = refExpr.getQualifierExpression();
            if (qualifierExpression instanceof PsiReferenceExpression) {
                PsiReferenceExpression qualifierRef = (PsiReferenceExpression) qualifierExpression;
                String className = qualifierRef.getReferenceName();

                if (className != null) {
                    // 尝试查找可能的完全限定类名
                    String[] possiblePackages = {
                            "", // 当前包
                            "com.api.", // 常见的API包
                            "org.api.",
                            "com.constants.",
                            "org.constants."
                    };

                    JavaPsiFacade facade = JavaPsiFacade.getInstance(refExpr.getProject());
                    GlobalSearchScope scope = GlobalSearchScope.allScope(refExpr.getProject());

                    for (String packagePrefix : possiblePackages) {
                        String fullClassName = packagePrefix + className;
                        PsiClass psiClass = facade.findClass(fullClassName, scope);

                        if (psiClass != null) {
                            PsiField field = psiClass.findFieldByName(referenceName, false);

                            if (field != null &&
                                    field.hasModifierProperty(PsiModifier.STATIC) &&
                                    field.hasModifierProperty(PsiModifier.FINAL)) {

                                PsiExpression initializer = field.getInitializer();
                                if (initializer instanceof PsiLiteralExpression) {
                                    Object value = ((PsiLiteralExpression) initializer).getValue();
                                    return value;
                                }
                            }
                        }
                    }

                    // 如果上述方法失败，尝试通过import语句查找完整类名
                    PsiFile containingFile = refExpr.getContainingFile();
                    if (containingFile instanceof PsiJavaFile) {
                        PsiJavaFile javaFile = (PsiJavaFile) containingFile;
                        PsiImportList importList = javaFile.getImportList();

                        if (importList != null) {
                            for (PsiImportStatement importStatement : importList.getImportStatements()) {
                                String importedName = importStatement.getQualifiedName();
                                if (importedName != null && importedName.endsWith("." + className)) {
                                    PsiClass importedClass = facade.findClass(importedName, scope);
                                    if (importedClass != null) {
                                        PsiField field = importedClass.findFieldByName(referenceName, false);
                                        if (field != null &&
                                                field.hasModifierProperty(PsiModifier.STATIC) &&
                                                field.hasModifierProperty(PsiModifier.FINAL)) {

                                            PsiExpression initializer = field.getInitializer();
                                            if (initializer instanceof PsiLiteralExpression) {
                                                Object value = ((PsiLiteralExpression) initializer).getValue();
                                                return value;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试从Spring注解获取Controller信息
     * 优先通过注解搜索获取所有@RestController和@Controller类
     *
     * @param endpoints 存储找到的端点
     * @return 是否成功找到Controller
     */
    private boolean scanFromSpringContainer(List<RestfulEndpointNavigationItem> endpoints) {
        try {
            // 尝试通过注解搜索找到所有Controller
            boolean foundControllers = scanControllersFromAnnotations(endpoints);

            if (foundControllers) {
                return true;
            }

            // 如果注解搜索失败，尝试通过Spring配置文件找到Bean定义
            return scanControllersFromSpringConfigs(endpoints);

        } catch (Exception e) {
            // 如果出现异常，返回false以回退到文件解析
            System.err.println("Failed to scan from Spring annotations: " + e.getMessage());
            return false;
        }
    }

    /**
     * 通过注解搜索获取Controller
     */
    private boolean scanControllersFromAnnotations(List<RestfulEndpointNavigationItem> endpoints) {
        try {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

            // 搜索@RestController注解的类
            Collection<PsiClass> restControllers = findClassesByAnnotation(
                    "org.springframework.web.bind.annotation.RestController", scope);
            for (PsiClass controllerClass : restControllers) {
                scanControllerFromSpringBean(controllerClass, endpoints);
            }

            // 搜索@Controller注解的类
            Collection<PsiClass> controllers = findClassesByAnnotation("org.springframework.stereotype.Controller",
                    scope);
            for (PsiClass controllerClass : controllers) {
                if (hasRequestMappingMethods(controllerClass)) {
                    scanControllerFromSpringBean(controllerClass, endpoints);
                }
            }

            return !endpoints.isEmpty();
        } catch (Exception e) {
            System.err.println("Failed to scan controllers from annotations: " + e.getMessage());
            return false;
        }
    }

    /**
     * 通过Spring配置文件搜索Controller
     */
    private boolean scanControllersFromSpringConfigs(List<RestfulEndpointNavigationItem> endpoints) {
        try {
            // 这里可以尝试搜索Spring XML配置文件或@Configuration类
            // 目前先返回false，因为大多数情况下注解扫描已经够用
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据注解名称查找类
     */
    private Collection<PsiClass> findClassesByAnnotation(String annotationName, GlobalSearchScope scope) {
        List<PsiClass> classes = new ArrayList<>();

        // 如果Java模块不可用，返回空列表
        if (!isJavaModuleAvailable()) {
            return classes;
        }

        try {
            // 通过文件扫描查找有指定注解的类
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(javaFileType, scope);

            for (VirtualFile virtualFile : javaFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (isJavaFile(psiFile)) {
                    try {
                        // 使用反射获取类
                        Object[] fileClasses = (Object[]) psiFile.getClass().getMethod("getClasses").invoke(psiFile);
                        for (Object classObj : fileClasses) {
                            PsiClass psiClass = (PsiClass) classObj;
                            if (hasAnnotation(psiClass, annotationName)) {
                                classes.add(psiClass);
                            }
                        }
                    } catch (Exception ex) {
                        // 反射调用失败，跳过这个文件
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding classes by annotation: " + e.getMessage());
        }

        return classes;
    }

    /**
     * 检查类是否有指定注解
     */
    private boolean hasAnnotation(PsiClass psiClass, String annotationName) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查类是否有RequestMapping相关的方法
     */
    private boolean hasRequestMappingMethods(PsiClass psiClass) {
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            PsiAnnotation[] annotations = method.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null &&
                        (qualifiedName.contains("Mapping") || qualifiedName.contains("GET") ||
                                qualifiedName.contains("POST") || qualifiedName.contains("PUT") ||
                                qualifiedName.contains("DELETE"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断类是否是REST Controller
     */
    private boolean isRestController(PsiClass psiClass) {
        if (psiClass == null)
            return false;

        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null &&
                    (qualifiedName.endsWith("RestController") ||
                            qualifiedName.endsWith("Controller") ||
                            qualifiedName.endsWith("RequestMapping"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从Spring Bean扫描Controller端点
     */
    private void scanControllerFromSpringBean(PsiClass controllerClass, List<RestfulEndpointNavigationItem> endpoints) {
        String classLevelPath = extractClassLevelPath(controllerClass);
        scanMethodsForEndpoints(controllerClass, classLevelPath, endpoints);
    }

    /**
     * 去重端点列表
     */
    private List<RestfulEndpointNavigationItem> deduplicateEndpoints(List<RestfulEndpointNavigationItem> endpoints) {
        Map<String, RestfulEndpointNavigationItem> uniqueEndpoints = new LinkedHashMap<>();

        for (RestfulEndpointNavigationItem endpoint : endpoints) {
            String key = endpoint.getHttpMethod() + ":" + endpoint.getPath() + ":" +
                    endpoint.getClassName() + "." + endpoint.getMethodName();
            uniqueEndpoints.put(key, endpoint);
        }

        return new ArrayList<>(uniqueEndpoints.values());
    }

    /**
     * 扫描Java文件中的RESTful注解并生成NavigationItem
     */
    private void scanJavaFilesForEndpoints(List<RestfulEndpointNavigationItem> endpoints) {
        // 如果Java模块不可用，跳过Java文件扫描
        if (!isJavaModuleAvailable()) {

            return;
        }

        try {
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(javaFileType,
                    GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : javaFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (isJavaFile(psiFile)) {
                    scanJavaFileForEndpointsWithReflection(psiFile, endpoints);
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning Java files for endpoints: " + e.getMessage());
        }
    }

    /**
     * 使用反射方式扫描Java文件并生成端点
     */
    private void scanJavaFileForEndpointsWithReflection(PsiFile javaFile,
            List<RestfulEndpointNavigationItem> endpoints) {
        try {
            // 通过反射获取classes
            Object[] classes = (Object[]) javaFile.getClass().getMethod("getClasses").invoke(javaFile);

            for (Object classObj : classes) {
                PsiClass psiClass = (PsiClass) classObj;
                String classLevelPath = extractClassLevelPath(psiClass);
                scanMethodsForEndpoints(psiClass, classLevelPath, endpoints);
            }
        } catch (Exception e) {
            System.err.println("Error scanning Java file for endpoints with reflection: " + e.getMessage());
        }
    }

    /**
     * 扫描单个Java文件并生成端点（已弃用，保留向后兼容性）
     */
    @Deprecated
    private void scanJavaFileForEndpoints(Object javaFile, List<RestfulEndpointNavigationItem> endpoints) {
        // 此方法保留向后兼容性，实际功能已移至scanJavaFileForEndpointsWithReflection
    }

    /**
     * 扫描类中的方法并生成端点
     */
    private void scanMethodsForEndpoints(PsiClass psiClass, String classLevelPath,
            List<RestfulEndpointNavigationItem> endpoints) {
        PsiMethod[] methods = psiClass.getMethods();

        for (PsiMethod method : methods) {
            scanMethodForEndpoints(method, classLevelPath, endpoints);
        }
    }

    /**
     * 扫描单个方法并生成端点
     */
    private void scanMethodForEndpoints(PsiMethod method, String classLevelPath,
            List<RestfulEndpointNavigationItem> endpoints) {
        PsiAnnotation[] annotations = method.getAnnotations();

        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null)
                continue;

            String httpMethod = getHttpMethod(qualifiedName);
            if (httpMethod != null) {
                // 先提取方法级别的路径
                String methodPath = extractPathFromAnnotation(annotation);


                // 然后使用buildFullUrl方法来处理常量解析和路径合并
                String fullPath = buildFullUrl(annotation, methodPath);


                if (!fullPath.isEmpty()) {
                    String className = method.getContainingClass().getName();
                    String methodName = method.getName();

                    RestfulEndpointNavigationItem endpoint = new RestfulEndpointNavigationItem(
                            httpMethod, fullPath, className, methodName, method, project);
                    endpoints.add(endpoint);

                }
            }
        }
    }

    /**
     * 导航到指定的URL对应的代码位置
     */
    public void navigateToUrl(String urlInfo) {
        // 从URL信息中提取类名和方法名
        Pattern pattern = Pattern.compile("\\(([^.]+)\\.([^)]+)\\)$");
        Matcher matcher = pattern.matcher(urlInfo);

        if (matcher.find()) {
            String className = matcher.group(1);
            String methodName = matcher.group(2);

            // 查找对应的类和方法
            PsiClass psiClass = findClassByName(className);
            if (psiClass != null) {
                PsiMethod method = findMethodByName(psiClass, methodName);
                if (method != null) {
                    navigateToElement(method);
                }
            }
        }
    }

    /**
     * 根据类名查找PsiClass
     */
    private PsiClass findClassByName(String className) {
        // 如果Java模块不可用，返回null
        if (!isJavaModuleAvailable()) {
            return null;
        }

        try {
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(javaFileType,
                    GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : javaFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (isJavaFile(psiFile)) {
                    try {
                        // 使用反射获取类
                        Object[] classes = (Object[]) psiFile.getClass().getMethod("getClasses").invoke(psiFile);
                        for (Object classObj : classes) {
                            PsiClass psiClass = (PsiClass) classObj;
                            if (className.equals(psiClass.getName())) {
                                return psiClass;
                            }
                        }
                    } catch (Exception ex) {
                        // 反射调用失败，跳过这个文件
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding class by name: " + e.getMessage());
        }

        return null;
    }

    /**
     * 根据方法名查找PsiMethod
     */
    private PsiMethod findMethodByName(PsiClass psiClass, String methodName) {
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    /**
     * 导航到指定的PSI元素
     */
    private void navigateToElement(PsiElement element) {
        if (element.getContainingFile() != null && element.getContainingFile().getVirtualFile() != null) {
            OpenFileDescriptor descriptor = new OpenFileDescriptor(
                    project,
                    element.getContainingFile().getVirtualFile(),
                    element.getTextOffset());
            descriptor.navigate(true);
        }
    }
}