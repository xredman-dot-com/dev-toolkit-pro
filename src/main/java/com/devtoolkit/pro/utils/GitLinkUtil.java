package com.devtoolkit.pro.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
// import git4idea.GitUtil;
// import git4idea.repo.GitRepository;
// import git4idea.repo.GitRepositoryManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private static final Pattern CUSTOM_GITLAB_HTTP_PATTERN = Pattern.compile("http://([^/]+)/([^/]+)/(.+?)(?:\\.git)?/?$");

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
            if (project == null || project.getBasePath() == null) {
                return false;
            }

            // 检查项目根目录是否存在.git文件夹
            File gitDir = new File(project.getBasePath(), ".git");
            return gitDir.exists() && (gitDir.isDirectory() || gitDir.isFile());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前文件的Git仓库信息
     */
    public static GitRepoInfo getGitRepoInfo(Project project, VirtualFile file) {
        try {
            if (project == null || project.getBasePath() == null) {
                System.err.println("Project or basePath is null");
                return null;
            }

            System.err.println("Project basePath: " + project.getBasePath());

            String remoteUrl = getRemoteUrl(project);
            String currentBranch = getCurrentBranch(project);

            System.err.println("Remote URL: " + remoteUrl);
            System.err.println("Current branch: " + currentBranch);

            if (remoteUrl != null) {
                GitRepoInfo repoInfo = parseRemoteUrl(remoteUrl, currentBranch);
                System.err.println("Parsed repo info: " + (repoInfo != null ? "success" : "failed"));
                return repoInfo;
            } else {
                System.err.println("Remote URL is null, cannot create repo info");
            }
        } catch (Exception e) {
            System.err.println("Failed to get git repo info: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前分支名
     */
    private static String getCurrentBranch(Project project) {
        try {
            File gitDir = new File(project.getBasePath(), ".git");
            if (!gitDir.exists()) {
                return "main";
            }

            // 读取HEAD文件获取当前分支
            File headFile = new File(gitDir, "HEAD");
            if (headFile.exists()) {
                String head = Files.readString(headFile.toPath()).trim();
                if (head.startsWith("ref: refs/heads/")) {
                    return head.substring("ref: refs/heads/".length());
                }
            }
            return "main";
        } catch (Exception e) {
            return "main";
        }
    }

    /**
     * 获取远程仓库URL
     */
    private static String getRemoteUrl(Project project) {
        try {
            File gitDir = new File(project.getBasePath(), ".git");
            if (!gitDir.exists()) {
                return null;
            }

            // 读取config文件获取远程仓库URL
            File configFile = new File(gitDir, "config");
            if (configFile.exists()) {
                String config = Files.readString(configFile.toPath());
                // 查找origin远程仓库的URL
                String[] lines = config.split("\n");
                boolean inOriginSection = false;
                for (String line : lines) {
                    line = line.trim();
                    if (line.equals("[remote \"origin\"]")) {
                        inOriginSection = true;
                    } else if (line.startsWith("[") && inOriginSection) {
                        inOriginSection = false;
                    } else if (inOriginSection && line.startsWith("url = ")) {
                        return line.substring("url = ".length());
                    }
                }
            }
            return null;
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

        // 自托管GitLab HTTP
        matcher = CUSTOM_GITLAB_HTTP_PATTERN.matcher(remoteUrl);
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
            // 直接使用项目根目录
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