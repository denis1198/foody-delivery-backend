package com.foody.delivery.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
		@NotBlank @Size(max = 255) String street,
		@NotBlank @Size(max = 20) String number,
		@Size(max = 255) String complement,
		@NotBlank @Size(max = 255) String neighborhood,
		@NotBlank @Size(max = 255) String city,
		@NotBlank @Size(max = 20) String zipCode) {
}
