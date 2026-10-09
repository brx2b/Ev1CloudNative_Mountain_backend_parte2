package com.mountainbackend.notificaciones.dto;

/** Petición directa POST /notifications/enviar (para Postman y el frontend). */
public record SolicitudNotificacion(
		String to,
		String subject,
		String message) {
}
