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
// import com.jetbrains.python.psi.*; // 可选依赖

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FastAPI框架RESTful端点扫描策略
 * 支持PyCharm中的FastAPI项目
 */
public class FastApiEndpointScanStrategy implements RestfulEndpointScanStrategy {
    
    private static final String STRATEGY_NAME = "FastAPI";
    private static final int PRIORITY = 2; // 中等优先级
    
    // FastAPI路由装饰器模式
    private static final String[] FASTAPI_DECORATORS = {
        "app.get", "app.post", "app.put", "app.delete", "app.patch",
        "router.get", "router.post", "router.put", "router.delete", "router.patch",
        "api.get", "api.post", "api.put", "api.delete", "api.patch"
    };
    
    // FastAPI路径提取正则
    private static final Pattern ROUTE_PATTERN = Pattern.compile("@(\\w+)\\.(get|post|put|delete|patch)\\s*\\(\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern ROUTE_PATTERN_EXTENDED = Pattern.compile("\\.(get|post|put|delete|patch)\\s*\\(\\s*['\"]([^'\"]*)['\"]");
    
    private PsiManager psiManager;
    
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
            // 扫描Python文件中的FastAPI路由
            scanPythonFiles(project, endpoints);
            
            // 去重并排序
            return deduplicateAndSort(endpoints);
            
        } catch (Exception e) {
            System.err.println("FastAPI strategy scan failed: " + e.getMessage());
            return new ArrayList<>();
        }
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
                   fileText.contains("FastAPI");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 扫描Python文件中的FastAPI路由
     */
    private void scanPythonFiles(Project project, List<RestfulEndpointNavigationItem> endpoints) {
        try {
            FileType pythonFileType = FileTypeManager.getInstance().getFileTypeByExtension("py");
            if (pythonFileType == null) return;
            
            Collection<VirtualFile> pythonFiles = FileTypeIndex.getFiles(pythonFileType, 
                GlobalSearchScope.projectScope(project));

            for (VirtualFile virtualFile : pythonFiles) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    scanPythonFile(psiFile, endpoints, project);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to scan Python files: " + e.getMessage());
        }
    }
    
    /**
     * 扫描单个Python文件
     */
    private void scanPythonFile(PsiFile pyFile, List<RestfulEndpointNavigationItem> endpoints, Project project) {
        try {
            String fileText = pyFile.getText();
            String fileName = pyFile.getName();
            
            // 使用正则表达式匹配FastAPI路由装饰器
            Matcher matcher = ROUTE_PATTERN.matcher(fileText);
            while (matcher.find()) {
                String appName = matcher.group(1);
                String httpMethod = matcher.group(2).toUpperCase();
                String path = matcher.group(3);
                
                // 查找对应的函数名（简化处理）
                String functionName = findFunctionNameAfterDecorator(fileText, matcher.start());
                if (functionName != null && !functionName.isEmpty()) {
                    String className = fileName.replace(".py", "");
                    
                    RestfulEndpointNavigationItem endpoint = new RestfulEndpointNavigationItem(
                        httpMethod, path, className, functionName, null, project
                    );
                    endpoints.add(endpoint);
                }
            }
            
            // 备用模式：更宽泛的匹配
            Matcher extendedMatcher = ROUTE_PATTERN_EXTENDED.matcher(fileText);
            while (extendedMatcher.find()) {
                String httpMethod = extendedMatcher.group(1).toUpperCase();
                String path = extendedMatcher.group(2);
                
                String functionName = findFunctionNameAfterDecorator(fileText, extendedMatcher.start());
                if (functionName != null && !functionName.isEmpty()) {
                    String className = fileName.replace(".py", "");
                    
                    // 检查是否已存在相同的端点
                    boolean exists = endpoints.stream().anyMatch(ep -> 
                        ep.getHttpMethod().equals(httpMethod) && 
                        ep.getPath().equals(path) && 
                        ep.getMethodName().equals(functionName)
                    );
                    
                    if (!exists) {
                        RestfulEndpointNavigationItem endpoint = new RestfulEndpointNavigationItem(
                            httpMethod, path, className, functionName, null, project
                        );
                        endpoints.add(endpoint);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error scanning Python file " + pyFile.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * 从文本中查找装饰器后面的函数名
     */
    private String findFunctionNameAfterDecorator(String fileText, int decoratorStart) {
        try {
            // 从装饰器位置往后查找 def 关键字
            String afterDecorator = fileText.substring(decoratorStart);
            Pattern functionPattern = Pattern.compile("\\ndef\\s+(\\w+)\\s*\\(");
            Matcher matcher = functionPattern.matcher(afterDecorator);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 如果没找到，尝试更宽泛的匹配
            Pattern relaxedPattern = Pattern.compile("def\\s+(\\w+)\\s*\\(");
            Matcher relaxedMatcher = relaxedPattern.matcher(afterDecorator.substring(0, Math.min(200, afterDecorator.length())));
            
            if (relaxedMatcher.find()) {
                return relaxedMatcher.group(1);
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