package com.tobiasgaleano.nexoshop.service.impl;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tobiasgaleano.nexoshop.dto.request.user.RegisterUserRequest;
import com.tobiasgaleano.nexoshop.dto.response.user.UserResponse;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.UserMapper;
import com.tobiasgaleano.nexoshop.model.entity.User;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;
import com.tobiasgaleano.nexoshop.repository.UserRepository;
import com.tobiasgaleano.nexoshop.service.UserService;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
	private final UserRepository repository;
	private final UserMapper mapper;
	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	public UserServiceImpl(UserRepository repository, UserMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public UserResponse register(RegisterUserRequest request) {
		if (request == null) throw new BusinessRuleException("Register request must not be null");
		if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
			throw new BusinessRuleException("Password exceeds BCrypt's 72-byte limit");
		String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
		if (repository.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("Email already exists");
		User user = new User(request.firstName(), request.lastName(), email,
				PasswordHash.fromEncoded(encoder.encode(request.password())), UserRole.CUSTOMER);
		try { return mapper.toResponse(repository.saveAndFlush(user)); }
		catch (DataIntegrityViolationException exception) { throw new DuplicateResourceException("Email already exists", exception); }
	}

	@Override
	public UserResponse getById(Long id) {
		return repository.findById(id).map(mapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
	}
}
