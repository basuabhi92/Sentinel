package org.ab.sentinel.dto.integrations;

import java.time.Instant;
import java.util.UUID;

public record AppIntegrationRequestDto(UUID userId, Integer appId, String accessToken, String scopes, Instant expiresAt) {}
