package com.devtoolkit.pro.ui;

import com.devtoolkit.pro.services.RestfulUrlService;
import com.devtoolkit.pro.navigation.RestfulEndpointNavigationItem;
import com.devtoolkit.pro.utils.FuzzySearchUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * RESTful URL搜索对话框
 */
public class SearchDialog extends DialogWrapper {
    private final Project project;
    private JBTextField searchField;
    private JBList<String> resultList;
    private DefaultListModel<String> listModel;
    private RestfulUrlService urlService;
    private List<String> allUrls;
    private List<RestfulEndpointNavigationItem> allEndpoints;

    public SearchDialog(Project project) {
        super(project, true);
        this.project = project;
        this.urlService = new RestfulUrlService(project);
        
        setTitle("Search RESTful URLs");
        setModal(false);
        init();
        
        // 加载所有URL
        loadUrls();
        
        // 设置焦点到搜索框
        SwingUtilities.invokeLater(() -> {
            if (searchField != null) {
                IdeFocusManager.getInstance(project).requestFocus(searchField, true);
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(600, 400));

        // 创建搜索框
        searchField = new JBTextField();
        searchField.getEmptyText().setText("Type to search RESTful URLs...");
        
        // 创建结果列表
        listModel = new DefaultListModel<>();
        resultList = new JBList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setCellRenderer(new UrlListCellRenderer());
        
        JBScrollPane scrollPane = new JBScrollPane(resultList);
        scrollPane.setPreferredSize(new Dimension(580, 350));

        // 添加搜索监听器
        searchField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                performSearch();
            }
        });

        // 添加键盘监听器
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        // 添加鼠标双击监听器
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSelectedUrl();
                }
            }
        });

        panel.add(searchField, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void loadUrls() {
        // 使用新的端点扫描方法，支持常量解析
        allEndpoints = urlService.findAllRestfulEndpoints();
        allUrls = new ArrayList<>();
        for (RestfulEndpointNavigationItem endpoint : allEndpoints) {
            allUrls.add(endpoint.getName()); // getName()返回"HTTP_METHOD path"格式
        }
        updateResultList(allUrls);
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            updateResultList(allUrls);
        } else {
            List<String> filteredUrls = FuzzySearchUtil.fuzzySearch(allUrls, query);
            updateResultList(filteredUrls);
        }
    }

    private void updateResultList(List<String> urls) {
        listModel.clear();
        for (String url : urls) {
            listModel.addElement(url);
        }
        if (!urls.isEmpty()) {
            resultList.setSelectedIndex(0);
        }
    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                if (listModel.getSize() > 0) {
                    int selectedIndex = resultList.getSelectedIndex();
                    if (selectedIndex < listModel.getSize() - 1) {
                        resultList.setSelectedIndex(selectedIndex + 1);
                    }
                }
                e.consume();
                break;
            case KeyEvent.VK_UP:
                if (listModel.getSize() > 0) {
                    int selectedIndex = resultList.getSelectedIndex();
                    if (selectedIndex > 0) {
                        resultList.setSelectedIndex(selectedIndex - 1);
                    }
                }
                e.consume();
                break;
            case KeyEvent.VK_ENTER:
                navigateToSelectedUrl();
                e.consume();
                break;
            case KeyEvent.VK_ESCAPE:
                close(CANCEL_EXIT_CODE);
                e.consume();
                break;
        }
    }

    private void navigateToSelectedUrl() {
        String selectedUrl = resultList.getSelectedValue();
        if (selectedUrl != null) {
            urlService.navigateToUrl(selectedUrl);
            close(OK_EXIT_CODE);
        }
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }

    /**
     * 自定义列表单元格渲染器
     */
    private static class UrlListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof String) {
                String url = (String) value;
                setText(url);
                setToolTipText(url);
                
                // 设置不同HTTP方法的颜色
                if (url.contains("GET")) {
                    setForeground(isSelected ? Color.WHITE : new Color(0, 128, 0));
                } else if (url.contains("POST")) {
                    setForeground(isSelected ? Color.WHITE : new Color(255, 140, 0));
                } else if (url.contains("PUT")) {
                    setForeground(isSelected ? Color.WHITE : new Color(0, 0, 255));
                } else if (url.contains("DELETE")) {
                    setForeground(isSelected ? Color.WHITE : new Color(255, 0, 0));
                }
            }
            
            return this;
        }
    }
}