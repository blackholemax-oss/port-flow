package dev.blackholemax.backend.dto;

public record RegisterRequest(
        String email,
        String password,
        String displayName
) {
}
