package com.tobiasgaleano.nexoshop.dto.response.user;

import java.time.Instant;

import com.tobiasgaleano.nexoshop.model.enums.UserRole;

public record UserResponse(Long id, String firstName, String lastName, String email,
		UserRole role, boolean active, Instant createdAt, Instant updatedAt) {
}
