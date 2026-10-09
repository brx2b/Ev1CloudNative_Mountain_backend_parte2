package com.mountainbackend.carrito.controlador;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainbackend.carrito.dto.SolicitudItem;
import com.mountainbackend.carrito.modelo.Carrito;
import com.mountainbackend.carrito.seguridad.FiltroValidacionJwt;
import com.mountainbackend.carrito.servicio.ServicioCarrito;
import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.http.HttpServletRequest;

/**
 * GET    /cart               -> carrito del usuario del token
 * POST   /cart/items         -> agrega (suma cantidad si existe)
 * PUT    /cart/items/{id}    -> fija cantidad (0 lo quita)
 * DELETE /cart/items/{id}    -> quita el producto
 * DELETE /cart               -> vacía el carrito
 */
@RestController
@RequestMapping("/cart")
public class ControladorCarrito {

	private final ServicioCarrito servicio;

	public ControladorCarrito(ServicioCarrito servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public Carrito obtener(HttpServletRequest http) {
		return servicio.obtener(emailDelToken(http));
	}

	@PostMapping("/items")
	public Carrito agregar(@RequestBody SolicitudItem solicitud, HttpServletRequest http) {
		return servicio.agregar(emailDelToken(http), solicitud);
	}

	@PutMapping("/items/{id}")
	public Carrito actualizar(@PathVariable("id") long id,
			@RequestBody SolicitudItem solicitud, HttpServletRequest http) {
		int quantity = solicitud.quantity() != null ? solicitud.quantity() : 1;
		return servicio.actualizarCantidad(emailDelToken(http), id, quantity);
	}

	@DeleteMapping("/items/{id}")
	public Carrito quitar(@PathVariable("id") long id, HttpServletRequest http) {
		return servicio.quitar(emailDelToken(http), id);
	}

	@DeleteMapping
	public ResponseEntity<Map<String, Object>> vaciar(HttpServletRequest http) {
		servicio.vaciar(emailDelToken(http));
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 200);
		cuerpo.put("message", "Carrito vaciado.");
		return ResponseEntity.ok(cuerpo);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> alErrorDeArgumento(IllegalArgumentException ex) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("status", 400);
		cuerpo.put("error", ex.getMessage());
		return ResponseEntity.badRequest().body(cuerpo);
	}

	private String emailDelToken(HttpServletRequest http) {
		JWTClaimsSet claims = (JWTClaimsSet) http.getAttribute(FiltroValidacionJwt.ATRIBUTO_CLAIMS_JWT);
		if (claims == null) {
			throw new IllegalArgumentException("Sin identidad del token.");
		}
		Object email = claims.getClaim("email");
		if (email == null) {
			email = claims.getClaim("preferred_username");
		}
		if (email == null) {
			email = claims.getSubject();
		}
		if (email == null) {
			throw new IllegalArgumentException("El token no trae identidad de usuario.");
		}
		return String.valueOf(email);
	}
}
