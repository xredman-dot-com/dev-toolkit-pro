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

### 从源码构建
1. 克隆项目到本地
2. 在项目根目录执行构建命令:
   ```bash
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

- IntelliJ IDEA 2023.2 或更高版本
- Java 17 或更高版本
- 支持Java项目 (Spring Boot, JAX-RS等)

## 开发信息

- **版本**: 1.0.0
- **兼容性**: IntelliJ IDEA 2023.2 - 2024.1.*
- **开发语言**: Java
- **构建工具**: Gradle

## 贡献

欢迎提交Issue和Pull Request来帮助改进这个插件。

## 许可证

本项目采用MIT许可证。详情请参阅LICENSE文件。