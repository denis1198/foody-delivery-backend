package com.foody.delivery.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Datas de negócio (dia, hora, dia da semana) sempre no fuso de Brasília. */
public final class BusinessTime {

	public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private BusinessTime() {
	}

	public static LocalDate today() {
		return LocalDate.now(ZONE);
	}

	public static Instant startOf(LocalDate day) {
		return day.atStartOfDay(ZONE).toInstant();
	}

	public static ZonedDateTime local(Instant instant) {
		return instant.atZone(ZONE);
	}

}
