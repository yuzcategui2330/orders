package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.Category;
import com.farmatodo.order.domains.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProductRepositorySearchTest {

	@Autowired
	private ProductRepository productRepository;

	@Test
	void returnsProductsWhoseStockIsStrictlyGreaterThanThreshold() {
		productRepository.save(Product.create("Amoxicillin", "Amox", Category.MEDICINES, 6));
		productRepository.save(Product.create("Amoxicillin Forte", "Amox F", Category.MEDICINES, 5));
		productRepository.save(Product.create("Shampoo", "Shamp", Category.HYGYENE, 20));

		Page<Product> page = productRepository.searchByNameOrShortName("amox", 5, PageRequest.of(0, 10));

		assertEquals(1, page.getTotalElements());
		assertEquals("Amoxicillin", page.getContent().getFirst().getName());
		assertEquals(6, page.getContent().getFirst().getStockQuantity());
	}

	@Test
	void matchesShortNameWhenNameDoesNotContainTheTerm() {
		productRepository.save(Product.create("Generic Tablet", "Amox", Category.MEDICINES, 9));

		Page<Product> page = productRepository.searchByNameOrShortName("amox", 0, PageRequest.of(0, 10));

		assertEquals(1, page.getTotalElements());
		assertEquals("Generic Tablet", page.getContent().getFirst().getName());
	}
}
