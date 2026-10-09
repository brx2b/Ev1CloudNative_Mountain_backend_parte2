package com.mountainbackend.notificaciones.controlador;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.notificaciones.dto.SolicitudNotificacion;
import com.mountainbackend.notificaciones.modelo.Notificacion;
import com.mountainbackend.notificaciones.servicio.ServicioNotificaciones;

/**
 * POST /notifications/enviar -> envío directo (probable desde Postman).
 * GET  /notifications         -> historial de enviadas/simuladas.
 * Protegido por el JWT Authorizer del API Gateway + filtro propio.
 */
@RestController
@RequestMapping("/notifications")
public class ControladorNotificaciones {

	private final ServicioNotificaciones servicio;

	public ControladorNotificaciones(ServicioNotificaciones servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/enviar")
	public ResponseEntity<Notificacion> enviar(@RequestBody SolicitudNotificacion solicitud) {
		Notificacion notif = servicio.enviar(solicitud);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/notifications/" + notif.id()))
			.body(notif);
	}

	@GetMapping
	public List<Notificacion> listar() {
		return servicio.listar();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeArgumento(IllegalArgumentException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 400);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.badRequest().body(cuerpo);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeEnvio(IllegalStateException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 502);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(cuerpo);
	}
}
