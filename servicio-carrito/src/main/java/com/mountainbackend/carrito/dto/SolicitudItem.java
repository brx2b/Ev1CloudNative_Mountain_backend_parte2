package com.mountainbackend.carrito.dto;

/** Agregar/actualizar item: acepta productId o id (como pedidos). */
public record SolicitudItem(
		Long productId,
		Long id,
		String name,
		Integer price,
		Integer quantity,
		String image) {
}
