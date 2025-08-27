package com.devtoolkit.pro.navigation;

import com.devtoolkit.pro.icons.HttpMethodIconProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import javax.swing.*;

/**
 * RESTful端点导航项
 * 用于在IntelliJ IDEA的搜索对话框中表示RESTful API端点
 */
public class RestfulEndpointNavigationItem implements NavigationItem {
    private final String httpMethod;
    private final String path;
    private final String className;
    private final String methodName;
    private final PsiMethod psiMethod;
    private final Project project;

    public RestfulEndpointNavigationItem(String httpMethod, String path,
                                       String className, String methodName,
                                       PsiMethod psiMethod, Project project) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.className = className;
        this.methodName = methodName;
        this.psiMethod = psiMethod;
        this.project = project;
    }

    @Override
    public String getName() {
        return path;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public String getPresentableText() {
                return getName();
            }

            @Override
            public String getLocationString() {
                return className + "." + methodName;
            }

            @Override
            public Icon getIcon(boolean unused) {
                // 使用HttpMethodIconProvider提供的彩色图标
                return HttpMethodIconProvider.getIcon(httpMethod);
            }
        };
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (psiMethod != null && psiMethod.isValid()) {
            psiMethod.navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return psiMethod != null && psiMethod.isValid() && psiMethod.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    // Getter方法
    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public String toString() {
        return getName() + " (" + getLocationString() + ")";
    }

    private String getLocationString() {
        return className + "." + methodName;
    }
}