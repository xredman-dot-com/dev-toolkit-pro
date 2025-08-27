package test;

import org.springframework.web.bind.annotation.*;

@RestController
public class SimpleTest {
    
    // 测试简单字符串常量
    private static final String BASE_PATH = "/api/v1";
    
    @GetMapping(BASE_PATH + "/test")
    public String test() {
        return "test";
    }
    
    // 测试外部常量引用
    @PostMapping(API.API_V1_PREFIX + "/save")
    public String save() {
        return "save";
    }
}

// 模拟外部常量类
class API {
    public static final String API_V1_PREFIX = "/api/v1";
}