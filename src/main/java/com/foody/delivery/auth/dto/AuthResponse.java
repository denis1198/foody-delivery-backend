package com.foody.delivery.auth.dto;

import com.foody.delivery.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
