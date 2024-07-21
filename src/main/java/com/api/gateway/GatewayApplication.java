package com.api.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import com.api.gateway.filter.AuthenticationFilter;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public KeyResolver keyResolver() {
		return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
	}

	@Autowired
	AuthenticationFilter abstractGatewayFilterFactory;

	@Bean
	RedisRateLimiter requestRateLimiter() {
		return new RedisRateLimiter(1, 1, 1);
	}

	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("route1",
						p -> p.path("/get")
								.filters(f -> f.filter(abstractGatewayFilterFactory).requestRateLimiter(
										r -> r.setRateLimiter(requestRateLimiter()).setKeyResolver(keyResolver())))
								.uri("http://localhost:8081"))
				.route("route2",
						p -> p.path("/matches/{segment}")
								.filters(a -> a.filter(abstractGatewayFilterFactory).requestRateLimiter(
										r -> r.setRateLimiter(requestRateLimiter()).setKeyResolver(keyResolver())))
								.uri("http://localhost:8082"))
				.build();
	}

}
