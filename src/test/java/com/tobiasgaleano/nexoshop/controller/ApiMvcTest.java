package com.tobiasgaleano.nexoshop.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tobiasgaleano.nexoshop.config.WebConfig;
import com.tobiasgaleano.nexoshop.dto.response.PageResponse;
import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;
import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;
import com.tobiasgaleano.nexoshop.dto.response.user.UserResponse;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.service.CartService;
import com.tobiasgaleano.nexoshop.service.CategoryService;
import com.tobiasgaleano.nexoshop.service.OrderService;
import com.tobiasgaleano.nexoshop.service.ProductService;
import com.tobiasgaleano.nexoshop.service.UserService;

@SpringBootTest(properties = {"spring.profiles.active=test"})
class ApiMvcTest {
	@Autowired WebApplicationContext context;
	MockMvc mvc;
	@MockitoBean UserService users;
	@MockitoBean CategoryService categories;
	@MockitoBean ProductService products;
	@MockitoBean CartService carts;
	@MockitoBean OrderService orders;
	@org.junit.jupiter.api.BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

	@Test void registersWithoutExposingPasswordAndSetsLocation() throws Exception {
		when(users.register(any())).thenReturn(new UserResponse(7L, "Tobias", "Galeano", "user@example.com", UserRole.CUSTOMER, true, Instant.now(), Instant.now()));
		mvc.perform(post("/api/v1/users").contentType("application/json").content("{\"firstName\":\"Tobias\",\"lastName\":\"Galeano\",\"email\":\"USER@example.com\",\"password\":\"secret123\"}"))
				.andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/v1/users/7"))
				.andExpect(jsonPath("$.email").value("user@example.com")).andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test void validatesBadJsonAndReturnsUniformError() throws Exception {
		mvc.perform(post("/api/v1/users").contentType("application/json").content("{\"email\":"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.path").value("/api/v1/users"));
		mvc.perform(post("/api/v1/users").contentType("application/json").content("{\"firstName\":\"\",\"lastName\":\"x\",\"email\":\"x\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test void mapsNotFoundAndDuplicateToContractCodes() throws Exception {
		when(categories.getById(99L)).thenThrow(new ResourceNotFoundException("Category", 99L));
		mvc.perform(get("/api/v1/categories/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
		when(categories.create(any())).thenThrow(new DuplicateResourceException("duplicate"));
		mvc.perform(post("/api/v1/categories").contentType("application/json").content("{\"name\":\"Books\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
	}

	@Test void exposesRepresentativeCategoryProductAndCorsResponses() throws Exception {
		when(categories.getAll()).thenReturn(List.of(new CategoryResponse(1L, "Books", null, true, Instant.now(), Instant.now())));
		when(products.getAll(any())).thenReturn(new PageResponse<>(List.of(new ProductResponse(2L, 1L, "Books", "SKU-1", "Book", null, java.math.BigDecimal.TEN, 3, null, true, Instant.now(), Instant.now())), 0, 20, 1, 1, true, true));
		mvc.perform(get("/api/v1/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Books"));
		mvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].sku").value("SKU-1"));
		mvc.perform(options("/api/v1/categories").header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
		mvc.perform(options("/api/v1/categories").header("Origin", "http://evil.example").header("Access-Control-Request-Method", "GET"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
