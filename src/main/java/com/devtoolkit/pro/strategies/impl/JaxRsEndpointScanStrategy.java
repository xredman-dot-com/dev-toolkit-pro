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
 * JAX-RS框架RESTful端点扫描策略
 * 支持JAX-RS标准的REST服务
 */
public class JaxRsEndpointScanStrategy implements RestfulEndpointScanStrategy {
    
    private static final String STRATEGY_NAME = "JAX-RS";
    private static final int PRIORITY = 3; // 较低优先级
    
    // JAX-RS注解
    private static final String[] JAXRS_PATH_ANNOTATIONS = {
        "javax.ws.rs.Path",
        "jakarta.ws.rs.Path"
    };
    
    private static final String[] JAXRS_HTTP_ANNOTATIONS = {
        "javax.ws.rs.GET", "javax.ws.rs.POST", "javax.ws.rs.PUT", 
        "javax.ws.rs.DELETE", "javax.ws.rs.PATCH",
        "jakarta.ws.rs.GET", "jakarta.ws.rs.POST", "jakarta.ws.rs.PUT", 
        "jakarta.ws.rs.DELETE", "jakarta.ws.rs.PATCH"
    };
    
    // URL路径提取正则
    private static final Pattern PATH_VALUE_PATTERN = Pattern.compile("\"([^\"]*)\"");
    
    private PsiManager psiManager;
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public boolean isApplicable(Project project) {
        return hasJaxRsDependencies(project) || hasJaxRsAnnotations(project);
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean supportsFramework(String frameworkName) {
        return "jax-rs".equalsIgnoreCase(frameworkName) || 
               "jaxrs".equalsIgnoreCase(frameworkName) ||
               "jersey".equalsIgnoreCase(frameworkName) ||
               "resteasy".equalsIgnoreCase(frameworkName);
    }
    
    @Override
    public List<RestfulEndpointNavigationItem> scanEndpoints(Project project) {
        this.psiManager = PsiManager.getInstance(project);
        List<RestfulEndpointNavigationItem> endpoints = new ArrayList<>();
        
        try {
            scanJavaFiles(project, endpoints);
            return deduplicateAndSort(endpoints);
        } catch (Exception e) {
            System.err.println("JAX-RS strategy scan failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查项目是否有JAX-RS依赖
     */
    private boolean hasJaxRsDependencies(Project project) {
        // 简化实现，可以进一步解析构建文件
        return true; // 假设可能有JAX-RS依赖
    }
    
    /**
     * 检查项目是否有JAX-RS注解
     */
    private boolean hasJaxRsAnnotations(Project project) {
        try {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            for (String annotation : JAXRS_PATH_ANNOTATIONS) {
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
     * 扫描Java文件
     */
    private void scanJavaFiles(Project project, List<RestfulEndpointNavigationItem> endpoints) {
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
            System.err.println("Failed to scan Java files for JAX-RS: " + e.getMessage());
        }
    }
    
    /**
     * 扫描单个Java文件
     */
    private void scanJavaFile(PsiJavaFile javaFile, List<RestfulEndpointNavigationItem> endpoints, Project project) {
        PsiClass[] classes = javaFile.getClasses();
        
        for (PsiClass psiClass : classes) {
            if (hasPathAnnotation(psiClass)) {
                String classLevelPath = extractClassLevelPath(psiClass);
                scanMethodsForEndpoints(psiClass, classLevelPath, endpoints, project);
            }
        }
    }
    
    /**
     * 检查类是否有@Path注解
     */
    private boolean hasPathAnnotation(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null) {
                for (String pathAnnotation : JAXRS_PATH_ANNOTATIONS) {
                    if (qualifiedName.equals(pathAnnotation)) {
                        return true;
                    }
                }
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
        String methodPath = "";
        String httpMethod = null;
        
        // 查找HTTP方法注解和路径注解
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) continue;
            
            // 检查HTTP方法注解
            if (httpMethod == null) {
                httpMethod = getHttpMethodFromAnnotation(qualifiedName);
            }
            
            // 检查路径注解
            for (String pathAnnotation : JAXRS_PATH_ANNOTATIONS) {
                if (qualifiedName.equals(pathAnnotation)) {
                    methodPath = extractPathFromAnnotation(annotation);
                    break;
                }
            }
        }
        
        // 如果找到HTTP方法注解，创建端点
        if (httpMethod != null) {
            String fullPath = combinePaths(classLevelPath, methodPath);
            if (fullPath.isEmpty()) {
                fullPath = "/"; // 默认根路径
            }
            
            String className = method.getContainingClass().getName();
            String methodName = method.getName();
            
            RestfulEndpointNavigationItem endpoint = new RestfulEndpointNavigationItem(
                httpMethod, fullPath, className, methodName, method, project
            );
            endpoints.add(endpoint);
        }
    }
    
    /**
     * 提取类级别的路径
     */
    private String extractClassLevelPath(PsiClass psiClass) {
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null) {
                for (String pathAnnotation : JAXRS_PATH_ANNOTATIONS) {
                    if (qualifiedName.equals(pathAnnotation)) {
                        return extractPathFromAnnotation(annotation);
                    }
                }
            }
        }
        
        return "";
    }
    
    /**
     * 从注解中提取路径
     */
    private String extractPathFromAnnotation(PsiAnnotation annotation) {
        String annotationText = annotation.getText();
        Matcher matcher = PATH_VALUE_PATTERN.matcher(annotationText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
    
    /**
     * 根据注解获取HTTP方法
     */
    private String getHttpMethodFromAnnotation(String qualifiedName) {
        if (qualifiedName.endsWith(".GET")) {
            return "GET";
        } else if (qualifiedName.endsWith(".POST")) {
            return "POST";
        } else if (qualifiedName.endsWith(".PUT")) {
            return "PUT";
        } else if (qualifiedName.endsWith(".DELETE")) {
            return "DELETE";
        } else if (qualifiedName.endsWith(".PATCH")) {
            return "PATCH";
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
                        if (hasSpecificAnnotation(psiClass, annotationName)) {
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
    private boolean hasSpecificAnnotation(PsiClass psiClass, String annotationName) {
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