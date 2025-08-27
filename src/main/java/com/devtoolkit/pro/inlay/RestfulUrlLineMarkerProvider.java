package com.devtoolkit.pro.inlay;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class RestfulUrlLineMarkerProvider implements LineMarkerProvider {
    private static final Logger LOG = Logger.getInstance(RestfulUrlLineMarkerProvider.class);
    
    private static final String[] SPRING_MAPPING_ANNOTATIONS = {
        "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", 
        "PatchMapping", "RequestMapping"
    };
    
    // 用于避免重复处理相同的注解
    private final Set<Object> processedAnnotations = new HashSet<>();

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        try {
            LOG.info("[LineMarker] Processing element: " + element.getClass().getSimpleName() + 
                    ", text: '" + element.getText() + "', file: " + 
                    (element.getContainingFile() != null ? element.getContainingFile().getName() : "null"));
            
            // 处理Java注解的标识符（叶子元素）
            if (element instanceof PsiIdentifier) {
                // 先检查是否已经为这个注解创建过LineMarker
                PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
                if (annotation != null && processedAnnotations.contains(annotation)) {
                    LOG.debug("[LineMarker] Java annotation already processed, skipping: " + annotation.getQualifiedName());
                    return null;
                }
                LOG.info("[LineMarker] Detected Java PsiIdentifier, delegating to handleJavaAnnotation");
                return handleJavaAnnotation(element);
            }
            
            // 处理Kotlin注解 - 检查是否是Kotlin注解的标识符
            if (isKotlinAnnotationIdentifier(element)) {
                // 先检查是否已经为这个注解创建过LineMarker
                Object ktAnnotationEntry = findKotlinAnnotationEntry(element);
                if (ktAnnotationEntry != null && processedAnnotations.contains(ktAnnotationEntry)) {
                    LOG.debug("[LineMarker] Kotlin annotation already processed, skipping");
                    return null;
                }
                LOG.info("[LineMarker] Detected Kotlin annotation identifier, delegating to handleKotlinAnnotation");
                return handleKotlinAnnotation(element);
            }
            
            LOG.debug("[LineMarker] Element not processed: " + element.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            LOG.error("[LineMarker] Error processing element: " + element, e);
            return null;
        }
    }
    
    private @Nullable LineMarkerInfo<?> handleJavaAnnotation(@NotNull PsiElement element) {
        LOG.info("[LineMarker-Java] Processing Java annotation for element: " + element.getText());
        
        // 检查父元素是否是注解
        PsiElement parent = element.getParent();
        LOG.debug("[LineMarker-Java] Parent element: " + (parent != null ? parent.getClass().getSimpleName() : "null"));
        
        if (!(parent instanceof PsiJavaCodeReferenceElement)) {
            LOG.debug("[LineMarker-Java] Parent is not PsiJavaCodeReferenceElement, skipping");
            return null;
        }
        
        PsiElement grandParent = parent.getParent();
        LOG.debug("[LineMarker-Java] GrandParent element: " + (grandParent != null ? grandParent.getClass().getSimpleName() : "null"));
        
        if (!(grandParent instanceof PsiAnnotation)) {
            LOG.debug("[LineMarker-Java] GrandParent is not PsiAnnotation, skipping");
            return null;
        }

        PsiAnnotation annotation = (PsiAnnotation) grandParent;
        
        String annotationName = getAnnotationName(annotation);
        LOG.info("[LineMarker-Java] Found annotation: " + annotationName);
        
        if (!isSpringMappingAnnotation(annotationName)) {
            LOG.debug("[LineMarker-Java] Not a Spring mapping annotation, skipping");
            return null;
        }

        // 只有在确认是Spring注解时才标记为已处理
        processedAnnotations.add(annotation);

        // 获取注解的值
        String path = extractPathFromAnnotation(annotation);
        LOG.info("[LineMarker-Java] Extracted path: " + path);
        
        if (path == null || path.isEmpty()) {
            LOG.warn("[LineMarker-Java] Path is null or empty, skipping");
            return null;
        }

        // 构建完整URL
        String fullUrl = buildFullUrl(annotation, path);
        LOG.info("[LineMarker-Java] Creating line marker for URL: " + fullUrl);
        
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            AllIcons.Actions.Copy,
            psiElement -> "Copy RESTful URL: " + fullUrl,
            new GutterIconNavigationHandler(fullUrl),
            GutterIconRenderer.Alignment.LEFT,
            () -> "RESTful URL"
        );
    }
    
    private @Nullable LineMarkerInfo<?> handleKotlinAnnotation(@NotNull PsiElement element) {
        try {
            LOG.info("[LineMarker-Kotlin] Processing Kotlin annotation for element: " + element.getText());
            
            // 使用反射来处理Kotlin PSI元素，避免直接依赖Kotlin类
            Object ktAnnotationEntry = findKotlinAnnotationEntry(element);
            if (ktAnnotationEntry == null) {
                LOG.warn("[LineMarker-Kotlin] No KtAnnotationEntry found for element");
                return null;
            }
            
            LOG.info("[LineMarker-Kotlin] Found KtAnnotationEntry: " + ktAnnotationEntry.getClass().getSimpleName());
            
            String annotationName = getKotlinAnnotationName(ktAnnotationEntry);
            LOG.info("[LineMarker-Kotlin] Kotlin annotation name: " + annotationName);
            
            if (!isSpringMappingAnnotation(annotationName)) {
                LOG.debug("[LineMarker-Kotlin] Not a Spring mapping annotation, skipping");
                return null;
            }
            
            // 只有在确认是Spring注解时才标记为已处理
            processedAnnotations.add(ktAnnotationEntry);
            
            String path = extractPathFromKotlinAnnotation(ktAnnotationEntry);
            LOG.info("[LineMarker-Kotlin] Extracted path: " + path);
            
            if (path == null || path.isEmpty()) {
                LOG.warn("[LineMarker-Kotlin] Path is null or empty, skipping");
                return null;
            }
            
            String fullUrl = buildFullUrlFromKotlin(ktAnnotationEntry, path);
            LOG.info("[LineMarker-Kotlin] Creating line marker for URL: " + fullUrl);
            
            return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                AllIcons.Actions.Copy,
                psiElement -> "Copy RESTful URL: " + fullUrl,
                new GutterIconNavigationHandler(fullUrl),
                GutterIconRenderer.Alignment.LEFT,
                () -> "RESTful URL"
            );
        } catch (Exception e) {
            LOG.error("[LineMarker-Kotlin] Error processing Kotlin annotation", e);
            return null;
        }
    }
    
    private boolean isKotlinAnnotationIdentifier(@NotNull PsiElement element) {
        try {
            // 检查是否是Kotlin文件中的标识符
            PsiFile file = element.getContainingFile();
            LOG.info("[LineMarker-Kotlin] Checking file: " + (file != null ? file.getName() : "null") + 
                     ", language: " + (file != null ? file.getLanguage().getDisplayName() : "null"));
            
            if (file == null || !"Kotlin".equals(file.getLanguage().getDisplayName())) {
                LOG.info("[LineMarker-Kotlin] Not a Kotlin file, skipping");
                return false;
            }
            
            // 检查元素类型是否是Kotlin的标识符
            String elementClassName = element.getClass().getSimpleName();
            String elementType = element.getNode() != null ? element.getNode().getElementType().toString() : "null";
            
            LOG.info("[LineMarker-Kotlin] Element class: " + elementClassName + ", element type: " + elementType + ", text: '" + element.getText() + "'");
            
            // 打印PSI树结构以便调试
            printPSITree(element, 0, 3);
            
            // 检查是否是Kotlin标识符且在注解上下文中
            if ("KtNameReferenceExpression".equals(elementClassName) || 
                "LeafPsiElement".equals(elementClassName) ||
                "KtTypeReference".equals(elementClassName)) {
                
                // 检查父元素是否包含注解相关的类型
                PsiElement parent = element.getParent();
                int depth = 0;
                while (parent != null && depth < 10) { // 限制搜索深度避免ProcessCanceledException
                    String parentType = parent.getClass().getSimpleName();
                    LOG.info("[LineMarker-Kotlin] Checking parent: " + parentType);
                    if (parentType.contains("Annotation") || parentType.contains("KtAnnotation")) {
                        LOG.info("[LineMarker-Kotlin] Found annotation context: " + parentType);
                        return true;
                    }
                    parent = parent.getParent();
                    depth++;
                }
            }
            
            boolean isKotlinIdentifier = "LeafPsiElement".equals(elementClassName) && 
                   element.getNode() != null && 
                   "IDENTIFIER".equals(element.getNode().getElementType().toString());
            
            LOG.info("[LineMarker-Kotlin] Is Kotlin identifier: " + isKotlinIdentifier);
            return isKotlinIdentifier;
        } catch (Exception e) {
            LOG.error("[LineMarker-Kotlin] Error checking Kotlin annotation identifier", e);
            return false;
        }
    }
    
    private void printPSITree(PsiElement element, int depth, int maxDepth) {
        if (depth > maxDepth) return;
        
        try {
            String indent = "  ".repeat(depth);
            String elementInfo = indent + "- " + element.getClass().getSimpleName() + ": '" + element.getText().replace("\n", "\\n") + "'";
            LOG.info("[LineMarker-Kotlin-PSI] " + elementInfo);
            
            if (depth < maxDepth) {
                PsiElement[] children = element.getChildren();
                for (PsiElement child : children) {
                    printPSITree(child, depth + 1, maxDepth);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }
    
    private Object findKotlinAnnotationEntry(@NotNull PsiElement element) {
        try {
            // 首先检查当前元素
            String className = element.getClass().getSimpleName();
            LOG.info("[LineMarker-Kotlin] Checking element class: " + className);
            
            if ("KtAnnotationEntry".equals(className)) {
                LOG.info("[LineMarker-Kotlin] Found KtAnnotationEntry directly");
                return element;
            }
            
            // 检查父元素
            PsiElement current = element.getParent();
            while (current != null) {
                className = current.getClass().getSimpleName();
                LOG.info("[LineMarker-Kotlin] Checking parent class: " + className);
                if ("KtAnnotationEntry".equals(className)) {
                    LOG.info("[LineMarker-Kotlin] Found KtAnnotationEntry in parent");
                    return current;
                }
                current = current.getParent();
            }
            
            // 检查子元素
            PsiElement[] children = element.getChildren();
            for (PsiElement child : children) {
                className = child.getClass().getSimpleName();
                LOG.info("[LineMarker-Kotlin] Checking child class: " + className);
                if ("KtAnnotationEntry".equals(className)) {
                    LOG.info("[LineMarker-Kotlin] Found KtAnnotationEntry in child");
                    return child;
                }
                // 递归检查子元素的子元素
                Object result = findKotlinAnnotationEntryRecursive(child);
                if (result != null) {
                    return result;
                }
            }
            
            LOG.info("[LineMarker-Kotlin] No KtAnnotationEntry found");
            return null;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin] Exception in findKotlinAnnotationEntry: " + e.getMessage());
            return null;
        }
    }
    
    private Object findKotlinAnnotationEntryRecursive(@NotNull PsiElement element) {
        try {
            String className = element.getClass().getSimpleName();
            if ("KtAnnotationEntry".equals(className)) {
                return element;
            }
            
            PsiElement[] children = element.getChildren();
            for (PsiElement child : children) {
                Object result = findKotlinAnnotationEntryRecursive(child);
                if (result != null) {
                    return result;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getKotlinAnnotationName(Object ktAnnotationEntry) {
        try {
            // 使用反射获取Kotlin注解名称
            Object shortName = ktAnnotationEntry.getClass().getMethod("getShortName").invoke(ktAnnotationEntry);
            if (shortName != null) {
                return shortName.toString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractPathFromKotlinAnnotation(Object ktAnnotationEntry) {
        try {
            // 使用反射获取Kotlin注解的值
            Object valueArguments = ktAnnotationEntry.getClass().getMethod("getValueArguments").invoke(ktAnnotationEntry);
            if (valueArguments instanceof List) {
                List<?> args = (List<?>) valueArguments;
                for (Object arg : args) {
                    Object argumentExpression = arg.getClass().getMethod("getArgumentExpression").invoke(arg);
                    if (argumentExpression != null) {
                        String text = argumentExpression.toString();
                        // 简单的字符串提取，去掉引号
                        if (text.startsWith("\"") && text.endsWith("\"")) {
                            return text.substring(1, text.length() - 1);
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String buildFullUrlFromKotlin(Object ktAnnotationEntry, String path) {
        try {
            // 查找类级别的@RequestMapping
            PsiElement element = (PsiElement) ktAnnotationEntry;
            Object ktClass = findKotlinClass(element);
            String basePath = "";
            
            if (ktClass != null) {
                String classPath = findKotlinClassRequestMapping(ktClass);
                if (classPath != null && !classPath.isEmpty()) {
                    basePath = classPath;
                }
            }

            // 确保路径以/开头
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (!basePath.isEmpty() && !basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }

            return "http://localhost:8080" + basePath + path;
        } catch (Exception e) {
            // 如果失败，返回简单的URL
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return "http://localhost:8080" + path;
        }
    }
    
    private Object findKotlinClass(@NotNull PsiElement element) {
        try {
            PsiElement current = element;
            while (current != null) {
                String className = current.getClass().getSimpleName();
                if ("KtClass".equals(className)) {
                    return current;
                }
                current = current.getParent();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String findKotlinClassRequestMapping(Object ktClass) {
        try {
            // 查找类上的@RequestMapping注解
            Object annotationEntries = ktClass.getClass().getMethod("getAnnotationEntries").invoke(ktClass);
            if (annotationEntries instanceof List) {
                List<?> entries = (List<?>) annotationEntries;
                for (Object entry : entries) {
                    String annotationName = getKotlinAnnotationName(entry);
                    if ("RequestMapping".equals(annotationName)) {
                        return extractPathFromKotlinAnnotation(entry);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        // 不需要实现，使用getLineMarkerInfo即可
    }

    private String extractPathFromAnnotation(PsiAnnotation annotation) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue("path");
        }
        
        if (value instanceof PsiLiteralExpression) {
            Object literalValue = ((PsiLiteralExpression) value).getValue();
            return literalValue != null ? literalValue.toString() : null;
        }
        
        return null;
    }

    private String buildFullUrl(PsiAnnotation annotation, String path) {
        // 查找类级别的@RequestMapping
        PsiClass containingClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class);
        String basePath = "";
        
        if (containingClass != null) {
            PsiAnnotation classMapping = containingClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
            if (classMapping != null) {
                String classPath = extractPathFromAnnotation(classMapping);
                if (classPath != null && !classPath.isEmpty()) {
                    basePath = classPath;
                }
            }
        }

        // 确保路径以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!basePath.isEmpty() && !basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }

        return "http://localhost:8080" + basePath + path;
    }

    private String getAnnotationName(PsiAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName == null) {
            return null;
        }
        
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private boolean isSpringMappingAnnotation(String annotationName) {
        if (annotationName == null) {
            return false;
        }
        
        for (String mappingAnnotation : SPRING_MAPPING_ANNOTATIONS) {
            if (mappingAnnotation.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static class GutterIconNavigationHandler implements com.intellij.codeInsight.daemon.GutterIconNavigationHandler<PsiElement> {
        private final String url;

        public GutterIconNavigationHandler(String url) {
            this.url = url;
        }

        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            // 复制URL到剪贴板
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
        }
    }
}