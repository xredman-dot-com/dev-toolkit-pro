package com.devtoolkit.pro.test

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class TestInlayHintsController {
    
    @GetMapping("/users")
    fun getUsers(): String {
        return "users"
    }
    
    @PostMapping("/users")
    fun createUser(): String {
        return "created"
    }
    
    @PutMapping("/users/{id}")
    fun updateUser(@PathVariable id: String): String {
        return "updated: $id"
    }
    
    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: String): String {
        return "deleted: $id"
    }
}