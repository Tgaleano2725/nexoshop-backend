package com.tobiasgaleano.nexoshop.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;
import com.tobiasgaleano.nexoshop.model.entity.Category;

class CategoryMapperTest {

	private final CategoryMapper mapper = new CategoryMapper();

	@Test
	void mapsCategoryWithoutExposingTheEntity() {
		Category category = new Category("  Electronics  ", "  Devices  ");

		CategoryResponse response = mapper.toResponse(category);

		assertThat(response.name()).isEqualTo("Electronics");
		assertThat(response.description()).isEqualTo("Devices");
		assertThat(response.active()).isTrue();
		assertThat(response.getClass().getRecordComponents())
				.noneMatch(component -> component.getType().equals(Category.class));
	}
}
