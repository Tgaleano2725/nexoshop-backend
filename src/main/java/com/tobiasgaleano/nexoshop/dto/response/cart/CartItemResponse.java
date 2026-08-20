package com.tobiasgaleano.nexoshop.dto.response.cart;

import java.math.BigDecimal;

public record CartItemResponse(
		Long productId,
		String sku,
		String name,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal,
		int availableStock) {
}
