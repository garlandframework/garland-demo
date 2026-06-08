package dev.garlandframework.demo.userservice.controller;

import lombok.RequiredArgsConstructor;
import dev.garlandframework.demo.userservice.security.JwtService;
import dev.garlandframework.demo.userservice.security.LoginRequest;
import dev.garlandframework.demo.userservice.security.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.security.credentials.username}")
    private String expectedUsername;

    @Value("${app.security.credentials.password}")
    private String expectedPassword;

    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (!expectedUsername.equals(request.username()) || !expectedPassword.equals(request.password())) {
            return ResponseEntity.status(401).body(new ErrorBody(401, "Invalid credentials"));
        }
        return ResponseEntity.ok(new TokenResponse(jwtService.generate(request.username())));
    }

    record ErrorBody(int status, String message) {}
}
