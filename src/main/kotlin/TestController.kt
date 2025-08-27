package com.devtoolkit.pro.test

// Simple Kotlin test file for plugin functionality
class TestController {
    
    // @GetMapping("/api/users")
    fun getUsers(): String {
        return "users"
    }
    
    // @PostMapping("/api/users")
    fun createUser(): String {
        return "created"
    }
    
    // @PutMapping("/api/users/{id}")
    fun updateUser(id: String): String {
        return "updated: $id"
    }
    
    // @DeleteMapping("/api/users/{id}")
    fun deleteUser(id: String): String {
        return "deleted: $id"
    }
}