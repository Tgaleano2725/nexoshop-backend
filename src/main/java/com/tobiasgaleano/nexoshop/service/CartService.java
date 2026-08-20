package com.tobiasgaleano.nexoshop.service;

import com.tobiasgaleano.nexoshop.dto.request.cart.AddCartItemRequest;
import com.tobiasgaleano.nexoshop.dto.request.cart.UpdateCartItemQuantityRequest;
import com.tobiasgaleano.nexoshop.dto.response.cart.CartResponse;

public interface CartService {

	CartResponse getOrCreate(Long userId);

	CartResponse getByUserId(Long userId);

	CartResponse addProduct(Long userId, AddCartItemRequest request);

	CartResponse setProductQuantity(Long userId, Long productId, UpdateCartItemQuantityRequest request);

	CartResponse removeProduct(Long userId, Long productId);

	CartResponse clear(Long userId);
}
