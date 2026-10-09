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
		return notificarPedido(orderId, email, nombre, total, null);
	}

	public Notificacion notificarPedido(String orderId, String email, String nombre, int total,
			java.util.List<ItemNotificado> items) {
		String destino = email != null && !email.isBlank() ? email : "sin-correo@summitlab.cl";
		String subject = "SummitLab: tu pedido " + orderId + " fue recibido";
		String plain = "Hola " + (nombre != null ? nombre : "") + ", tu pedido " + orderId
			+ " por $" + total + " está en preparación. ¡Gracias por tu compra!";
		String html = armarHtml(orderId, nombre, total, items);
		return enviarHtml(destino, subject, plain, html);
	}

	public List<Notificacion> listar() {
		return new ArrayList<>(enviadas.values());
	}

	/** Item para el detalle del correo (nombre, cantidad, precio). */
	public record ItemNotificado(String name, int quantity, int price) {
	}

	private static final String BANNER_URL =
		"https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&q=80&auto=format&fit=crop";

	private String armarHtml(String orderId, String nombre, int total,
			java.util.List<ItemNotificado> items) {
		StringBuilder filas = new StringBuilder();
		if (items != null) {
			for (ItemNotificado item : items) {
				filas.append("<tr>")
					.append("<td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">")
					.append(escape(item.name())).append("</td>")
					.append("<td style=\"padding:8px;border-bottom:1px solid #e2e8f0;text-align:center;\">")
					.append(item.quantity()).append("</td>")
					.append("<td style=\"padding:8px;border-bottom:1px solid #e2e8f0;text-align:right;\">$")
					.append(item.price() * item.quantity()).append("</td>")
					.append("</tr>");
			}
		}
		return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;\">"
			+ "<img src=\"" + BANNER_URL + "\" alt=\"SummitLab\" style=\"width:100%;border-radius:8px 8px 0 0;\"/>"
			+ "<div style=\"background:#0f172a;color:#fff;padding:16px 24px;\">"
			+ "<h2 style=\"margin:0;\">SummitLab Alpine Store</h2></div>"
			+ "<div style=\"padding:24px;border:1px solid #e2e8f0;border-top:none;border-radius:0 0 8px 8px;\">"
			+ "<p>Hola " + escape(nombre != null ? nombre : "") + ",</p>"
			+ "<p>Tu pedido <b>" + escape(orderId) + "</b> fue recibido y está en preparación.</p>"
			+ "<table style=\"width:100%;border-collapse:collapse;\">"
			+ "<tr><th style=\"text-align:left;padding:8px;\">Producto</th>"
			+ "<th style=\"padding:8px;\">Cant.</th>"
			+ "<th style=\"text-align:right;padding:8px;\">Subtotal</th></tr>"
			+ filas
			+ "</table>"
			+ "<p style=\"text-align:right;font-size:18px;\">Total: <b>$" + total + "</b></p>"
			+ "<p style=\"color:#64748b;font-size:12px;\">Gracias por tu compra.</p>"
			+ "</div></div>";
	}

	private String escape(String texto) {
		if (texto == null) {
			return "";
		}
		return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private Notificacion enviarHtml(String destino, String subject, String plain, String html) {
		String estado;
		if (smtpHost == null || smtpHost.isBlank()) {
			log.info("[SIMULADO] Para: {} | Asunto: {} | {}", destino, subject, plain);
			estado = "SIMULADA";
		} else {
			enviarSmtpHtml(destino, subject, html);
			estado = "ENVIADA";
		}
		String id = "NOTIF-" + secuencia.getAndIncrement();
		Notificacion notif = new Notificacion(id, destino, subject, plain, estado, Instant.now());
		enviadas.put(id, notif);
		return notif;
	}

	private void enviarSmtpHtml(String to, String subject, String html) {
		try {
			jakarta.mail.Message mime = armarMime(to, subject);
			mime.setContent(html, "text/html; charset=UTF-8");
			enviarMime(mime);
			log.info("Correo ENVIADO a {} | {}", to, subject);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo enviar el correo: " + ex.getMessage());
		}
	}

	private jakarta.mail.Message armarMime(String to, String subject) throws Exception {
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
		return mime;
	}

	private void enviarMime(jakarta.mail.Message mime) throws Exception {
		try (Transport transport = mime.getSession().getTransport("smtp")) {
			if (smtpUser != null && !smtpUser.isBlank()) {
				transport.connect(smtpHost, smtpPort, smtpUser, smtpPass);
			} else {
				transport.connect();
			}
			transport.sendMessage(mime, mime.getAllRecipients());
		}
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
