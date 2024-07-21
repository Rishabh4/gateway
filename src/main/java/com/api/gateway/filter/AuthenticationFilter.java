package com.api.gateway.filter;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.api.gateway.JwtUtil;
import com.api.gateway.RouteValidator;

import io.jsonwebtoken.ExpiredJwtException;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config>
		implements GatewayFilter {

	@Autowired
	private RouteValidator validator;

	@Autowired
	private JwtUtil jwtUtil;

	public AuthenticationFilter() {
		super(Config.class);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return ((exchange, chain) -> chain.filter(exchange));
	}

	public static class Config {
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (validator.isSecured.test(request)) {
			if (this.isAuthMissing(request)) {
				return this.onError(exchange, HttpStatus.UNAUTHORIZED);
			}

			final String token = this.getAuthHeader(request);

			try {
				if (jwtUtil.isTokenExpired(token)) {
					return this.onError(exchange, HttpStatus.FORBIDDEN);
				}
				if (!jwtUtil.isAuthorized(token)) {
					return this.onError(exchange, HttpStatus.UNAUTHORIZED);
				}
			} catch (ExpiredJwtException jwtException) {
				return this.onError(exchange, HttpStatus.UNAUTHORIZED);
			}
		}
		return chain.filter(exchange);
	}

	private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		return response.setComplete();
	}

	private String getAuthHeader(ServerHttpRequest request) {
		return request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).get(0).substring(7);
	}

	private boolean isAuthMissing(ServerHttpRequest request) {
		return !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION);
	}
}