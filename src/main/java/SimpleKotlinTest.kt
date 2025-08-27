package com.devtoolkit.pro.test

// 模拟Spring注解的简单测试
// 使用注释形式的注解来测试文本分析功能
class SimpleKotlinTest {

    // @GetMapping("/test")
    fun getTest(): String {
        return "Hello from Kotlin"
    }

    // @PostMapping("/create")
    fun createTest(data: String): String {
        return "Created: $data"
    }

    // @PutMapping("/update/{id}")
    fun updateTest(id: Long, data: String): String {
        return "Updated $id: $data"
    }

    // @DeleteMapping("/delete/{id}")
    fun deleteTest(id: Long): String {
        return "Deleted: $id"
    }
}