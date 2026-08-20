package com.tobiasgaleano.nexoshop.dto.request.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
		@NotNull @Positive Long categoryId,
		@NotBlank @Size(max = 50) String sku,
		@NotBlank @Size(max = 150) String name,
		String description,
		@NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
		@Size(max = 500) String imageUrl) {
}
