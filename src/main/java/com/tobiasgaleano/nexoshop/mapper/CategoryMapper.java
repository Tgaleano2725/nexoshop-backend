package com.tobiasgaleano.nexoshop.mapper;

import org.springframework.stereotype.Component;

import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;
import com.tobiasgaleano.nexoshop.model.entity.Category;

@Component
public class CategoryMapper {

	public CategoryResponse toResponse(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.isActive(),
				category.getCreatedAt(), category.getUpdatedAt());
	}
}
