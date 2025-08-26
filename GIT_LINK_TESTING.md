# Git链接功能测试指南

## 🧪 测试前准备

### 环境要求
1. **IntelliJ IDEA 2023.2+** 
2. **Java 17+**
3. **Git仓库项目** (必须是Git仓库，且配置了远程仓库)
4. **网络连接** (用于访问GitHub/GitLab等)

### 准备测试项目
1. 打开任意Git仓库项目
2. 确保项目有远程仓库 (origin)
3. 确保远程仓库是GitHub、GitLab或类似平台

## 🎯 功能测试步骤

### 测试1: 基本功能验证

1. **打开编辑器文件**
   - 在IntelliJ IDEA中打开任意代码文件
   - 确保文件在Git仓库中

2. **检查右键菜单**
   - 在编辑器中右键点击
   - 查找"Copy Git Link"选项
   - ✅ 应该能看到该选项且可点击

3. **验证平台识别**
   - 点击"Copy Git Link"
   - ✅ 弹出菜单应显示正确的Git平台名称（如"GitHub Links"）

### 测试2: 文件链接复制

1. **复制文件链接**
   - 右键 → Copy Git Link → 📄 Copy File Link
   - ✅ 应显示成功复制的消息
   - ✅ 剪贴板应包含完整文件URL

2. **验证链接格式**
   ```
   GitHub: https://github.com/owner/repo/blob/branch/path/file.ext
   GitLab: https://gitlab.com/owner/repo/-/blob/branch/path/file.ext
   ```
   - ✅ URL格式应符合对应平台规范
   - ✅ 分支名应为当前Git分支

3. **测试链接可访问性**
   - 在浏览器中打开复制的链接
   - ✅ 应能成功打开对应文件页面

### 测试3: 当前行链接复制

1. **定位到特定行**
   - 将光标移动到第20行
   - 右键 → Copy Git Link → 🎯 Copy Current Line Link

2. **验证行号链接**
   ```
   GitHub: https://github.com/owner/repo/blob/branch/path/file.ext#L20
   GitLab: https://gitlab.com/owner/repo/-/blob/branch/path/file.ext#L20
   ```
   - ✅ URL应包含正确的行号标识
   - ✅ 消息应显示"定位到第 20 行"

3. **浏览器验证**
   - 在浏览器中打开链接
   - ✅ 应自动滚动并高亮显示第20行

### 测试4: 选中行范围链接

1. **选择多行文本**
   - 选中第15-25行的代码
   - 右键 → Copy Git Link → 📋 Copy Selected Lines Link

2. **验证范围链接**
   ```
   GitHub: https://github.com/owner/repo/blob/branch/path/file.ext#L15-L25
   GitLab: https://gitlab.com/owner/repo/-/blob/branch/path/file.ext#L15-25
   ```
   - ✅ URL应包含正确的行范围
   - ✅ 消息应显示"定位到第 15-25 行"

3. **浏览器验证**
   - 在浏览器中打开链接  
   - ✅ 应高亮显示第15-25行范围

### 测试5: 错误场景处理

1. **非Git仓库测试**
   - 在非Git项目中打开文件
   - 右键查看菜单
   - ✅ "Copy Git Link"选项应不可见或禁用

2. **无远程仓库测试**
   - 在只有本地Git但无远程仓库的项目中
   - 尝试复制Git链接
   - ✅ 应显示合适的错误提示

3. **网络连接测试**
   - 复制链接后在离线状态下打开
   - ✅ 链接格式应正确（即使无法访问）

## 🔧 平台特定测试

### GitHub测试
- 测试SSH格式: `git@github.com:owner/repo.git`
- 测试HTTPS格式: `https://github.com/owner/repo.git`
- ✅ 两种格式都应正确生成链接

### GitLab测试  
- 测试gitlab.com仓库
- 测试自托管GitLab实例
- ✅ 应正确识别并生成对应格式链接

### 自托管Git测试
- 测试企业内部GitLab
- 测试其他Git托管平台
- ✅ 应正确解析自定义域名

## 🐛 常见问题排查

### 问题1: 右键菜单无"Copy Git Link"选项
**排查步骤:**
1. 检查当前项目是否为Git仓库: `git status`
2. 检查是否有远程仓库: `git remote -v` 
3. 重启IntelliJ IDEA
4. 检查插件是否正确安装

### 问题2: 提示"无法获取Git仓库信息"
**排查步骤:**
1. 确认远程仓库URL格式正确
2. 检查网络连接
3. 尝试在终端执行: `git remote get-url origin`

### 问题3: 生成的链接格式不正确
**排查步骤:**
1. 检查远程仓库URL: `git remote -v`
2. 确认当前分支: `git branch --show-current`
3. 检查文件是否在仓库中: `git ls-files | grep filename`

### 问题4: 链接无法在浏览器中打开
**排查步骤:**
1. 确认文件已推送到远程仓库
2. 检查仓库权限设置
3. 确认分支在远程仓库中存在

## ✅ 测试检查清单

- [ ] 右键菜单显示"Copy Git Link"选项
- [ ] 正确识别Git平台（GitHub/GitLab等）
- [ ] 文件链接复制功能正常
- [ ] 当前行链接包含正确行号
- [ ] 选中行范围链接格式正确
- [ ] 生成的链接在浏览器中可访问
- [ ] 错误场景有合适的提示信息
- [ ] 支持SSH和HTTPS两种Git URL格式
- [ ] 自动检测当前Git分支
- [ ] 消息提示包含完整链接和行号信息

## 📊 性能测试

### 响应时间测试
- 右键菜单弹出时间: < 100ms
- 链接生成时间: < 50ms  
- 剪贴板复制时间: < 10ms

### 内存占用测试
- 功能使用前后内存变化: < 5MB
- 无内存泄漏

---

**测试完成后，请在项目Issues中报告任何发现的问题。**