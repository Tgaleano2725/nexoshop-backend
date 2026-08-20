package com.tobiasgaleano.nexoshop.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tobiasgaleano.nexoshop.dto.request.order.CreateOrderRequest;
import com.tobiasgaleano.nexoshop.dto.response.order.OrderResponse;

public interface OrderService {
	OrderResponse checkout(Long userId, CreateOrderRequest request);
	OrderResponse getById(Long userId, Long orderId);
	Page<OrderResponse> listByUser(Long userId, Pageable pageable);
	OrderResponse confirm(Long userId, Long orderId);
	OrderResponse startPreparing(Long userId, Long orderId);
	OrderResponse ship(Long userId, Long orderId);
	OrderResponse deliver(Long userId, Long orderId);
	OrderResponse markPaymentPaid(Long userId, Long orderId);
	OrderResponse markPaymentFailed(Long userId, Long orderId);
	OrderResponse refund(Long userId, Long orderId);
	OrderResponse cancel(Long userId, Long orderId);
}
