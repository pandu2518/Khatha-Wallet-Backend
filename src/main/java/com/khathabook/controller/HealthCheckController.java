package com.khathabook.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class HealthCheckController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "UP", "message", "Khatha Wallet Backend is Alive!");
    }
}
