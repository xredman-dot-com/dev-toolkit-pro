package com.devtoolkit.pro.inlay;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
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

public class RestfulUrlLineMarkerProvider implements LineMarkerProvider {
    
    private static final String[] SPRING_MAPPING_ANNOTATIONS = {
        "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", 
        "PatchMapping", "RequestMapping"
    };

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // 只处理注解的标识符（叶子元素）
        if (!(element instanceof PsiIdentifier)) {
            return null;
        }

        // 检查父元素是否是注解
        PsiElement parent = element.getParent();
        if (!(parent instanceof PsiJavaCodeReferenceElement)) {
            return null;
        }
        
        PsiElement grandParent = parent.getParent();
        if (!(grandParent instanceof PsiAnnotation)) {
            return null;
        }

        PsiAnnotation annotation = (PsiAnnotation) grandParent;
        String annotationName = getAnnotationName(annotation);
        
        if (!isSpringMappingAnnotation(annotationName)) {
            return null;
        }

        // 获取注解的值
        String path = extractPathFromAnnotation(annotation);
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 构建完整URL
        String fullUrl = buildFullUrl(annotation, path);
        
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