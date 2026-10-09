package com.lambda.validador.authorizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.lambda.validador.security.JwtValidationService;
import com.lambda.validador.security.MultiIssuerJwtDecoder;
import com.lambda.validador.support.TestTokens;

/**
 * Comprueba el contrato de entrada y salida del authorizer: donde busca el token,
 * como lo interpreta y que devuelve a API Gateway. La validacion criptografica se
 * prueba aparte, en JwtValidationServiceTest.
 *
 * <p>Los tokens se firman de verdad porque el enrutador multi-emisor lee el claim
 * "iss" del token antes de delegar: un texto arbitrario se rechazaria antes de
 * llegar al decoder.</p>
 */
class JwtAuthorizerFunctionTest {

    private static final String ISSUER = "https://login.example.com/tenant/v2.0";
    private static final TestTokens TOKENS = TestTokens.generar();

    private JwtAuthorizerFunction authorizer(JwtDecoder decoder) {
        return new JwtAuthorizerFunction(new JwtValidationService(new MultiIssuerJwtDecoder(Map.of(ISSUER, decoder))));
    }

    /** Decoder que siempre valida: aqui lo que se prueba es la cabecera, no el token. */
    private JwtAuthorizerFunction authorizerQueAcepta() {
        return authorizer(valor -> jwtValido());
    }

    /** Token firmado cuyo unico proposito es llegar al enrutador. */
    private String token() {
        return TOKENS.firmar(ISSUER, null, Map.of(), Instant.now().plusSeconds(3600));
    }

    private Jwt jwtValido() {
        return Jwt.withTokenValue(token())
                .header("alg", "RS256")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("iss", ISSUER)
                .claim("sub", "usuario-1")
                .build();
    }

    private APIGatewayV2CustomAuthorizerEvent evento(String authorization) {
        APIGatewayV2CustomAuthorizerEvent event = new APIGatewayV2CustomAuthorizerEvent();
        event.setVersion("2.0");
        event.setType("REQUEST");
        event.setRouteArn("arn:aws:execute-api:us-east-1:123456789012:abcdef/test/GET/pedidos");
        event.setRawPath("/pedidos");
        if (authorization != null) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("authorization", authorization);
            event.setHeaders(headers);
        }
        return event;
    }

    @Test
    void autorizaCuandoElTokenEsValido() {
        assertThat(authorizerQueAcepta().authorize(evento("Bearer " + token())).isAuthorized()).isTrue();
    }

    @Test
    void laRespuestaAutorizadaEsSoloElBooleano() {
        String json = serializa(authorizerQueAcepta().authorize(evento("Bearer " + token())));

        assertThat(json).isEqualTo("{\"isAuthorized\":true}");
    }

    @Test
    void laRespuestaDenegadaEsSoloElBooleano() {
        String json = serializa(authorizerQueAcepta().authorize(evento(null)));

        assertThat(json).isEqualTo("{\"isAuthorized\":false}");
    }

    /**
     * El backend vuelve a validar el token y extrae los claims, asi que el
     * authorizer no debe copiar ningun claim al context. API Gateway los escribiria
     * en los access logs de la API, y ahi acabarian visibles para quien tenga
     * permiso de lectura sobre el log group.
     */
    @Test
    void noSeEscapaNingunClaimDelTokenEnLaRespuesta() {
        Jwt conDatos = Jwt.withTokenValue(token())
                .header("alg", "RS256")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("iss", ISSUER)
                .claim("sub", "usuario-1")
                .claim("preferred_username", "alguien@ejemplo.cl")
                .claim("roles", List.of("Administrador"))
                .claim("scp", "write-read")
                .build();

        String json = serializa(authorizer(valor -> conDatos).authorize(evento("Bearer " + token())));

        assertThat(json).isEqualTo("{\"isAuthorized\":true}");
    }

    private String serializa(AuthorizerResult result) {
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(result);
    }

    @Test
    void aceptaLaCabeceraEnCualquierCombinacionDeMayusculas() {
        APIGatewayV2CustomAuthorizerEvent event = new APIGatewayV2CustomAuthorizerEvent();
        event.setHeaders(Map.of("Authorization", "Bearer " + token()));

        assertThat(authorizerQueAcepta().authorize(event).isAuthorized()).isTrue();
    }

    @Test
    void quitaElPrefijoBearer() {
        AtomicReference<String> recibido = new AtomicReference<>();
        String esperado = token();
        JwtAuthorizerFunction authorizer = authorizer(valor -> {
            recibido.set(valor);
            return jwtValido();
        });

        assertThat(authorizer.authorize(evento("Bearer " + esperado)).isAuthorized()).isTrue();
        assertThat(recibido.get()).isEqualTo(esperado);
    }

    @Test
    void aceptaElPrefijoBearerEnMinusculas() {
        AtomicReference<String> recibido = new AtomicReference<>();
        String esperado = token();
        JwtAuthorizerFunction authorizer = authorizer(valor -> {
            recibido.set(valor);
            return jwtValido();
        });

        assertThat(authorizer.authorize(evento("bearer " + esperado)).isAuthorized()).isTrue();
        assertThat(recibido.get()).isEqualTo(esperado);
    }

    @Test
    void aceptaElTokenSinPrefijo() {
        AtomicReference<String> recibido = new AtomicReference<>();
        String esperado = token();
        JwtAuthorizerFunction authorizer = authorizer(valor -> {
            recibido.set(valor);
            return jwtValido();
        });

        assertThat(authorizer.authorize(evento(esperado)).isAuthorized()).isTrue();
        assertThat(recibido.get()).isEqualTo(esperado);
    }

    @Test
    void rechazaSiNoLlegaLaCabeceraAuthorization() {
        assertThat(authorizerQueAcepta().authorize(evento(null)).isAuthorized()).isFalse();
    }

    @Test
    void rechazaSiLaCabeceraVieneVacia() {
        assertThat(authorizerQueAcepta().authorize(evento("   ")).isAuthorized()).isFalse();
    }

    @Test
    void rechazaSiSoloLlegaElPrefijoBearer() {
        assertThat(authorizerQueAcepta().authorize(evento("Bearer ")).isAuthorized()).isFalse();
    }

    @Test
    void rechazaSiNoHayNingunaCabecera() {
        APIGatewayV2CustomAuthorizerEvent event = new APIGatewayV2CustomAuthorizerEvent();

        assertThat(authorizerQueAcepta().authorize(event).isAuthorized()).isFalse();
    }

    @Test
    void rechazaCuandoElTokenNoSuperaLaValidacion() {
        JwtDecoder decoder = valor -> {
            throw new JwtException("token invalido");
        };

        assertThat(authorizer(decoder).authorize(evento("Bearer " + token())).isAuthorized()).isFalse();
    }

    @Test
    void rechazaAnteUnErrorInesperadoEnVezDeDejarPasar() {
        JwtDecoder decoder = valor -> {
            throw new IllegalStateException("el JWK Set no responde");
        };

        assertThat(authorizer(decoder).authorize(evento("Bearer " + token())).isAuthorized()).isFalse();
    }

    @Test
    void rechazaUnTokenQueNoEsUnJwt() {
        assertThat(authorizerQueAcepta().authorize(evento("Bearer no-es-un-jwt")).isAuthorized()).isFalse();
    }
}
