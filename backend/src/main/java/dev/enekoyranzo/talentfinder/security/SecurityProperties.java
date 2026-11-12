package dev.enekoyranzo.talentfinder.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotBlank String jwtSecret,
        @Min(1) @Max(24) int accessTokenHours,
        @NotEmpty List<String> allowedOrigins,
        boolean cookieSecure,
        @Min(1) int loginMaxAttempts,
        @Min(1) int loginLockMinutes
) {
}