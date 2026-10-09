package com.mountainbackend.carrito.modelo;

/** Item del carrito (contrato en inglés como el frontend). */
public record ItemCarrito(
		long productId,
		String name,
		int price,
		int quantity) {
}
