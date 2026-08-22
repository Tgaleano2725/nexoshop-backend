package com.tobiasgaleano.nexoshop.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import com.tobiasgaleano.nexoshop.dto.request.product.*;
import com.tobiasgaleano.nexoshop.dto.response.PageResponse;
import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;
import com.tobiasgaleano.nexoshop.service.ProductService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController @Validated @RequestMapping("/api/v1/products")
public class ProductController {
	private final ProductService service;
	public ProductController(ProductService service) { this.service = service; }
	@PostMapping public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest r, UriComponentsBuilder u) {
		ProductResponse x = service.create(r); return ResponseEntity.created(u.path("/api/v1/products/{id}").build(x.id())).body(x);
	}
	@PutMapping("/{id}") public ProductResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateProductRequest r) { return service.update(id, r); }
	@GetMapping("/{id}") public ProductResponse get(@PathVariable @Positive Long id) { return service.getById(id); }
	@GetMapping("/sku/{sku}") public ProductResponse getSku(@PathVariable String sku) { return service.getBySku(sku); }
	@GetMapping public PageResponse<ProductResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly,
			@RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="20") @Positive int size) {
		var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
		return activeOnly ? service.getActive(pageable) : service.getAll(pageable);
	}
	@PostMapping("/{id}/activate") public ProductResponse activate(@PathVariable @Positive Long id) { return service.activate(id); }
	@PostMapping("/{id}/deactivate") public ProductResponse deactivate(@PathVariable @Positive Long id) { return service.deactivate(id); }
	@PostMapping("/{id}/stock/increase") public ProductResponse increase(@PathVariable @Positive Long id, @Valid @RequestBody StockAdjustmentRequest r) { return service.increaseStock(id, r); }
	@PostMapping("/{id}/stock/decrease") public ProductResponse decrease(@PathVariable @Positive Long id, @Valid @RequestBody StockAdjustmentRequest r) { return service.decreaseStock(id, r); }
}
