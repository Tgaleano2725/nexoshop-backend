package com.tobiasgaleano.nexoshop.service;

import java.util.List;

import com.tobiasgaleano.nexoshop.dto.request.category.CreateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.request.category.UpdateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;

public interface CategoryService {

	CategoryResponse create(CreateCategoryRequest request);

	CategoryResponse update(Long id, UpdateCategoryRequest request);

	CategoryResponse getById(Long id);

	List<CategoryResponse> getAll();

	CategoryResponse activate(Long id);

	CategoryResponse deactivate(Long id);
}
