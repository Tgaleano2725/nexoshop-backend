package com.tobiasgaleano.nexoshop.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import com.tobiasgaleano.nexoshop.dto.request.category.*;
import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;
import com.tobiasgaleano.nexoshop.service.CategoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController @Validated @RequestMapping("/api/v1/categories")
public class CategoryController {
	private final CategoryService service;
	public CategoryController(CategoryService service) { this.service = service; }
	@PostMapping public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest r, UriComponentsBuilder u) {
		CategoryResponse x = service.create(r); return ResponseEntity.created(u.path("/api/v1/categories/{id}").build(x.id())).body(x);
	}
	@PutMapping("/{id}") public CategoryResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateCategoryRequest r) { return service.update(id, r); }
	@GetMapping("/{id}") public CategoryResponse get(@PathVariable @Positive Long id) { return service.getById(id); }
	@GetMapping public List<CategoryResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
		return activeOnly ? service.getActive() : service.getAll();
	}
	@PostMapping("/{id}/activate") public CategoryResponse activate(@PathVariable @Positive Long id) { return service.activate(id); }
	@PostMapping("/{id}/deactivate") public CategoryResponse deactivate(@PathVariable @Positive Long id) { return service.deactivate(id); }
}
