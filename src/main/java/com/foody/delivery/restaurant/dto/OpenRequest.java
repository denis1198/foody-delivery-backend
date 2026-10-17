package com.foody.delivery.restaurant.dto;

import jakarta.validation.constraints.NotNull;

public record OpenRequest(@NotNull Boolean open) {
}
