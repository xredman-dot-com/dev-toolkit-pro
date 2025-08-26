# Dev Toolkit Pro - 开发者快速入门

## 🚀 快速开始

### 前置要求

- **macOS/Linux** (推荐)
- **asdf** 版本管理工具
- **just** 命令行运行工具
- **Git** 版本控制

### 一键环境设置

```bash
# 1. 克隆项目
git clone https://github.com/anybots/dev-toolkit-pro.git
cd dev-toolkit-pro

# 2. 运行环境设置脚本
./setup-env.sh

# 3. 查看可用命令
just
```

## 🛠️ 开发工作流

### 日常开发

```bash
# 检查环境
just env

# 开发模式（自动监听文件变化）
just dev

# 快速构建测试
just build
```

### 调试插件

```bash
# 启动IDEA调试实例
just debug

# 清理后调试（推荐在修改重要文件后使用）
just debug-clean
```

### 发布前检查

```bash
# 完整的CI流程
just ci

# 构建最终插件包
just package
```

## 📋 可用命令详解

### 构建命令

| 命令 | 描述 | 使用场景 |
|------|------|----------|
| `just build` | 编译项目 | 快速验证代码 |
| `just package` | 构建插件包 | 准备发布 |
| `just clean` | 清理构建文件 | 解决构建问题 |
| `just test` | 运行测试 | 验证功能 |

### 调试命令

| 命令 | 描述 | 使用场景 |
|------|------|----------|
| `just debug` | 启动调试IDEA | 测试插件功能 |
| `just debug-clean` | 清理后调试 | 重大修改后 |
| `just rebuild-debug` | 强制重构建并调试 | 解决调试问题 |

### 工具命令

| 命令 | 描述 | 使用场景 |
|------|------|----------|
| `just env` | 显示环境信息 | 环境诊断 |
| `just deps` | 下载依赖 | 首次设置 |
| `just verify` | 验证插件配置 | 发布前检查 |
| `just dev` | 开发模式 | 实时开发 |

### 高级命令

| 命令 | 描述 | 使用场景 |
|------|------|----------|
| `just ci` | 完整CI流程 | 发布前验证 |
| `just install-local` | 安装到本地IDEA | 本地测试 |
| `just logs` | 查看构建日志 | 问题诊断 |

## 🔧 环境配置详解

### asdf配置

项目使用`.tool-versions`文件管理Java版本：

```
java temurin-21.0.5+11.0.LTS
```

这确保所有开发者使用相同的Java版本，避免环境不一致问题。

### JDK 21特性

项目现在完全支持JDK 21的所有特性：

- **性能优化**: 更好的JVM性能
- **语言特性**: 可以使用最新的Java语言特性
- **兼容性**: 与最新IntelliJ IDEA版本完全兼容

## 🐛 调试指南

### 启动调试实例

```bash
just debug
```

这将：
1. 检查Java环境
2. 编译项目代码
3. 启动一个新的IDEA实例
4. 在新实例中加载开发中的插件

### 调试技巧

1. **查看调试日志**
   ```bash
   just logs
   ```

2. **重置调试环境**
   ```bash
   just debug-clean
   ```

3. **强制重新构建**
   ```bash
   just rebuild-debug
   ```

### 常见问题

#### Q: 调试实例启动失败
A: 检查Java环境设置
```bash
just env
```

#### Q: 插件在调试实例中不可见
A: 尝试强制重新构建
```bash
just rebuild-debug
```

#### Q: 编译错误
A: 清理构建文件后重试
```bash
just clean
just build
```

## 📦 发布流程

### 1. 开发完成
```bash
# 运行完整测试
just ci
```

### 2. 构建发布包
```bash
# 构建最终插件包
just package
```

### 3. 验证插件包
```bash
# 本地安装测试
just install-local
```

### 4. 发布
构建成功的插件包位于 `build/distributions/` 目录中。

## 💡 最佳实践

### 开发习惯

1. **每日开始**: `just env` 检查环境
2. **开发时**: `just dev` 启用监听模式
3. **测试前**: `just debug` 启动调试实例
4. **提交前**: `just ci` 运行完整验证

### 性能优化

1. **使用开发模式**
   ```bash
   just dev  # 自动监听文件变化
   ```

2. **复用调试实例**
   - 不要频繁重启调试实例
   - 使用热重载功能

3. **定期清理**
   ```bash
   just clean  # 定期清理构建缓存
   ```

## 📞 支持

如果遇到问题：

1. 查看环境信息: `just env`
2. 查看构建日志: `just logs`
3. 重置环境: `./setup-env.sh`
4. 联系开发团队: maoba@anybots.cloud

---

**Happy Coding! 🚀**