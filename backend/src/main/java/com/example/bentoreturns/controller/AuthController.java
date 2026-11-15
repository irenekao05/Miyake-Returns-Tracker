package com.example.bentoreturns.controller;

import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class AuthController {
    @GetMapping("/api/csrf")
    public Map<String, String> getCsrfToken(CsrfToken csrfToken) {
        return Map.of(
            "token", csrfToken.getToken(),
            "headerName", csrfToken.getHeaderName()
        );
    }
}
