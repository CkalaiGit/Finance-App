package com.cairedine.finance.app.user;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<@NonNull UserContext> me(JwtAuthenticationToken auth) {
        UserContext ctx = (UserContext) auth.getDetails();
        return ResponseEntity.ok(ctx);
    }
}