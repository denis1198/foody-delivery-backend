package com.foody.delivery.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.auth.dto.AuthResponse;
import com.foody.delivery.auth.dto.LoginRequest;
import com.foody.delivery.auth.dto.RegisterRequest;
import com.foody.delivery.common.ConflictException;
import com.foody.delivery.security.JwtService;
import com.foody.delivery.user.User;
import com.foody.delivery.user.UserRepository;
import com.foody.delivery.user.UserResponse;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("Já existe um usuário cadastrado com este e-mail.");
		}
		User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
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

}
