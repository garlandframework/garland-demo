package org.mtodemo.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.mtodemo.userservice.security.JwtService;
import org.mtodemo.userservice.security.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    @Value("${app.security.credentials.username}")
    private String expectedClientId;

    @Value("${app.security.credentials.password}")
    private String expectedClientSecret;

    private final JwtService jwtService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(
            @RequestParam String grant_type,
            @RequestParam String client_id,
            @RequestParam String client_secret) {

        if (!expectedClientId.equals(client_id) || !expectedClientSecret.equals(client_secret)) {
            return ResponseEntity.status(401).body(new ErrorBody(401, "Invalid client credentials"));
        }
        return ResponseEntity.ok(new TokenResponse(jwtService.generate(client_id)));
    }

    record ErrorBody(int status, String message) {}
}
