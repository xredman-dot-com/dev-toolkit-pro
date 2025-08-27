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
    
    public RestfulUrlInlayHintsProvider() {
        LOG.info("[InlayHints] *** PROVIDER CONSTRUCTOR CALLED *** RestfulUrlInlayHintsProvider created");
    }

    private static final String[] SPRING_MAPPING_ANNOTATIONS = {
        "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
        "PatchMapping", "RequestMapping"
    };

    @Override
    public boolean isVisibleInSettings() {
        LOG.info("[InlayHints] *** IS VISIBLE IN SETTINGS CALLED *** returning true");
        return true;
    }

    @Override
    public boolean isLanguageSupported(@NotNull com.intellij.lang.Language language) {
        boolean supported = "JAVA".equals(language.getID()) || "kotlin".equals(language.getID());
        LOG.info("[InlayHints] *** IS LANGUAGE SUPPORTED *** Language: " + language.getID() + ", supported: " + supported);
        return supported;
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
        LOG.info("[InlayHints] *** GET COLLECTOR FOR CALLED *** file: " + file.getName() + 
                ", language: " + file.getLanguage().getDisplayName() + 
                ", file type: " + file.getClass().getSimpleName());
        
        // 支持Java和Kotlin文件
        if (!(file instanceof PsiJavaFile) && !"Kotlin".equals(file.getLanguage().getDisplayName())) {
            LOG.info("[InlayHints] File not supported, skipping: " + file.getName());
            return null;
        }

        LOG.info("[InlayHints] *** CREATING COLLECTOR *** for file: " + file.getName());
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
                LOG.info("[InlayHints-Collect] *** COLLECT CALLED *** Processing element: " + element.getClass().getSimpleName() + 
                         ", text: '" + element.getText().substring(0, Math.min(50, element.getText().length())) + "...");
                
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
                
                // 跳过其他元素的处理，避免重复显示
                // 注释掉通用元素处理，因为Kotlin函数已经在上面处理了
                /*
                // 处理其他可能包含注解的元素（如LeafPsiElement等）
                String elementText = element.getText();
                if (elementText != null) {
                    for (String annotation : SPRING_MAPPING_ANNOTATIONS) {
                        if (elementText.contains("@" + annotation)) {
                            LOG.info("[InlayHints-Collect] Found element with annotation: " + annotation + ", element type: " + element.getClass().getSimpleName());
                            return processGenericElement(element, sink);
                        }
                    }
                }
                */
                
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
        
        private boolean isKotlinElement(PsiElement element) {
            String className = element.getClass().getName();
            return className.contains("Kt") || className.contains("kotlin");
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
                
                // 根据元素类型选择正确的URL构建方法
                String fullUrl;
                if (isKotlinElement(element)) {
                    fullUrl = buildFullUrlFromKotlin(element, path);
                } else {
                    fullUrl = buildFullUrlFromJava(element, path);
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
            LOG.info("[InlayHints-Kotlin] Processing Kotlin function: " + element.getClass().getName() + ", text: " + element.getText().substring(0, Math.min(100, element.getText().length())));
            
            try {
                // 尝试多种方法获取Kotlin函数的注解，只使用第一种成功的方法
                List<PsiElement> annotations = new ArrayList<>();
                boolean foundAnnotations = false;
                
                // 方法1: 尝试getAnnotationEntries
                if (!foundAnnotations) {
                    try {
                        Method getAnnotationEntriesMethod = element.getClass().getMethod("getAnnotationEntries");
                        Object annotationEntries = getAnnotationEntriesMethod.invoke(element);
                        if (annotationEntries instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<PsiElement> entries = (List<PsiElement>) annotationEntries;
                            if (!entries.isEmpty()) {
                                annotations.addAll(entries);
                                foundAnnotations = true;
                                LOG.info("[InlayHints-Kotlin] Found " + entries.size() + " annotations via getAnnotationEntries");
                            }
                        }
                    } catch (Exception e) {
                        LOG.info("[InlayHints-Kotlin] getAnnotationEntries failed: " + e.getMessage());
                    }
                }
                
                // 方法2: 尝试getModifierList
                if (!foundAnnotations) {
                    try {
                        Method getModifierListMethod = element.getClass().getMethod("getModifierList");
                        Object modifierList = getModifierListMethod.invoke(element);
                        if (modifierList != null) {
                            Method getAnnotationsMethod = modifierList.getClass().getMethod("getAnnotations");
                            Object annotationsArray = getAnnotationsMethod.invoke(modifierList);
                            if (annotationsArray instanceof PsiElement[]) {
                                PsiElement[] annotationArray = (PsiElement[]) annotationsArray;
                                if (annotationArray.length > 0) {
                                    for (PsiElement annotation : annotationArray) {
                                        annotations.add(annotation);
                                    }
                                    foundAnnotations = true;
                                    LOG.info("[InlayHints-Kotlin] Found " + annotationArray.length + " annotations via getModifierList");
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.info("[InlayHints-Kotlin] getModifierList failed: " + e.getMessage());
                    }
                }
                
                // 方法3: 直接查找子元素中的注解
                if (!foundAnnotations) {
                    PsiElement[] children = element.getChildren();
                    LOG.info("[InlayHints-Kotlin] Searching in " + children.length + " children for annotations");
                    for (PsiElement child : children) {
                        String childClassName = child.getClass().getName();
                        LOG.info("[InlayHints-Kotlin] Child class: " + childClassName + ", text: " + child.getText());
                        if (childClassName.contains("Annotation") || childClassName.contains("KtAnnotation")) {
                            annotations.add(child);
                            foundAnnotations = true;
                            LOG.info("[InlayHints-Kotlin] Found annotation child: " + childClassName);
                        }
                    }
                }
                
                LOG.info("[InlayHints-Kotlin] Total annotations found: " + annotations.size());
                
                // 处理找到的注解
                for (PsiElement annotation : annotations) {
                    String annotationText = annotation.getText();
                    String annotationName = extractKotlinAnnotationName(annotationText);
                    
                    LOG.info("[InlayHints-Kotlin] Processing annotation: " + annotationName + ", text: " + annotationText);
                    
                    if (!isSpringMappingAnnotation(annotationName)) {
                        LOG.info("[InlayHints-Kotlin] Skipping non-Spring annotation: " + annotationName);
                        continue;
                    }
                    
                    // 提取路径值
                    String path = extractKotlinAnnotationValue(annotationText);
                    LOG.info("[InlayHints-Kotlin] Extracted path: " + path);
                    
                    if (path == null || path.isEmpty()) {
                        LOG.info("[InlayHints-Kotlin] Path is null or empty, skipping");
                        continue;
                    }
                    
                    // 获取HTTP方法
                    String httpMethod = getHttpMethodFromAnnotation(annotationName);
                    LOG.info("[InlayHints-Kotlin] HTTP method: " + httpMethod);
                    
                    // 构建完整URL - 使用与LineMarker相同的逻辑
                    String fullUrl = buildFullUrlFromKotlin(element, path);
                    String displayText = httpMethod + " " + fullUrl;
                    
                    LOG.info("[InlayHints-Kotlin] Final display text: " + displayText);
                    
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
                    
                    LOG.info("[InlayHints-Kotlin] Successfully added inlay hint for: " + displayText);
                    
                    break; // 只处理第一个匹配的注解
                }
            } catch (Exception e) {
                LOG.error("[InlayHints-Kotlin] Error processing Kotlin function: " + e.getMessage(), e);
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
                LOG.info("[InlayHints-Kotlin] Searching for @RequestMapping on class: " + ktClass.getClass().getSimpleName());
                // 查找类上的@RequestMapping注解
                Object annotationEntries = ktClass.getClass().getMethod("getAnnotationEntries").invoke(ktClass);
                if (annotationEntries instanceof List) {
                    List<?> entries = (List<?>) annotationEntries;
                    LOG.info("[InlayHints-Kotlin] Found " + entries.size() + " annotations on class");
                    for (Object entry : entries) {
                        String annotationName = getKotlinAnnotationName(entry);
                        LOG.info("[InlayHints-Kotlin] Processing annotation: " + annotationName);
                        if ("RequestMapping".equals(annotationName)) {
                            // 使用与LineMarker相同的Kotlin注解路径提取方法
                            String path = extractPathFromKotlinAnnotation(entry);
                            LOG.info("[InlayHints-Kotlin] Extracted class path: " + path);
                            return path;
                        }
                    }
                }
                LOG.info("[InlayHints-Kotlin] No @RequestMapping found on class");
                return null;
            } catch (Exception e) {
                LOG.error("[InlayHints-Kotlin] Error finding class RequestMapping", e);
                return null;
            }
        }
        
        private String getKotlinAnnotationName(Object ktAnnotationEntry) {
            try {
                LOG.info("[InlayHints-Kotlin] Processing annotation entry: " + ktAnnotationEntry.getClass().getSimpleName());
                
                // 使用与LineMarker相同的方法：getShortName()
                Object shortName = ktAnnotationEntry.getClass().getMethod("getShortName").invoke(ktAnnotationEntry);
                if (shortName != null) {
                    String annotationName = shortName.toString();
                    LOG.info("[InlayHints-Kotlin] Extracted annotation name via getShortName: '" + annotationName + "'");
                    return annotationName;
                }
                
                LOG.info("[InlayHints-Kotlin] getShortName returned null");
                return "";
            } catch (Exception e) {
                LOG.error("[InlayHints-Kotlin] Error extracting annotation name", e);
                return "";
            }
        }
        
        private String buildFullUrlFromKotlin(PsiElement element, String path) {
            LOG.info("[InlayHints-Kotlin] buildFullUrlFromKotlin - Input element: " + element.getClass().getSimpleName() + ", path: " + path);
            
            try {
                // 查找类级别的@RequestMapping
                Object ktClass = findKotlinClass(element);
                String basePath = "";
                
                LOG.info("[InlayHints-Kotlin] Found Kotlin class: " + (ktClass != null ? ktClass.getClass().getSimpleName() : "null"));
                
                if (ktClass != null) {
                    String classPath = findKotlinClassRequestMapping(ktClass);
                    LOG.info("[InlayHints-Kotlin] Class level path: " + classPath);
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

                // 合并基础路径和方法路径
                String fullPath = basePath + path;
                
                // 清理重复的斜杠
                fullPath = fullPath.replaceAll("/+", "/");
                
                LOG.info("[InlayHints-Kotlin] Final path: basePath=" + basePath + ", methodPath=" + path + ", fullPath=" + fullPath);
                
                // 只返回相对路径，不包含host:port
                return fullPath;
            } catch (Exception e) {
                LOG.error("[InlayHints-Kotlin] Error in buildFullUrlFromKotlin", e);
                // 如果失败，返回简单路径
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                return path;
            }
        }
        
        private String buildFullUrlFromJava(PsiElement element, String path) {
            LOG.info("[InlayHints-Java] buildFullUrlFromJava - Input element: " + element.getClass().getSimpleName() + ", path: " + path);
            
            try {
                // 查找类级别的@RequestMapping
                PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                String basePath = "";
                
                LOG.info("[InlayHints-Java] Found containing class: " + (containingClass != null ? containingClass.getName() : "null"));
                
                if (containingClass != null) {
                    PsiAnnotation classMapping = containingClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
                    if (classMapping != null) {
                        String classPath = urlService.extractPathFromAnnotation(classMapping);
                        LOG.info("[InlayHints-Java] Class level path from annotation: " + classPath);
                        if (classPath != null && !classPath.isEmpty()) {
                            basePath = classPath;
                        }
                    } else {
                        LOG.info("[InlayHints-Java] No @RequestMapping found on class");
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
                
                LOG.info("[InlayHints-Java] Final path: basePath=" + basePath + ", methodPath=" + path + ", fullPath=" + fullPath);
                
                // 只返回相对路径，不包含host:port
                return fullPath;
            } catch (Exception e) {
                LOG.error("[InlayHints-Java] Error in buildFullUrlFromJava", e);
                // 如果失败，返回简单路径
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                return path;
            }
        }
        
        private String extractPathFromKotlinAnnotation(Object ktAnnotationEntry) {
            try {
                LOG.info("[InlayHints-Kotlin-Debug] ========== Extracting path from Kotlin annotation ===========");
                LOG.info("[InlayHints-Kotlin-Debug] KtAnnotationEntry class: " + ktAnnotationEntry.getClass().getName());

                // 使用反射获取Kotlin注解的值
                Object valueArguments = ktAnnotationEntry.getClass().getMethod("getValueArguments").invoke(ktAnnotationEntry);
                LOG.info("[InlayHints-Kotlin-Debug] ValueArguments type: " + (valueArguments != null ? valueArguments.getClass().getSimpleName() : "null"));

                if (valueArguments instanceof List) {
                    List<?> args = (List<?>) valueArguments;
                    LOG.info("[InlayHints-Kotlin-Debug] Number of value arguments: " + args.size());

                    for (int i = 0; i < args.size(); i++) {
                        Object arg = args.get(i);
                        LOG.info("[InlayHints-Kotlin-Debug] Processing argument " + i + ": " + arg.getClass().getSimpleName());

                        Object argumentExpression = arg.getClass().getMethod("getArgumentExpression").invoke(arg);
                        if (argumentExpression != null) {
                            LOG.info("[InlayHints-Kotlin-Debug] Argument expression type: " + argumentExpression.getClass().getSimpleName());
                            LOG.info("[InlayHints-Kotlin-Debug] Argument expression text: " + argumentExpression.toString());

                            // 尝试解析Kotlin表达式
                            LOG.info("[InlayHints-Kotlin-Debug] Calling evaluateKotlinExpression...");
                            String result = evaluateKotlinExpression(argumentExpression);
                            LOG.info("[InlayHints-Kotlin-Debug] evaluateKotlinExpression result: '" + result + "'");

                            if (result != null) {
                                LOG.info("[InlayHints-Kotlin-Debug] Returning extracted path: '" + result + "'");
                                return result;
                            }

                            // 回退到简单的字符串提取
                            String text = argumentExpression.toString();
                            if (text.startsWith("\"") && text.endsWith("\"")) {
                                String literalResult = text.substring(1, text.length() - 1);
                                LOG.info("[InlayHints-Kotlin-Debug] Literal string result: " + literalResult);
                                return literalResult;
                            }
                        }
                    }
                }
                LOG.warn("[InlayHints-Kotlin-Debug] No valid path found in Kotlin annotation");
                return null;
            } catch (Exception e) {
                LOG.warn("[InlayHints-Kotlin-Debug] Error extracting path from Kotlin annotation", e);
                return null;
            }
        }
        
        private String extractPathFromKotlinAnnotationImproved(Object ktAnnotationEntry) {
            try {
                LOG.info("[InlayHints-Kotlin-Debug] ========== Extracting path from Kotlin annotation (Improved) ===========");
                LOG.info("[InlayHints-Kotlin-Debug] KtAnnotationEntry class: " + ktAnnotationEntry.getClass().getName());

                // 使用反射获取Kotlin注解的值
                Object valueArguments = ktAnnotationEntry.getClass().getMethod("getValueArguments").invoke(ktAnnotationEntry);
                LOG.info("[InlayHints-Kotlin-Debug] ValueArguments type: " + (valueArguments != null ? valueArguments.getClass().getSimpleName() : "null"));

                if (valueArguments instanceof List) {
                    List<?> args = (List<?>) valueArguments;
                    LOG.info("[InlayHints-Kotlin-Debug] Number of value arguments: " + args.size());

                    for (int i = 0; i < args.size(); i++) {
                        Object arg = args.get(i);
                        LOG.info("[InlayHints-Kotlin-Debug] Processing argument " + i + ": " + arg.getClass().getSimpleName());

                        Object argumentExpression = arg.getClass().getMethod("getArgumentExpression").invoke(arg);
                        if (argumentExpression != null) {
                            LOG.info("[InlayHints-Kotlin-Debug] Argument expression type: " + argumentExpression.getClass().getSimpleName());
                            LOG.info("[InlayHints-Kotlin-Debug] Argument expression text: " + argumentExpression.toString());

                            String className = argumentExpression.getClass().getSimpleName();
                            
                            // 处理KtStringTemplateExpression
                            if (className.equals("KtStringTemplateExpression")) {
                                LOG.info("[InlayHints-Kotlin-Debug] Found KtStringTemplateExpression! Processing...");
                                String result = extractFromKtStringTemplateExpression(argumentExpression);
                                if (result != null) {
                                    LOG.info("[InlayHints-Kotlin-Debug] KtStringTemplateExpression result: " + result);
                                    return result;
                                }
                            }
                            // 处理普通字符串字面量
                            else {
                                String text = argumentExpression.toString();
                                LOG.info("[InlayHints-Kotlin-Debug] Processing as string literal: " + text);
                                // 简单的字符串提取，去掉引号
                                if (text.startsWith("\"") && text.endsWith("\"")) {
                                    String result = text.substring(1, text.length() - 1);
                                    LOG.info("[InlayHints-Kotlin-Debug] String literal result: " + result);
                                    return result;
                                }
                            }
                        }
                    }
                }
                LOG.info("[InlayHints-Kotlin-Debug] No valid path found in annotation");
                return null;
            } catch (Exception e) {
                LOG.error("[InlayHints-Kotlin-Debug] Error extracting path from Kotlin annotation", e);
                return null;
            }
        }
        
        private String evaluateKotlinExpression(Object argumentExpression) {
            try {
                String className = argumentExpression.getClass().getSimpleName();
                LOG.info("[InlayHints-Kotlin-Debug] Evaluating Kotlin expression of type: " + className);

                // 处理Kotlin字符串字面量
                if ("KtStringTemplateExpression".equals(className)) {
                    try {
                        // 使用反射获取KtStringTemplateExpression的内容
                        Object[] entries = (Object[]) argumentExpression.getClass().getMethod("getEntries").invoke(argumentExpression);
                        if (entries != null && entries.length > 0) {
                            StringBuilder result = new StringBuilder();
                            for (Object entry : entries) {
                                String entryClassName = entry.getClass().getSimpleName();
                                if ("KtLiteralStringTemplateEntry".equals(entryClassName)) {
                                    String text = (String) entry.getClass().getMethod("getText").invoke(entry);
                                    result.append(text);
                                }
                            }
                            String finalResult = result.toString();
                            LOG.info("[InlayHints-Kotlin-Debug] KtStringTemplateExpression result: '" + finalResult + "'");
                            return finalResult;
                        }
                    } catch (Exception e) {
                        LOG.warn("[InlayHints-Kotlin-Debug] Error processing KtStringTemplateExpression", e);
                    }
                    
                    // 回退方法：直接从文本中提取
                    String text = argumentExpression.toString();
                    LOG.info("[InlayHints-Kotlin-Debug] KtStringTemplateExpression text: '" + text + "'");
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        String result = text.substring(1, text.length() - 1);
                        LOG.info("[InlayHints-Kotlin-Debug] Extracted from quotes: '" + result + "'");
                        return result;
                    }
                }

                // 处理Kotlin二元表达式（如字符串连接）
                if ("KtBinaryExpression".equals(className)) {
                    return evaluateKotlinBinaryExpression(argumentExpression);
                }

                // 处理Kotlin引用表达式（如常量引用）
                if ("KtNameReferenceExpression".equals(className) || "KtDotQualifiedExpression".equals(className)) {
                    return evaluateKotlinReference(argumentExpression);
                }

                LOG.warn("[InlayHints-Kotlin-Debug] Unhandled Kotlin expression type: " + className);
                return null;
            } catch (Exception e) {
                LOG.warn("[InlayHints-Kotlin-Debug] Error evaluating Kotlin expression", e);
                return null;
            }
        }

        private String evaluateKotlinBinaryExpression(Object binaryExpr) {
            try {
                // 使用反射获取左右操作数
                Object left = binaryExpr.getClass().getMethod("getLeft").invoke(binaryExpr);
                Object right = binaryExpr.getClass().getMethod("getRight").invoke(binaryExpr);

                if (left != null && right != null) {
                    String leftValue = evaluateKotlinExpression(left);
                    String rightValue = evaluateKotlinExpression(right);

                    if (leftValue != null && rightValue != null) {
                        LOG.info("[InlayHints-Kotlin-Debug] Binary expression: " + leftValue + " + " + rightValue);
                        return leftValue + rightValue;
                    }
                }
            } catch (Exception e) {
                LOG.warn("[InlayHints-Kotlin-Debug] Error evaluating Kotlin binary expression", e);
            }
            return null;
        }

        private String evaluateKotlinReference(Object refExpr) {
            try {
                // 尝试解析Kotlin常量引用
                if (refExpr instanceof PsiElement) {
                    PsiElement psiElement = (PsiElement) refExpr;
                    // 使用PSI解析引用
                    PsiReference[] references = psiElement.getReferences();
                    for (PsiReference ref : references) {
                        PsiElement resolved = ref.resolve();
                        if (resolved instanceof PsiField) {
                            PsiField field = (PsiField) resolved;
                            if (field.hasModifierProperty(PsiModifier.STATIC) &&
                                field.hasModifierProperty(PsiModifier.FINAL)) {
                                PsiExpression initializer = field.getInitializer();
                                if (initializer instanceof PsiLiteralExpression) {
                                    Object value = ((PsiLiteralExpression) initializer).getValue();
                                    if (value != null) {
                                        LOG.info("[InlayHints-Kotlin-Debug] Resolved Kotlin constant: " + value);
                                        return value.toString();
                                    }
                                }
                            }
                        }
                    }
                }

                LOG.warn("[InlayHints-Kotlin-Debug] Could not resolve Kotlin reference: " + refExpr.toString());
            } catch (Exception e) {
                LOG.warn("[InlayHints-Kotlin-Debug] Error evaluating Kotlin reference", e);
            }
            return null;
        }

        private String extractFromKtStringTemplateExpression(Object ktStringTemplateExpression) {
            try {
                LOG.info("[InlayHints-Kotlin-Debug] Extracting from KtStringTemplateExpression...");
                
                // 获取模板条目
                Object entries = ktStringTemplateExpression.getClass().getMethod("getEntries").invoke(ktStringTemplateExpression);
                if (entries instanceof Object[]) {
                    Object[] entryArray = (Object[]) entries;
                    LOG.info("[InlayHints-Kotlin-Debug] Found " + entryArray.length + " template entries");
                    
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < entryArray.length; i++) {
                        Object entry = entryArray[i];
                        String entryClassName = entry.getClass().getSimpleName();
                        LOG.info("[InlayHints-Kotlin-Debug] Processing entry " + i + ": " + entryClassName);
                        
                        if (entryClassName.equals("KtLiteralStringTemplateEntry")) {
                            // 使用反射获取文本内容
                            try {
                                Method getTextMethod = entry.getClass().getMethod("getText");
                                String text = (String) getTextMethod.invoke(entry);
                                LOG.info("[InlayHints-Kotlin-Debug] KtLiteralStringTemplateEntry text: " + text);
                                result.append(text);
                            } catch (Exception e) {
                                LOG.warn("[InlayHints-Kotlin-Debug] Failed to get text from KtLiteralStringTemplateEntry", e);
                                // 回退方法：直接使用toString
                                String fallbackText = entry.toString();
                                LOG.info("[InlayHints-Kotlin-Debug] Fallback text: " + fallbackText);
                                result.append(fallbackText);
                            }
                        } else {
                            LOG.info("[InlayHints-Kotlin-Debug] Unhandled entry type: " + entryClassName);
                            // 对于其他类型的条目，使用toString作为回退
                            result.append(entry.toString());
                        }
                    }
                    
                    String finalResult = result.toString();
                    LOG.info("[InlayHints-Kotlin-Debug] Final KtStringTemplateExpression result: " + finalResult);
                    return finalResult;
                }
                
                LOG.info("[InlayHints-Kotlin-Debug] No entries found in KtStringTemplateExpression");
                return null;
            } catch (Exception e) {
                LOG.error("[InlayHints-Kotlin-Debug] Error processing KtStringTemplateExpression", e);
                return null;
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
                
                // 构建完整URL - 使用与Kotlin相同的逻辑
                String fullUrl = buildFullUrlFromJava(element, path);
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