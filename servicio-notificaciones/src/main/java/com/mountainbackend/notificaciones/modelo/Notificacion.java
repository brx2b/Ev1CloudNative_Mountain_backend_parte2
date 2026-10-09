package com.mountainbackend.notificaciones.modelo;

import java.time.Instant;

/** Notificación registrada (en memoria). estado: ENVIADA o SIMULADA. */
public record Notificacion(
		String id,
		String to,
		String subject,
		String message,
		String estado,
		Instant createdAt) {
}
