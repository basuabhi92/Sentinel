package org.ab.sentinel.dto.github;

import java.time.Instant;

public record GithubTokenValidationResultDto(boolean ok, Integer statusCode, String reason, String lastModified, Integer xPollInterval, Instant expiresAt, long daysRemaining) {}
