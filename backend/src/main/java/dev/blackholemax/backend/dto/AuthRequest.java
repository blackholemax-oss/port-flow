package dev.blackholemax.backend.dto;

public record AuthRequest(
        String email,
        String password
) {
}
