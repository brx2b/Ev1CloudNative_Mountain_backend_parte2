package com.mountainbackend.carrito.modelo;

import java.time.Instant;
import java.util.List;

/** Carrito persistente (en memoria) de un usuario. */
public record Carrito(
		String email,
		List<ItemCarrito> items,
		int subtotal,
		Instant updatedAt) {
}
