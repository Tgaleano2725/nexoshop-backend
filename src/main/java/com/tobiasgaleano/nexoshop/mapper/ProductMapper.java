package com.tobiasgaleano.nexoshop.mapper;

import org.springframework.stereotype.Component;

import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Product;

@Component
public class ProductMapper {

	public ProductResponse toResponse(Product product) {
		Category category = product.getCategory();
		return new ProductResponse(product.getId(), category.getId(), category.getName(), product.getSku(),
				product.getName(), product.getDescription(), product.getPrice(), product.getStock(), product.getImageUrl(),
				product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
	}
}
