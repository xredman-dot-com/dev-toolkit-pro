# Dev Toolkit Pro

IntelliJ IDEA/PyCharm插件，提供RESTful API搜索和Git链接复制功能。

## 功能特性

### RESTful API搜索
- 支持Spring Boot、FastAPI、JAX-RS框架
- 快捷键：`Ctrl+Shift+R` 或 `Cmd+Shift+R`
- 系统搜索集成：`Ctrl+O` 或 `Cmd+O`
- 模糊搜索，快速定位API端点

### Git链接复制
- 右键菜单快速复制Git链接
- 支持GitHub、GitLab等平台
- 支持文件、当前行、选中行范围链接

## 安装方法

### 从源码构建
1. 克隆项目：`git clone <repository-url>`
2. 构建插件：`./gradlew buildPlugin`
3. 在IDE中选择 `File > Settings > Plugins > Install Plugin from Disk`
4. 选择 `build/distributions/` 目录下的插件文件

## 使用方法

### RESTful API搜索
- 快捷键：`Ctrl+Shift+R` 或 `Cmd+Shift+R`
- 系统搜索：`Ctrl+O` 或 `Cmd+O`，选择"Restful Endpoints"标签
- 输入关键词搜索API路径、HTTP方法或控制器名称
- 双击结果跳转到代码位置

### Git链接复制
- 在编辑器中右键选择"Copy Git Link"菜单
- 支持复制文件、当前行或选中行范围的Git链接

## 系统要求

- IntelliJ IDEA 2023.2+
- PyCharm 2023.2+
- Java 21+

## 版本信息

- 版本：0.0.1
- 兼容性：IntelliJ IDEA/PyCharm 2023.2+

## 许可证

MIT许可证