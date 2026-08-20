package com.tobiasgaleano.nexoshop.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tobiasgaleano.nexoshop.dto.request.category.CreateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.CreateProductRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.StockAdjustmentRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CatalogDtoValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsBlankAndOversizedCategoryData() {
		CreateCategoryRequest request = new CreateCategoryRequest(" ", "x".repeat(501));

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("name", "description");
	}

	@Test
	void acceptsExactlyRepresentableProductInput() {
		CreateProductRequest request = new CreateProductRequest(1L, "SKU-1", "Keyboard", null,
				new BigDecimal("9999999999.99"), 0, null);

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void rejectsInvalidProductMoneyAndStock() {
		CreateProductRequest request = new CreateProductRequest(0L, "", "", null,
				new BigDecimal("1.001"), -1, null);

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getPropertyPath().toString())
				.contains("categoryId", "sku", "name", "price", "stock");
		assertThat(validator.validate(new StockAdjustmentRequest(0))).isNotEmpty();
	}
}
