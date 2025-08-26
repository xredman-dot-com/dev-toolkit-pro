package com.devtoolkit.pro.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Git链接工具类
 * 用于生成当前编辑器文件的在线Git链接
 */
public class GitLinkUtil {
    
    // GitHub URL模式
    private static final Pattern GITHUB_SSH_PATTERN = Pattern.compile("git@github\\.com:([^/]+)/(.+)\\.git");
    private static final Pattern GITHUB_HTTPS_PATTERN = Pattern.compile("https://github\\.com/([^/]+)/(.+?)(?:\\.git)?/?$");
    
    // GitLab URL模式  
    private static final Pattern GITLAB_SSH_PATTERN = Pattern.compile("git@gitlab\\.com:([^/]+)/(.+)\\.git");
    private static final Pattern GITLAB_HTTPS_PATTERN = Pattern.compile("https://gitlab\\.com/([^/]+)/(.+?)(?:\\.git)?/?$");
    
    // 自托管GitLab模式
    private static final Pattern CUSTOM_GITLAB_SSH_PATTERN = Pattern.compile("git@([^:]+):([^/]+)/(.+)\\.git");
    private static final Pattern CUSTOM_GITLAB_HTTPS_PATTERN = Pattern.compile("https://([^/]+)/([^/]+)/(.+?)(?:\\.git)?/?$");
    
    /**
     * Git仓库信息类
     */
    public static class GitRepoInfo {
        private String host;
        private String owner;
        private String repo;
        private GitPlatform platform;
        private String baseUrl;
        private String currentBranch;
        
        public GitRepoInfo(String host, String owner, String repo, GitPlatform platform, String currentBranch) {
            this.host = host;
            this.owner = owner;
            this.repo = repo;
            this.platform = platform;
            this.currentBranch = currentBranch != null ? currentBranch : "main";
            this.baseUrl = generateBaseUrl();
        }
        
        private String generateBaseUrl() {
            switch (platform) {
                case GITHUB:
                    return "https://github.com/" + owner + "/" + repo;
                case GITLAB:
                    return "https://gitlab.com/" + owner + "/" + repo;
                case CUSTOM_GITLAB:
                    return "https://" + host + "/" + owner + "/" + repo;
                default:
                    return null;
            }
        }
        
        // Getters
        public String getHost() { return host; }
        public String getOwner() { return owner; }
        public String getRepo() { return repo; }
        public GitPlatform getPlatform() { return platform; }
        public String getBaseUrl() { return baseUrl; }
        public String getCurrentBranch() { return currentBranch; }
    }
    
    /**
     * Git平台枚举
     */
    public enum GitPlatform {
        GITHUB("GitHub"),
        GITLAB("GitLab"),
        CUSTOM_GITLAB("GitLab (Custom)"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        GitPlatform(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 检查项目是否为Git仓库
     */
    public static boolean isGitRepository(Project project) {
        try {
            GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
            return !manager.getRepositories().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取当前文件的Git仓库信息
     */
    public static GitRepoInfo getGitRepoInfo(Project project, VirtualFile file) {
        try {
            GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
            GitRepository repo = GitUtil.getRepositoryForFile(project, file);
            
            if (repo == null) {
                return null;
            }
            
            String remoteUrl = getRemoteUrl(repo);
            String currentBranch = getCurrentBranch(repo);
            
            if (remoteUrl != null) {
                return parseRemoteUrl(remoteUrl, currentBranch);
            }
        } catch (Exception e) {
            System.err.println("Failed to get git repo info: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取当前分支名
     */
    private static String getCurrentBranch(GitRepository repo) {
        try {
            return repo.getCurrentBranch() != null ? repo.getCurrentBranch().getName() : "main";
        } catch (Exception e) {
            return "main";
        }
    }
    
    /**
     * 获取远程仓库URL
     */
    private static String getRemoteUrl(GitRepository repo) {
        try {
            // 尝试获取origin远程仓库URL
            if (repo.getRemotes().stream().anyMatch(remote -> "origin".equals(remote.getName()))) {
                return repo.getRemotes().stream()
                    .filter(remote -> "origin".equals(remote.getName()))
                    .findFirst()
                    .map(remote -> remote.getFirstUrl())
                    .orElse(null);
            }
            
            // 如果没有origin，使用第一个远程仓库
            return repo.getRemotes().stream()
                .findFirst()
                .map(remote -> remote.getFirstUrl())
                .orElse(null);
                
        } catch (Exception e) {
            System.err.println("Failed to get remote URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析远程仓库URL
     */
    private static GitRepoInfo parseRemoteUrl(String remoteUrl, String currentBranch) {
        if (remoteUrl == null || remoteUrl.isEmpty()) {
            return null;
        }
        
        // GitHub SSH
        Matcher matcher = GITHUB_SSH_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("github.com", matcher.group(1), matcher.group(2), GitPlatform.GITHUB, currentBranch);
        }
        
        // GitHub HTTPS
        matcher = GITHUB_HTTPS_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("github.com", matcher.group(1), matcher.group(2), GitPlatform.GITHUB, currentBranch);
        }
        
        // GitLab SSH
        matcher = GITLAB_SSH_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("gitlab.com", matcher.group(1), matcher.group(2), GitPlatform.GITLAB, currentBranch);
        }
        
        // GitLab HTTPS
        matcher = GITLAB_HTTPS_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("gitlab.com", matcher.group(1), matcher.group(2), GitPlatform.GITLAB, currentBranch);
        }
        
        // 自托管GitLab SSH
        matcher = CUSTOM_GITLAB_SSH_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo(matcher.group(1), matcher.group(2), matcher.group(3), GitPlatform.CUSTOM_GITLAB, currentBranch);
        }
        
        // 自托管GitLab HTTPS
        matcher = CUSTOM_GITLAB_HTTPS_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo(matcher.group(1), matcher.group(2), matcher.group(3), GitPlatform.CUSTOM_GITLAB, currentBranch);
        }
        
        return null;
    }
    
    /**
     * 生成文件的在线链接
     */
    public static String generateFileUrl(GitRepoInfo repoInfo, String relativePath) {
        if (repoInfo == null || repoInfo.getBaseUrl() == null) {
            return null;
        }
        
        String baseUrl = repoInfo.getBaseUrl();
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        String branch = repoInfo.getCurrentBranch();
        
        switch (repoInfo.getPlatform()) {
            case GITHUB:
                return baseUrl + "/blob/" + branch + "/" + path;
            case GITLAB:
            case CUSTOM_GITLAB:
                return baseUrl + "/-/blob/" + branch + "/" + path;
            default:
                return null;
        }
    }
    
    /**
     * 生成文件特定行的在线链接
     */
    public static String generateFileUrlWithLine(GitRepoInfo repoInfo, String relativePath, int lineNumber) {
        String fileUrl = generateFileUrl(repoInfo, relativePath);
        if (fileUrl == null) {
            return null;
        }
        
        switch (repoInfo.getPlatform()) {
            case GITHUB:
                return fileUrl + "#L" + lineNumber;
            case GITLAB:
            case CUSTOM_GITLAB:
                return fileUrl + "#L" + lineNumber;
            default:
                return fileUrl;
        }
    }
    
    /**
     * 生成文件行范围的在线链接
     */
    public static String generateFileUrlWithLineRange(GitRepoInfo repoInfo, String relativePath, int startLine, int endLine) {
        String fileUrl = generateFileUrl(repoInfo, relativePath);
        if (fileUrl == null) {
            return null;
        }
        
        switch (repoInfo.getPlatform()) {
            case GITHUB:
                return fileUrl + "#L" + startLine + "-L" + endLine;
            case GITLAB:
            case CUSTOM_GITLAB:
                return fileUrl + "#L" + startLine + "-" + endLine;
            default:
                return fileUrl;
        }
    }
    
    /**
     * 获取文件相对于项目根目录的路径
     */
    public static String getRelativePath(Project project, VirtualFile file) {
        try {
            GitRepository repo = GitUtil.getRepositoryForFile(project, file);
            
            if (repo != null) {
                // 尝试使用Git仓库根目录
                try {
                    // 直接使用项目根目录作为repo根目录
                    VirtualFile projectRoot = project.getBaseDir();
                    if (projectRoot != null) {
                        String projectPath = projectRoot.getPath();
                        String filePath = file.getPath();
                        
                        if (filePath.startsWith(projectPath)) {
                            return filePath.substring(projectPath.length() + 1);
                        }
                    }
                } catch (Exception ex) {
                    // 如果Git方式失败，继续使用项目根目录
                }
            }
            
            // 如果git方式失败，尝试使用项目根目录
            VirtualFile projectRoot = project.getBaseDir();
            if (projectRoot != null) {
                String projectPath = projectRoot.getPath();
                String filePath = file.getPath();
                
                if (filePath.startsWith(projectPath)) {
                    return filePath.substring(projectPath.length() + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get relative path: " + e.getMessage());
            // 如果git方式失败，尝试使用项目根目录
            try {
                VirtualFile projectRoot = project.getBaseDir();
                if (projectRoot != null) {
                    String projectPath = projectRoot.getPath();
                    String filePath = file.getPath();
                    
                    if (filePath.startsWith(projectPath)) {
                        return filePath.substring(projectPath.length() + 1);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to get relative path with project base dir: " + ex.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 从编辑器获取当前行号
     */
    public static int getCurrentLineNumber(Editor editor) {
        try {
            int offset = editor.getCaretModel().getOffset();
            return editor.getDocument().getLineNumber(offset) + 1; // 转换为1-based行号
        } catch (Exception e) {
            return 1;
        }
    }
    
    /**
     * 从编辑器获取选中的行范围
     */
    public static int[] getSelectedLineRange(Editor editor) {
        try {
            int startOffset = editor.getSelectionModel().getSelectionStart();
            int endOffset = editor.getSelectionModel().getSelectionEnd();
            
            if (startOffset == endOffset) {
                // 没有选中文本，返回当前行
                int currentLine = getCurrentLineNumber(editor);
                return new int[]{currentLine, currentLine};
            }
            
            int startLine = editor.getDocument().getLineNumber(startOffset) + 1;
            int endLine = editor.getDocument().getLineNumber(endOffset) + 1;
            
            return new int[]{startLine, endLine};
        } catch (Exception e) {
            return new int[]{1, 1};
        }
    }
}