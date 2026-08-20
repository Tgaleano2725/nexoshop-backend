package com.tobiasgaleano.nexoshop.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tobiasgaleano.nexoshop.dto.request.order.CreateOrderRequest;
import com.tobiasgaleano.nexoshop.dto.response.PageResponse;
import com.tobiasgaleano.nexoshop.dto.response.order.OrderResponse;
import com.tobiasgaleano.nexoshop.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController @Validated @RequestMapping("/api/v1/users/{userId}/orders")
public class OrderController {
	private final OrderService service;
	public OrderController(OrderService service) { this.service = service; }
	@PostMapping("/checkout") public ResponseEntity<OrderResponse> checkout(@PathVariable @Positive Long userId, @Valid @RequestBody CreateOrderRequest r) { return ResponseEntity.ok(service.checkout(userId, r)); }
	@GetMapping("/{orderId}") public OrderResponse get(@PathVariable @Positive Long userId, @PathVariable @Positive Long orderId) { return service.getById(userId, orderId); }
	@GetMapping public PageResponse<OrderResponse> list(@PathVariable @Positive Long userId, @RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="20") @Positive int size) { return PageResponse.from(service.listByUser(userId, PageRequest.of(page, size, Sort.by("id").descending()))); }
	@PostMapping("/{orderId}/cancel") public OrderResponse cancel(@PathVariable @Positive Long userId, @PathVariable @Positive Long orderId) { return service.cancel(userId, orderId); }
	@PostMapping("/{orderId}/confirm") public OrderResponse confirm(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.confirm(u, orderId); }
	@PostMapping("/{orderId}/preparing") public OrderResponse preparing(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.startPreparing(u, orderId); }
	@PostMapping("/{orderId}/ship") public OrderResponse ship(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.ship(u, orderId); }
	@PostMapping("/{orderId}/deliver") public OrderResponse deliver(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.deliver(u, orderId); }
	@PostMapping("/{orderId}/payment/paid") public OrderResponse paid(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.markPaymentPaid(u, orderId); }
	@PostMapping("/{orderId}/payment/failed") public OrderResponse failed(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.markPaymentFailed(u, orderId); }
	@PostMapping("/{orderId}/payment/refund") public OrderResponse refund(@PathVariable("userId") @Positive Long u, @PathVariable @Positive Long orderId) { return service.refund(u, orderId); }
}
