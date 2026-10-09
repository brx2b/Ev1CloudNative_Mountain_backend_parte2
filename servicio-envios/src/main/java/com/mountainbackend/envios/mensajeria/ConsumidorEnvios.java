package com.mountainbackend.envios.mensajeria;

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
import com.mountainbackend.envios.servicio.ServicioEnvios;

/** Consume pedido.creado y abre el despacho en PENDIENTE. */
@Configuration
public class ConsumidorEnvios {

	public static final String COLA_ENVIOS = "pedidos.creados.envios";

	private static final Logger log = LoggerFactory.getLogger(ConsumidorEnvios.class);

	private final ServicioEnvios servicio;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.mq.exchange:pedidos.eventos}")
	private String exchange;

	@Value("${app.mq.routing-pedido-creado:pedido.creado}")
	private String routingKey;

	public ConsumidorEnvios(ServicioEnvios servicio) {
		this.servicio = servicio;
	}

	@Bean
	public DirectExchange exchangePedidos() {
		return new DirectExchange(exchange, true, false);
	}

	@Bean
	public Queue colaEnvios() {
		return new Queue(COLA_ENVIOS, true);
	}

	@Bean
	public Binding bindingEnvios(DirectExchange exchangePedidos, Queue colaEnvios) {
		return BindingBuilder.bind(colaEnvios).to(exchangePedidos).with(routingKey);
	}

	@RabbitListener(queues = COLA_ENVIOS)
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
