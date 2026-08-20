package com.tobiasgaleano.nexoshop.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tobiasgaleano.nexoshop.dto.request.cart.AddCartItemRequest;
import com.tobiasgaleano.nexoshop.dto.request.cart.UpdateCartItemQuantityRequest;
import com.tobiasgaleano.nexoshop.dto.response.cart.CartResponse;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.CartMapper;
import com.tobiasgaleano.nexoshop.model.entity.Cart;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.model.entity.User;
import com.tobiasgaleano.nexoshop.repository.CartRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;
import com.tobiasgaleano.nexoshop.repository.UserRepository;
import com.tobiasgaleano.nexoshop.service.CartService;

@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

	private final UserRepository userRepository;
	private final CartRepository cartRepository;
	private final ProductRepository productRepository;
	private final CartMapper cartMapper;

	public CartServiceImpl(UserRepository userRepository, CartRepository cartRepository,
			ProductRepository productRepository, CartMapper cartMapper) {
		this.userRepository = userRepository;
		this.cartRepository = cartRepository;
		this.productRepository = productRepository;
		this.cartMapper = cartMapper;
	}

	@Override
	@Transactional
	public CartResponse getOrCreate(Long userId) {
		User user = lockActiveUser(userId);
		Cart cart = lockOrCreateCart(user);
		return mapCart(cart);
	}

	@Override
	public CartResponse getByUserId(Long userId) {
		User user = findActiveUser(userId);
		Cart cart = cartRepository.findDetailedByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cart for user", userId));
		return mapCart(cart);
	}

	@Override
	@Transactional
	public CartResponse addProduct(Long userId, AddCartItemRequest request) {
		requireRequest(request, "Add cart item request must not be null");
		User user = lockActiveUser(userId);
		Cart cart = lockOrCreateCart(user);
		Product product = findAvailableProduct(request.productId());
		int quantity = requireQuantity(request.quantity());
		int finalQuantity = performDomainAction(() -> Math.addExact(cart.quantityOf(product), quantity));
		ensureStock(product, finalQuantity);
		performDomainAction(() -> cart.addProduct(product, quantity));
		flushCart();
		return mapCart(cart);
	}

	@Override
	@Transactional
	public CartResponse setProductQuantity(Long userId, Long productId, UpdateCartItemQuantityRequest request) {
		requireRequest(request, "Update cart item request must not be null");
		lockActiveUser(userId);
		Cart cart = lockExistingCart(userId);
		Product product = findAvailableProduct(productId);
		int quantity = requireQuantity(request.quantity());
		ensureStock(product, quantity);
		performDomainAction(() -> {
			cart.changeQuantity(product, quantity);
			return cart;
		});
		flushCart();
		return mapCart(cart);
	}

	@Override
	@Transactional
	public CartResponse removeProduct(Long userId, Long productId) {
		lockActiveUser(userId);
		Cart cart = lockExistingCart(userId);
		Product product = findProduct(productId);
		performDomainAction(() -> {
			cart.removeProduct(product);
			return cart;
		});
		flushCart();
		return mapCart(cart);
	}

	@Override
	@Transactional
	public CartResponse clear(Long userId) {
		lockActiveUser(userId);
		Cart cart = lockExistingCart(userId);
		cart.clear();
		flushCart();
		return mapCart(cart);
	}

	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		ensureActiveUser(user);
		return user;
	}

	private User lockActiveUser(Long userId) {
		User user = userRepository.findLockedById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		ensureActiveUser(user);
		return user;
	}

	private Cart lockOrCreateCart(User user) {
		return cartRepository.findLockedByUserId(user.getId())
				.map(this::loadDetailedCart)
				.orElseGet(() -> saveAndFlush(new Cart(user)));
	}

	private Cart lockExistingCart(Long userId) {
		Cart locked = cartRepository.findLockedByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart for user", userId));
		return loadDetailedCart(locked);
	}

	private Cart loadDetailedCart(Cart cart) {
		return cartRepository.findDetailedById(cart.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cart", cart.getId()));
	}

	private Product findAvailableProduct(Long productId) {
		Product product = findProduct(productId);
		if (!product.isActive()) {
			throw new BusinessRuleException("Product must be active");
		}
		if (!product.getCategory().isActive()) {
			throw new BusinessRuleException("Product category must be active");
		}
		return product;
	}

	private Product findProduct(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", productId));
	}

	private Cart saveAndFlush(Cart cart) {
		try {
			return cartRepository.saveAndFlush(cart);
		} catch (DataIntegrityViolationException exception) {
			throwIfDuplicate(exception);
			throw exception;
		}
	}

	private void flushCart() {
		try {
			cartRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throwIfDuplicate(exception);
			throw exception;
		}
	}

	private CartResponse mapCart(Cart cart) {
		return performDomainAction(() -> cartMapper.toResponse(cart));
	}

	private static void ensureActiveUser(User user) {
		if (!user.isActive()) {
			throw new BusinessRuleException("User must be active");
		}
	}

	private static void ensureStock(Product product, int quantity) {
		if (quantity > product.getStock()) {
			throw new BusinessRuleException("Requested quantity exceeds current stock");
		}
	}

	private static int requireQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new BusinessRuleException("Quantity must be greater than zero");
		}
		return quantity;
	}

	private static void requireRequest(Object request, String message) {
		if (request == null) {
			throw new BusinessRuleException(message);
		}
	}

	private static void throwIfDuplicate(DataIntegrityViolationException exception) {
		if (UniqueConstraintTranslator.isUniqueViolation(exception)) {
			throw new DuplicateResourceException("Cart uniqueness conflict", exception);
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
