package com.mountainbackend.envios.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mountainbackend.envios.modelo.Envio;

/** Registro de despachos en memoria (MS nuevo a elección, rúbrica). */
@Service
public class ServicioEnvios {

	private static final Logger log = LoggerFactory.getLogger(ServicioEnvios.class);
	private static final Set<String> ESTADOS = Set.of("PENDIENTE", "EN_PREPARACION", "EN_CAMINO", "ENTREGADO");

	private final Map<String, Envio> envios = new ConcurrentHashMap<>();
	private final Map<String, String> porPedido = new ConcurrentHashMap<>();
	private final AtomicLong secuencia = new AtomicLong(5000);

	public Envio crearDesdePedido(String orderId, String customerEmail) {
		if (orderId == null || orderId.isBlank()) {
			throw new IllegalArgumentException("El orderId es obligatorio.");
		}
		String existente = porPedido.get(orderId);
		if (existente != null) {
			return envios.get(existente);
		}
		String id = "ENV-" + secuencia.getAndIncrement();
		Instant ahora = Instant.now();
		Envio envio = new Envio(id, orderId, customerEmail, "PENDIENTE", ahora, ahora);
		envios.put(id, envio);
		porPedido.put(orderId, id);
		log.info("Envío {} creado para pedido {}", id, orderId);
		return envio;
	}

	public List<Envio> listar() {
		return new ArrayList<>(envios.values());
	}

	public Optional<Envio> buscarPorId(String id) {
		return Optional.ofNullable(envios.get(id));
	}

	public Envio avanzarEstado(String id, String estado) {
		Envio actual = envios.get(id);
		if (actual == null) {
			throw new IllegalArgumentException("Envío no encontrado: " + id);
		}
		String nuevo = estado != null ? estado.trim().toUpperCase() : "";
		if (!ESTADOS.contains(nuevo)) {
			throw new IllegalArgumentException("Estado inválido. Válidos: " + ESTADOS);
		}
		Envio actualizado = new Envio(actual.id(), actual.orderId(), actual.customerEmail(),
			nuevo, actual.createdAt(), Instant.now());
		envios.put(id, actualizado);
		return actualizado;
	}
}
