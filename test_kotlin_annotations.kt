package com.devtoolkit.pro.test

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class TestKotlinAnnotations {
    
    @GetMapping("/users")
    fun getUsers(): String {
        return "users"
    }
    
    @PostMapping("/users")
    fun createUser(): String {
        return "created"
    }
    
    @PutMapping("/users/{id}")
    fun updateUser(): String {
        return "updated"
    }
    
    @DeleteMapping("/users/{id}")
    fun deleteUser(): String {
        return "deleted"
    }
}