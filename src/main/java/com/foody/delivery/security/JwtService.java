package com.foody.delivery.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.foody.delivery.user.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("name", user.getName())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(properties.expiration())))
				.signWith(key)
				.compact();
	}

	/** Retorna o e-mail (subject) se o token for válido e não estiver expirado. */
	public Optional<String> extractSubject(String token) {
		try {
			return Optional.ofNullable(
					Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}
