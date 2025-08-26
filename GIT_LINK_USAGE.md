# Git链接复制功能使用说明

## 🔗 功能概述

Dev Toolkit Pro 插件新增了Git链接复制功能，可以在编辑器中右键点击，快速复制当前文件在GitHub、GitLab等Git托管平台上的在线链接，并能精确定位到具体行数。

## 🎯 功能特点

### 📁 支持的Git平台
- **GitHub** (github.com)
- **GitLab** (gitlab.com)
- **自托管GitLab** (自定义域名)
- **自动识别** SSH和HTTPS远程仓库地址

### 🎨 链接类型
1. **📄 文件链接** - 复制整个文件的在线链接
2. **🎯 当前行链接** - 复制光标所在行的精确链接
3. **📋 选中行范围链接** - 复制选中文本的行范围链接

### ⚡ 智能特性
- **自动分支检测** - 基于当前Git分支生成链接
- **相对路径计算** - 自动计算文件相对于仓库根目录的路径
- **行号精确定位** - 支持单行和行范围的精确定位
- **多仓库支持** - 自动检测文件所属的Git仓库

## 🚀 使用方法

### 基本使用步骤

1. **打开Git仓库中的文件**
   - 确保当前项目是Git仓库
   - 在编辑器中打开任意文件

2. **右键调出上下文菜单**
   - 在编辑器中右键点击
   - 查找"Copy Git Link"选项

3. **选择链接类型**
   - 📄 Copy File Link - 复制文件链接
   - 🎯 Copy Current Line Link - 复制当前行链接  
   - 📋 Copy Selected Lines Link - 复制选中行范围链接

4. **获取链接**
   - 链接自动复制到剪贴板
   - 显示确认消息，包含具体链接信息

### 使用场景示例

#### 场景1: 分享代码文件
```
操作: 右键 → Copy File Link
结果: https://github.com/username/repo/blob/main/src/main/java/Example.java
用途: 分享整个文件给团队成员
```

#### 场景2: 指出具体问题行
```
操作: 光标定位到第42行 → 右键 → Copy Current Line Link  
结果: https://github.com/username/repo/blob/main/src/main/java/Example.java#L42
用途: 在代码评审中指出具体问题
```

#### 场景3: 讨论代码段
```
操作: 选择第15-20行 → 右键 → Copy Selected Lines Link
结果: https://github.com/username/repo/blob/main/src/main/java/Example.java#L15-L20  
用途: 讨论特定的代码段或函数
```

## 🔧 支持的链接格式

### GitHub格式
- 文件: `https://github.com/owner/repo/blob/branch/path/file.ext`
- 单行: `https://github.com/owner/repo/blob/branch/path/file.ext#L42`
- 行范围: `https://github.com/owner/repo/blob/branch/path/file.ext#L15-L20`

### GitLab格式  
- 文件: `https://gitlab.com/owner/repo/-/blob/branch/path/file.ext`
- 单行: `https://gitlab.com/owner/repo/-/blob/branch/path/file.ext#L42`
- 行范围: `https://gitlab.com/owner/repo/-/blob/branch/path/file.ext#L15-20`

### 自托管GitLab格式
- 文件: `https://git.company.com/owner/repo/-/blob/branch/path/file.ext`
- 单行: `https://git.company.com/owner/repo/-/blob/branch/path/file.ext#L42`
- 行范围: `https://git.company.com/owner/repo/-/blob/branch/path/file.ext#L15-20`

## 💡 使用技巧

### 1. 快速分享代码
在代码评审、技术讨论或错误报告中，可以快速生成精确的代码链接，让其他人能直接跳转到具体位置。

### 2. 文档编写
在编写项目文档时，可以引用具体的代码文件和行号，提供准确的代码示例。

### 3. 问题跟踪
在GitHub Issues、GitLab Issues或其他问题跟踪系统中，可以精确链接到问题代码位置。

### 4. 团队协作
在Slack、钉钉等团队沟通工具中分享代码时，提供精确的上下文。

## 🛠️ 故障排除

### 常见问题

**Q: 右键菜单中没有"Copy Git Link"选项**
A: 请检查：
- 当前项目是否为Git仓库
- 是否安装了Git4Idea插件（通常随IntelliJ IDEA安装）
- 重启IDE后重试

**Q: 提示"无法获取Git仓库信息"**  
A: 请检查：
- Git远程仓库是否已配置
- 远程仓库URL格式是否正确
- 网络连接是否正常

**Q: 生成的链接无法访问**
A: 请检查：
- 分支名是否正确（默认使用当前分支）
- 文件是否已推送到远程仓库
- 仓库权限设置

**Q: 支持哪些Git托管平台？**
A: 目前支持：
- GitHub (github.com)
- GitLab (gitlab.com)  
- 自托管GitLab实例
- 其他使用类似URL格式的Git平台

## 🔄 更新日志

### v1.0.0
- ✨ 新增编辑器Git链接复制功能
- 🎯 支持精确行号定位
- 📋 支持选中行范围链接
- 🔗 支持GitHub、GitLab等主流平台
- ⚡ 自动分支检测和路径计算

---

💻 **开发团队**: anybots.cloud  
📧 **联系邮箱**: maoba@anybots.cloud  
🔗 **项目地址**: [Dev Toolkit Pro](https://github.com/anybots/dev-toolkit-pro)