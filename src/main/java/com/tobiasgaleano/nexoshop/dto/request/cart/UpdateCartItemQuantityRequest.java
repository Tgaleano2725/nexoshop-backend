package com.tobiasgaleano.nexoshop.dto.request.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemQuantityRequest(@NotNull @Positive Integer quantity) {
}
