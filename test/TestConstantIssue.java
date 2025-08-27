package test;

import org.springframework.web.bind.annotation.*;

// 模拟第三方jar包中的常量类
class API {
    public static final String API_V1_PREFIX = "/api/v1";
}

@RestController
@RequestMapping(API.API_V1_PREFIX + "/fetch")
public class TestConstantIssue {
    
    // 这个应该显示完整路径 /api/v1/fetch/delete
    @GetMapping("/delete")
    public String fetchDelete() {
        return "fetch delete";
    }
    
    // 这个应该显示完整路径 /api/v1/fetch/save
    @PostMapping("/save")
    public String saveData() {
        return "save data";
    }
    
    // 简单的字符串作为对比
    @GetMapping("/simple")
    public String simple() {
        return "simple";
    }
}