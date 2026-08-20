package com.tobiasgaleano.nexoshop.dto.response.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
		Long cartId,
		Long userId,
		List<CartItemResponse> items,
		int lineCount,
		int totalUnits,
		BigDecimal subtotal,
		Instant createdAt,
		Instant updatedAt) {

	public CartResponse {
		items = List.copyOf(items);
	}
}
