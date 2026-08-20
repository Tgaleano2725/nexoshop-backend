package com.tobiasgaleano.nexoshop.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tobiasgaleano.nexoshop.dto.request.cart.AddCartItemRequest;
import com.tobiasgaleano.nexoshop.dto.request.cart.UpdateCartItemQuantityRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CartDtoValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void acceptsPositiveProductAndQuantity() {
		assertThat(validator.validate(new AddCartItemRequest(1L, 2))).isEmpty();
		assertThat(validator.validate(new UpdateCartItemQuantityRequest(3))).isEmpty();
	}

	@Test
	void rejectsMissingOrNonPositiveValues() {
		assertThat(validator.validate(new AddCartItemRequest(null, 0)))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("productId", "quantity");
		assertThat(validator.validate(new UpdateCartItemQuantityRequest(-1))).isNotEmpty();
	}
}
