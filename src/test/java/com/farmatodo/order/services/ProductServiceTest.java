package com.farmatodo.order.services;

import com.farmatodo.order.config.ProductSearchProperties;
import com.farmatodo.order.domains.Category;
import com.farmatodo.order.domains.Product;
import com.farmatodo.order.domains.ProductSearchPage;
import com.farmatodo.order.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private SearchLogService searchLogService;

	private ProductService productService;

	@BeforeEach
	void setUp() {
		productService = new ProductService(productRepository, searchLogService, new ProductSearchProperties(5));
	}

	@Test
	void searchKeepsOnlyStockAboveConfiguredThreshold() {
		when(productRepository.searchByNameOrShortName(eq("amox"), eq(5), any(Pageable.class)))
				.thenReturn(new PageImpl<>(
						List.of(Product.create("Amoxicillin", "Amox", Category.MEDICINES, 6)),
						PageRequest.of(0, 20),
						1));

		ProductSearchPage page = productService.search("amox", null, null);

		ArgumentCaptor<Integer> minStock = ArgumentCaptor.forClass(Integer.class);
		verify(productRepository).searchByNameOrShortName(eq("amox"), minStock.capture(), any(Pageable.class));
		assertEquals(5, minStock.getValue());
		assertEquals(1, page.totalElements());
		verify(searchLogService).record("amox", 1L);
	}

	@Test
	void searchReturnsResultsWhenAuditLogFails() {
		when(productRepository.searchByNameOrShortName(eq("amox"), eq(5), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(Product.create("Amoxicillin", "Amox", Category.MEDICINES, 8))));
		doThrow(new RuntimeException("queue full")).when(searchLogService).record(any(), anyLong());

		ProductSearchPage page = assertDoesNotThrow(() -> productService.search("amox", 0, 10));

		assertEquals(1, page.products().size());
	}
}
