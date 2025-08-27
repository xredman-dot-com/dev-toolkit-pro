package com.devtoolkit.pro.inlay;

import com.devtoolkit.pro.services.RestfulUrlService;
import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class RestfulUrlInlayHintsProvider implements InlayHintsProvider<NoSettings>, DumbAware {
    public static final String PROVIDER_ID = "restful.url.hints";

    private static final String[] SPRING_MAPPING_ANNOTATIONS = {
        "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
        "PatchMapping", "RequestMapping"
    };

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return new SettingsKey<>(PROVIDER_ID);
    }

    @NotNull
    @Override
    public String getName() {
        return "RESTful URL hints";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "@GetMapping(\"/api/users\")\npublic List<User> getUsers() { ... }";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                return new JPanel();
            }
        };
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor, @NotNull NoSettings settings, @NotNull InlayHintsSink sink) {
        // 只对Java文件提供Inlay Hints
        if (!(file instanceof PsiJavaFile)) {
            return null;
        }

        return new RestfulUrlInlayCollector(editor, file.getProject());
    }

    private static class RestfulUrlInlayCollector extends FactoryInlayHintsCollector {
        private final RestfulUrlService urlService;

        public RestfulUrlInlayCollector(@NotNull Editor editor, @NotNull com.intellij.openapi.project.Project project) {
            super(editor);
            this.urlService = new RestfulUrlService(project);
        }

        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            try {
                // 查找方法上的Spring注解
                if (!(element instanceof PsiMethod)) {
                    return true;
                }

                PsiMethod method = (PsiMethod) element;
                PsiAnnotation[] annotations = method.getAnnotations();

                for (PsiAnnotation annotation : annotations) {
                    String annotationName = getAnnotationName(annotation);

                    if (!isSpringMappingAnnotation(annotationName)) {
                        continue;
                    }

                    // 获取注解的值
                    String path = urlService.extractPathFromAnnotation(annotation);
                    if (path == null || path.isEmpty()) {
                        continue;
                    }

                    // 获取HTTP方法
                    String httpMethod = getHttpMethodFromAnnotation(annotationName);

                    // 使用RestfulUrlService获取更准确的URL信息
                    List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
                    String fullUrl = urlService.buildFullUrl(annotation, path);

                    // 组合HTTP方法和URL
                    String displayText = httpMethod + " " + fullUrl;

                    // 获取编辑器字体大小
                    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
                    int fontSize = scheme.getEditorFontSize();

                    // 创建带有复制功能的 Inlay Hint，使用baseline对齐的图标
                    PresentationFactory factory = getFactory();

                    // 创建自定义的baseline对齐presentation
                    InlayPresentation presentation = createBaselineAlignedPresentation(factory, displayText);

                    // 添加点击事件
                    InlayPresentation clickablePresentation = factory.onClick(presentation, MouseButton.Left, (event, translated) -> {
                        // 复制到剪贴板
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(displayText), null
                        );
                        return null;
                    });

                    // 添加工具提示
                    InlayPresentation withTooltip = factory.withTooltip("点击复制 RESTful URL: " + displayText, clickablePresentation);

                    // 在注解后添加inlay hint
                    sink.addInlineElement(annotation.getTextRange().getEndOffset(), false, withTooltip, false);

                    break; // 只处理第一个匹配的注解
                }

                return true;
            } catch (Exception e) {
                // 忽略异常，继续处理
                return true;
            }
        }

        /**
         * 创建baseline对齐的presentation
         */
        private InlayPresentation createBaselineAlignedPresentation(PresentationFactory factory, String fullUrl) {
            // 使用roundWithBackground来创建一个带背景的presentation，这样可以更好地控制对齐
            InlayPresentation iconPresentation = factory.text(" 📋 ");
            InlayPresentation urlPresentation = factory.text(fullUrl);

            // 使用roundWithBackground来包装，这样可以确保baseline对齐
            InlayPresentation iconWithBackground = factory.roundWithBackground(iconPresentation);
            InlayPresentation urlWithBackground = factory.roundWithBackground(urlPresentation);

            // 使用seq组合，但现在每个部分都有相同的baseline
            return factory.seq(iconWithBackground, factory.text(" "), urlWithBackground);
        }

        // 已移动到RestfulUrlService中作为公共方法

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

        private String getHttpMethodFromAnnotation(String annotationName) {
            if (annotationName == null) {
                return "GET";
            }

            if (annotationName.contains("GetMapping")) {
                return "GET";
            } else if (annotationName.contains("PostMapping")) {
                return "POST";
            } else if (annotationName.contains("PutMapping")) {
                return "PUT";
            } else if (annotationName.contains("DeleteMapping")) {
                return "DELETE";
            } else if (annotationName.contains("PatchMapping")) {
                return "PATCH";
            } else if (annotationName.contains("RequestMapping")) {
                // 对于@RequestMapping，需要检查method属性，默认为GET
                return "GET";
            }

            return "GET";
        }

        // 已移动到RestfulUrlService中作为公共方法
    }
}