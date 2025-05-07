package com.linkedout.common.constant;

/**
 * RabbitMQConstants는 교환기, 큐, 라우팅 키와 같은 RabbitMQ 설정에
 * 사용되는 상수들을 포함하는 유틸리티 클래스입니다.
 * 이 클래스에 정의된 상수들은 RabbitMQ 컴포넌트들의 명명 규칙을
 * 표준화하고 관리하기 위해 애플리케이션 전반에서 사용됩니다.
 * <p>
 * 이 클래스는 인스턴스화를 목적으로 하지 않습니다.
 * <p>
 * 포함된 상수:
 * 1. 특정 도메인을 위한 교환기 이름
 * 2. 다양한 메시지 소비자를 위한 큐 이름
 * 3. 메시지를 올바른 큐로 전달하기 위한 라우팅 키
 */
public class RabbitMQConstants {
	private RabbitMQConstants() {
	}

	// 교환기(Exchange) 상수
	public static final String API_EXCHANGE = "api.exchange";
	public static final String SERVICE_EXCHANGE = "service.exchange";

	// Queue
	public static final String GATEWAY_QUEUE = "gateway.queue";

	public static final String AUTH_API_QUEUE = "auth.api.queue";
	public static final String AUTH_SERVICE_QUEUE = "auth.service.queue";

	public static final String ACCOUNT_API_QUEUE = "account.api.queue";
	public static final String ACCOUNT_SERVICE_QUEUE = "account.service.queue";

	// 라우팅 키(Routing Key) 상수
	public static final String API_RESPONSE_ROUTING_KEY = "api.response";
	public static final String SERVICE_RESPONSE_ROUTING_KEY = "api.response";

	public static final String AUTH_API_ROUTING_KEY = "auth.api.request";
	public static final String AUTH_SERVICE_ROUTING_KEY = "auth.service.request";
	public static final String ACCOUNT_ROUTING_KEY = "account.request";
}
