# Dev Toolkit Pro - 开发任务管理
# 使用 asdf + JDK 21 进行构建和调试

# 默认任务：显示帮助信息
default:
    @just --list

# 检查Java环境
_check-java:
    #!/usr/bin/env zsh
    echo "🔍 检查Java环境..."
    if ! command -v asdf >/dev/null 2>&1; then
        echo "❌ asdf 未安装，请先安装 asdf 版本管理工具"
        echo "安装命令: brew install asdf"
        exit 1
    fi
    
    if ! asdf current java >/dev/null 2>&1; then
        echo "❌ Java 未通过 asdf 安装，请安装 JDK 21"
        echo "安装命令: asdf plugin add java && asdf install java temurin-21.0.5+11.0.LTS"
        exit 1
    fi
    
    export JAVA_HOME=$(asdf where java)
    echo "✅ Java 环境检查通过"
    echo "   JAVA_HOME: $JAVA_HOME"
    java -version

# 清理构建产物
clean: _check-java
    #!/usr/bin/env zsh
    echo "🧹 清理构建产物..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew clean
    echo "✅ 清理完成"

# 构建插件包
build: _check-java
    #!/usr/bin/env zsh
    echo "🔨 构建插件包..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew build
    echo "✅ 构建完成"
    echo "📦 插件包位置: build/distributions/"
    ls -la build/distributions/

# 构建插件包（跳过测试）
build-fast: _check-java
    #!/usr/bin/env zsh
    echo "🚀 快速构建插件包（跳过测试）..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew build -x test
    echo "✅ 快速构建完成"
    echo "📦 插件包位置: build/distributions/"
    ls -la build/distributions/

# 启动IDEA调试实例
debug: _check-java
    #!/usr/bin/env zsh
    echo "🐛 启动IDEA调试实例..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew runIde

# 运行单元测试
test: _check-java
    #!/usr/bin/env zsh
    echo "🧪 运行单元测试..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew test
    echo "✅ 测试完成"
    echo "📊 测试报告: build/reports/tests/test/index.html"

# 代码检查和格式化
check: _check-java
    #!/usr/bin/env zsh
    echo "🔍 代码检查..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew check
    echo "✅ 代码检查完成"

# 生成插件可分发包
dist: _check-java
    #!/usr/bin/env zsh
    echo "📦 生成插件可分发包..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew buildPlugin
    echo "✅ 分发包生成完成"
    echo "📦 分发包位置: build/distributions/"
    ls -la build/distributions/*.zip

# 安装插件到本地IDEA
install: _check-java
    #!/usr/bin/env zsh
    echo "💾 安装插件到本地IDEA..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew publishPlugin
    echo "✅ 插件安装完成"

# 开发模式：监听文件变化并自动构建
dev: _check-java
    #!/usr/bin/env zsh
    echo "👨‍💻 启动开发模式（文件监听）..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew build --continuous

# 验证插件
verify: _check-java
    #!/usr/bin/env zsh
    echo "✅ 验证插件..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew verifyPlugin
    echo "✅ 插件验证完成"

# 显示项目信息
info: _check-java
    #!/usr/bin/env zsh
    echo "📋 项目信息:"
    echo "   项目名称: Dev Toolkit Pro"
    echo "   Java版本: $(java -version 2>&1 | head -n 1)"
    echo "   JAVA_HOME: $(asdf where java)"
    echo "   Gradle版本: $(./gradlew --version | grep Gradle)"
    echo "   项目路径: $(pwd)"

# 全流程：清理 -> 构建 -> 测试 -> 调试
all: clean build test debug

# 快速开发流程：清理 -> 快速构建 -> 调试
quick: clean build-fast debug

# 发布准备：清理 -> 构建 -> 测试 -> 验证 -> 打包
release: clean build test verify dist
    echo "🎉 发布准备完成！"
    echo "📦 可分发包: build/distributions/"
    ls -la build/distributions/*.zip

# 重置开发环境
reset: _check-java
    #!/usr/bin/env zsh
    echo "🔄 重置开发环境..."
    export JAVA_HOME=$(asdf where java)
    ./gradlew clean
    rm -rf .gradle/
    rm -rf build/
    echo "✅ 开发环境重置完成"

# 显示 Gradle 任务
gradle-tasks: _check-java
    #!/usr/bin/env zsh
    echo "📋 可用的 Gradle 任务:"
    export JAVA_HOME=$(asdf where java)
    ./gradlew tasks
