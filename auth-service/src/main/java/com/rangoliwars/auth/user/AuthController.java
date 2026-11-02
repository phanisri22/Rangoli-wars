package com.rangoliwars.auth.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credentials(
            @NotBlank(message = "Username is required")
            @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "Username must be 3-20 letters, digits or _")
            String username,
            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 72, message = "Password must be 6-72 characters")
            String password) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AuthResult register(@Valid @RequestBody Credentials body) {
        return authService.register(body.username(), body.password());
    }

    @PostMapping("/login")
    public AuthService.AuthResult login(@RequestBody Credentials body) {
        return authService.login(String.valueOf(body.username()), String.valueOf(body.password()));
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return Map.of("username", auth.getName(), "role", role);
    }
}
