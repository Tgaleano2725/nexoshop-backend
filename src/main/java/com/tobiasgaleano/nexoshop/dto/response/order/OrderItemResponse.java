package com.tobiasgaleano.nexoshop.dto.response.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productSku, String productName,
		BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
}
