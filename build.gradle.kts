plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"  // 使用最新版本
}

// 配置Java工具链使用JDK 21，但编译目标为17以兼容IntelliJ IDEA 2023.2
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.devtoolkit"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // 移除显式的kotlin-stdlib依赖，让IntelliJ平台自动管理
    // implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // SQL格式化依赖
    implementation("com.github.vertical-blank:sql-formatter:2.0.4")
    
    // 测试依赖 - 简化配置避免冲突
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

// Configure Gradle IntelliJ Plugin - 兼容多IDE环境
intellij {
    version.set("2024.2")  // 更新到2024.2以支持最新IDE版本
    type.set("IC") // 使用Community Edition作为基础
    plugins.set(listOf("java", "Git4Idea"))  // Git插件
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"  // 修改为17以兼容IntelliJ IDEA 2023.2
        targetCompatibility = "17"  // 修改为17以兼容IntelliJ IDEA 2023.2
        options.encoding = "UTF-8"
        // JDK优化选项
        options.compilerArgs.addAll(listOf(
            "-Xlint:all",
            "-Xlint:-serial",
            "-parameters"
        ))
    }
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"  // Kotlin暂时使用JVM 17作为目标，兼容JDK 21
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    patchPluginXml {
        sinceBuild.set("242")  // 支持从2024.2开始，匹配当前IDE版本
        untilBuild.set("253.*")  // 支持到2025.3版本
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // 确保runIde任务使用正确的JVM，兼容IntelliJ IDEA 2023.2
    runIde {
        jvmArgs = listOf(
            "-Xmx2048m",
            "-XX:ReservedCodeCacheSize=512m",
            // 调试相关
            "-Didea.auto.reload.plugins=true",
            "-Didea.ProcessCanceledException=disabled"
        )
    }
    
    // 构建插件任务优化
    buildPlugin {
        archiveFileName.set("dev-toolkit-pro-${project.version}.zip")
    }
    
    // 测试任务配置
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        // 简化JVM参数以避免冲突
        jvmArgs = listOf(
            "-Xmx1024m"
        )
    }
}