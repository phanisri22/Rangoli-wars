package com.rangoliwars.auth.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    public record UserRecord(long id, String username, String passwordHash, String role) {
    }

    private static final RowMapper<UserRecord> MAPPER = (rs, i) -> new UserRecord(
            rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("user_role"));

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRecord> findByUsername(String username) {
        return jdbc.query("SELECT id, username, password_hash, user_role FROM app_users WHERE username = ?",
                MAPPER, username).stream().findFirst();
    }

    public void insert(String username, String passwordHash, String role) {
        jdbc.update("INSERT INTO app_users (username, password_hash, user_role) VALUES (?, ?, ?)",
                username, passwordHash, role);
    }
}
