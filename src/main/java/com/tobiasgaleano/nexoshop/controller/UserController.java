package com.tobiasgaleano.nexoshop.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.tobiasgaleano.nexoshop.dto.request.user.RegisterUserRequest;
import com.tobiasgaleano.nexoshop.dto.response.user.UserResponse;
import com.tobiasgaleano.nexoshop.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController @Validated @RequestMapping("/api/v1/users")
public class UserController {
	private final UserService service;
	public UserController(UserService service) { this.service = service; }
	@PostMapping
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request, UriComponentsBuilder uri) {
		UserResponse response = service.register(request);
		return ResponseEntity.created(uri.path("/api/v1/users/{id}").build(response.id())).body(response);
	}
	@GetMapping("/{id}")
	public UserResponse get(@PathVariable @Positive Long id) { return service.getById(id); }
}
