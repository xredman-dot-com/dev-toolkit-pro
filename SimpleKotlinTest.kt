package com.devtoolkit.pro.test

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class SimpleKotlinTest {
    
    @GetMapping("/users")
    fun getUsers() {
        // 获取用户列表
    }
    
    @PostMapping("/users")
    fun createUser() {
        // 创建用户
    }
    
    @PutMapping("/users/{id}")
    fun updateUser() {
        // 更新用户
    }
    
    @DeleteMapping("/users/{id}")
    fun deleteUser() {
        // 删除用户
    }
    
    @RequestMapping("/test")
    fun testEndpoint() {
        // 测试端点
    }
}