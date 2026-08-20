package com.tobiasgaleano.nexoshop.dto.response.category;

import java.time.Instant;

public record CategoryResponse(
		Long id,
		String name,
		String description,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
