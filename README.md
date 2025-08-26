# Dev Toolkit Pro

一个强大的IntelliJ IDEA插件，旨在提升开发者的工作效率。

## 功能特性

### 🔍 全局RESTful URL模糊搜索

#### 🎯 智能策略模式
- **多框架支持**: 自动识别并支持 Spring Boot、FastAPI、JAX-RS 等多种框架
- **智能选择**: 根据项目类型自动选择最适合的扫描策略
- **优先级调度**: 按框架流行度和检测准确性排序策略
- **容错回退**: 单个策略失败时自动尝试其他策略

#### 📱 多IDE环境支持
- **IntelliJ IDEA**: 支持 Spring Boot、Spring MVC、JAX-RS 项目
- **PyCharm**: 支持 FastAPI、Flask（开发中）项目  
- **通用支持**: 在任何IDE中都能回退到基础文件扫描

### 🔗 Git仓库集成功能（新增）

#### 📝 编辑器Git链接复制
- **快捷复制**: 在编辑器中右键快速复制Git链接
- **精确定位**: 支持复制当前行、选中行范围的精确链接
- **多平台支持**: 支持GitHub、GitLab、自托管GitLab等平台
- **智能识别**: 自动检测当前分支和仓库信息
- **三种链接类型**:
  - 📄 文件链接: 复制整个文件的在线链接
  - 🎯 当前行链接: 复制光标所在行的精确链接
  - 📋 选中行范围链接: 复制选中文本的行范围链接

#### 专用搜索对话框
- **快捷键触发**: 使用 `Ctrl+Shift+R` (Windows/Linux) 或 `Cmd+Shift+R` (macOS) 快速打开搜索对话框
- **智能扫描**: 使用策略模式自动扫描项目中的RESTful API端点
- **模糊匹配**: 支持高效的模糊搜索算法，快速定位目标API
- **一键导航**: 双击搜索结果直接跳转到对应的代码位置
- **框架感知**: 根据项目框架自动调整扫描逻辑

#### 🆕 系统搜索集成
- **快捷键**: 使用 `Ctrl+O` (Windows/Linux) 或 `Cmd+O` (macOS) 打开IntelliJ IDEA系统搜索对话框
- **新增标签页**: 在系统搜索对话框中新增 "Restful Endpoints" 标签页
- **统一体验**: 与IntelliJ IDEA原生搜索功能完美融合
- **快速切换**: 可以在Classes、Files、Symbols等标签页之间快速切换到RESTful端点搜索
- **智能识别**: 自动识别并显示项目中的所有RESTful API端点

## 安装方法

### 🛠️ 开发环境设置（推荐）

#### 🚀 现代化开发工作流程（推荐）

使用 **asdf** + **justfile** 实现现代化的开发环境管理：

1. **一键环境设置**
   ```bash
   # 运行自动设置脚本（推荐）
   ./setup-env.sh
   
   # 脚本会自动安装和配置：
   # - asdf 版本管理工具
   # - JDK 21 (Temurin)
   # - just 任务运行器
   # - 验证环境配置
   ```

2. **查看开发任务**
   ```bash
   just  # 显示所有可用的开发任务
   ```

3. **常用开发流程**
   ```bash
   # 🚀 快速开发（清理+快速构建+调试）
   just quick
   
   # 🔄 完整流程（清理+构建+测试+调试）
   just all
   
   # 📦 发布准备（完整构建+验证+打包）
   just release
   ```

#### 📋 可用的开发任务

| 任务 | 说明 | 用途 |
|------|------|------|
| `just build` | 完整构建项目 | 日常开发构建 |
| `just build-fast` | 快速构建（跳过测试） | 快速验证代码 |
| `just debug` | 启动IDEA调试实例 | 插件开发调试 |
| `just test` | 运行单元测试 | 代码质量保证 |
| `just clean` | 清理构建产物 | 环境重置 |
| `just dist` | 生成分发包 | 插件发布 |
| `just dev` | 开发模式（文件监听） | 持续开发 |
| `just info` | 显示环境信息 | 故障排除 |
| `just reset` | 重置开发环境 | 彻底清理 |

#### ⚡ 开发建议

- **日常开发**: 使用 `just quick` 快速开始
- **代码提交前**: 运行 `just test` 确保测试通过
- **插件调试**: 使用 `just debug` 启动调试实例
- **发布准备**: 使用 `just release` 生成最终版本
- **故障排除**: 使用 `just info` 检查环境状态

#### 🔧 手动环境配置（备选）

   ```bash
   # macOS
   brew install asdf just
   
   # 添加到shell配置
   echo '. $(brew --prefix asdf)/libexec/asdf.sh' >> ~/.zshrc
   source ~/.zshrc
   
   # 安装和配置JDK 21
   asdf plugin add java
   asdf install java temurin-21.0.5+11.0.LTS
   asdf local java temurin-21.0.5+11.0.LTS
   ```

### 📋 传统构建方法（备选）

#### 从源码构建
1. 克隆项目到本地
2. 在项目根目录执行构建命令:
   ```bash
   # 使用系统安装的JDK 21
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
   ./gradlew buildPlugin
   ```
3. 在IntelliJ IDEA中选择 `File > Settings > Plugins > Install Plugin from Disk`
4. 选择构建生成的插件文件 (位于 `build/distributions/` 目录)

## 使用方法

### 基本使用

#### 传统搜索对话框
1. **打开搜索对话框**: 按下 `Ctrl+Shift+R` (Windows/Linux) 或 `Cmd+Shift+R` (macOS)
2. **输入搜索关键词**: 在搜索框中输入API路径、HTTP方法或控制器名称的任意部分
3. **浏览搜索结果**: 使用上下箭头键浏览搜索结果
4. **跳转到代码**: 按回车键或双击结果项跳转到对应的代码位置

#### 🆕 系统搜索集成 (推荐)
1. **打开系统搜索**: 按下 `Ctrl+O` (Windows/Linux) 或 `Cmd+O` (macOS)
2. **选择RESTful端点标签**: 在弹出的对话框中，您会看到多个标签页：
   - All
   - Classes  
   - Files
   - Inventory
   - Symbols
   - Actions
   - **Restful Endpoints** 🆕 (新增)
3. **点击 "Restful Endpoints"**: 选择该标签页进入RESTful API搜索模式
4. **输入搜索关键词**: 在搜索框中输入您要查找的API相关关键词
5. **实时筛选**: 系统会实时显示匹配的RESTful端点
6. **快速导航**: 选择端点后按Enter或双击即可直接跳转到对应的控制器方法

### 搜索技巧
- **路径搜索**: 输入 `user` 可以找到包含 `/user` 路径的所有API
- **HTTP方法搜索**: 输入 `get` 可以找到所有GET请求的API
- **控制器搜索**: 输入控制器类名的部分字符
- **组合搜索**: 可以组合使用多个关键词进行精确搜索

### 支持的框架和注解

#### 🍃 Spring 框架 (Java)
- `@RestController` (Spring Boot)
- `@Controller` (Spring MVC)
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@PatchMapping`

#### 🚀 FastAPI 框架 (Python)
- `@app.get("/path")`
- `@app.post("/path")`
- `@app.put("/path")`
- `@app.delete("/path")`
- `@app.patch("/path")`
- `@router.get("/path")`
- `@router.post("/path")`
- 以及其他路由器装饰器

##### 🔗 复杂路由支持
- **多层路由器嵌套**: 支持 `router.include_router(sub_router)` 的多层嵌套
- **前缀计算**: 自动计算 `include_router(prefix="/api/v1")` 的完整路径
- **路由器依赖解析**: 智能处理路由器间的依赖关系
- **拓扑排序**: 使用拓扑排序算法确保正确的include顺序
- **路径组合**: 智能组合基础路径、路由器前缀和endpoint路径

##### ⭐ 高级特性支持（新增增强功能）
- **🏷️ 标签和元数据**: 解析 `tags=["users", "admin"]` 和路由描述信息
- **📝 文档信息**: 提取 `summary`、`description` 等OpenAPI文档信息
- **🔄 依赖注入**: 检测 `Depends(get_current_user)` 等依赖注入模式
- **🛡️ 中间件分析**: 识别 `add_middleware` 配置和类型
- **📊 响应模型**: 解析 `response_model=UserResponse` 等Pydantic模型
- **⚠️ 废弃标记**: 识别 `deprecated=True` 的已废弃端点
- **🎯 智能显示**: 端点列表显示包含标签、描述、依赖等丰富信息

#### 🔌 JAX-RS 框架 (Java)
- `@Path` (javax.ws.rs / jakarta.ws.rs)
- `@GET`
- `@POST`
- `@PUT`
- `@DELETE`
- `@PATCH`

## 搜索结果格式

搜索结果以以下格式显示:
```
HTTP方法 路径 (控制器类名.方法名)
```

示例:
```
GET /api/users (UserController.getAllUsers)
POST /api/users (UserController.createUser)
PUT /api/users/{id} (UserController.updateUser)
DELETE /api/users/{id} (UserController.deleteUser)
```

## 快捷键

| 操作 | Windows/Linux | macOS |
|------|---------------|-------|
| 打开专用搜索对话框 | `Ctrl+Shift+R` | `Cmd+Shift+R` |
| 打开系统搜索对话框 🆕 | `Ctrl+O` | `Cmd+O` |
| 向下选择 | `↓` | `↓` |
| 向上选择 | `↑` | `↑` |
| 跳转到选中项 | `Enter` | `Enter` |
| 关闭对话框 | `Esc` | `Esc` |

## 系统要求

- **IntelliJ IDEA**: 2023.2 或更高版本（支持2024.x, 2025.x）
- **PyCharm**: 2023.2 或更高版本（支持2024.x, 2025.x）
- **Java**: 21 或更高版本（推荐使用asdf管理）
- **操作系统**: macOS、Linux、Windows
- **开发工具**: 
  - asdf (版本管理，推荐)
  - just (任务运行器，推荐)
  - Git (版本控制，必需)

## 开发信息

- **版本**: 1.0.1
- **兼容性**: IntelliJ IDEA 2023.2 - 2025.3.*, PyCharm 2023.2 - 2025.3.*
- **开发语言**: Java + Kotlin
- **构建工具**: Gradle 8.x
- **Java版本**: JDK 21 (LTS)
- **架构**: 策略模式 + 依赖注入

### 📁 项目结构

```
dev_toolkit_pro/
├── .tool-versions          # asdf Java版本配置
├── justfile               # 开发任务定义
├── setup-env.sh          # 环境自动设置脚本  
├── DEVELOPMENT.md         # 开发者文档
├── build.gradle.kts       # Gradle构建配置
├── src/main/java/         # Java源代码
├── src/main/resources/    # 资源文件
└── src/test/             # 测试代码
```

### 🔄 开发工作流

1. **环境准备**: `./setup-env.sh` 一键配置开发环境
2. **日常开发**: `just quick` 快速构建和调试
3. **代码测试**: `just test` 运行单元测试
4. **插件调试**: `just debug` 启动IDEA调试实例
5. **发布准备**: `just release` 完整构建流程

## 贡献

欢迎提交Issue和Pull Request来帮助改进这个插件。

## 许可证

本项目采用MIT许可证。详情请参阅LICENSE文件。