package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public String getAllProducts() {
        return "Get all products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id) {
        return "Get product by id: " + id;
    }

    @PostMapping
    public String createProduct(@RequestBody String product) {
        return "Create product: " + product;
    }

    @PutMapping("/{id}")
    public String updateProduct(@PathVariable Long id, @RequestBody String product) {
        return "Update product: " + id + " with " + product;
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        return "Delete product: " + id;
    }

    @GetMapping("/category/{category}")
    public String getProductsByCategory(@PathVariable String category) {
        return "Get products by category: " + category;
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam String name, @RequestParam(required = false) String category) {
        return "Search products with name: " + name + " and category: " + category;
    }
}