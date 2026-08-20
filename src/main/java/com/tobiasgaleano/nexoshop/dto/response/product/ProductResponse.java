package com.tobiasgaleano.nexoshop.dto.response.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
		Long id,
		Long categoryId,
		String categoryName,
		String sku,
		String name,
		String description,
		BigDecimal price,
		int stock,
		String imageUrl,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
