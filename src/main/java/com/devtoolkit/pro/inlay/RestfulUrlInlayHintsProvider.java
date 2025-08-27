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
                    String path = extractPathFromAnnotation(annotation);
                    if (path == null || path.isEmpty()) {
                        continue;
                    }

                    // 获取HTTP方法
                    String httpMethod = getHttpMethodFromAnnotation(annotationName);

                    // 使用RestfulUrlService获取更准确的URL信息
                    List<RestfulEndpointNavigationItem> endpoints = urlService.findAllRestfulEndpoints();
                    String fullUrl = findUrlForMethod(endpoints, method, path);

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

        private String extractPathFromAnnotation(PsiAnnotation annotation) {
            PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            if (value == null) {
                value = annotation.findAttributeValue("path");
            }

            if (value != null) {
                // 尝试使用PSI常量求值来解析表达式
                Object constantValue = evaluateConstantExpression(value);
                if (constantValue != null) {
                    return constantValue.toString();
                }

                // 回退到直接字面量处理
                if (value instanceof PsiLiteralExpression) {
                    Object literalValue = ((PsiLiteralExpression) value).getValue();
                    return literalValue != null ? literalValue.toString() : null;
                }
            }

            return null;
        }

        /**
         * 使用PSI常量求值来解析常量表达式，如 API.API_V1_PREFIX + "/fetch"
         */
        private Object evaluateConstantExpression(PsiElement element) {
            try {
                // 获取PSI常量求值助手
                PsiConstantEvaluationHelper evaluationHelper =
                    JavaPsiFacade.getInstance(element.getProject()).getConstantEvaluationHelper();

                // 尝试计算常量表达式的值
                Object result = evaluationHelper.computeConstantExpression(element, false);

                // 如果标准求值失败，尝试手动解析常量引用
                if (result == null && element instanceof PsiExpression) {
                    result = resolveConstantReference((PsiExpression) element);
                }

                return result;
            } catch (Exception e) {
                // 如果求值失败，尝试手动解析
                if (element instanceof PsiExpression) {
                    return resolveConstantReference((PsiExpression) element);
                }
                return null;
            }
        }

        /**
         * 手动解析常量引用，特别是第三方jar包中的常量
         */
        private Object resolveConstantReference(PsiExpression expression) {
            try {
                // 处理二元表达式（如 API.API_V1_PREFIX + "/fetch"）
                if (expression instanceof PsiBinaryExpression) {
                    PsiBinaryExpression binaryExpr = (PsiBinaryExpression) expression;
                    if (binaryExpr.getOperationTokenType() == JavaTokenType.PLUS) {
                        Object left = resolveConstantReference(binaryExpr.getLOperand());
                        Object right = resolveConstantReference(binaryExpr.getROperand());
                        if (left != null && right != null) {
                            return left.toString() + right.toString();
                        }
                    }
                }

                // 处理引用表达式（如 API.API_V1_PREFIX）
                if (expression instanceof PsiReferenceExpression) {
                    PsiReferenceExpression refExpr = (PsiReferenceExpression) expression;
                    PsiElement resolved = refExpr.resolve();

                    if (resolved instanceof PsiField) {
                        PsiField field = (PsiField) resolved;
                        // 检查是否是常量字段（static final）
                        if (field.hasModifierProperty(PsiModifier.STATIC) &&
                            field.hasModifierProperty(PsiModifier.FINAL)) {

                            PsiExpression initializer = field.getInitializer();
                            if (initializer instanceof PsiLiteralExpression) {
                                return ((PsiLiteralExpression) initializer).getValue();
                            } else if (initializer != null) {
                                // 递归解析初始化表达式
                                return resolveConstantReference(initializer);
                            }
                        }
                    } else if (resolved == null) {
                        // 如果标准resolve失败，尝试使用JavaPsiFacade查找第三方jar包中的类和字段
                        Object result = resolveExternalConstant(refExpr);
                        if (result != null) {
                            return result;
                        }
                    }
                }

                // 处理字面量表达式
                if (expression instanceof PsiLiteralExpression) {
                    return ((PsiLiteralExpression) expression).getValue();
                }

                return null;
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 解析第三方jar包中的常量，使用JavaPsiFacade主动查找
         */
        private Object resolveExternalConstant(PsiReferenceExpression refExpr) {
            try {
                String referenceName = refExpr.getReferenceName();
                if (referenceName == null) {
                    return null;
                }

                // 获取限定符表达式（如 API.API_V1_PREFIX 中的 API）
                PsiExpression qualifierExpression = refExpr.getQualifierExpression();
                if (qualifierExpression instanceof PsiReferenceExpression) {
                    PsiReferenceExpression qualifierRef = (PsiReferenceExpression) qualifierExpression;
                    String className = qualifierRef.getReferenceName();

                    if (className != null) {


                        // 尝试查找可能的完全限定类名
                        String[] possiblePackages = {
                            "", // 当前包
                            "com.api.", // 常见的API包
                            "org.api.",
                            "com.constants.",
                            "org.constants."
                        };

                        JavaPsiFacade facade = JavaPsiFacade.getInstance(refExpr.getProject());
                        GlobalSearchScope scope = GlobalSearchScope.allScope(refExpr.getProject());

                        for (String packagePrefix : possiblePackages) {
                            String fullClassName = packagePrefix + className;
                            PsiClass psiClass = facade.findClass(fullClassName, scope);

                            if (psiClass != null) {
                                PsiField field = psiClass.findFieldByName(referenceName, false);

                                if (field != null &&
                                    field.hasModifierProperty(PsiModifier.STATIC) &&
                                    field.hasModifierProperty(PsiModifier.FINAL)) {

                                    PsiExpression initializer = field.getInitializer();
                                    if (initializer instanceof PsiLiteralExpression) {
                                        Object value = ((PsiLiteralExpression) initializer).getValue();
                                        return value;
                                    }
                                }
                            }
                        }

                        // 如果上述方法失败，尝试通过import语句查找完整类名
                        PsiFile containingFile = refExpr.getContainingFile();
                        if (containingFile instanceof PsiJavaFile) {
                            PsiJavaFile javaFile = (PsiJavaFile) containingFile;
                            PsiImportList importList = javaFile.getImportList();

                            if (importList != null) {
                                for (PsiImportStatement importStatement : importList.getImportStatements()) {
                                    String importedName = importStatement.getQualifiedName();
                                    if (importedName != null && importedName.endsWith("." + className)) {
                                        PsiClass importedClass = facade.findClass(importedName, scope);
                                        if (importedClass != null) {
                                            PsiField field = importedClass.findFieldByName(referenceName, false);
                                            if (field != null &&
                                                field.hasModifierProperty(PsiModifier.STATIC) &&
                                                field.hasModifierProperty(PsiModifier.FINAL)) {

                                                PsiExpression initializer = field.getInitializer();
                                                if (initializer instanceof PsiLiteralExpression) {
                                                    Object value = ((PsiLiteralExpression) initializer).getValue();
                                                    return value;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                return null;
            } catch (Exception e) {
                return null;
            }
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

            // 只返回路径部分，不包含host:port
            return fullPath;
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

        /**
         * 从端点列表中查找匹配的URL
         */
        private String findUrlForMethod(List<RestfulEndpointNavigationItem> endpoints, PsiMethod method, String fallbackPath) {
            // 优先使用buildFullUrl来处理常量解析
            PsiAnnotation[] annotations = method.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if (isSpringMappingAnnotation(getAnnotationName(annotation))) {
                    return buildFullUrl(annotation, fallbackPath);
                }
            }

            // 如果buildFullUrl失败，尝试从RestfulUrlService获取的端点列表中查找
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                String className = containingClass.getName();
                String methodName = method.getName();

                for (RestfulEndpointNavigationItem endpoint : endpoints) {
                    if (endpoint.getClassName().equals(className) && endpoint.getMethodName().equals(methodName)) {
                        return endpoint.getPath();
                    }
                }
            }

            return fallbackPath;
        }
    }
}