package org.ab.sentinel.dto;

public record UserDto(String email, String passwordHash, String name) {}
