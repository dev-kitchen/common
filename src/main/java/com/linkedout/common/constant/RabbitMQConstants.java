package com.linkedout.common.constant;

public class RabbitMQConstants {
	private RabbitMQConstants() {
	}

	// 큐(Queue) 상수
	public static final String GATEWAY_QUEUE = "api-gateway-queue";
	public static final String AUTH_QUEUE = "auth-queue";

	// 교환기(Exchange) 상수
	public static final String AUTH_EXCHANGE = "auth-exchange";

	// 라우팅 키(Routing Key) 상수
	public static final String AUTH_ROUTING_KEY = "auth.request";
	public static final String AUTH_RESPONSE_ROUTING_KEY = "auth.response";
}
