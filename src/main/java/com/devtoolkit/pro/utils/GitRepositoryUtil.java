package com.devtoolkit.pro.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
// 暂时注释掉Git相关导入以解决兼容性问题
// import git4idea.GitUtil;
// import git4idea.repo.GitRepository;
// import git4idea.repo.GitRepositoryManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Git仓库工具类
 * 用于检测Git仓库信息并生成在线链接
 */
public class GitRepositoryUtil {
    
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
        
        public GitRepoInfo(String host, String owner, String repo, GitPlatform platform) {
            this.host = host;
            this.owner = owner;
            this.repo = repo;
            this.platform = platform;
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
            // 使用更直接的方式检查Git仓库
            VirtualFile projectRoot = project.getBaseDir();
            if (projectRoot == null) {
                return false;
            }
            
            // 检查.git目录是否存在
            VirtualFile gitDir = projectRoot.findChild(".git");
            return gitDir != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取Git仓库信息
     * 暂时禁用以解决兼容性问题
     */
    public static GitRepoInfo getGitRepoInfo(Project project) {
        // 暂时注释掉Git API调用以解决兼容性问题
        /*
        try {
            // 使用GitUtil更兼容的方式获取仓库
            GitRepository repo = GitUtil.getRepositoryForFile(project, project.getBaseDir());
            if (repo == null) {
                return null;
            }
            
            String remoteUrl = getRemoteUrl(repo);
            
            if (remoteUrl != null) {
                return parseRemoteUrl(remoteUrl);
            }
        } catch (Exception e) {
            System.err.println("Failed to get git repo info: " + e.getMessage());
        }
        */
        return null;
    }
    
    /**
     * 获取远程仓库URL
     * 暂时禁用以解决兼容性问题
     */
    private static String getRemoteUrl(Object repo) {
        // 暂时注释掉Git API调用以解决兼容性问题
        /*
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
        }
        */
        return null;
    }
    
    /**
     * 解析远程仓库URL
     */
    private static GitRepoInfo parseRemoteUrl(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isEmpty()) {
            return null;
        }
        
        // GitHub SSH
        Matcher matcher = GITHUB_SSH_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("github.com", matcher.group(1), matcher.group(2), GitPlatform.GITHUB);
        }
        
        // GitHub HTTPS
        matcher = GITHUB_HTTPS_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("github.com", matcher.group(1), matcher.group(2), GitPlatform.GITHUB);
        }
        
        // GitLab SSH
        matcher = GITLAB_SSH_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("gitlab.com", matcher.group(1), matcher.group(2), GitPlatform.GITLAB);
        }
        
        // GitLab HTTPS
        matcher = GITLAB_HTTPS_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo("gitlab.com", matcher.group(1), matcher.group(2), GitPlatform.GITLAB);
        }
        
        // 自托管GitLab SSH
        matcher = CUSTOM_GITLAB_SSH_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo(matcher.group(1), matcher.group(2), matcher.group(3), GitPlatform.CUSTOM_GITLAB);
        }
        
        // 自托管GitLab HTTPS
        matcher = CUSTOM_GITLAB_HTTPS_PATTERN.matcher(remoteUrl);
        if (matcher.matches()) {
            return new GitRepoInfo(matcher.group(1), matcher.group(2), matcher.group(3), GitPlatform.CUSTOM_GITLAB);
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
        
        switch (repoInfo.getPlatform()) {
            case GITHUB:
                return baseUrl + "/blob/main/" + path;
            case GITLAB:
            case CUSTOM_GITLAB:
                return baseUrl + "/-/blob/main/" + path;
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
        VirtualFile projectRoot = project.getBaseDir();
        if (projectRoot == null || file == null) {
            return null;
        }
        
        String projectPath = projectRoot.getPath();
        String filePath = file.getPath();
        
        if (filePath.startsWith(projectPath)) {
            return filePath.substring(projectPath.length() + 1);
        }
        
        return null;
    }
}