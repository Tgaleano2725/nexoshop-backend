package com.tobiasgaleano.nexoshop.service;

import com.tobiasgaleano.nexoshop.dto.request.user.RegisterUserRequest;
import com.tobiasgaleano.nexoshop.dto.response.user.UserResponse;

public interface UserService {
	UserResponse register(RegisterUserRequest request);
	UserResponse getById(Long id);
}
