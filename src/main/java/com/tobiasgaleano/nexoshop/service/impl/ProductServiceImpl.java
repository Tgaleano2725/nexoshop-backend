package com.tobiasgaleano.nexoshop.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tobiasgaleano.nexoshop.dto.request.product.CreateProductRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.StockAdjustmentRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.UpdateProductRequest;
import com.tobiasgaleano.nexoshop.dto.response.PageResponse;
import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.ProductMapper;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.repository.CategoryRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;
import com.tobiasgaleano.nexoshop.service.ProductService;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

	private static final Sort STABLE_SORT = Sort.by(Sort.Direction.ASC, "id");

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductMapper productMapper;

	public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
			ProductMapper productMapper) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.productMapper = productMapper;
	}

	@Override
	@Transactional
	public ProductResponse create(CreateProductRequest request) {
		requireRequest(request, "Product request must not be null");
		String sku = normalizeSku(request.sku());
		ensureSkuAvailable(sku, null);
		Category category = findCategory(request.categoryId());
		ensureCategoryAllowsActiveProduct(category);
		Product product = performDomainAction(() -> new Product(category, sku, request.name(), request.description(),
				request.price(), requireStock(request.stock()), request.imageUrl()));
		return productMapper.toResponse(saveAndFlush(product));
	}

	@Override
	@Transactional
	public ProductResponse update(Long id, UpdateProductRequest request) {
		requireRequest(request, "Product request must not be null");
		Product product = findProduct(id);
		String sku = normalizeSku(request.sku());
		ensureSkuAvailable(sku, id);
		Category category = findCategory(request.categoryId());
		if (product.isActive()) {
			ensureCategoryAllowsActiveProduct(category);
		}
		performDomainAction(() -> {
			product.changeCategory(category);
			product.changeSku(sku);
			product.updateDescriptiveData(request.name(), request.description(), request.imageUrl());
			product.changePrice(request.price());
			return product;
		});
		flushProduct();
		return productMapper.toResponse(product);
	}

	@Override
	public ProductResponse getById(Long id) {
		return productMapper.toResponse(findProduct(id));
	}

	@Override
	public ProductResponse getBySku(String sku) {
		String normalizedSku = normalizeSku(sku);
		Product product = productRepository.findBySku(normalizedSku)
				.orElseThrow(() -> new ResourceNotFoundException("Product", normalizedSku));
		return productMapper.toResponse(product);
	}

	@Override
	public PageResponse<ProductResponse> getAll(Pageable pageable) {
		Page<ProductResponse> page = productRepository.findAll(stablePageable(pageable)).map(productMapper::toResponse);
		return PageResponse.from(page);
	}

	@Override
	public PageResponse<ProductResponse> getActive(Pageable pageable) {
		Page<ProductResponse> page = productRepository.findByActiveTrue(stablePageable(pageable))
				.map(productMapper::toResponse);
		return PageResponse.from(page);
	}

	@Override
	@Transactional
	public ProductResponse activate(Long id) {
		Product product = findProduct(id);
		ensureCategoryAllowsActiveProduct(product.getCategory());
		product.activate();
		flushProduct();
		return productMapper.toResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse deactivate(Long id) {
		Product product = findProduct(id);
		product.deactivate();
		flushProduct();
		return productMapper.toResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse increaseStock(Long id, StockAdjustmentRequest request) {
		Product product = findProductForStockUpdate(id);
		int quantity = requireQuantity(request);
		performDomainAction(() -> {
			product.increaseStock(quantity);
			return product;
		});
		flushProduct();
		return productMapper.toResponse(product);
	}

	@Override
	@Transactional
	public ProductResponse decreaseStock(Long id, StockAdjustmentRequest request) {
		Product product = findProductForStockUpdate(id);
		int quantity = requireQuantity(request);
		performDomainAction(() -> {
			product.decreaseStock(quantity);
			return product;
		});
		flushProduct();
		return productMapper.toResponse(product);
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
	}

	private Product findProductForStockUpdate(Long id) {
		return productRepository.findLockedById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

	private void ensureSkuAvailable(String sku, Long excludedId) {
		boolean duplicate = excludedId == null
				? productRepository.existsBySku(sku)
				: productRepository.existsBySkuAndIdNot(sku, excludedId);
		if (duplicate) {
			throw new DuplicateResourceException("A product with that SKU already exists");
		}
	}

	private static void ensureCategoryAllowsActiveProduct(Category category) {
		if (!category.isActive()) {
			throw new BusinessRuleException("An active product requires an active category");
		}
	}

	private Product saveAndFlush(Product product) {
		try {
			return productRepository.saveAndFlush(product);
		} catch (DataIntegrityViolationException exception) {
			throwIfDuplicate(exception);
			throw exception;
		}
	}

	private void flushProduct() {
		try {
			productRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throwIfDuplicate(exception);
			throw exception;
		}
	}

	private static void throwIfDuplicate(DataIntegrityViolationException exception) {
		if (UniqueConstraintTranslator.isUniqueViolation(exception)) {
			throw new DuplicateResourceException("A product with that SKU already exists", exception);
		}
	}

	private static Pageable stablePageable(Pageable pageable) {
		requireRequest(pageable, "Pageable must not be null");
		Sort sort = pageable.getSort().isSorted() ? pageable.getSort().and(STABLE_SORT) : STABLE_SORT;
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

	private static String normalizeSku(String sku) {
		return sku == null ? null : sku.trim();
	}

	private static int requireStock(Integer stock) {
		if (stock == null) {
			throw new BusinessRuleException("Stock must not be null");
		}
		return stock;
	}

	private static int requireQuantity(StockAdjustmentRequest request) {
		requireRequest(request, "Stock adjustment request must not be null");
		if (request.quantity() == null) {
			throw new BusinessRuleException("Quantity must not be null");
		}
		return request.quantity();
	}

	private static void requireRequest(Object request, String message) {
		if (request == null) {
			throw new BusinessRuleException(message);
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
