#!/bin/bash

# Dev Toolkit Pro - 环境设置脚本
# 自动配置开发环境：asdf + JDK 21 + justfile

set -e  # 遇到错误立即退出

echo "🚀 Dev Toolkit Pro 环境设置开始..."
echo "================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查操作系统
detect_os() {
    print_info "检测操作系统..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
        print_success "检测到 macOS"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
        print_success "检测到 Linux"
    else
        print_error "不支持的操作系统: $OSTYPE"
        exit 1
    fi
}

# 检查并安装 Homebrew (仅 macOS)
install_homebrew() {
    if [[ "$OS" == "macos" ]]; then
        print_info "检查 Homebrew..."
        if ! command -v brew &> /dev/null; then
            print_warning "Homebrew 未安装，正在安装..."
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            print_success "Homebrew 安装完成"
        else
            print_success "Homebrew 已安装"
        fi
    fi
}

# 检查并安装 asdf
install_asdf() {
    print_info "检查 asdf..."
    if ! command -v asdf &> /dev/null; then
        print_warning "asdf 未安装，正在安装..."
        
        if [[ "$OS" == "macos" ]]; then
            brew install asdf
        elif [[ "$OS" == "linux" ]]; then
            git clone https://github.com/asdf-vm/asdf.git ~/.asdf --branch v0.13.1
            echo '. "$HOME/.asdf/asdf.sh"' >> ~/.bashrc
            echo '. "$HOME/.asdf/completions/asdf.bash"' >> ~/.bashrc
            # 对于 zsh 用户
            if [[ -f ~/.zshrc ]]; then
                echo '. "$HOME/.asdf/asdf.sh"' >> ~/.zshrc
                echo '. "$HOME/.asdf/completions/asdf.bash"' >> ~/.zshrc
            fi
        fi
        
        print_success "asdf 安装完成"
        print_warning "请重新加载 shell 配置或重启终端"
        
        # 尝试重新加载 asdf
        if [[ -f ~/.asdf/asdf.sh ]]; then
            source ~/.asdf/asdf.sh
        fi
    else
        print_success "asdf 已安装"
    fi
}

# 安装 Java 插件和 JDK 21
install_java() {
    print_info "配置 Java 环境..."
    
    # 添加 Java 插件
    if ! asdf plugin list | grep -q java; then
        print_info "添加 asdf Java 插件..."
        asdf plugin add java
        print_success "Java 插件添加完成"
    else
        print_success "Java 插件已存在"
    fi
    
    # 检查是否已安装 JDK 21
    local java_version="temurin-21.0.5+11.0.LTS"
    if ! asdf list java | grep -q "$java_version"; then
        print_info "安装 JDK 21..."
        asdf install java "$java_version"
        print_success "JDK 21 安装完成"
    else
        print_success "JDK 21 已安装"
    fi
    
    # 设置项目级别的 Java 版本
    print_info "设置项目 Java 版本..."
    asdf local java "$java_version"
    print_success "项目 Java 版本设置完成"
}

# 检查并安装 justfile
install_just() {
    print_info "检查 just..."
    if ! command -v just &> /dev/null; then
        print_warning "just 未安装，正在安装..."
        
        if [[ "$OS" == "macos" ]]; then
            brew install just
        elif [[ "$OS" == "linux" ]]; then
            # 对于 Linux，使用 cargo 安装
            if command -v cargo &> /dev/null; then
                cargo install just
            else
                print_warning "请手动安装 just: https://github.com/casey/just#installation"
                return 1
            fi
        fi
        
        print_success "just 安装完成"
    else
        print_success "just 已安装"
    fi
}

# 验证环境
verify_environment() {
    print_info "验证开发环境..."
    
    # 验证 asdf
    if command -v asdf &> /dev/null; then
        print_success "asdf: $(asdf --version)"
    else
        print_error "asdf 验证失败"
        return 1
    fi
    
    # 验证 Java
    if asdf current java &> /dev/null; then
        local java_info=$(asdf current java)
        print_success "Java: $java_info"
        
        # 验证 JAVA_HOME
        export JAVA_HOME=$(asdf where java)
        print_success "JAVA_HOME: $JAVA_HOME"
        
        # 验证 Java 版本
        java -version 2>&1 | head -n 1
    else
        print_error "Java 验证失败"
        return 1
    fi
    
    # 验证 just
    if command -v just &> /dev/null; then
        print_success "just: $(just --version)"
    else
        print_warning "just 验证失败（可选）"
    fi
    
    # 验证 Gradle
    if [[ -f "./gradlew" ]]; then
        print_success "Gradle Wrapper: 已找到"
        export JAVA_HOME=$(asdf where java)
        ./gradlew --version | grep "Gradle"
    else
        print_warning "Gradle Wrapper 未找到"
    fi
}

# 创建开发者帮助信息
create_dev_help() {
    print_info "创建开发者帮助文档..."
    
    cat > DEVELOPMENT.md << 'EOF'
# 开发环境配置指南

## 🛠️ 环境要求

- **asdf**: 版本管理工具
- **JDK 21**: Java 开发环境  
- **just**: 任务运行器（可选）
- **Git**: 版本控制

## 🚀 快速开始

### 1. 环境设置
```bash
# 运行环境设置脚本
./setup-env.sh

# 或手动配置
asdf plugin add java
asdf install java temurin-21.0.5+11.0.LTS
asdf local java temurin-21.0.5+11.0.LTS
```

### 2. 开发流程

#### 使用 justfile（推荐）
```bash
# 查看所有可用任务
just

# 快速开发流程
just quick          # 清理 -> 快速构建 -> 调试

# 完整开发流程  
just all            # 清理 -> 构建 -> 测试 -> 调试

# 单独任务
just build          # 构建插件
just test           # 运行测试
just debug          # 启动调试IDEA
just clean          # 清理构建产物
```

#### 使用 Gradle（传统方式）
```bash
# 确保使用正确的 Java 版本
export JAVA_HOME=$(asdf where java)

# 构建项目
./gradlew build

# 启动调试IDEA
./gradlew runIde

# 运行测试
./gradlew test
```

## 📋 任务说明

### 构建任务
- `just build` - 完整构建（包含测试）
- `just build-fast` - 快速构建（跳过测试）
- `just clean` - 清理构建产物

### 测试任务
- `just test` - 运行单元测试
- `just check` - 代码检查和格式化

### 调试任务
- `just debug` - 启动IDEA调试实例
- `just dev` - 开发模式（文件监听）

### 发布任务
- `just dist` - 生成分发包
- `just verify` - 验证插件
- `just release` - 完整发布流程

### 工具任务
- `just info` - 显示项目信息
- `just reset` - 重置开发环境

## 🔧 故障排除

### Java 环境问题
```bash
# 检查 Java 版本
java -version

# 重新设置 JAVA_HOME
export JAVA_HOME=$(asdf where java)

# 检查 asdf Java 版本
asdf current java
```

### Gradle 问题
```bash
# 清理 Gradle 缓存
./gradlew clean
rm -rf .gradle/

# 重新构建
./gradlew build
```

### IDE 调试问题
```bash
# 检查 JVM 参数
just info

# 重置环境后重试
just reset
just debug
```

## 📦 项目结构

```
dev_toolkit_pro/
├── .tool-versions          # asdf 版本配置
├── justfile               # 任务管理文件
├── build.gradle.kts       # Gradle 构建配置
├── setup-env.sh          # 环境设置脚本
├── DEVELOPMENT.md         # 开发文档
└── src/                   # 源代码目录
    ├── main/
    │   ├── java/
    │   └── resources/
    └── test/
```

## 🎯 开发建议

1. **使用 justfile**: 简化常用开发任务
2. **代码质量**: 提交前运行 `just check`
3. **测试先行**: 使用 `just test` 确保测试通过
4. **调试优先**: 使用 `just debug` 进行实时调试
5. **环境隔离**: 使用 asdf 管理多版本 Java

## 📞 获取帮助

- 运行 `just` 查看所有可用任务
- 运行 `just info` 查看环境信息
- 查看 `build.gradle.kts` 了解构建配置
- 检查 `.tool-versions` 确认版本要求
EOF

    print_success "开发者帮助文档已创建: DEVELOPMENT.md"
}

# 主流程
main() {
    echo
    detect_os
    echo
    
    install_homebrew
    echo
    
    install_asdf
    echo
    
    install_java
    echo
    
    install_just
    echo
    
    verify_environment
    echo
    
    create_dev_help
    echo
    
    print_success "🎉 环境设置完成！"
    echo
    print_info "接下来的步骤："
    echo "  1. 重新加载终端或运行: source ~/.zshrc"
    echo "  2. 验证环境: just info"
    echo "  3. 开始开发: just quick"
    echo
    print_info "查看帮助: just --list"
    print_info "开发文档: cat DEVELOPMENT.md"
}

# 运行主流程
main "$@"