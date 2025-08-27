package com.devtoolkit.pro.strategies.impl;

import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.strategies.RestfulEndpointScanStrategy;
import com.devtoolkit.pro.services.RestfulUrlService;
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
 * Spring框架RESTful端点扫描策略
 * 支持Spring Boot和Spring MVC项目
 */
public class SpringEndpointScanStrategy implements RestfulEndpointScanStrategy {
    
    private static final String STRATEGY_NAME = "Spring";
    private static final int PRIORITY = 1; // 高优先级
    
    // Spring注解模式
    private static final String[] SPRING_CONTROLLER_ANNOTATIONS = {
        "org.springframework.web.bind.annotation.RestController",
        "org.springframework.stereotype.Controller"
    };
    
    private static final String[] SPRING_MAPPING_ANNOTATIONS = {
        "RequestMapping", "GetMapping", "PostMapping", 
        "PutMapping", "DeleteMapping", "PatchMapping"
    };
    
    // URL路径提取正则
    private static final Pattern URL_PATTERN = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("value\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern PATH_PATTERN = Pattern.compile("path\\s*=\\s*\"([^\"]*)\"");
    
    private PsiManager psiManager;
    private RestfulUrlService urlService;
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean isApplicable(Project project) {
        // 检查项目中是否存在Spring相关的依赖或注解
        return hasSpringDependencies(project) || hasSpringAnnotations(project);
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean supportsFramework(String frameworkName) {
        return "spring".equalsIgnoreCase(frameworkName) || 
               "spring-boot".equalsIgnoreCase(frameworkName) ||
               "spring-mvc".equalsIgnoreCase(frameworkName);
    }
    
    @Override
    public List<RestfulEndpointNavigationItem> scanEndpoints(Project project) {
        this.psiManager = PsiManager.getInstance(project);
        this.urlService = new RestfulUrlService(project);
        List<RestfulEndpointNavigationItem> endpoints = new ArrayList<>();
        
        try {
            // 第一步：通过Spring注解扫描
            boolean foundFromAnnotations = scanFromSpringAnnotations(project, endpoints);
            
            // 第二步：如果注解扫描结果不理想，回退到文件扫描
            if (!foundFromAnnotations || endpoints.size() < 3) {
                scanFromJavaFiles(project, endpoints);
            }
            
            // 去重并排序
            return deduplicateAndSort(endpoints);
            
        } catch (Exception e) {
            System.err.println("Spring strategy scan failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查项目是否有Spring依赖
     */
    private boolean hasSpringDependencies(Project project) {
        try {
            // 检查gradle.build或pom.xml中的Spring依赖
            // 这里简化实现，实际可以解析构建文件
            VirtualFile[] contentRoots = project.getBaseDir().getChildren();
            for (VirtualFile file : contentRoots) {
                if ("build.gradle".equals(file.getName()) || 
                    "build.gradle.kts".equals(file.getName()) ||
                    "pom.xml".equals(file.getName())) {
                    // 可以进一步解析文件内容检查Spring依赖
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }
    
    /**
     * 检查项目是否有Spring注解
     */
    private boolean hasSpringAnnotations(Project project) {
        try {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            for (String annotation : SPRING_CONTROLLER_ANNOTATIONS) {
                if (!findClassesByAnnotation(annotation, scope, project).isEmpty()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }
    
    /**
     * 通过Spring注解扫描Controller
     */
    private boolean scanFromSpringAnnotations(Project project, List<RestfulEndpointNavigationItem> endpoints) {
        try {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            
            // 扫描@RestController注解的类
            for (String annotation : SPRING_CONTROLLER_ANNOTATIONS) {
                Collection<PsiClass> controllers = findClassesByAnnotation(annotation, scope, project);
                for (PsiClass controllerClass : controllers) {
                    if (hasRequestMappingMethods(controllerClass)) {
                        scanControllerClass(controllerClass, endpoints, project);
                    }
                }
            }
            
            return !endpoints.isEmpty();
        } catch (Exception e) {
            System.err.println("Failed to scan from Spring annotations: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 通过文件扫描（回退方案）
     */
    private void scanFromJavaFiles(Project project, List<RestfulEndpointNavigationItem> endpoints) {
        try {
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(javaFileType, 
                GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : javaFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile instanceof PsiJavaFile) {
                    scanJavaFile((PsiJavaFile) psiFile, endpoints, project);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to scan from Java files: " + e.getMessage());
        }
    }
    
    /**
     * 根据注解名称查找类
     */
    private Collection<PsiClass> findClassesByAnnotation(String annotationName, GlobalSearchScope scope, Project project) {
        List<PsiClass> classes = new ArrayList<>();
        
        try {
            FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(javaFileType, scope);

            for (VirtualFile virtualFile : javaFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile instanceof PsiJavaFile) {
                    PsiClass[] fileClasses = ((PsiJavaFile) psiFile).getClasses();
                    for (PsiClass psiClass : fileClasses) {
                        if (hasAnnotation(psiClass, annotationName)) {
                            classes.add(psiClass);
                        }
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
                if (qualifiedName != null) {
                    for (String mappingAnnotation : SPRING_MAPPING_ANNOTATIONS) {
                        if (qualifiedName.endsWith(mappingAnnotation)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 扫描Controller类
     */
    private void scanControllerClass(PsiClass controllerClass, List<RestfulEndpointNavigationItem> endpoints, Project project) {
        String classLevelPath = extractClassLevelPath(controllerClass);
        scanMethodsForEndpoints(controllerClass, classLevelPath, endpoints, project);
    }
    
    /**
     * 扫描Java文件
     */
    private void scanJavaFile(PsiJavaFile javaFile, List<RestfulEndpointNavigationItem> endpoints, Project project) {
        PsiClass[] classes = javaFile.getClasses();
        
        for (PsiClass psiClass : classes) {
            if (isControllerClass(psiClass)) {
                String classLevelPath = extractClassLevelPath(psiClass);
                scanMethodsForEndpoints(psiClass, classLevelPath, endpoints, project);
            }
        }
    }
    
    /**
     * 检查是否为Controller类
     */
    private boolean isControllerClass(PsiClass psiClass) {
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
     * 扫描类中的方法
     */
    private void scanMethodsForEndpoints(PsiClass psiClass, String classLevelPath, 
                                       List<RestfulEndpointNavigationItem> endpoints, Project project) {
        PsiMethod[] methods = psiClass.getMethods();
        
        for (PsiMethod method : methods) {
            scanMethodForEndpoints(method, classLevelPath, endpoints, project);
        }
    }
    
    /**
     * 扫描单个方法
     */
    private void scanMethodForEndpoints(PsiMethod method, String classLevelPath, 
                                      List<RestfulEndpointNavigationItem> endpoints, Project project) {
        PsiAnnotation[] annotations = method.getAnnotations();
        
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) continue;
            
            String httpMethod = getHttpMethod(qualifiedName);
            if (httpMethod != null) {
                String methodPath = extractPathFromAnnotation(annotation);
                String fullPath = urlService.buildFullUrl(annotation, methodPath);
                
                if (!fullPath.isEmpty()) {
                    String className = method.getContainingClass().getName();
                    String methodName = method.getName();
                    
                    RestfulEndpointNavigationItem endpoint = new RestfulEndpointNavigationItem(
                        httpMethod, fullPath, className, methodName, method, project
                    );
                    endpoints.add(endpoint);
                }
            }
        }
    }
    
    /**
     * 提取类级别的路径
     * 注意：由于现在使用RestfulUrlService.buildFullUrl来处理完整路径构建，
     * 该方法返回空字符串，避免重复处理类级别路径
     */
    private String extractClassLevelPath(PsiClass psiClass) {
        // buildFullUrl会自动处理类级别的@RequestMapping，所以这里返回空字符串
        return "";
    }
    
    /**
     * 从注解中提取路径
     */
    private String extractPathFromAnnotation(PsiAnnotation annotation) {
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