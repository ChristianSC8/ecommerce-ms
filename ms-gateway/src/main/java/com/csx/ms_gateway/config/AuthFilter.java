package com.csx.ms_gateway.config;

import com.csx.ms_gateway.dto.TokenDto;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final String BEARER_PREFIX = "Bearer";
    private static final int TOKEN_PARTS_LENGTH = 2;
    private static final String AUTH_SERVICE_URL = "http://ms-auth/auth/validate?token=";

    private final WebClient.Builder webClientBuilder;

    public AuthFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!hasAuthorizationHeader(exchange)) {
                return onError(exchange, HttpStatus.BAD_REQUEST);
            }

            String token = extractToken(exchange);
            if (token == null) {
                return onError(exchange, HttpStatus.BAD_REQUEST);
            }

            return validateToken(token)
                    .map(tokenDto -> exchange)
                    .flatMap(chain::filter)
                    .onErrorResume(e -> onError(exchange, HttpStatus.UNAUTHORIZED));
        };
    }

    private boolean hasAuthorizationHeader(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getHeaders()
                .containsKey(HttpHeaders.AUTHORIZATION);
    }

    private String extractToken(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null) {
            return null;
        }

        String[] parts = authHeader.split(" ");

        if (parts.length != TOKEN_PARTS_LENGTH || !BEARER_PREFIX.equals(parts[0])) {
            return null;
        }

        return parts[1];
    }

    private Mono<TokenDto> validateToken(String token) {
        return webClientBuilder.build()
                .post()
                .uri(AUTH_SERVICE_URL + token)
                .retrieve()
                .bodyToMono(TokenDto.class);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    public static class Config {
    }
}