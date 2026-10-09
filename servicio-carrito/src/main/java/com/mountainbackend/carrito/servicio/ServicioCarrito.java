package com.mountainbackend.carrito.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.mountainbackend.carrito.dto.SolicitudItem;
import com.mountainbackend.carrito.modelo.Carrito;
import com.mountainbackend.carrito.modelo.ItemCarrito;

/** Carrito por usuario (email del JWT) en memoria. */
@Service
public class ServicioCarrito {

	private final Map<String, Map<Long, ItemCarrito>> carritos = new ConcurrentHashMap<>();

	public Carrito obtener(String email) {
		return armar(email);
	}

	public Carrito agregar(String email, SolicitudItem solicitud) {
		Long idProducto = solicitud.productId() != null ? solicitud.productId() : solicitud.id();
		if (idProducto == null || idProducto <= 0) {
			throw new IllegalArgumentException("El item debe incluir 'productId' válido.");
		}
		int cantidad = solicitud.quantity() != null ? solicitud.quantity() : 1;
		if (cantidad < 1) {
			throw new IllegalArgumentException("El campo 'quantity' debe ser mayor o igual a 1.");
		}
		carritos.computeIfAbsent(email, k -> new ConcurrentHashMap<>())
			.merge(idProducto,
				new ItemCarrito(idProducto,
					solicitud.name() != null ? solicitud.name() : "",
					solicitud.price() != null ? solicitud.price() : 0,
					cantidad,
					solicitud.image()),
				(anterior, nuevo) -> new ItemCarrito(anterior.productId(), anterior.name(),
					nuevo.price() > 0 ? nuevo.price() : anterior.price(),
					anterior.quantity() + nuevo.quantity(),
					nuevo.image() != null ? nuevo.image() : anterior.image()));
		return armar(email);
	}

	public Carrito actualizarCantidad(String email, long productId, int quantity) {
		Map<Long, ItemCarrito> carrito = carritos.get(email);
		if (carrito == null || !carrito.containsKey(productId)) {
			throw new IllegalArgumentException("El producto no está en el carrito.");
		}
		if (quantity <= 0) {
			carrito.remove(productId);
		} else {
			ItemCarrito anterior = carrito.get(productId);
			carrito.put(productId, new ItemCarrito(anterior.productId(), anterior.name(),
				anterior.price(), quantity, anterior.image()));
		}
		return armar(email);
	}

	public Carrito quitar(String email, long productId) {
		Map<Long, ItemCarrito> carrito = carritos.get(email);
		if (carrito != null) {
			carrito.remove(productId);
		}
		return armar(email);
	}

	public void vaciar(String email) {
		carritos.remove(email);
	}

	private Carrito armar(String email) {
		List<ItemCarrito> items = new ArrayList<>(
			carritos.getOrDefault(email, Map.of()).values());
		int subtotal = items.stream().mapToInt(i -> i.price() * i.quantity()).sum();
		return new Carrito(email, List.copyOf(items), subtotal, Instant.now());
	}
}
