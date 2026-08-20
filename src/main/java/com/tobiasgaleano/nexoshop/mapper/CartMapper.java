package com.tobiasgaleano.nexoshop.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tobiasgaleano.nexoshop.dto.response.cart.CartItemResponse;
import com.tobiasgaleano.nexoshop.dto.response.cart.CartResponse;
import com.tobiasgaleano.nexoshop.model.entity.Cart;
import com.tobiasgaleano.nexoshop.model.entity.CartItem;
import com.tobiasgaleano.nexoshop.model.entity.Product;

@Component
public class CartMapper {

	private static final Comparator<CartItem> STABLE_ITEM_ORDER = Comparator
			.comparing(CartItem::getId, Comparator.nullsLast(Long::compareTo))
			.thenComparing(item -> item.getProduct().getSku());

	public CartResponse toResponse(Cart cart) {
		List<CartItemResponse> items = cart.getItems().stream()
				.sorted(STABLE_ITEM_ORDER)
				.map(this::toItemResponse)
				.toList();
		return new CartResponse(cart.getId(), cart.getUser().getId(), items, items.size(), cart.getTotalUnits(),
				cart.calculateSubtotal(), cart.getCreatedAt(), cart.getUpdatedAt());
	}

	private CartItemResponse toItemResponse(CartItem item) {
		Product product = item.getProduct();
		return new CartItemResponse(product.getId(), product.getSku(), product.getName(), product.getPrice(),
				item.getQuantity(), item.calculateLineTotal(), product.getStock());
	}
}
