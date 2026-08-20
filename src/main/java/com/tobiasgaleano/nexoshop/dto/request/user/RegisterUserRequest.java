package com.tobiasgaleano.nexoshop.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@NotBlank @Size(max = 80) String firstName,
		@NotBlank @Size(max = 80) String lastName,
		@NotBlank @Email @Size(max = 150) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
