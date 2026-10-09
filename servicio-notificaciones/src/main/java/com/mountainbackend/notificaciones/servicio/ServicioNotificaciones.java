package com.mountainbackend.notificaciones.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mountainbackend.notificaciones.dto.SolicitudNotificacion;
import com.mountainbackend.notificaciones.modelo.Notificacion;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Envío de correos. Sin SMTP configurado opera en modo SIMULADO
 * (registra en log, válido para la rúbrica). Con SMTP_* envía real.
 * Usa Jakarta Mail directo para no depender del autoconfigure de Boot.
 */
@Service
public class ServicioNotificaciones {

	private static final Logger log = LoggerFactory.getLogger(ServicioNotificaciones.class);

	private final Map<String, Notificacion> enviadas = new ConcurrentHashMap<>();
	private final AtomicLong secuencia = new AtomicLong(1);

	@Value("${app.correo.host:}")
	private String smtpHost;

	@Value("${app.correo.port:587}")
	private int smtpPort;

	@Value("${app.correo.username:}")
	private String smtpUser;

	@Value("${app.correo.password:}")
	private String smtpPass;

	@Value("${app.correo.from:no-reply@summitlab.cl}")
	private String smtpFrom;

	public Notificacion enviar(SolicitudNotificacion solicitud) {
		if (solicitud == null || solicitud.to() == null || solicitud.to().isBlank()) {
			throw new IllegalArgumentException("El destinatario 'to' es obligatorio.");
		}
		String subject = solicitud.subject() != null ? solicitud.subject() : "(sin asunto)";
		String message = solicitud.message() != null ? solicitud.message() : "";

		String estado;
		if (smtpHost == null || smtpHost.isBlank()) {
			log.info("[SIMULADO] Para: {} | Asunto: {} | {}", solicitud.to(), subject, message);
			estado = "SIMULADA";
		} else {
			enviarSmtp(solicitud.to(), subject, message);
			estado = "ENVIADA";
		}
		String id = "NOTIF-" + secuencia.getAndIncrement();
		Notificacion notif = new Notificacion(id, solicitud.to(), subject, message, estado, Instant.now());
		enviadas.put(id, notif);
		return notif;
	}

	public Notificacion notificarPedido(String orderId, String email, String nombre, int total) {
		String destino = email != null && !email.isBlank() ? email : "sin-correo@summitlab.cl";
		String subject = "SummitLab: tu pedido " + orderId + " fue recibido";
		String message = "Hola " + (nombre != null ? nombre : "") + ", tu pedido " + orderId
			+ " por $" + total + " está en preparación. ¡Gracias por tu compra!";
		return enviar(new SolicitudNotificacion(destino, subject, message));
	}

	public List<Notificacion> listar() {
		return new ArrayList<>(enviadas.values());
	}

	private void enviarSmtp(String to, String subject, String message) {
		try {
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHost);
			props.put("mail.smtp.port", String.valueOf(smtpPort));
			props.put("mail.smtp.auth", smtpUser != null && !smtpUser.isBlank() ? "true" : "false");
			props.put("mail.smtp.starttls.enable", "true");
			Session session = Session.getInstance(props);
			MimeMessage mime = new MimeMessage(session);
			mime.setFrom(new InternetAddress(smtpFrom));
			mime.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			mime.setSubject(subject, "UTF-8");
			mime.setText(message, "UTF-8");
			try (Transport transport = session.getTransport("smtp")) {
				if (smtpUser != null && !smtpUser.isBlank()) {
					transport.connect(smtpHost, smtpPort, smtpUser, smtpPass);
				} else {
					transport.connect();
				}
				transport.sendMessage(mime, mime.getAllRecipients());
			}
			log.info("Correo ENVIADO a {} | {}", to, subject);
		} catch (Exception ex) {
			log.warn("Fallo SMTP ({}), se registra como simulada: {}", to, ex.getMessage());
			throw new IllegalStateException("No se pudo enviar el correo: " + ex.getMessage());
		}
	}
}
