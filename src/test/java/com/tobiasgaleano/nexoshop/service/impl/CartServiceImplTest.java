package com.tobiasgaleano.nexoshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tobiasgaleano.nexoshop.dto.request.cart.AddCartItemRequest;
import com.tobiasgaleano.nexoshop.dto.request.cart.UpdateCartItemQuantityRequest;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.CartMapper;
import com.tobiasgaleano.nexoshop.model.entity.BaseEntity;
import com.tobiasgaleano.nexoshop.model.entity.Cart;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.model.entity.User;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;
import com.tobiasgaleano.nexoshop.repository.CartRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;
import com.tobiasgaleano.nexoshop.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CartRepository cartRepository;

	@Mock
	private ProductRepository productRepository;

	private CartServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CartServiceImpl(userRepository, cartRepository, productRepository, new CartMapper());
	}

	@Test
	void lazilyCreatesCartForActiveUser() {
		User user = user(1L);
		when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
		when(cartRepository.findLockedByUserId(1L)).thenReturn(Optional.empty());
		when(cartRepository.saveAndFlush(any(Cart.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));

		var response = service.getOrCreate(1L);

		assertThat(response.cartId()).isEqualTo(10L);
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.items()).isEmpty();
	}

	@Test
	void reportsMissingAndInactiveUsers() {
		when(userRepository.findLockedById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.getOrCreate(99L)).isInstanceOf(ResourceNotFoundException.class);

		User inactive = user(1L);
		inactive.deactivate();
		when(userRepository.findLockedById(1L)).thenReturn(Optional.of(inactive));
		assertThatThrownBy(() -> service.getOrCreate(1L))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("active");
		verify(cartRepository, never()).findLockedByUserId(any());
	}

	@Test
	void getsExistingCartWithDetailedFetch() {
		User user = user(1L);
		Cart cart = cart(user, 10L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cartRepository.findDetailedByUserId(1L)).thenReturn(Optional.of(cart));

		assertThat(service.getByUserId(1L).cartId()).isEqualTo(10L);
	}

	@Test
	void addsFirstProductWithoutChangingStockAndLocksInConsistentOrder() {
		User user = user(1L);
		Product product = product(20L, 10);
		when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
		when(cartRepository.findLockedByUserId(1L)).thenReturn(Optional.empty());
		when(cartRepository.saveAndFlush(any(Cart.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));
		when(productRepository.findById(20L)).thenReturn(Optional.of(product));

		var response = service.addProduct(1L, new AddCartItemRequest(20L, 3));

		assertThat(response.items()).singleElement().extracting(item -> item.quantity()).isEqualTo(3);
		assertThat(product.getStock()).isEqualTo(10);
		InOrder locks = inOrder(userRepository, cartRepository);
		locks.verify(userRepository).findLockedById(1L);
		locks.verify(cartRepository).findLockedByUserId(1L);
	}

	@Test
	void addsRepeatedProductToSingleItem() {
		User user = user(1L);
		Product product = product(20L, 10);
		Cart cart = detailedCart(user, product, 2);
		stubLockedCart(user, cart);
		when(productRepository.findById(20L)).thenReturn(Optional.of(product));

		var response = service.addProduct(1L, new AddCartItemRequest(20L, 3));

		assertThat(response.items()).singleElement().extracting(item -> item.quantity()).isEqualTo(5);
		assertThat(cart.getItems()).hasSize(1);
	}

	@Test
	void rejectsMissingInactiveOrUnavailableProducts() {
		User user = user(1L);
		Cart cart = cart(user, 10L);
		stubLockedCart(user, cart);
		when(productRepository.findById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.addProduct(1L, new AddCartItemRequest(99L, 1)))
				.isInstanceOf(ResourceNotFoundException.class);

		Product inactive = product(20L, 10);
		inactive.deactivate();
		when(productRepository.findById(20L)).thenReturn(Optional.of(inactive));
		assertThatThrownBy(() -> service.addProduct(1L, new AddCartItemRequest(20L, 1)))
				.isInstanceOf(BusinessRuleException.class);

		Product inactiveCategory = product(21L, 10);
		inactiveCategory.getCategory().deactivate();
		when(productRepository.findById(21L)).thenReturn(Optional.of(inactiveCategory));
		assertThatThrownBy(() -> service.addProduct(1L, new AddCartItemRequest(21L, 1)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsQuantityAboveStockAndNonPositiveQuantity() {
		User user = user(1L);
		Cart cart = cart(user, 10L);
		stubLockedCart(user, cart);
		when(productRepository.findById(20L)).thenReturn(Optional.of(product(20L, 2)));

		assertThatThrownBy(() -> service.addProduct(1L, new AddCartItemRequest(20L, 3)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("stock");
		assertThatThrownBy(() -> service.addProduct(1L, new AddCartItemRequest(20L, 0)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void setsFinalQuantityAndRejectsMissingItem() {
		User user = user(1L);
		Product product = product(20L, 10);
		Cart cart = detailedCart(user, product, 2);
		stubLockedCart(user, cart);
		when(productRepository.findById(20L)).thenReturn(Optional.of(product));

		assertThat(service.setProductQuantity(1L, 20L, new UpdateCartItemQuantityRequest(7))
				.items().getFirst().quantity()).isEqualTo(7);

		Product absent = product(21L, 10);
		when(productRepository.findById(21L)).thenReturn(Optional.of(absent));
		assertThatThrownBy(() -> service.setProductQuantity(1L, 21L, new UpdateCartItemQuantityRequest(1)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not present");
	}

	@Test
	void removesAndClearsItemsThroughAggregate() {
		User user = user(1L);
		Product product = product(20L, 10);
		Cart cart = detailedCart(user, product, 2);
		stubLockedCart(user, cart);
		when(productRepository.findById(20L)).thenReturn(Optional.of(product));

		assertThat(service.removeProduct(1L, 20L).items()).isEmpty();

		cart.addProduct(product, 1);
		assertThat(service.clear(1L).items()).isEmpty();
	}

	private void stubLockedCart(User user, Cart cart) {
		when(userRepository.findLockedById(user.getId())).thenReturn(Optional.of(user));
		when(cartRepository.findLockedByUserId(user.getId())).thenReturn(Optional.of(cart));
		when(cartRepository.findDetailedById(cart.getId())).thenReturn(Optional.of(cart));
	}

	private static Cart detailedCart(User user, Product product, int quantity) {
		Cart cart = cart(user, 10L);
		cart.addProduct(product, quantity);
		return cart;
	}

	private static Cart cart(User user, Long id) {
		return withId(new Cart(user), id);
	}

	private static User user(Long id) {
		return withId(new User("Tobias", "Galeano", "user@example.com",
				PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53)), UserRole.CUSTOMER), id);
	}

	private static Product product(Long id, int stock) {
		return withId(new Product(new Category("Electronics", null), "SKU-1", "Keyboard", null,
				new BigDecimal("25.50"), stock, null), id);
	}

	private static <T extends BaseEntity> T withId(T entity, Long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
