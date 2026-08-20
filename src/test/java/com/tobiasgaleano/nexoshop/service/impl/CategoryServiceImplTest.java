package com.tobiasgaleano.nexoshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.tobiasgaleano.nexoshop.dto.request.category.CreateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.request.category.UpdateCategoryRequest;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.CategoryMapper;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

	@Mock
	private CategoryRepository categoryRepository;

	private CategoryServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CategoryServiceImpl(categoryRepository, new CategoryMapper());
	}

	@Test
	void createsAndNormalizesAValidCategory() {
		when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(new CreateCategoryRequest("  Electronics  ", "  Devices  "));

		assertThat(response.name()).isEqualTo("Electronics");
		assertThat(response.description()).isEqualTo("Devices");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectsDuplicateNameBeforeWriting() {
		when(categoryRepository.existsByNameIgnoreCase("electronics")).thenReturn(true);

		assertThatThrownBy(() -> service.create(new CreateCategoryRequest(" electronics ", null)))
				.isInstanceOf(DuplicateResourceException.class);
		verify(categoryRepository, never()).saveAndFlush(any());
	}

	@Test
	void translatesRealUniqueConstraintViolation() {
		DataIntegrityViolationException violation = new DataIntegrityViolationException("constraint",
				new SQLException("duplicate", "23505"));
		when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(violation);

		assertThatThrownBy(() -> service.create(new CreateCategoryRequest("Electronics", null)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("A category with that name already exists");
	}

	@Test
	void reportsMissingCategory() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void updatesCategoryAndChecksDuplicateExcludingItsId() {
		Category category = new Category("Old", null);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		var response = service.update(1L, new UpdateCategoryRequest(" New ", " Description "));

		assertThat(response.name()).isEqualTo("New");
		assertThat(response.description()).isEqualTo("Description");
		verify(categoryRepository).existsByNameIgnoreCaseAndIdNot("New", 1L);
		verify(categoryRepository).flush();
	}

	@Test
	void rejectsDuplicateDuringUpdate() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("Old", null)));
		when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Existing", 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(1L, new UpdateCategoryRequest("Existing", null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void activatesAndDeactivatesCategory() {
		Category category = new Category("Electronics", null);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		assertThat(service.deactivate(1L).active()).isFalse();
		assertThat(service.activate(1L).active()).isTrue();
		verify(categoryRepository, org.mockito.Mockito.times(2)).flush();
	}

	@Test
	void listsCategoriesUsingRepositoryStableOrderAndMapping() {
		when(categoryRepository.findAllByOrderByNameAscIdAsc())
				.thenReturn(List.of(new Category("Accessories", null), new Category("Electronics", null)));

		assertThat(service.getAll()).extracting(response -> response.name())
				.containsExactly("Accessories", "Electronics");
	}
}
