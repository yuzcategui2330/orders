package com.farmatodo.order.controller;

import com.farmatodo.order.domains.ProductSearchPage;
import com.farmatodo.order.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@GetMapping("/search")
	public ResponseEntity<ProductSearchPage> search(
			@RequestParam("query") String query,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size) {
		return ResponseEntity.ok(productService.search(query, page, size));
	}
}
