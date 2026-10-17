package com.foody.delivery.auth.dto;

import com.foody.delivery.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de cliente ou vendedor. Para {@link Role#SELLER}, {@code restaurantName} e
 * {@code restaurantCategory} são obrigatórios e o restaurante é criado junto com a conta.
 */
public record RegisterRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Email @Size(max = 160) String email,
		@NotBlank @Size(min = 6, max = 72) String password,
		Role role,
		@Size(max = 120) String restaurantName,
		@Size(max = 60) String restaurantCategory) {

	public Role roleOrDefault() {
		return role == null ? Role.CUSTOMER : role;
	}

}
