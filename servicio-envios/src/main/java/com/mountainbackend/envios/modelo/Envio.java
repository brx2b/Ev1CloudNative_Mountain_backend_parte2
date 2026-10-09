package com.mountainbackend.envios.modelo;

import java.time.Instant;

/** Despacho de un pedido. Estados: PENDIENTE -> EN_PREPARACION -> EN_CAMINO -> ENTREGADO. */
public record Envio(
		String id,
		String orderId,
		String customerEmail,
		String estado,
		Instant createdAt,
		Instant updatedAt) {
}
