package com.farmatodo.order.controller;

import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.request.MakeRegistrationRequest;
import com.farmatodo.order.services.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

	private final ClientService clientService;

	@PostMapping("/make-registration")
	public ResponseEntity<Client> makeRegistration(@RequestBody MakeRegistrationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clientService.makeRegistration(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Client> update(@PathVariable("id") Long id, @RequestBody Client client) {
		return ResponseEntity.ok(clientService.update(id, client));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Client> findById(@PathVariable("id") Long id) {
		return ResponseEntity.ok(clientService.findById(id));
	}

	@GetMapping
	public ResponseEntity<List<Client>> list() {
		return ResponseEntity.ok(clientService.list());
	}
}
