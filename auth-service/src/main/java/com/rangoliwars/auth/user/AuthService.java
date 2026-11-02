package com.rangoliwars.auth.user;

import com.rangoliwars.auth.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    public record AuthResult(String token, String username, String role) {
    }

    public static class UsernameTakenException extends RuntimeException {
        public UsernameTakenException() {
            super("That username is already taken");
        }
    }

    private static final String DEFAULT_ROLE = "PLAYER";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResult register(String username, String password) {
        String name = normalize(username);
        if (users.findByUsername(name).isPresent()) {
            throw new UsernameTakenException();
        }
        try {
            users.insert(name, encoder.encode(password), DEFAULT_ROLE);
        } catch (DuplicateKeyException e) {
            throw new UsernameTakenException();
        }
        return new AuthResult(jwt.issue(name, DEFAULT_ROLE), name, DEFAULT_ROLE);
    }

    public AuthResult login(String username, String password) {
        UserRepository.UserRecord user = users.findByUsername(normalize(username))
                .filter(u -> encoder.matches(password, u.passwordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return new AuthResult(jwt.issue(user.username(), user.role()), user.username(), user.role());
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
