# Dev Toolkit Pro - IDE版本兼容性

## 🚀 最新更新

**版本**: 1.0.1  
**更新日期**: 2025-08-26

## ✅ 支持的IDE版本

### IntelliJ IDEA
- **最低版本**: 2023.2 (Build 232.x)
- **测试版本**: 2024.3 (Build 243.x)
- **支持范围**: 2023.2 - 2025.3.* (Build 232.x - 253.x)
- **类型**: Community Edition (IC) / Ultimate Edition (IU)

### PyCharm
- **最低版本**: 2023.2 (Build 232.x)
- **测试版本**: 2024.3 (Build 243.x)
- **支持范围**: 2023.2 - 2025.3.* (Build 232.x - 253.x)
- **类型**: Community Edition (PC) / Professional Edition (PY)

## 🔧 技术规格

### Java要求
- **工具链**: JDK 21 (开发时)
- **编译目标**: Java 17 (运行时兼容)
- **兼容性**: 向下兼容Java 17+的IDE运行时

### 关键配置
```kotlin
// build.gradle.kts
intellij {
    version.set("2024.3")  // 基于最新稳定版本构建
    type.set("IC")
    plugins.set(listOf("java", "Git4Idea"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("232")      // 支持从2023.2开始
        untilBuild.set("253.*")    // 支持到2025.3版本
    }
}
```

## 🧪 测试覆盖

### 已测试版本
- ✅ IntelliJ IDEA 2023.2 Community Edition
- 🔄 IntelliJ IDEA 2024.3 Community Edition (构建中)
- ⏳ PyCharm 2024.x Professional (待测试)
- ⏳ PyCharm 2025.x Community (待测试)

### 测试功能
- ✅ RESTful URL搜索功能
- ✅ Git链接复制功能
- ✅ 快捷键支持
- ✅ 系统搜索集成
- ✅ 多框架支持 (Spring Boot, FastAPI, JAX-RS)

## 🐛 已知问题

### 2025.x版本 (Build 252.x+)
- ⚠️ 可能存在新的API变更，正在适配中
- ⚠️ 部分UI组件可能需要调整

### 解决方案
1. 确保使用最新版本的插件 (v1.0.1+)
2. 如遇到兼容性问题，请反馈给开发团队
3. 可降级到稳定支持的版本使用

## 📦 安装指南

### 从文件安装
1. 下载最新版本的插件包: `dev-toolkit-pro-1.0.1.zip`
2. 在IDE中打开 `File > Settings > Plugins`
3. 点击 ⚙️ 图标 > `Install Plugin from Disk...`
4. 选择下载的zip文件
5. 重启IDE

### 版本验证
安装后可以通过以下方式验证兼容性：
- 使用快捷键 `Ctrl+Shift+R` (Windows/Linux) 或 `Cmd+Shift+R` (macOS)
- 检查Tools菜单中的"Dev Toolkit Pro"选项
- 在编辑器中右键查看"Copy Git Link"选项

## 🔄 更新策略

### 自动更新
- 插件会根据IDE版本自动调整兼容性
- 建议开启IDE的插件自动更新功能

### 手动更新
- 定期检查插件更新
- 关注项目GitHub页面的Release信息

## 📞 支持与反馈

如果在新版本IDE中遇到兼容性问题，请提供以下信息：

1. **IDE信息**:
   - IDE名称和版本 (Help > About)
   - Build号
   - 操作系统

2. **插件信息**:
   - 插件版本
   - 错误信息截图
   - 相关日志

3. **联系方式**:
   - 邮箱: maoba@anybots.cloud
   - GitHub Issues: [项目地址]

---

💡 **提示**: 为获得最佳体验，建议使用2024.x版本的IntelliJ IDEA或PyCharm。