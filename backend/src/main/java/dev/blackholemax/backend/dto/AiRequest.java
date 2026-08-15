package dev.blackholemax.backend.dto;

public record AiRequest() {

    public record GenerateBioRequest(
            String keywords
    ) {
    }

    public record PolishRequest(
            String text,
            String title
    ) {
    }

    public record ColorRequest(
            String occupation
    ) {
    }

    public record SeoRequest(
            String userName,
            String bio
    ) {
    }
}