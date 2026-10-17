package com.foody.delivery.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DeliveryAddress {

	@Column(nullable = false)
	private String street;

	@Column(nullable = false, length = 20)
	private String number;

	private String complement;

	@Column(nullable = false)
	private String neighborhood;

	@Column(nullable = false)
	private String city;

	@Column(nullable = false, length = 20)
	private String zipCode;

	protected DeliveryAddress() {
	}

	public DeliveryAddress(String street, String number, String complement, String neighborhood, String city,
			String zipCode) {
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.neighborhood = neighborhood;
		this.city = city;
		this.zipCode = zipCode;
	}

	public String getStreet() {
		return street;
	}

	public String getNumber() {
		return number;
	}

	public String getComplement() {
		return complement;
	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public String getCity() {
		return city;
	}

	public String getZipCode() {
		return zipCode;
	}

}
