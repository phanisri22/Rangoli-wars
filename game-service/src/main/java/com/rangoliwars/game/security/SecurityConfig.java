package com.rangoliwars.game.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtVerifier jwt,
                                            @Value("${app.internal-api-key}") String internalKey) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**", "/actuator/health", "/error",
                                "/api/game/v3/api-docs/**", "/api/game/swagger-ui/**", "/api/game/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/internal/**").hasRole("INTERNAL")
                        .anyRequest().hasRole("PLAYER"))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new TokenFilter(jwt, internalKey), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Authenticates players by JWT and other services by the shared internal key. */
    static final class TokenFilter extends OncePerRequestFilter {

        private final JwtVerifier jwt;
        private final byte[] internalKey;

        TokenFilter(JwtVerifier jwt, String internalKey) {
            this.jwt = jwt;
            this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String key = request.getHeader(INTERNAL_KEY_HEADER);
            if (key != null && MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8))) {
                authenticate("internal", "ROLE_INTERNAL");
            } else {
                jwt.usernameFromHeader(request.getHeader(HttpHeaders.AUTHORIZATION))
                        .ifPresent(username -> authenticate(username, "ROLE_PLAYER"));
            }
            chain.doFilter(request, response);
        }

        private static void authenticate(String name, String role) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(name, null, List.of(new SimpleGrantedAuthority(role))));
        }
    }
}
