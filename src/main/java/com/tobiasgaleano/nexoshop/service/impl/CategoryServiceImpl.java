package com.tobiasgaleano.nexoshop.service.impl;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tobiasgaleano.nexoshop.dto.request.category.CreateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.request.category.UpdateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.CategoryMapper;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.repository.CategoryRepository;
import com.tobiasgaleano.nexoshop.service.CategoryService;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
		this.categoryRepository = categoryRepository;
		this.categoryMapper = categoryMapper;
	}

	@Override
	@Transactional
	public CategoryResponse create(CreateCategoryRequest request) {
		requireRequest(request);
		String name = normalizeName(request.name());
		ensureNameAvailable(name, null);
		Category category = performDomainAction(() -> new Category(name, request.description()));
		return categoryMapper.toResponse(saveAndFlush(category));
	}

	@Override
	@Transactional
	public CategoryResponse update(Long id, UpdateCategoryRequest request) {
		requireRequest(request);
		Category category = findCategory(id);
		String name = normalizeName(request.name());
		ensureNameAvailable(name, id);
		performDomainAction(() -> {
			category.updateDetails(name, request.description());
			return category;
		});
		flushCategory();
		return categoryMapper.toResponse(category);
	}

	@Override
	public CategoryResponse getById(Long id) {
		return categoryMapper.toResponse(findCategory(id));
	}

	@Override
	public List<CategoryResponse> getAll() {
		return categoryRepository.findAllByOrderByNameAscIdAsc().stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public CategoryResponse activate(Long id) {
		Category category = findCategory(id);
		category.activate();
		flushCategory();
		return categoryMapper.toResponse(category);
	}

	@Override
	@Transactional
	public CategoryResponse deactivate(Long id) {
		Category category = findCategory(id);
		category.deactivate();
		flushCategory();
		return categoryMapper.toResponse(category);
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

	private void ensureNameAvailable(String name, Long excludedId) {
		boolean duplicate = excludedId == null
				? categoryRepository.existsByNameIgnoreCase(name)
				: categoryRepository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
		if (duplicate) {
			throw new DuplicateResourceException("A category with that name already exists");
		}
	}

	private Category saveAndFlush(Category category) {
		try {
			return categoryRepository.saveAndFlush(category);
		} catch (DataIntegrityViolationException exception) {
			throwIfDuplicate(exception);
			throw exception;
		}
	}

	private void flushCategory() {
		try {
			categoryRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throwIfDuplicate(exception);
			throw exception;
		}
	}

	private static void throwIfDuplicate(DataIntegrityViolationException exception) {
		if (UniqueConstraintTranslator.isUniqueViolation(exception)) {
			throw new DuplicateResourceException("A category with that name already exists", exception);
		}
	}

	private static String normalizeName(String name) {
		return name == null ? null : name.trim();
	}

	private static void requireRequest(Object request) {
		if (request == null) {
			throw new BusinessRuleException("Category request must not be null");
		}
	}

	private static <T> T performDomainAction(DomainAction<T> action) {
		try {
			return action.execute();
		} catch (IllegalArgumentException | ArithmeticException exception) {
			throw new BusinessRuleException(exception.getMessage(), exception);
		}
	}

	@FunctionalInterface
	private interface DomainAction<T> {
		T execute();
	}
}
