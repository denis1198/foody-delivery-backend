package com.foody.delivery.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.auth.dto.AuthResponse;
import com.foody.delivery.auth.dto.LoginRequest;
import com.foody.delivery.auth.dto.RegisterRequest;
import com.foody.delivery.common.BadRequestException;
import com.foody.delivery.common.ConflictException;
import com.foody.delivery.restaurant.RestaurantService;
import com.foody.delivery.security.JwtService;
import com.foody.delivery.user.Role;
import com.foody.delivery.user.User;
import com.foody.delivery.user.UserRepository;
import com.foody.delivery.user.UserResponse;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RestaurantService restaurantService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			RestaurantService restaurantService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.restaurantService = restaurantService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("Já existe um usuário cadastrado com este e-mail.");
		}
		Role role = request.roleOrDefault();
		if (role == Role.SELLER) {
			if (isBlank(request.restaurantName())) {
				throw new BadRequestException("restaurantName", "Informe o nome do restaurante.");
			}
			if (isBlank(request.restaurantCategory())) {
				throw new BadRequestException("restaurantCategory", "Informe a categoria do restaurante.");
			}
		}
		User user = users.save(
				new User(request.name().trim(), email, passwordEncoder.encode(request.password()), role));
		if (role == Role.SELLER) {
			restaurantService.createFor(user, request.restaurantName(), request.restaurantCategory());
		}
		return toResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		User user = users.findByEmailIgnoreCase(request.email().trim())
				.filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
				.orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));
		return toResponse(user);
	}

	private AuthResponse toResponse(User user) {
		return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
