//package org.devanalyzer.devport.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//public class HealthCheckController {
//
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, String>> healthCheck() {
//        return ResponseEntity.ok(Map.of("status", "ok"));
//    }
//}


//temp testing:
package org.devanalyzer.devport.controller;

import org.devanalyzer.devport.service.GitHubService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    private final GitHubService gitHubService;

    // Spring automatically injects your service here
    public HealthCheckController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // Temporary test route to verify GitHub API Integration
    @GetMapping("/test-github/{username}")
    public Object testGitHub(@PathVariable String username) {
        return gitHubService.fetchUserProfile(username);
    }
}