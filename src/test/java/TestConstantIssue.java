// Test file for constant resolution
public class TestConstantIssue {
    
    public static class API {
        public static final String API_V1_PREFIX = "/api/v1";
    }
    
    // Simulated Spring annotations for testing
    // @RequestMapping(API.API_V1_PREFIX + "/test")
    public class TestController {
        
        // @GetMapping("/simple")
        public String getSimple() {
            return "simple";
        }
        
        // @PostMapping(API.API_V1_PREFIX + "/complex")
        public String postComplex() {
            return "complex";
        }
    }
}