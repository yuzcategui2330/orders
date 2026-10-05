package com.farmatodo.order.controller;

import com.farmatodo.order.domains.request.AddItemRequest;
import com.farmatodo.order.domains.request.CreateOrderRequest;
import com.farmatodo.order.domains.request.PayOrderRequest;
import com.farmatodo.order.domains.request.UpdateItemRequest;
import com.farmatodo.order.domains.response.OrderResponse;
import com.farmatodo.order.domains.response.PaymentResponse;
import com.farmatodo.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
	}

	@GetMapping
	public ResponseEntity<List<OrderResponse>> list(@RequestParam(name = "status", required = false) String status) {
		return ResponseEntity.ok(orderService.list(status));
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderResponse> findById(@PathVariable("id") Long id) {
		return ResponseEntity.ok(orderService.findById(id));
	}

	@PostMapping("/{id}/items")
	public ResponseEntity<OrderResponse> addItem(@PathVariable("id") Long id, @RequestBody AddItemRequest request) {
		return ResponseEntity.ok(orderService.addItem(id, request));
	}

	@PutMapping("/{id}/items")
	public ResponseEntity<OrderResponse> updateItem(@PathVariable("id") Long id, @RequestBody UpdateItemRequest request) {
		return ResponseEntity.ok(orderService.updateItem(id, request));
	}

	@DeleteMapping("/{id}/items/{productId}")
	public ResponseEntity<OrderResponse> removeItem(
			@PathVariable("id") Long id,
			@PathVariable("productId") Long productId) {
		return ResponseEntity.ok(orderService.removeItem(id, productId));
	}

	@PostMapping("/{id}/pay")
	public ResponseEntity<PaymentResponse> pay(@PathVariable("id") Long id, @RequestBody PayOrderRequest request) {
		return ResponseEntity.ok(orderService.pay(id, request));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<OrderResponse> cancel(@PathVariable("id") Long id) {
		return ResponseEntity.ok(orderService.cancel(id));
	}
}
