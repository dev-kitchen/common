package com.linkedout.common.messaging.apiClient;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.MessageResponseHandler;
import com.linkedout.common.messaging.apiClient.converter.AuthenticationConverter;
import com.linkedout.common.messaging.apiClient.converter.DataBufferConverter;
import com.linkedout.common.messaging.apiClient.converter.HeaderConverter;
import com.linkedout.common.messaging.apiClient.resolver.OperationResolver;
import com.linkedout.common.messaging.apiClient.resolver.ServiceResolver;
import com.linkedout.common.model.dto.EnrichedRequestDTO;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import com.linkedout.common.model.dto.auth.AuthenticationDTO;
import com.linkedout.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiMessageSender {

	private final RabbitTemplate rabbitTemplate;
	private final JsonUtils jsonUtils;
	private final MessageResponseHandler messageResponseHandler;
	private final ServiceIdentifier serviceIdentifier;
	private final OperationResolver operationResolver;
	private final ServiceResolver serviceResolver;
	private final AuthenticationConverter authenticationConverter;
	private final DataBufferConverter dataBufferConverter;
	private final HeaderConverter headerConverter;


	/**
	 * 서비스 메시지를 준비하고 대상 서비스로 전송하여 처리합니다.
	 * 이 메서드는 들어오는 HTTP 요청을 처리하고, 요청 경로와 메서드를 기반으로
	 * 대상 서비스와 작업을 결정하며, 요청 데이터를 완성된 서비스 메시지 객체로
	 * 가공하여 RabbitMQ를 통해 대상 라우팅 키로 전송합니다.
	 * 그 후 서비스로부터의 응답을 비동기적으로 대기합니다.
	 *
	 * @param exchange HTTP 요청과 관련 컨텍스트를 포함하는 서버 웹 교환 객체
	 * @return 대상 서비스로부터의 응답을 포함하는 {@code ServiceMessageDTO<?>} 타입의 Mono.
	 * 처리 또는 응답이 시간 초과되면 오류를 반환합니다.
	 */
	public <T> Mono<ServiceMessageDTO<?>> prepareAndSendMessage(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();

		// 경로에서 타겟 서비스와 작업 결정
		String targetService = serviceResolver.resolveTargetService(path);
		String operation = operationResolver.resolve(path, request.getMethod().name());

		// 메시지 상관관계 ID 생성
		String correlationId = UUID.randomUUID().toString();

		// 기본 서비스 메시지 빌더 생성
		ServiceMessageDTO.ServiceMessageDTOBuilder<Object> messageBuilder = ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation)
			.replyTo(serviceIdentifier.getResponseRoutingKey());

		// 인증 정보 가져오기
		return ReactiveSecurityContextHolder.getContext()
			.map(context -> {
				Authentication authentication = context.getAuthentication();
				if (authentication != null && authentication.isAuthenticated()) {
					// Authentication 객체를 AuthenticationDTO로 변환
					AuthenticationDTO authDTO = authenticationConverter.convert(authentication);
					messageBuilder.authentication(authDTO);
				}
				return messageBuilder;
			})
			.defaultIfEmpty(messageBuilder)  // 인증 정보가 없는 경우
			.flatMap(builder ->
				request.getBody()
					.collectList()
					.flatMap(dataBuffers -> {
						String requestBody = dataBufferConverter.convertToString(dataBuffers);

						Object requestData;
						try {
							if (!requestBody.isEmpty()) {
								requestData = jsonUtils.fromJson(requestBody, Object.class);
							} else {
								requestData = new HashMap<>(); // 빈 요청 본문인 경우
							}
						} catch (Exception e) {
							log.error("요청 본문 변환 중 오류 발생", e);
							requestData = requestBody; // 변환 실패 시 원본 문자열 사용
						}

						EnrichedRequestDTO<Object> enrichedRequest = new EnrichedRequestDTO<>();
						enrichedRequest.setBody(requestData);
						enrichedRequest.setPath(path);
						enrichedRequest.setMethod(request.getMethod().name());
						enrichedRequest.setHeaders(headerConverter.toMap(request.getHeaders()));
						enrichedRequest.setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());

						// payload 설정
						ServiceMessageDTO<Object> message = builder
							.payload(enrichedRequest)
							.build();

						Mono<ServiceMessageDTO<?>> responseMono =
							messageResponseHandler.awaitResponse(correlationId);

						String routingKey = targetService + ".consumer.routing.key";

						// 로그에 인증 정보 포함 여부 표시
						boolean isAuthenticated = message.getAuthentication() != null;
						String userId = isAuthenticated ? message.getAuthentication().getPrincipal() : "anonymous";

						log.info("서비스 메시지 발송: target={}, operation={}, correlationId={}, authenticated={}, userId={}",
							routingKey, operation, correlationId, isAuthenticated, userId);

						rabbitTemplate.convertAndSend(
							RabbitMQConstants.SERVICE_EXCHANGE,
							routingKey,
							message,
							msg -> {
								msg.getMessageProperties().setCorrelationId(correlationId);
								msg.getMessageProperties()
									.getHeaders()
									.put(AmqpHeaders.CORRELATION_ID, correlationId);
								msg.getMessageProperties().setReplyTo(serviceIdentifier.getResponseRoutingKey());
								return msg;
							});

						// 비동기 응답 처리
						return responseMono
							.timeout(Duration.ofSeconds(30))
							.onErrorResume(
								TimeoutException.class,
								e -> {
									log.error("서비스 응답 타임아웃: correlationId={}, path={}", correlationId, path);
									return Mono.error(e);
								});
					})
			);
	}
}