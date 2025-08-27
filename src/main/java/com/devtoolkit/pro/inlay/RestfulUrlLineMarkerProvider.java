package com.devtoolkit.pro.inlay;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.IconLoader;
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
    private static final Icon PLUGIN_ICON = IconLoader.getIcon("/icons/pluginIcon.svg", RestfulUrlLineMarkerProvider.class);

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
            new GutterIconNavigationHandler(fullUrl),
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
                new GutterIconNavigationHandler(fullUrl),
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

    private String getAnnotationName(PsiAnnotation annotation) {
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