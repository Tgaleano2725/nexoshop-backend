package com.tobiasgaleano.nexoshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.tobiasgaleano.nexoshop.dto.request.product.CreateProductRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.StockAdjustmentRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.UpdateProductRequest;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.ProductMapper;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.repository.CategoryRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	private ProductServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ProductServiceImpl(productRepository, categoryRepository, new ProductMapper());
	}

	@Test
	void createsValidProductAssociatedWithExistingCategory() {
		Category category = category();
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(createRequest());

		assertThat(response.categoryName()).isEqualTo("Electronics");
		assertThat(response.sku()).isEqualTo("SKU-1");
		assertThat(response.price()).isEqualByComparingTo("25.50");
		assertThat(response.stock()).isEqualTo(10);
	}

	@Test
	void rejectsMissingOrInactiveCategory() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.create(createRequest()))
				.isInstanceOf(ResourceNotFoundException.class);

		Category inactive = category();
		inactive.deactivate();
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(inactive));
		assertThatThrownBy(() -> service.create(createRequest()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("active category");
	}

	@Test
	void rejectsDuplicateSkuBeforeWriting() {
		when(productRepository.existsBySku("SKU-1")).thenReturn(true);

		assertThatThrownBy(() -> service.create(createRequest()))
				.isInstanceOf(DuplicateResourceException.class);
		verify(productRepository, never()).saveAndFlush(any());
	}

	@Test
	void translatesInvalidPriceToBusinessRule() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category()));
		CreateProductRequest request = new CreateProductRequest(1L, "SKU-1", "Keyboard", null,
				new BigDecimal("1.001"), 0, null);

		assertThatThrownBy(() -> service.create(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("decimal places");
	}

	@Test
	void updatesAllowedProductData() {
		Product product = product();
		Category accessories = new Category("Accessories", null);
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(accessories));

		var response = service.update(5L, new UpdateProductRequest(2L, " SKU-2 ", "Mouse", "Wireless",
				new BigDecimal("15.00"), " image "));

		assertThat(response.categoryName()).isEqualTo("Accessories");
		assertThat(response.sku()).isEqualTo("SKU-2");
		assertThat(response.name()).isEqualTo("Mouse");
		assertThat(response.price()).isEqualByComparingTo("15.00");
		verify(productRepository).existsBySkuAndIdNot("SKU-2", 5L);
		verify(productRepository).flush();
	}

	@Test
	void rejectsDuplicateSkuDuringUpdate() {
		when(productRepository.findById(5L)).thenReturn(Optional.of(product()));
		when(productRepository.existsBySkuAndIdNot("SKU-2", 5L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(5L, new UpdateProductRequest(1L, "SKU-2", "Mouse", null,
				new BigDecimal("15.00"), null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void obtainsProductByIdAndSku() {
		Product product = product();
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));

		assertThat(service.getById(5L).name()).isEqualTo("Keyboard");
		assertThat(service.getBySku(" SKU-1 ").name()).isEqualTo("Keyboard");
		assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void returnsStableImmutablePagination() {
		Pageable requested = PageRequest.of(0, 2);
		when(productRepository.findAll(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(product()), requested, 1));

		var response = service.getAll(requested);

		assertThat(response.content()).hasSize(1);
		assertThatThrownBy(() -> response.content().clear()).isInstanceOf(UnsupportedOperationException.class);
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(productRepository).findAll(captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void listsOnlyActiveProductsThroughDedicatedRepositoryQuery() {
		Pageable requested = PageRequest.of(0, 10);
		when(productRepository.findByActiveTrue(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(product()), requested, 1));

		assertThat(service.getActive(requested).content()).hasSize(1);
	}

	@Test
	void activatesOnlyWhenCategoryIsActiveAndDeactivates() {
		Product product = product();
		product.deactivate();
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));

		assertThat(service.activate(5L).active()).isTrue();
		assertThat(service.deactivate(5L).active()).isFalse();

		product.getCategory().deactivate();
		assertThatThrownBy(() -> service.activate(5L))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void increasesAndDecreasesStockAndRejectsInsufficientQuantity() {
		Product product = product();
		when(productRepository.findLockedById(5L)).thenReturn(Optional.of(product));

		assertThat(service.increaseStock(5L, new StockAdjustmentRequest(5)).stock()).isEqualTo(15);
		assertThat(service.decreaseStock(5L, new StockAdjustmentRequest(3)).stock()).isEqualTo(12);
		assertThatThrownBy(() -> service.decreaseStock(5L, new StockAdjustmentRequest(13)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Insufficient stock");
	}

	private static CreateProductRequest createRequest() {
		return new CreateProductRequest(1L, " SKU-1 ", "Keyboard", "Mechanical",
				new BigDecimal("25.50"), 10, null);
	}

	private static Category category() {
		return new Category("Electronics", null);
	}

	private static Product product() {
		return new Product(category(), "SKU-1", "Keyboard", null, new BigDecimal("25.50"), 10, null);
	}
}
