package com.tobiasgaleano.nexoshop.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tobiasgaleano.nexoshop.dto.response.order.OrderItemResponse;
import com.tobiasgaleano.nexoshop.dto.response.order.OrderResponse;
import com.tobiasgaleano.nexoshop.model.entity.Order;
import com.tobiasgaleano.nexoshop.model.entity.OrderItem;

@Component
public class OrderMapper {
	public OrderResponse toResponse(Order order) {
		List<OrderItemResponse> items = order.getItems().stream()
				.sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
				.map(item -> new OrderItemResponse(item.getProduct().getId(), item.getProductSku(),
						item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))
				.toList();
		return new OrderResponse(order.getId(), order.getOrderNumber(), order.getUser().getId(), order.getStatus(),
				order.getPaymentMethod(), order.getPaymentStatus(), order.getRecipientName(), order.getRecipientPhone(),
				order.getShippingAddress(), order.getShippingCity(), order.getShippingReference(), items,
				order.getSubtotal(), order.getShippingCost(), order.getTotal(), order.getCreatedAt(), order.getUpdatedAt());
	}
}
