package com.tobiasgaleano.nexoshop.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Product;

class ProductMapperTest {

	private final ProductMapper mapper = new ProductMapper();

	@Test
	void mapsCategoryAsScalarDataWithoutExposingEntities() {
		Category category = new Category("Electronics", null);
		Product product = new Product(category, "SKU-1", "Keyboard", "Mechanical",
				new BigDecimal("25.50"), 10, "https://example.test/keyboard.png");

		ProductResponse response = mapper.toResponse(product);

		assertThat(response.categoryName()).isEqualTo("Electronics");
		assertThat(response.sku()).isEqualTo("SKU-1");
		assertThat(response.price()).isEqualByComparingTo("25.50");
		assertThat(response.stock()).isEqualTo(10);
		assertThat(response.getClass().getRecordComponents())
				.noneMatch(component -> component.getType().equals(Product.class)
						|| component.getType().equals(Category.class));
	}
}
