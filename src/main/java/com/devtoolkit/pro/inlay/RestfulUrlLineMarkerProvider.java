package com.devtoolkit.pro.inlay;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Collection;
import java.util.List;

public class RestfulUrlLineMarkerProvider implements LineMarkerProvider {
    private static final Logger LOG = Logger.getInstance(RestfulUrlLineMarkerProvider.class);
    private static final Icon PLUGIN_ICON = IconLoader.getIcon("/icons/smallPluginIcon.svg", RestfulUrlLineMarkerProvider.class);
    private static final Icon COPY_URL_ICON = IconLoader.getIcon("/icons/copy_url.svg", RestfulUrlLineMarkerProvider.class);
    private static final Icon COPY_MARKDOWN_ICON = IconLoader.getIcon("/icons/copy_markdown.svg", RestfulUrlLineMarkerProvider.class);
    private static final Icon COPY_CURL_ICON = IconLoader.getIcon("/icons/copy_curl.svg", RestfulUrlLineMarkerProvider.class);

    private static final String[] SPRING_MAPPING_ANNOTATIONS = {
        "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
        "PatchMapping", "RequestMapping"
    };



    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        try {
            // 只记录Kotlin文件的处理
            PsiFile file = element.getContainingFile();
            if (file != null && "Kotlin".equals(file.getLanguage().getDisplayName())) {
                LOG.warn("[LineMarker-DEBUG] Processing Kotlin element: " + element.getClass().getSimpleName() +
                        ", text: '" + element.getText() + "', file: " + file.getName() +
                        ", hashCode: " + element.hashCode() + ", textRange: " + element.getTextRange());
            }

            // 处理Java注解的标识符（叶子元素）
            if (element instanceof PsiIdentifier) {
                LOG.info("[LineMarker] Detected Java PsiIdentifier, delegating to handleJavaAnnotation");
                return handleJavaAnnotation(element);
            }

            // 处理Kotlin注解 - 检查是否是Kotlin注解的标识符
            if (isKotlinAnnotationIdentifier(element)) {
                LOG.warn("[LineMarker-Debug] Detected Kotlin annotation identifier, delegating to handleKotlinAnnotation, hashCode: " + element.hashCode());
                LineMarkerInfo<?> result = handleKotlinAnnotation(element);
                if (result != null) {
                    LOG.warn("[LineMarker-Debug] Created LineMarkerInfo for element: " + element.getText() + ", hashCode: " + element.hashCode());
                }
                return result;
            }

            LOG.debug("[LineMarker] Element not processed: " + element.getClass().getSimpleName());
            return null;
        } catch (com.intellij.openapi.progress.ProcessCanceledException e) {
            // ProcessCanceledException is a control-flow exception and should be rethrown, not logged
            throw e;
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
            PLUGIN_ICON, // 使用插件自定义图标
            psiElement -> "Copy RESTful URL: " + fullUrl,
            new GutterIconNavigationHandler(fullUrl, element),
            GutterIconRenderer.Alignment.RIGHT,
            () -> fullUrl // 直接显示URL文本
        );
    }

    private @Nullable LineMarkerInfo<?> handleKotlinAnnotation(@NotNull PsiElement element) {
        try {
            LOG.info("[LineMarker-Kotlin-Debug] ========== Processing Kotlin annotation ===========");
            LOG.info("[LineMarker-Kotlin-Debug] Element text: " + element.getText());
            LOG.info("[LineMarker-Kotlin-Debug] Element class: " + element.getClass().getSimpleName());
            LOG.info("[LineMarker-Kotlin-Debug] Element parent: " + (element.getParent() != null ? element.getParent().getClass().getSimpleName() : "null"));

            // 使用反射来处理Kotlin PSI元素，避免直接依赖Kotlin类
            Object ktAnnotationEntry = findKotlinAnnotationEntry(element);
            if (ktAnnotationEntry == null) {
                LOG.warn("[LineMarker-Kotlin-Debug] No KtAnnotationEntry found for element");
                return null;
            }

            LOG.info("[LineMarker-Kotlin-Debug] Found KtAnnotationEntry: " + ktAnnotationEntry.getClass().getSimpleName());
            LOG.info("[LineMarker-Kotlin-Debug] KtAnnotationEntry text: " + ktAnnotationEntry.toString());

            String annotationName = getKotlinAnnotationName(ktAnnotationEntry);
            LOG.info("[LineMarker-Kotlin-Debug] Kotlin annotation name: " + annotationName);

            if (!isSpringMappingAnnotation(annotationName)) {
                LOG.debug("[LineMarker-Kotlin-Debug] Not a Spring mapping annotation, skipping");
                return null;
            }

            // 检查注解是否在方法上，而不是在类上
            if (!isAnnotationOnMethod(ktAnnotationEntry)) {
                LOG.info("[LineMarker-Kotlin-Debug] Annotation is on class level, skipping");
                return null;
            }

            LOG.info("[LineMarker-Kotlin-Debug] ========== Extracting path from annotation ===========");
            String path = extractPathFromKotlinAnnotation(ktAnnotationEntry);
            LOG.info("[LineMarker-Kotlin-Debug] Extracted method-level path: '" + path + "'");

            if (path == null || path.isEmpty()) {
                LOG.warn("[LineMarker-Kotlin-Debug] Path is null or empty, skipping");
                return null;
            }

            LOG.info("[LineMarker-Kotlin-Debug] ========== Building full URL ===========");
            LOG.info("[LineMarker-Kotlin-Debug] Input path to buildFullUrlFromKotlin: '" + path + "'");
            String fullUrl = buildFullUrlFromKotlin(ktAnnotationEntry, path);
            LOG.info("[LineMarker-Kotlin-Debug] Final full URL result: '" + fullUrl + "'");
            LOG.info("[LineMarker-Kotlin-Debug] ========== Creating line marker ===========");

            return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                PLUGIN_ICON, // 使用插件自定义图标
                psiElement -> "Copy RESTful URL: " + fullUrl,
                new GutterIconNavigationHandler(fullUrl, element),
                GutterIconRenderer.Alignment.RIGHT,
                () -> fullUrl // 直接显示URL文本
            );
        } catch (com.intellij.openapi.progress.ProcessCanceledException e) {
            // ProcessCanceledException is a control-flow exception and should be rethrown, not logged
            throw e;
        } catch (Exception e) {
            LOG.error("[LineMarker-Kotlin-Debug] Error processing Kotlin annotation", e);
            return null;
        }
    }

    private boolean isKotlinAnnotationIdentifier(@NotNull PsiElement element) {
        try {
            // 检查是否是Kotlin文件中的标识符
            PsiFile file = element.getContainingFile();
            if (file == null || !"Kotlin".equals(file.getLanguage().getDisplayName())) {
                return false;
            }

            String elementClassName = element.getClass().getSimpleName();
            String elementText = element.getText();

            // 只处理注解相关的元素类型，避免重复处理
            boolean shouldProcess = "KtNameReferenceExpression".equals(elementClassName) ||
                                  "KtConstructorCalleeExpression".equals(elementClassName) ||
                                  ("LeafPsiElement".equals(elementClassName) && element.getNode() != null &&
                                   "IDENTIFIER".equals(element.getNode().getElementType().toString()));

            if (shouldProcess) {
                LOG.warn("[LineMarker-DEBUG] Processing Kotlin element: " + elementClassName + ", text: '" + elementText + "', file: " + file.getName());

                // 只处理KtConstructorCalleeExpression类型的元素，这是注解名称的直接容器
                if ("KtConstructorCalleeExpression".equals(elementClassName)) {
                    // 直接检查元素文本是否是Spring注解名
                    if (isSpringMappingAnnotation(elementText)) {
                        LOG.warn("[LineMarker-Kotlin] Found Spring mapping annotation identifier: " + elementText);
                        return true;
                    }
                }

                // 移除了对其他元素类型的处理，避免重复显示
            }

            return false;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin] Exception in isKotlinAnnotationIdentifier: " + e.getMessage());
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

    private PsiElement findParentOfType(@NotNull PsiElement element, String typeName) {
        try {
            PsiElement current = element.getParent();
            while (current != null) {
                String className = current.getClass().getSimpleName();
                if (typeName.equals(className)) {
                    return current;
                }
                current = current.getParent();
            }
            return null;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin] Exception in findParentOfType: " + e.getMessage());
            return null;
        }
    }

    private Object[] getKotlinAnnotations(@NotNull PsiElement element) {
        try {
            // 使用反射获取Kotlin元素的注解
            Object annotationEntries = element.getClass().getMethod("getAnnotationEntries").invoke(element);
            if (annotationEntries instanceof List) {
                List<?> entries = (List<?>) annotationEntries;
                return entries.toArray();
            }
            return null;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin] Exception in getKotlinAnnotations: " + e.getMessage());
            return null;
        }
    }

    private boolean isAnnotationOnMethod(Object ktAnnotationEntry) {
        try {
            // 获取注解的父元素
            PsiElement annotationElement = (PsiElement) ktAnnotationEntry;
            PsiElement parent = annotationElement.getParent();

            while (parent != null) {
                String parentClassName = parent.getClass().getSimpleName();
                LOG.debug("[LineMarker-Kotlin-Debug] Checking parent: " + parentClassName);

                // 检查是否是方法
                if ("KtNamedFunction".equals(parentClassName) || "KtFunction".equals(parentClassName)) {
                    LOG.info("[LineMarker-Kotlin-Debug] Found method parent: " + parentClassName);
                    return true;
                }

                // 检查是否是类
                if ("KtClass".equals(parentClassName)) {
                    LOG.info("[LineMarker-Kotlin-Debug] Found class parent: " + parentClassName);
                    return false;
                }

                parent = parent.getParent();
            }

            return false;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin] Exception in isAnnotationOnMethod: " + e.getMessage());
            return false;
        }
    }

    private Object findKotlinAnnotationEntry(@NotNull PsiElement element) {
        try {
            // 首先检查当前元素
            String className = element.getClass().getSimpleName();
            LOG.warn("[LineMarker-Kotlin] Checking element class: " + className);

            if ("KtAnnotationEntry".equals(className)) {
                LOG.warn("[LineMarker-Kotlin] Found KtAnnotationEntry directly");
                return element;
            }

            // 如果当前元素是注解标识符，需要查找包含它的KtFunction或KtClass的注解
            if ("KtNameReferenceExpression".equals(className) ||
                ("LeafPsiElement".equals(className) && element.getNode() != null &&
                 "IDENTIFIER".equals(element.getNode().getElementType().toString()))) {

                String text = element.getText();
                LOG.warn("[LineMarker-Kotlin] Processing identifier: " + text);

                // 查找包含此元素的KtFunction
                PsiElement ktFunction = findParentOfType(element, "KtFunction");
                if (ktFunction == null) {
                    ktFunction = findParentOfType(element, "KtNamedFunction");
                }
                if (ktFunction != null) {
                    LOG.warn("[LineMarker-Kotlin] Found KtFunction/KtNamedFunction parent, searching for annotations");
                    Object[] annotations = getKotlinAnnotations(ktFunction);
                    if (annotations != null && annotations.length > 0) {
                        for (Object annotation : annotations) {
                            String annotationName = getKotlinAnnotationName(annotation);
                            LOG.warn("[LineMarker-Kotlin] Function annotation: " + annotationName);
                            if (annotationName != null && isSpringMappingAnnotation(annotationName)) {
                                LOG.warn("[LineMarker-Kotlin] Found matching Spring annotation: " + annotationName);
                                return annotation;
                            }
                        }
                    }
                }

                // 查找包含此元素的KtClass
                PsiElement ktClass = findParentOfType(element, "KtClass");
                if (ktClass != null) {
                    LOG.warn("[LineMarker-Kotlin] Found KtClass parent, searching for annotations");
                    Object[] annotations = getKotlinAnnotations(ktClass);
                    if (annotations != null && annotations.length > 0) {
                        for (Object annotation : annotations) {
                            String annotationName = getKotlinAnnotationName(annotation);
                            LOG.warn("[LineMarker-Kotlin] Class annotation: " + annotationName);
                            if (annotationName != null && isSpringMappingAnnotation(annotationName)) {
                                LOG.warn("[LineMarker-Kotlin] Found matching Spring annotation: " + annotationName);
                                return annotation;
                            }
                        }
                    }
                }
            }

            // 检查父元素
            PsiElement current = element.getParent();
            while (current != null) {
                className = current.getClass().getSimpleName();
                LOG.warn("[LineMarker-Kotlin] Checking parent class: " + className);
                if ("KtAnnotationEntry".equals(className)) {
                    LOG.warn("[LineMarker-Kotlin] Found KtAnnotationEntry in parent");
                    return current;
                }
                current = current.getParent();
            }

            // 检查子元素
            PsiElement[] children = element.getChildren();
            for (PsiElement child : children) {
                className = child.getClass().getSimpleName();
                LOG.warn("[LineMarker-Kotlin] Checking child class: " + className);
                if ("KtAnnotationEntry".equals(className)) {
                    LOG.warn("[LineMarker-Kotlin] Found KtAnnotationEntry in child");
                    return child;
                }
                // 递归检查子元素的子元素
                Object result = findKotlinAnnotationEntryRecursive(child);
                if (result != null) {
                    return result;
                }
            }

            LOG.warn("[LineMarker-Kotlin] No KtAnnotationEntry found");
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



    private String extractPathFromKotlinAnnotation(Object ktAnnotationEntry) {
        try {
            LOG.info("[LineMarker-Kotlin-Debug] ========== Extracting path from Kotlin annotation ===========");
            LOG.info("[LineMarker-Kotlin-Debug] KtAnnotationEntry class: " + ktAnnotationEntry.getClass().getName());

            // 使用反射获取Kotlin注解的值
            Object valueArguments = ktAnnotationEntry.getClass().getMethod("getValueArguments").invoke(ktAnnotationEntry);
            LOG.info("[LineMarker-Kotlin-Debug] ValueArguments type: " + (valueArguments != null ? valueArguments.getClass().getSimpleName() : "null"));

            if (valueArguments instanceof List) {
                List<?> args = (List<?>) valueArguments;
                LOG.info("[LineMarker-Kotlin-Debug] Number of value arguments: " + args.size());

                for (int i = 0; i < args.size(); i++) {
                    Object arg = args.get(i);
                    LOG.info("[LineMarker-Kotlin-Debug] Processing argument " + i + ": " + arg.getClass().getSimpleName());

                    Object argumentExpression = arg.getClass().getMethod("getArgumentExpression").invoke(arg);
                    if (argumentExpression != null) {
                        LOG.info("[LineMarker-Kotlin-Debug] Argument expression type: " + argumentExpression.getClass().getSimpleName());
                        LOG.info("[LineMarker-Kotlin-Debug] Argument expression text: " + argumentExpression.toString());

                        // 尝试解析Kotlin表达式
                        LOG.info("[LineMarker-Kotlin-Debug] Calling evaluateKotlinExpression...");
                        String result = evaluateKotlinExpression(argumentExpression);
                        LOG.info("[LineMarker-Kotlin-Debug] evaluateKotlinExpression result: '" + result + "'");

                        if (result != null) {
                            LOG.info("[LineMarker-Kotlin-Debug] Returning extracted path: '" + result + "'");
                            return result;
                        }

                        // 回退到简单的字符串提取
                        String text = argumentExpression.toString();
                        if (text.startsWith("\"") && text.endsWith("\"")) {
                            String literalResult = text.substring(1, text.length() - 1);
                            LOG.info("[LineMarker-Kotlin-Debug] Literal string result: " + literalResult);
                            return literalResult;
                        }
                    }
                }
            }
            LOG.warn("[LineMarker-Kotlin-Debug] No valid path found in Kotlin annotation");
            return null;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin-Debug] Error extracting path from Kotlin annotation", e);
            return null;
        }
    }

    private String evaluateKotlinExpression(Object argumentExpression) {
        try {
            String className = argumentExpression.getClass().getSimpleName();
            LOG.info("[LineMarker-Kotlin-Debug] Evaluating Kotlin expression of type: " + className);

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
                        LOG.info("[LineMarker-Kotlin-Debug] KtStringTemplateExpression result: '" + finalResult + "'");
                        return finalResult;
                    }
                } catch (Exception e) {
                    LOG.warn("[LineMarker-Kotlin-Debug] Error processing KtStringTemplateExpression", e);
                }

                // 回退方法：直接从文本中提取
                String text = argumentExpression.toString();
                LOG.info("[LineMarker-Kotlin-Debug] KtStringTemplateExpression text: '" + text + "'");
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    String result = text.substring(1, text.length() - 1);
                    LOG.info("[LineMarker-Kotlin-Debug] Extracted from quotes: '" + result + "'");
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

            LOG.warn("[LineMarker-Kotlin-Debug] Unhandled Kotlin expression type: " + className);
            return null;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin-Debug] Error evaluating Kotlin expression", e);
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
                    LOG.info("[LineMarker-Kotlin-Debug] Binary expression: " + leftValue + " + " + rightValue);
                    return leftValue + rightValue;
                }
            }
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin-Debug] Error evaluating Kotlin binary expression", e);
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
                                    LOG.info("[LineMarker-Kotlin-Debug] Resolved Kotlin constant: " + value);
                                    return value.toString();
                                }
                            }
                        }
                    }
                }
            }

            LOG.warn("[LineMarker-Kotlin-Debug] Could not resolve Kotlin reference: " + refExpr.toString());
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin-Debug] Error evaluating Kotlin reference", e);
        }
        return null;
    }

    private String buildFullUrlFromKotlin(Object ktAnnotationEntry, String path) {
        try {
            LOG.info("[LineMarker-Kotlin-Debug] Building full URL from Kotlin annotation");
            LOG.info("[LineMarker-Kotlin-Debug] Method path: " + path);

            // 查找类级别的@RequestMapping
            PsiElement element = (PsiElement) ktAnnotationEntry;
            Object ktClass = findKotlinClass(element);
            String basePath = "";

            LOG.info("[LineMarker-Kotlin-Debug] Found Kotlin class: " + (ktClass != null ? ktClass.getClass().getSimpleName() : "null"));

            if (ktClass != null) {
                String classPath = findKotlinClassRequestMapping(ktClass);
                LOG.info("[LineMarker-Kotlin-Debug] Class-level path from findKotlinClassRequestMapping: " + classPath);
                if (classPath != null && !classPath.isEmpty()) {
                    basePath = classPath;
                }
            }

            LOG.info("[LineMarker-Kotlin-Debug] Base path (class-level): " + basePath);
            LOG.info("[LineMarker-Kotlin-Debug] Method path (before processing): " + path);

            // 确保路径以/开头
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (!basePath.isEmpty() && !basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }

            LOG.info("[LineMarker-Kotlin-Debug] Base path (after processing): " + basePath);
            LOG.info("[LineMarker-Kotlin-Debug] Method path (after processing): " + path);

            // 合并基础路径和方法路径
            String fullPath = basePath + path;
            LOG.info("[LineMarker-Kotlin-Debug] Combined path (before cleanup): " + fullPath);

            // 清理重复的斜杠
            fullPath = fullPath.replaceAll("/+", "/");
            LOG.info("[LineMarker-Kotlin-Debug] Final path (after cleanup): " + fullPath);

            // 返回带有服务器前缀的完整URL
            String finalUrl = "http://localhost:8080" + fullPath;
            LOG.info("[LineMarker-Kotlin-Debug] Final URL: " + finalUrl);
            return finalUrl;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin-Debug] Error building full URL from Kotlin", e);
            // 如果失败，返回简单路径
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
            LOG.info("[LineMarker-Kotlin-Debug] ========== Searching for class-level @RequestMapping annotation ===========");
            LOG.info("[LineMarker-Kotlin-Debug] KtClass type: " + ktClass.getClass().getName());
            LOG.info("[LineMarker-Kotlin-Debug] KtClass text: " + ktClass.toString());

            // 查找类上的@RequestMapping注解
            Object annotationEntries = ktClass.getClass().getMethod("getAnnotationEntries").invoke(ktClass);
            LOG.info("[LineMarker-Kotlin-Debug] AnnotationEntries type: " + (annotationEntries != null ? annotationEntries.getClass().getSimpleName() : "null"));

            if (annotationEntries instanceof List) {
                List<?> entries = (List<?>) annotationEntries;
                LOG.info("[LineMarker-Kotlin-Debug] Number of class annotations: " + entries.size());

                for (int i = 0; i < entries.size(); i++) {
                    Object entry = entries.get(i);
                    LOG.info("[LineMarker-Kotlin-Debug] Processing class annotation " + i + ": " + entry.getClass().getSimpleName());
                    LOG.info("[LineMarker-Kotlin-Debug] Annotation entry text: " + entry.toString());

                    String annotationName = getKotlinAnnotationName(entry);
                    LOG.info("[LineMarker-Kotlin-Debug] Found class annotation name: '" + annotationName + "'");

                    if ("RequestMapping".equals(annotationName)) {
                        LOG.info("[LineMarker-Kotlin-Debug] Found @RequestMapping annotation! Extracting path...");
                        String classPath = extractPathFromKotlinAnnotation(entry);
                        LOG.info("[LineMarker-Kotlin-Debug] Class-level RequestMapping path extracted: '" + classPath + "'");
                        return classPath;
                    }
                }
            } else {
                LOG.info("[LineMarker-Kotlin-Debug] AnnotationEntries is not a List or is null");
            }

            LOG.info("[LineMarker-Kotlin-Debug] No class-level @RequestMapping found");
            return null;
        } catch (Exception e) {
            LOG.warn("[LineMarker-Kotlin-Debug] Error finding class RequestMapping", e);
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

        LOG.info("[LineMarker-Debug] Annotation value type: " + (value != null ? value.getClass().getSimpleName() : "null"));
        LOG.info("[LineMarker-Debug] Annotation value text: " + (value != null ? value.getText() : "null"));

        if (value instanceof PsiLiteralExpression) {
            Object literalValue = ((PsiLiteralExpression) value).getValue();
            LOG.info("[LineMarker-Debug] Literal value: " + literalValue);
            return literalValue != null ? literalValue.toString() : null;
        }

        // 处理字符串连接表达式，如 API.API_V1_PREFIX + "/fetch"
        if (value instanceof PsiBinaryExpression) {
            LOG.info("[LineMarker-Debug] Processing binary expression: " + value.getText());
            String result = evaluateBinaryExpression((PsiBinaryExpression) value);
            LOG.info("[LineMarker-Debug] Binary expression result: " + result);
            return result;
        }

        // 处理单个常量引用，如 API.API_V1_PREFIX
        if (value instanceof PsiReferenceExpression) {
            LOG.info("[LineMarker-Debug] Processing reference expression: " + value.getText());
            String result = evaluateConstantReference((PsiReferenceExpression) value);
            LOG.info("[LineMarker-Debug] Reference expression result: " + result);
            return result;
        }

        LOG.warn("[LineMarker-Debug] Unhandled annotation value type: " + (value != null ? value.getClass().getName() : "null"));
        return null;
    }

    private String evaluateBinaryExpression(PsiBinaryExpression binaryExpr) {
        try {
            PsiExpression left = binaryExpr.getLOperand();
            PsiExpression right = binaryExpr.getROperand();

            if (right == null) return null;

            String leftValue = evaluateExpression(left);
            String rightValue = evaluateExpression(right);

            if (leftValue != null && rightValue != null) {
                return leftValue + rightValue;
            }
        } catch (Exception e) {
            LOG.warn("Failed to evaluate binary expression: " + binaryExpr.getText(), e);
        }
        return null;
    }

    private String evaluateExpression(PsiExpression expr) {
        if (expr instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteralExpression) expr).getValue();
            return value != null ? value.toString() : null;
        }

        if (expr instanceof PsiReferenceExpression) {
            return evaluateConstantReference((PsiReferenceExpression) expr);
        }

        if (expr instanceof PsiBinaryExpression) {
            return evaluateBinaryExpression((PsiBinaryExpression) expr);
        }

        return null;
    }

    private String evaluateConstantReference(PsiReferenceExpression refExpr) {
        try {
            LOG.info("[LineMarker-Debug] Resolving reference: " + refExpr.getText());
            PsiElement resolved = refExpr.resolve();
            LOG.info("[LineMarker-Debug] Resolved element: " + (resolved != null ? resolved.getClass().getSimpleName() : "null"));

            if (resolved instanceof PsiField) {
                PsiField field = (PsiField) resolved;
                LOG.info("[LineMarker-Debug] Field name: " + field.getName());
                LOG.info("[LineMarker-Debug] Field modifiers - static: " + field.hasModifierProperty(PsiModifier.STATIC) +
                        ", final: " + field.hasModifierProperty(PsiModifier.FINAL));

                // 检查是否是静态final字段（常量）
                if (field.hasModifierProperty(PsiModifier.STATIC) &&
                    field.hasModifierProperty(PsiModifier.FINAL)) {

                    PsiExpression initializer = field.getInitializer();
                    LOG.info("[LineMarker-Debug] Field initializer: " + (initializer != null ? initializer.getText() : "null"));

                    if (initializer instanceof PsiLiteralExpression) {
                        Object value = ((PsiLiteralExpression) initializer).getValue();
                        LOG.info("[LineMarker-Debug] Field literal value: " + value);
                        return value != null ? value.toString() : null;
                    } else {
                        LOG.warn("[LineMarker-Debug] Field initializer is not a literal expression: " +
                                (initializer != null ? initializer.getClass().getSimpleName() : "null"));
                    }
                } else {
                    LOG.warn("[LineMarker-Debug] Field is not static final constant");
                }
            } else {
                LOG.warn("[LineMarker-Debug] Resolved element is not a field");
            }
        } catch (Exception e) {
            LOG.warn("Failed to evaluate constant reference: " + refExpr.getText(), e);
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

        // 合并基础路径和方法路径
        String fullPath = basePath + path;

        // 清理重复的斜杠
        fullPath = fullPath.replaceAll("/+", "/");

        // 返回带有服务器前缀的完整URL
        return "http://localhost:8080" + fullPath;
    }

    private static String getAnnotationName(PsiAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName == null) {
            return null;
        }

        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private boolean isSpringMappingAnnotation(String annotationName) {
        return "RequestMapping".equals(annotationName) ||
               "GetMapping".equals(annotationName) ||
               "PostMapping".equals(annotationName) ||
               "PutMapping".equals(annotationName) ||
               "DeleteMapping".equals(annotationName) ||
               "PatchMapping".equals(annotationName) ||
               "RestController".equals(annotationName) ||
               "Controller".equals(annotationName);
    }

    // Swagger注解信息类
    private static class SwaggerInfo {
        String summary = "";
        String description = "";
        String[] tags = new String[0];
        String[] produces = new String[0];
        String[] consumes = new String[0];
        java.util.List<ParameterInfo> parameters = new java.util.ArrayList<>();
        ResponseInfo response = new ResponseInfo();
    }

    private static class ParameterInfo {
        String name = "";
        String type = "";
        String description = "";
        boolean required = false;
        String in = ""; // query, path, body, header
    }

    private static class ResponseInfo {
        String type = "";
        String description = "";
    }

    /**
     * 提取Swagger注解信息
     */
    private static SwaggerInfo extractSwaggerInfo(PsiElement element) {
        SwaggerInfo info = new SwaggerInfo();
        try {
            // 检查是否是Kotlin文件
            PsiFile file = element.getContainingFile();
            boolean isKotlin = file != null && "Kotlin".equals(file.getLanguage().getDisplayName());

            if (isKotlin) {
                return extractKotlinSwaggerInfo(element);
            } else {
                return extractJavaSwaggerInfo(element);
            }
        } catch (Exception e) {
            LOG.warn("提取Swagger信息失败", e);
        }
        return info;
    }

    /**
     * 提取Java Swagger注解信息
     */
    private static SwaggerInfo extractJavaSwaggerInfo(PsiElement element) {
        SwaggerInfo info = new SwaggerInfo();
        try {
            // 查找包含此元素的方法
            PsiMethod method = findContainingMethod(element);
            if (method == null) {
                return info;
            }

            // 解析方法上的注解
            PsiAnnotation[] annotations = method.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String annotationName = getAnnotationName(annotation);
                if (annotationName == null) continue;

                switch (annotationName) {
                    case "ApiOperation":
                        info.summary = getAnnotationStringValue(annotation, "value", "");
                        info.description = getAnnotationStringValue(annotation, "notes", "");
                        info.tags = getAnnotationStringArrayValue(annotation, "tags");
                        break;
                    case "Operation":
                        info.summary = getAnnotationStringValue(annotation, "summary", "");
                        info.description = getAnnotationStringValue(annotation, "description", "");
                        break;
                }
            }

            // 解析方法参数
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                ParameterInfo paramInfo = extractParameterInfo(parameter);
                if (paramInfo != null) {
                    info.parameters.add(paramInfo);
                }
            }

            // 解析返回类型
            PsiType returnType = method.getReturnType();
            if (returnType != null) {
                info.response.type = returnType.getPresentableText();
            }

        } catch (Exception e) {
            LOG.warn("提取Java Swagger信息失败", e);
        }
        return info;
    }

    /**
     * 提取Kotlin Swagger注解信息
     */
    private static SwaggerInfo extractKotlinSwaggerInfo(PsiElement element) {
        SwaggerInfo info = new SwaggerInfo();
        try {
            // 查找包含此元素的Kotlin方法
            Object ktFunction = findKotlinFunction(element);
            if (ktFunction == null) {
                return info;
            }

            // 解析Kotlin方法上的注解
            Object[] annotations = getKotlinAnnotations(ktFunction);
            if (annotations != null) {
                for (Object annotation : annotations) {
                    String annotationName = getKotlinAnnotationName(annotation);
                    if (annotationName == null) continue;

                    switch (annotationName) {
                        case "ApiOperation":
                            info.summary = extractKotlinAnnotationStringValue(annotation, "value", "");
                            info.description = extractKotlinAnnotationStringValue(annotation, "notes", "");
                            break;
                        case "Operation":
                            info.summary = extractKotlinAnnotationStringValue(annotation, "summary", "");
                            info.description = extractKotlinAnnotationStringValue(annotation, "description", "");
                            break;
                    }
                }
            }

            // 解析Kotlin方法参数
            Object[] parameters = getKotlinFunctionParameters(ktFunction);
            if (parameters != null) {
                for (Object parameter : parameters) {
                    ParameterInfo paramInfo = extractKotlinParameterInfo(parameter);
                    if (paramInfo != null) {
                        info.parameters.add(paramInfo);
                    }
                }
            }

            // 解析Kotlin返回类型
            String returnType = getKotlinFunctionReturnType(ktFunction);
            if (returnType != null && !returnType.isEmpty()) {
                info.response.type = returnType;
            }

        } catch (Exception e) {
             LOG.warn("提取Kotlin Swagger信息失败", e);
         }
         return info;
     }

     /**
      * 查找包含元素的Kotlin函数
      */
     private static Object findKotlinFunction(PsiElement element) {
         try {
             PsiElement current = element;
             while (current != null) {
                 if (current.getClass().getSimpleName().contains("KtNamedFunction")) {
                     return current;
                 }
                 current = current.getParent();
             }
         } catch (Exception e) {
             LOG.warn("查找Kotlin函数失败", e);
         }
         return null;
     }

     /**
      * 获取Kotlin函数的注解
      */
     private static Object[] getKotlinAnnotations(Object ktFunction) {
         try {
             if (ktFunction != null) {
                 // 使用反射获取Kotlin注解
                 java.lang.reflect.Method getAnnotationsMethod = ktFunction.getClass().getMethod("getAnnotationEntries");
                 Object annotationEntries = getAnnotationsMethod.invoke(ktFunction);
                 if (annotationEntries instanceof java.util.List) {
                     java.util.List<?> list = (java.util.List<?>) annotationEntries;
                     return list.toArray();
                 }
             }
         } catch (Exception e) {
             LOG.warn("获取Kotlin注解失败", e);
         }
         return new Object[0];
     }

     /**
      * 获取Kotlin注解名称
      */
     private static String getKotlinAnnotationName(Object annotation) {
         try {
             if (annotation != null) {
                 java.lang.reflect.Method getShortNameMethod = annotation.getClass().getMethod("getShortName");
                 Object shortName = getShortNameMethod.invoke(annotation);
                 return shortName != null ? shortName.toString() : null;
             }
         } catch (Exception e) {
             LOG.warn("获取Kotlin注解名称失败", e);
         }
         return null;
     }

     /**
      * 提取Kotlin注解字符串值
      */
     private static String extractKotlinAnnotationStringValue(Object annotation, String attributeName, String defaultValue) {
         try {
             if (annotation != null) {
                 // 简化处理，返回默认值
                 return defaultValue;
             }
         } catch (Exception e) {
             LOG.warn("提取Kotlin注解值失败", e);
         }
         return defaultValue;
     }

     /**
      * 获取Kotlin函数参数
      */
     private static Object[] getKotlinFunctionParameters(Object ktFunction) {
         try {
             if (ktFunction != null) {
                 java.lang.reflect.Method getValueParametersMethod = ktFunction.getClass().getMethod("getValueParameters");
                 Object parameters = getValueParametersMethod.invoke(ktFunction);
                 if (parameters instanceof java.util.List) {
                     java.util.List<?> list = (java.util.List<?>) parameters;
                     return list.toArray();
                 }
             }
         } catch (Exception e) {
             LOG.warn("获取Kotlin函数参数失败", e);
         }
         return new Object[0];
     }

     /**
      * 提取Kotlin参数信息
      */
     private static ParameterInfo extractKotlinParameterInfo(Object parameter) {
         try {
             if (parameter != null) {
                 ParameterInfo info = new ParameterInfo();
                 // 获取参数名称
                 java.lang.reflect.Method getNameMethod = parameter.getClass().getMethod("getName");
                 Object name = getNameMethod.invoke(parameter);
                 info.name = name != null ? name.toString() : "unknown";

                 // 获取参数类型
                 java.lang.reflect.Method getTypeMethod = parameter.getClass().getMethod("getTypeReference");
                 Object typeRef = getTypeMethod.invoke(parameter);
                 if (typeRef != null) {
                     java.lang.reflect.Method getTextMethod = typeRef.getClass().getMethod("getText");
                     Object typeText = getTextMethod.invoke(typeRef);
                     info.type = typeText != null ? typeText.toString() : "Any";
                 } else {
                     info.type = "Any";
                 }

                 info.required = true; // 默认必需
                 return info;
             }
         } catch (Exception e) {
             LOG.warn("提取Kotlin参数信息失败", e);
         }
         return null;
     }

     /**
      * 获取Kotlin函数返回类型
      */
     private static String getKotlinFunctionReturnType(Object ktFunction) {
         try {
             if (ktFunction != null) {
                 java.lang.reflect.Method getTypeReferenceMethod = ktFunction.getClass().getMethod("getTypeReference");
                 Object typeRef = getTypeReferenceMethod.invoke(ktFunction);
                 if (typeRef != null) {
                     java.lang.reflect.Method getTextMethod = typeRef.getClass().getMethod("getText");
                     Object typeText = getTextMethod.invoke(typeRef);
                     return typeText != null ? typeText.toString() : "Unit";
                 }
             }
         } catch (Exception e) {
             LOG.warn("获取Kotlin返回类型失败", e);
         }
         return "Unit";
     }

    /**
     * 查找包含指定元素的方法
     */
    private static PsiMethod findContainingMethod(PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof PsiMethod) {
                return (PsiMethod) current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 提取参数信息
     */
    private static ParameterInfo extractParameterInfo(PsiParameter parameter) {
        ParameterInfo info = new ParameterInfo();
        info.name = parameter.getName();
        info.type = parameter.getType().getPresentableText();

        // 解析参数注解
        PsiAnnotation[] annotations = parameter.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String annotationName = getAnnotationName(annotation);
            if (annotationName == null) continue;

            switch (annotationName) {
                case "RequestParam":
                    info.in = "query";
                    String paramName = getAnnotationStringValue(annotation, "value", "");
                    if (!paramName.isEmpty()) {
                        info.name = paramName;
                    }
                    info.required = getAnnotationBooleanValue(annotation, "required", true);
                    break;
                case "PathVariable":
                    info.in = "path";
                    String pathName = getAnnotationStringValue(annotation, "value", "");
                    if (!pathName.isEmpty()) {
                        info.name = pathName;
                    }
                    break;
                case "RequestBody":
                    info.in = "body";
                    info.required = getAnnotationBooleanValue(annotation, "required", true);
                    break;
                case "RequestHeader":
                    info.in = "header";
                    String headerName = getAnnotationStringValue(annotation, "value", "");
                    if (!headerName.isEmpty()) {
                        info.name = headerName;
                    }
                    break;
                case "ApiParam":
                    info.description = getAnnotationStringValue(annotation, "value", "");
                    break;
                case "Parameter":
                    info.description = getAnnotationStringValue(annotation, "description", "");
                    break;
            }
        }

        return info;
    }

    /**
     * 获取注解的字符串值
     */
    private static String getAnnotationStringValue(PsiAnnotation annotation, String attributeName, String defaultValue) {
        try {
            PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
            if (value != null) {
                String text = value.getText();
                // 移除引号
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    return text.substring(1, text.length() - 1);
                }
                return text;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return defaultValue;
    }

    /**
     * 获取注解的字符串数组值
     */
    private static String[] getAnnotationStringArrayValue(PsiAnnotation annotation, String attributeName) {
        try {
            PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
            if (value instanceof PsiArrayInitializerMemberValue) {
                PsiArrayInitializerMemberValue arrayValue = (PsiArrayInitializerMemberValue) value;
                PsiAnnotationMemberValue[] initializers = arrayValue.getInitializers();
                String[] result = new String[initializers.length];
                for (int i = 0; i < initializers.length; i++) {
                    String text = initializers[i].getText();
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        result[i] = text.substring(1, text.length() - 1);
                    } else {
                        result[i] = text;
                    }
                }
                return result;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return new String[0];
    }

    /**
     * 获取注解的布尔值
     */
    private static boolean getAnnotationBooleanValue(PsiAnnotation annotation, String attributeName, boolean defaultValue) {
        try {
            PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
            if (value != null) {
                return Boolean.parseBoolean(value.getText());
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return defaultValue;
    }

    /**
     * 生成Markdown文档
     */
    private static String generateMarkdownDoc(PsiElement element, String url) {
        SwaggerInfo info = extractSwaggerInfo(element);
        StringBuilder markdown = new StringBuilder();

        // 提取HTTP方法
        String httpMethod = extractHttpMethod(element);

        // API标题
        String title = info.summary.isEmpty() ? "API接口" : info.summary;
        markdown.append("## ").append(title).append("\n\n");

        // 基本信息
        markdown.append("**请求方式:** ").append(httpMethod.toUpperCase()).append("\n\n");
        markdown.append("**请求URL:** `").append(url).append("`\n\n");

        if (!info.description.isEmpty()) {
            markdown.append("**接口描述:** ").append(info.description).append("\n\n");
        }

        if (info.tags.length > 0) {
            markdown.append("**标签:** ").append(String.join(", ", info.tags)).append("\n\n");
        }

        // 请求参数表格
        if (!info.parameters.isEmpty()) {
            markdown.append("### 请求参数\n\n");
            markdown.append("| 参数名 | 类型 | 位置 | 必填 | 说明 |\n");
            markdown.append("|--------|------|------|------|------|\n");

            for (ParameterInfo param : info.parameters) {
                markdown.append("| ").append(param.name)
                        .append(" | ").append(param.type)
                        .append(" | ").append(param.in.isEmpty() ? "query" : param.in)
                        .append(" | ").append(param.required ? "是" : "否")
                        .append(" | ").append(param.description)
                        .append(" |\n");
            }
            markdown.append("\n");
        }

        // 响应信息
        markdown.append("### 响应信息\n\n");
        markdown.append("| 字段 | 类型 | 说明 |\n");
        markdown.append("|------|------|------|\n");

        if (!info.response.type.isEmpty()) {
            markdown.append("| 返回值 | ").append(info.response.type)
                    .append(" | ").append(info.response.description.isEmpty() ? "接口返回数据" : info.response.description)
                    .append(" |\n");
        } else {
            markdown.append("| 返回值 | Object | 接口返回数据 |\n");
        }

        return markdown.toString();
    }

    /**
     * 生成curl命令
     */
    private static String generateCurlCommand(PsiElement element, String url) {
        SwaggerInfo info = extractSwaggerInfo(element);
        StringBuilder curl = new StringBuilder();

        // 提取HTTP方法
        String httpMethod = extractHttpMethod(element);

        curl.append("curl -X ").append(httpMethod.toUpperCase());

        // 添加URL
        curl.append(" \\").append("\n  '").append(url).append("'");

        // 添加请求头
        curl.append(" \\").append("\n  -H 'Content-Type: application/json'");
        curl.append(" \\").append("\n  -H 'Accept: application/json'");

        // 检查是否有body参数
        boolean hasBodyParam = info.parameters.stream().anyMatch(p -> "body".equals(p.in));

        if (hasBodyParam && ("POST".equalsIgnoreCase(httpMethod) || "PUT".equalsIgnoreCase(httpMethod) || "PATCH".equalsIgnoreCase(httpMethod))) {
            curl.append(" \\").append("\n  -d '{");

            // 添加示例JSON数据
            boolean first = true;
            for (ParameterInfo param : info.parameters) {
                if ("body".equals(param.in)) {
                    if (!first) {
                        curl.append(",");
                    }
                    curl.append("\n    \"").append(param.name).append("\": ");

                    // 根据类型生成示例值
                    String exampleValue = generateExampleValue(param.type);
                    curl.append(exampleValue);
                    first = false;
                }
            }

            if (first) {
                // 如果没有具体的body参数，添加通用示例
                curl.append("\n    \"key\": \"value\"");
            }

            curl.append("\n  }'");
        }

        // 添加查询参数示例
        java.util.List<ParameterInfo> queryParams = info.parameters.stream()
                .filter(p -> "query".equals(p.in) || p.in.isEmpty())
                .collect(java.util.stream.Collectors.toList());

        if (!queryParams.isEmpty()) {
            if (!url.contains("?")) {
                curl.append("?");
            } else {
                curl.append("&");
            }

            for (int i = 0; i < queryParams.size(); i++) {
                ParameterInfo param = queryParams.get(i);
                if (i > 0) {
                    curl.append("&");
                }
                curl.append(param.name).append("=").append(generateExampleValue(param.type));
            }
        }

        return curl.toString();
    }

    /**
     * 提取HTTP方法
     */
    private static String extractHttpMethod(PsiElement element) {
        try {
            // 查找包含此元素的方法
            PsiMethod method = findContainingMethod(element);
            if (method == null) {
                return "GET";
            }

            // 检查方法上的注解
            PsiAnnotation[] annotations = method.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String annotationName = getAnnotationName(annotation);
                if (annotationName == null) continue;

                switch (annotationName) {
                    case "GetMapping":
                        return "GET";
                    case "PostMapping":
                        return "POST";
                    case "PutMapping":
                        return "PUT";
                    case "DeleteMapping":
                        return "DELETE";
                    case "PatchMapping":
                        return "PATCH";
                    case "RequestMapping":
                        // 检查method属性
                        PsiAnnotationMemberValue methodValue = annotation.findAttributeValue("method");
                        if (methodValue != null) {
                            String methodText = methodValue.getText();
                            if (methodText.contains("POST")) return "POST";
                            if (methodText.contains("PUT")) return "PUT";
                            if (methodText.contains("DELETE")) return "DELETE";
                            if (methodText.contains("PATCH")) return "PATCH";
                        }
                        return "GET"; // 默认GET
                }
            }
        } catch (Exception e) {
            LOG.warn("提取HTTP方法失败", e);
        }
        return "GET";
    }

    /**
     * 根据类型生成示例值
     */
    private static String generateExampleValue(String type) {
        if (type == null || type.isEmpty()) {
            return "\"example\"";
        }

        type = type.toLowerCase();
        if (type.contains("string")) {
            return "\"example\"";
        } else if (type.contains("int") || type.contains("long")) {
            return "123";
        } else if (type.contains("double") || type.contains("float")) {
            return "123.45";
        } else if (type.contains("boolean")) {
            return "true";
        } else if (type.contains("date")) {
            return "\"2024-01-01\"";
        } else {
            return "\"example\"";
        }
    }

    /**
     * 为菜单项添加悬停效果
     */


    private static class GutterIconNavigationHandler implements com.intellij.codeInsight.daemon.GutterIconNavigationHandler<PsiElement> {
        private final String url;
        private final PsiElement sourceElement;

        public GutterIconNavigationHandler(String url, PsiElement sourceElement) {
            this.url = url;
            this.sourceElement = sourceElement;
        }

        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            // 创建弹出菜单
            JPopupMenu popupMenu = new JPopupMenu();

            // 添加"拷贝URL"菜单项
            JMenuItem copyUrlItem = new JMenuItem("拷贝URL", COPY_URL_ICON);
            copyUrlItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // 复制URL到剪贴板
                    CopyPasteManager.getInstance().setContents(new StringSelection(url));

                    // 显示复制成功的通知
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("RestfulTool")
                        .createNotification("复制成功", "已复制: " + url, NotificationType.INFORMATION)
                        .notify(elt.getProject());
                }
            });
            popupMenu.add(copyUrlItem);

            // 添加"复制Markdown"菜单项
            JMenuItem copyMarkdownItem = new JMenuItem("复制Markdown", COPY_MARKDOWN_ICON);
            copyMarkdownItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        String markdown = generateMarkdownDoc(sourceElement, url);
                        CopyPasteManager.getInstance().setContents(new StringSelection(markdown));

                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RestfulTool")
                            .createNotification("复制成功", "已复制Markdown文档", NotificationType.INFORMATION)
                            .notify(elt.getProject());
                    } catch (Exception ex) {
                        LOG.error("生成Markdown文档失败", ex);
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RestfulTool")
                            .createNotification("复制失败", "生成Markdown文档时出错: " + ex.getMessage(), NotificationType.ERROR)
                            .notify(elt.getProject());
                    }
                }
            });
            popupMenu.add(copyMarkdownItem);

            // 添加"复制curl"菜单项
            JMenuItem copyCurlItem = new JMenuItem("复制curl", COPY_CURL_ICON);
            copyCurlItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        String curlCommand = generateCurlCommand(sourceElement, url);
                        CopyPasteManager.getInstance().setContents(new StringSelection(curlCommand));

                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RestfulTool")
                            .createNotification("复制成功", "已复制curl命令", NotificationType.INFORMATION)
                            .notify(elt.getProject());
                    } catch (Exception ex) {
                        LOG.error("生成curl命令失败", ex);
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("RestfulTool")
                            .createNotification("复制失败", "生成curl命令时出错: " + ex.getMessage(), NotificationType.ERROR)
                            .notify(elt.getProject());
                    }
                }
            });
            popupMenu.add(copyCurlItem);

            // 显示弹出菜单
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}