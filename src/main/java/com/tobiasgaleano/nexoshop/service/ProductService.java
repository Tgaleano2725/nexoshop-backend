package com.tobiasgaleano.nexoshop.service;

import org.springframework.data.domain.Pageable;

import com.tobiasgaleano.nexoshop.dto.request.product.CreateProductRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.StockAdjustmentRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.UpdateProductRequest;
import com.tobiasgaleano.nexoshop.dto.response.PageResponse;
import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;

public interface ProductService {

	ProductResponse create(CreateProductRequest request);

	ProductResponse update(Long id, UpdateProductRequest request);

	ProductResponse getById(Long id);

	ProductResponse getBySku(String sku);

	PageResponse<ProductResponse> getAll(Pageable pageable);

	PageResponse<ProductResponse> getActive(Pageable pageable);

	ProductResponse activate(Long id);

	ProductResponse deactivate(Long id);

	ProductResponse increaseStock(Long id, StockAdjustmentRequest request);

	ProductResponse decreaseStock(Long id, StockAdjustmentRequest request);
}
