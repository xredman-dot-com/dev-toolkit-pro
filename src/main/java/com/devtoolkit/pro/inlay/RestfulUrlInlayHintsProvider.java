package com.devtoolkit.pro.inlay;

import com.devtoolkit.pro.services.RestfulUrlService;
import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.ui.JBColor;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class RestfulUrlInlayHintsProvider implements InlayHintsProvider<NoSettings>, DumbAware {
    private static final Logger LOG = Logger.getInstance(RestfulUrlInlayHintsProvider.class);
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
        LOG.info("[InlayHints] getCollectorFor called for file: " + file.getName() + 
                ", language: " + file.getLanguage().getDisplayName() + 
                ", file type: " + file.getClass().getSimpleName());
        
        // 支持Java和Kotlin文件
        if (!(file instanceof PsiJavaFile) && !"Kotlin".equals(file.getLanguage().getDisplayName())) {
            LOG.debug("[InlayHints] File not supported, skipping: " + file.getName());
            return null;
        }

        LOG.info("[InlayHints] Creating collector for file: " + file.getName());
        return new RestfulUrlInlayCollector(editor, file.getProject());
    }

    private static class RestfulUrlInlayCollector extends FactoryInlayHintsCollector {
        private final RestfulUrlService urlService;

        public RestfulUrlInlayCollector(@NotNull Editor editor, @NotNull com.intellij.openapi.project.Project project) {
            super(editor);
            this.urlService = new RestfulUrlService(project);
            LOG.info("[InlayHints-Collector] Created collector for project: " + project.getName());
        }

        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            try {
                LOG.debug("[InlayHints-Collect] Processing element: " + element.getClass().getSimpleName() + 
                         ", text: '" + element.getText().substring(0, Math.min(50, element.getText().length())) + "...'");
                
                // 处理Java方法
                if (element instanceof PsiMethod) {
                    LOG.info("[InlayHints-Collect] Found Java method: " + ((PsiMethod) element).getName());
                    return processJavaMethod((PsiMethod) element, sink);
                }
                
                // 处理Kotlin函数 - 使用更通用的方法
                if (isKotlinFunction(element)) {
                    LOG.info("[InlayHints-Collect] Found Kotlin function");
                    return processKotlinFunction(element, sink);
                }
                
                // 只有当不是方法或函数时，才返回true，避免重复处理
                return true;
                
            } catch (Exception e) {
                LOG.error("[InlayHints-Collect] Error processing element: " + element, e);
                return true;
            }
        }
        
        private boolean processJavaMethod(PsiMethod method, InlayHintsSink sink) {
            try {
                LOG.info("[InlayHints-Java] Processing Java method: " + method.getName());
                PsiAnnotation[] annotations = method.getAnnotations();
                LOG.debug("[InlayHints-Java] Found " + annotations.length + " annotations");

                // 只处理第一个找到的Spring映射注解，避免重复显示
                PsiAnnotation targetAnnotation = null;
                String targetAnnotationName = null;
                
                for (PsiAnnotation annotation : annotations) {
                    String annotationName = getAnnotationName(annotation);
                    LOG.debug("[InlayHints-Java] Checking annotation: " + annotationName);

                    if (isSpringMappingAnnotation(annotationName)) {
                        targetAnnotation = annotation;
                        targetAnnotationName = annotationName;
                        LOG.info("[InlayHints-Java] Found Spring mapping annotation: " + annotationName);
                        break; // 只处理第一个找到的映射注解
                    }
                }
                
                if (targetAnnotation == null) {
                    LOG.debug("[InlayHints-Java] No Spring mapping annotation found");
                    return true;
                }

                // 获取注解的值
                String path = urlService.extractPathFromAnnotation(targetAnnotation);
                LOG.info("[InlayHints-Java] Extracted path: " + path);
                if (path == null || path.isEmpty()) {
                    LOG.warn("[InlayHints-Java] Path is null or empty, skipping");
                    return true;
                }

                // 获取HTTP方法
                String httpMethod = getHttpMethodFromAnnotation(targetAnnotationName);

                // 使用RestfulUrlService构建完整URL（包含类级别路径）
                String fullUrl = urlService.buildFullUrl(targetAnnotation, path);

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
                    
                    // 显示复制成功的通知
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("RestfulTool")
                        .createNotification("复制成功", "已复制: " + displayText, NotificationType.INFORMATION)
                        .notify(method.getProject());
                    
                    return null;
                });

                // 添加工具提示
                InlayPresentation withTooltip = factory.withTooltip("点击复制 RESTful URL: " + displayText, clickablePresentation);

                // 在注解后添加inlay hint
                LOG.info("[InlayHints-Java] Adding inlay hint at offset: " + targetAnnotation.getTextRange().getEndOffset() + 
                        ", display text: " + displayText);
                sink.addInlineElement(targetAnnotation.getTextRange().getEndOffset(), false, withTooltip, false);

                return true;
            } catch (Exception e) {
                LOG.error("[InlayHints-Java] Error processing Java method: " + method.getName(), e);
                return true;
            }
        }
        
        private boolean isKotlinFunction(PsiElement element) {
            // 使用反射检测Kotlin函数
            String className = element.getClass().getName();
            boolean isKotlinFunction = className.contains("Kt") && 
                   (className.contains("Function") || className.contains("NamedFunction") || 
                    className.contains("KtNamedFunction") || className.contains("KtFunction"));
            LOG.debug("[InlayHints-Kotlin] Checking if Kotlin function: " + className + ", result: " + isKotlinFunction);
            return isKotlinFunction;
        }
        
        private boolean processGenericElement(PsiElement element, InlayHintsSink sink) {
            try {
                String elementText = element.getText();
                if (elementText == null) return true;
                
                // 检查是否包含Spring注解（包括注释形式）
                for (String annotation : SPRING_MAPPING_ANNOTATIONS) {
                    // 检查普通注解形式
                    if (elementText.contains("@" + annotation)) {
                        return processElementWithAnnotation(element, annotation, sink);
                    }
                    // 检查注释形式的注解
                    if (elementText.contains("// @" + annotation)) {
                        return processCommentedAnnotation(element, annotation, sink);
                    }
                }
                
                return true;
            } catch (Exception e) {
                return true;
            }
        }
        
        private boolean processElementWithAnnotation(PsiElement element, String annotationName, InlayHintsSink sink) {
            try {
                String elementText = element.getText();
                
                // 提取注解中的路径值
                String path = extractPathFromText(elementText, annotationName);
                if (path == null || path.isEmpty()) {
                    return true;
                }
                
                // 获取HTTP方法
                String httpMethod = getHttpMethodFromAnnotation(annotationName);
                
                // 构建完整URL
                String fullUrl = urlService.buildFullUrl(null, path);
                String displayText = httpMethod + " " + fullUrl;
                
                // 创建Inlay presentation
                PresentationFactory factory = getFactory();
                InlayPresentation presentation = createBaselineAlignedPresentation(factory, displayText);
                
                // 添加点击事件
                InlayPresentation clickablePresentation = factory.onClick(presentation, MouseButton.Left, (event, translated) -> {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(displayText), null
                    );
                    
                    // 显示复制成功的通知
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("RestfulTool")
                        .createNotification("复制成功", "已复制: " + displayText, NotificationType.INFORMATION)
                        .notify(element.getProject());
                    
                    return null;
                });
                
                // 添加工具提示
                InlayPresentation withTooltip = factory.withTooltip("点击复制 RESTful URL: " + displayText, clickablePresentation);
                
                // 在元素后添加inlay hint
                sink.addInlineElement(element.getTextRange().getEndOffset(), false, withTooltip, false);
                
                return true;
            } catch (Exception e) {
                return true;
            }
        }
        
        private String extractPathFromText(String text, String annotationName) {
            try {
                // 查找注解
                String annotationPattern = "@" + annotationName;
                int annotationIndex = text.indexOf(annotationPattern);
                if (annotationIndex == -1) return null;
                
                // 查找括号内的内容
                int openParen = text.indexOf("(", annotationIndex);
                if (openParen == -1) return null;
                
                int closeParen = text.indexOf(")", openParen);
                if (closeParen == -1) return null;
                
                String content = text.substring(openParen + 1, closeParen).trim();
                
                // 提取字符串值
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    return content.substring(1, content.length() - 1);
                }
                
                // 处理value = "..."的情况
                if (content.contains("value")) {
                    int valueIndex = content.indexOf("value");
                    int equalIndex = content.indexOf("=", valueIndex);
                    if (equalIndex != -1) {
                        String valueContent = content.substring(equalIndex + 1).trim();
                        if (valueContent.startsWith("\"") && valueContent.endsWith("\"")) {
                            return valueContent.substring(1, valueContent.length() - 1);
                        }
                    }
                }
                
                return content.replaceAll("[\"']", "");
            } catch (Exception e) {
                return null;
            }
        }
        
        private boolean processKotlinFunction(PsiElement element, InlayHintsSink sink) {
            try {
                // 尝试多种方法获取Kotlin函数的注解
                List<PsiElement> annotations = new ArrayList<>();
                
                // 方法1: 尝试getAnnotationEntries
                try {
                    Method getAnnotationEntriesMethod = element.getClass().getMethod("getAnnotationEntries");
                    Object annotationEntries = getAnnotationEntriesMethod.invoke(element);
                    if (annotationEntries instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<PsiElement> entries = (List<PsiElement>) annotationEntries;
                        annotations.addAll(entries);
                    }
                } catch (Exception ignored) {}
                
                // 方法2: 尝试getModifierList
                if (annotations.isEmpty()) {
                    try {
                        Method getModifierListMethod = element.getClass().getMethod("getModifierList");
                        Object modifierList = getModifierListMethod.invoke(element);
                        if (modifierList != null) {
                            Method getAnnotationsMethod = modifierList.getClass().getMethod("getAnnotations");
                            Object annotationsArray = getAnnotationsMethod.invoke(modifierList);
                            if (annotationsArray instanceof PsiElement[]) {
                                PsiElement[] annotationArray = (PsiElement[]) annotationsArray;
                                for (PsiElement annotation : annotationArray) {
                                    annotations.add(annotation);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                
                // 方法3: 直接查找子元素中的注解
                if (annotations.isEmpty()) {
                    PsiElement[] children = element.getChildren();
                    for (PsiElement child : children) {
                        String childClassName = child.getClass().getName();
                        if (childClassName.contains("Annotation") || childClassName.contains("KtAnnotation")) {
                            annotations.add(child);
                        }
                    }
                }
                
                // 处理找到的注解
                for (PsiElement annotation : annotations) {
                    String annotationText = annotation.getText();
                    String annotationName = extractKotlinAnnotationName(annotationText);
                    
                    if (!isSpringMappingAnnotation(annotationName)) {
                        continue;
                    }
                    
                    // 提取路径值
                    String path = extractKotlinAnnotationValue(annotationText);
                    if (path == null || path.isEmpty()) {
                        continue;
                    }
                    
                    // 获取HTTP方法
                    String httpMethod = getHttpMethodFromAnnotation(annotationName);
                    
                    // 构建完整URL - 尝试获取包含类的信息
                    String fullUrl;
                    try {
                        // 对于Kotlin文件，需要查找Kotlin类
                        Object ktClass = findKotlinClass(element);
                        if (ktClass != null) {
                            String classPath = findKotlinClassRequestMapping(ktClass);
                            if (classPath != null && !classPath.isEmpty()) {
                                // 手动构建完整URL，因为urlService.buildFullUrl可能不支持Kotlin类
                                String basePath = classPath;
                                if (!basePath.startsWith("/")) {
                                    basePath = "/" + basePath;
                                }
                                if (!path.startsWith("/")) {
                                    path = "/" + path;
                                }
                                fullUrl = "http://localhost:8080" + basePath + path;
                            } else {
                                fullUrl = urlService.buildFullUrl(null, path);
                            }
                        } else {
                            fullUrl = urlService.buildFullUrl(null, path);
                        }
                    } catch (Exception e) {
                        LOG.warn("[InlayHints-Kotlin] Failed to get class-level path, using method path only", e);
                        fullUrl = urlService.buildFullUrl(null, path);
                    }
                    String displayText = httpMethod + " " + fullUrl;
                    
                    // 创建Inlay presentation
                    PresentationFactory factory = getFactory();
                    InlayPresentation presentation = createBaselineAlignedPresentation(factory, displayText);
                    
                    // 添加点击事件
                    InlayPresentation clickablePresentation = factory.onClick(presentation, MouseButton.Left, (event, translated) -> {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(displayText), null
                        );
                        
                        // 显示复制成功的通知
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RestfulTool")
                            .createNotification("复制成功", "已复制: " + displayText, NotificationType.INFORMATION)
                            .notify(element.getProject());
                        
                        return null;
                    });
                    
                    // 添加工具提示
                    InlayPresentation withTooltip = factory.withTooltip("点击复制 RESTful URL: " + displayText, clickablePresentation);
                    
                    // 在注解后添加inlay hint
                    sink.addInlineElement(annotation.getTextRange().getEndOffset(), false, withTooltip, false);
                    
                    break; // 只处理第一个匹配的注解
                }
            } catch (Exception e) {
                // 静默处理异常
            }
            return true;
        }
        
        private String extractKotlinAnnotationName(String annotationText) {
            // 从@GetMapping等注解文本中提取注解名
            if (annotationText.startsWith("@")) {
                String name = annotationText.substring(1);
                int parenIndex = name.indexOf('(');
                if (parenIndex > 0) {
                    name = name.substring(0, parenIndex);
                }
                return name;
            }
            return "";
        }
        
        private String extractKotlinAnnotationValue(String annotationText) {
            // 简单的值提取，查找引号内的内容
            int firstQuote = annotationText.indexOf('"');
            if (firstQuote >= 0) {
                int secondQuote = annotationText.indexOf('"', firstQuote + 1);
                if (secondQuote > firstQuote) {
                    return annotationText.substring(firstQuote + 1, secondQuote);
                }
            }
            return null;
        }

        /**
         * 创建baseline对齐的presentation
         */
        private InlayPresentation createBaselineAlignedPresentation(PresentationFactory factory, String fullUrl) {
            // 创建文字presentation
            InlayPresentation textPresentation = factory.text(fullUrl);
            
            // 使用roundWithBackground来创建带浅色圆角背景的presentation
            // 这会自动应用IntelliJ的默认Inlay背景色
            return factory.roundWithBackground(textPresentation);
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
                            return extractKotlinAnnotationValue(entry.toString());
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        
        private String getKotlinAnnotationName(Object ktAnnotationEntry) {
            try {
                String text = ktAnnotationEntry.toString();
                if (text.contains("@")) {
                    int start = text.indexOf("@") + 1;
                    int end = text.indexOf("(", start);
                    if (end == -1) {
                        end = text.length();
                    }
                    return text.substring(start, end).trim();
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }
        
        private boolean processCommentedAnnotation(PsiElement element, String annotationName, InlayHintsSink sink) {
            try {
                String elementText = element.getText();
                
                // 提取注释中注解的路径值
                String path = extractPathFromCommentedAnnotation(elementText, annotationName);
                if (path == null || path.isEmpty()) {
                    return true;
                }
                
                // 获取HTTP方法
                String httpMethod = getHttpMethodFromAnnotation(annotationName);
                
                // 构建完整URL
                String fullUrl = urlService.buildFullUrl(null, path);
                String displayText = httpMethod + " " + fullUrl;
                
                // 创建Inlay presentation
                PresentationFactory factory = getFactory();
                InlayPresentation presentation = createBaselineAlignedPresentation(factory, displayText);
                
                // 添加点击事件
                InlayPresentation clickablePresentation = factory.onClick(presentation, MouseButton.Left, (event, translated) -> {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(displayText), null
                    );
                    
                    // 显示复制成功的通知
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("RestfulTool")
                        .createNotification("复制成功", "已复制: " + displayText, NotificationType.INFORMATION)
                        .notify(element.getProject());
                    
                    return null;
                });
                
                // 添加工具提示
                InlayPresentation withTooltip = factory.withTooltip("点击复制 RESTful URL: " + displayText, clickablePresentation);
                
                // 在元素后添加inlay hint
                sink.addInlineElement(element.getTextRange().getEndOffset(), false, withTooltip, false);
                
                return true;
            } catch (Exception e) {
                return true;
            }
        }
        
        private String extractPathFromCommentedAnnotation(String text, String annotationName) {
            try {
                // 查找注释中的注解
                String annotationPattern = "// @" + annotationName;
                int annotationIndex = text.indexOf(annotationPattern);
                if (annotationIndex == -1) return null;
                
                // 查找括号内的内容
                int openParen = text.indexOf("(", annotationIndex);
                if (openParen == -1) return null;
                
                int closeParen = text.indexOf(")", openParen);
                if (closeParen == -1) return null;
                
                String content = text.substring(openParen + 1, closeParen).trim();
                
                // 移除引号
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length() - 1);
                } else if (content.startsWith("'") && content.endsWith("'")) {
                    content = content.substring(1, content.length() - 1);
                }
                
                return content;
            } catch (Exception e) {
                return null;
            }
        }

        // 已移动到RestfulUrlService中作为公共方法
    }
}