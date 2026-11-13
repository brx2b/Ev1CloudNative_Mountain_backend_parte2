package com.mountainbackend.notificaciones.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountainbackend.notificaciones.servicio.ServicioNotificaciones;

/** Consume pedidos.creados (topico Kafka en backend-3) y notifica al cliente por correo (o simulado). */
@Component
public class ConsumidorNotificaciones {

	private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificaciones.class);

	private final ServicioNotificaciones servicio;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ConsumidorNotificaciones(ServicioNotificaciones servicio) {
		this.servicio = servicio;
	}

	@KafkaListener(
		topics = "${app.kafka.topic.pedidos:pedidos.creados}",
		groupId = "notificaciones-pedidos",
		containerFactory = "kafkaListenerContainerFactory")
	public void alPedidoCreado(String json) {
		try {
			JsonNode evento = objectMapper.readTree(json);
			String orderId = evento.path("orderId").asText("?");
			String email = evento.path("customerEmail").asText("");
			String nombre = evento.path("customerName").asText("");
			int total = evento.path("total").asInt(0);
			java.util.List<ServicioNotificaciones.ItemNotificado> items = new java.util.ArrayList<>();
			JsonNode lineas = evento.path("items");
			if (lineas.isArray()) {
				for (JsonNode linea : lineas) {
					items.add(new ServicioNotificaciones.ItemNotificado(
						linea.path("name").asText("Producto"),
						linea.path("quantity").asInt(1),
						linea.path("price").asInt(0)));
				}
			}
			servicio.notificarPedido(orderId, email, nombre, total, items);
			log.info("Notificación procesada para {}", orderId);
		} catch (Exception ex) {
			log.warn("Evento pedido.creado inválido: {}", ex.getMessage());
		}
	}
}
