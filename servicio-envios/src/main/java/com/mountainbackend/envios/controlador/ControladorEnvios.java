package com.mountainbackend.envios.controlador;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.envios.dto.SolicitudEstado;
import com.mountainbackend.envios.modelo.Envio;
import com.mountainbackend.envios.servicio.ServicioEnvios;

/**
 * GET   /shipments        -> lista de despachos
 * GET   /shipments/{id}   -> detalle
 * PATCH /shipments/{id}   -> cambia estado {estado: EN_PREPARACION|EN_CAMINO|ENTREGADO}
 * Protegido por el JWT Authorizer del API Gateway + filtro propio.
 */
@RestController
@RequestMapping("/shipments")
public class ControladorEnvios {

	private final ServicioEnvios servicio;

	public ControladorEnvios(ServicioEnvios servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public List<Envio> listar() {
		return servicio.listar();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Envio> obtenerPorId(@PathVariable("id") String id) {
		return servicio.buscarPorId(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PatchMapping("/{id}")
	public ResponseEntity<Envio> cambiarEstado(@PathVariable("id") String id,
			@RequestBody SolicitudEstado solicitud) {
		return ResponseEntity.ok(servicio.avanzarEstado(id,
			solicitud != null ? solicitud.estado() : null));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeArgumento(IllegalArgumentException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", ex.getMessage().startsWith("Envío no encontrado") ? 404 : 400);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.status((int) cuerpo.get("status")).body(cuerpo);
	}
}
