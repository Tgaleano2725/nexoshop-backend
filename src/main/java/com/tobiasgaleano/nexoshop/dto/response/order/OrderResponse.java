package com.tobiasgaleano.nexoshop.dto.response.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.tobiasgaleano.nexoshop.model.enums.OrderStatus;
import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.model.enums.PaymentStatus;

public record OrderResponse(Long orderId, String orderNumber, Long userId, OrderStatus status,
		PaymentMethod paymentMethod, PaymentStatus paymentStatus, String recipientName,
		String recipientPhone, String shippingAddress, String shippingCity, String shippingReference,
		List<OrderItemResponse> items, BigDecimal subtotal, BigDecimal shippingCost, BigDecimal total,
		Instant createdAt, Instant updatedAt) {
	public OrderResponse {
		items = List.copyOf(items);
	}
}
