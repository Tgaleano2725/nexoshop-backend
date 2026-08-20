package com.tobiasgaleano.nexoshop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tobiasgaleano.nexoshop.dto.request.cart.*;
import com.tobiasgaleano.nexoshop.dto.response.cart.CartResponse;
import com.tobiasgaleano.nexoshop.service.CartService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController @Validated @RequestMapping("/api/v1/users/{userId}/cart")
public class CartController {
	private final CartService service;
	public CartController(CartService service) { this.service = service; }
	@GetMapping public CartResponse get(@PathVariable @Positive Long userId) { return service.getOrCreate(userId); }
	@PostMapping("/items") public ResponseEntity<CartResponse> add(@PathVariable @Positive Long userId, @Valid @RequestBody AddCartItemRequest r) { return ResponseEntity.ok(service.addProduct(userId, r)); }
	@PutMapping("/items/{productId}") public CartResponse update(@PathVariable @Positive Long userId, @PathVariable @Positive Long productId, @Valid @RequestBody UpdateCartItemQuantityRequest r) { return service.setProductQuantity(userId, productId, r); }
	@DeleteMapping("/items/{productId}") public CartResponse remove(@PathVariable @Positive Long userId, @PathVariable @Positive Long productId) { return service.removeProduct(userId, productId); }
	@DeleteMapping public CartResponse clear(@PathVariable @Positive Long userId) { return service.clear(userId); }
}
