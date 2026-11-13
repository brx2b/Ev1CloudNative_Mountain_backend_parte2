package com.mountainbackend.envios.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountainbackend.envios.servicio.ServicioEnvios;

/** Consume pedidos.creados (topico Kafka en backend-3) y abre el despacho en PENDIENTE. */
@Component
public class ConsumidorEnvios {

	private static final Logger log = LoggerFactory.getLogger(ConsumidorEnvios.class);

	private final ServicioEnvios servicio;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ConsumidorEnvios(ServicioEnvios servicio) {
		this.servicio = servicio;
	}

	@KafkaListener(
		topics = "${app.kafka.topic.pedidos:pedidos.creados}",
		groupId = "envios-pedidos",
		containerFactory = "kafkaListenerContainerFactory")
	public void alPedidoCreado(String json) {
		try {
			JsonNode evento = objectMapper.readTree(json);
			String orderId = evento.path("orderId").asText("");
			String email = evento.path("customerEmail").asText("");
			servicio.crearDesdePedido(orderId, email);
		} catch (Exception ex) {
			log.warn("Evento pedido.creado inválido: {}", ex.getMessage());
		}
	}
}
