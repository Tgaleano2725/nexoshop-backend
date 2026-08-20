package com.tobiasgaleano.nexoshop.dto.request.order;

import java.math.BigDecimal;

import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
		@NotNull PaymentMethod paymentMethod,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal shippingCost,
		@NotBlank @Size(max = 160) String recipientName,
		@NotBlank @Size(max = 30) String recipientPhone,
		@NotBlank @Size(max = 255) String shippingAddress,
		@NotBlank @Size(max = 100) String shippingCity,
		@Size(max = 255) String shippingReference) {
}
