package com.mountainbackend.notificaciones.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountainbackend.notificaciones.servicio.ServicioNotificaciones;

/** Consume pedido.creado y notifica al cliente por correo (o simulado). */
@Configuration
public class ConsumidorNotificaciones {

	public static final String COLA_PEDIDOS_CREADOS = "pedidos.creados";

	private static final Logger log = LoggerFactory.getLogger(ConsumidorNotificaciones.class);

	private final ServicioNotificaciones servicio;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.mq.exchange:pedidos.eventos}")
	private String exchange;

	@Value("${app.mq.routing-pedido-creado:pedido.creado}")
	private String routingKey;

	public ConsumidorNotificaciones(ServicioNotificaciones servicio) {
		this.servicio = servicio;
	}

	@Bean
	public DirectExchange exchangePedidos() {
		return new DirectExchange(exchange, true, false);
	}

	@Bean
	public Queue colaPedidosCreados() {
		return new Queue(COLA_PEDIDOS_CREADOS, true);
	}

	@Bean
	public Binding bindingPedidosCreados(DirectExchange exchangePedidos, Queue colaPedidosCreados) {
		return BindingBuilder.bind(colaPedidosCreados).to(exchangePedidos).with(routingKey);
	}

	@RabbitListener(queues = COLA_PEDIDOS_CREADOS)
	public void alPedidoCreado(String json) {
		try {
			JsonNode evento = objectMapper.readTree(json);
			String orderId = evento.path("orderId").asText("?");
			String email = evento.path("customerEmail").asText("");
			String nombre = evento.path("customerName").asText("");
			int total = evento.path("total").asInt(0);
			servicio.notificarPedido(orderId, email, nombre, total);
			log.info("Notificación procesada para {}", orderId);
		} catch (Exception ex) {
			log.warn("Evento pedido.creado inválido: {}", ex.getMessage());
		}
	}
}
