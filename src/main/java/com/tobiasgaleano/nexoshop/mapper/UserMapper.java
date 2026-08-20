package com.tobiasgaleano.nexoshop.mapper;

import org.springframework.stereotype.Component;

import com.tobiasgaleano.nexoshop.dto.response.user.UserResponse;
import com.tobiasgaleano.nexoshop.model.entity.User;

@Component
public class UserMapper {
	public UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
				user.getRole(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
	}
}
