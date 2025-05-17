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
import java.util.Map;
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
	 * ServerWebExchange를 통해 수신된 HTTP 요청을 기반으로 서비스 메시지를 준비하고 전송합니다.
	 * 대상 서비스와 작업을 식별하고, 인증된 메시지를 구성하며, 요청 데이터를 보강하고,
	 * 지정된 서비스로 메시지를 전송하여 추가 처리를 수행합니다.
	 *
	 * @param exchange HTTP 요청과 컨텍스트를 포함하는 ServerWebExchange 인스턴스
	 * @return 메시지 전송 프로세스가 완료되면 ServiceMessageDTO를 방출하는 Mono,
	 * 작업이 실패하면 오류를 방출합니다.
	 */
	public <T> Mono<ServiceMessageDTO<?>> prepareAndSendMessage(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		String targetService = serviceResolver.resolveTargetService(path);
		String operation = operationResolver.resolve(path, request.getMethod().name());

		// 메시지 상관관계 ID 생성
		String correlationId = UUID.randomUUID().toString();

		return getAuthenticatedMessageBuilder(correlationId, operation)
			.flatMap(builder ->
				request.getBody()
					.collectList()
					.flatMap(dataBuffers -> {
						String requestBody = dataBufferConverter.convertToString(dataBuffers);
						Object requestData = parseRequestBody(requestBody);

						EnrichedRequestDTO<Object> enrichedRequest = createEnrichedRequest(
							requestData, path, request.getMethod().name(),
							headerConverter.toMap(request.getHeaders()),
							exchange.getRequest().getQueryParams().toSingleValueMap()
						);

						return sendAndAwaitResponse(
							builder, enrichedRequest, correlationId, targetService, operation
						);
					})
			);
	}

	/**
	 * HTTP 요청과 검증된 요청 본문을 기반으로 메시지를 준비하고 전송합니다.
	 * 대상 서비스와 작업을 식별하고, 상관관계 ID를 생성하며, 인증된 메시지를 구성하여
	 * 요청 데이터를 보강한 후 결정된 대상 서비스로 전송하여 응답을 기다립니다.
	 *
	 * @param <T>           응답 페이로드의 타입
	 * @param exchange      HTTP 요청과 응답 객체를 포함하는 ServerWebExchange
	 * @param validatedBody 보강된 요청 구성에 사용될 사전 검증된 요청 본문
	 * @return 대상 서비스로부터의 응답이 포함된 ServiceMessageDTO를 방출하는 Mono
	 */
	public <T> Mono<ServiceMessageDTO<?>> prepareAndSendMessage(ServerWebExchange exchange, Object validatedBody) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		String targetService = serviceResolver.resolveTargetService(path);
		String operation = operationResolver.resolve(path, request.getMethod().name());

		// 메시지 상관관계 ID 생성
		String correlationId = UUID.randomUUID().toString();

		return getAuthenticatedMessageBuilder(correlationId, operation)
			.flatMap(builder -> {
				EnrichedRequestDTO<Object> enrichedRequest = createEnrichedRequest(
					validatedBody, path, request.getMethod().name(),
					headerConverter.toMap(request.getHeaders()),
					exchange.getRequest().getQueryParams().toSingleValueMap()
				);

				return sendAndAwaitResponse(
					builder, enrichedRequest, correlationId, targetService, operation
				);
			});
	}

	/**
	 * 인증된 ServiceMessageDTO를 구성하기 위한 빌더를 조회합니다.
	 * 빌더는 상관관계 ID, 발신자 서비스명, 작업 유형, 응답 라우팅 키 등으로 초기화됩니다.
	 * 리액티브 보안 컨텍스트에서 인증 정보를 사용할 수 있는 경우 빌더에 포함됩니다.
	 *
	 * @param correlationId 메시지 수명주기 추적을 위한 고유 식별자
	 * @param operation     메시지의 작업 유형
	 * @return 제공된 세부정보와 선택적 인증 데이터로 사전 구성된 ServiceMessageDTO.ServiceMessageDTOBuilder
	 * 인스턴스를 방출하는 Mono
	 */
	private Mono<ServiceMessageDTO.ServiceMessageDTOBuilder<Object>> getAuthenticatedMessageBuilder(
		String correlationId, String operation) {

		ServiceMessageDTO.ServiceMessageDTOBuilder<Object> messageBuilder = ServiceMessageDTO.builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation)
			.replyTo(serviceIdentifier.getResponseRoutingKey());

		return ReactiveSecurityContextHolder.getContext()
			.map(context -> {
				Authentication authentication = context.getAuthentication();
				if (authentication != null && authentication.isAuthenticated()) {
					AuthenticationDTO authDTO = authenticationConverter.convert(authentication);
					messageBuilder.authentication(authDTO);
				}
				return messageBuilder;
			})
			.defaultIfEmpty(messageBuilder);  // 인증 정보가 없는 경우
	}

	/**
	 * 제공된 요청 본문을 적절한 객체 표현으로 파싱합니다.
	 * 요청 본문이 비어있지 않은 경우 jsonUtils.fromJson을 사용하여 객체로 변환을 시도합니다.
	 * 요청 본문이 비어있는 경우 빈 HashMap을 반환합니다.
	 * 파싱 오류 발생 시 원본 요청 본문 문자열을 그대로 반환합니다.
	 *
	 * @param requestBody 원본 요청 본문 문자열
	 * @return 요청 본문의 역직렬화된 객체 표현, 본문이 비어있는 경우 빈 HashMap,
	 * 또는 파싱 실패 시 원본 문자열
	 */
	private Object parseRequestBody(String requestBody) {
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
		return requestData;
	}

	/**
	 * 제공된 매개변수로 EnrichedRequestDTO 인스턴스를 생성하고 반환합니다.
	 *
	 * @param body        포함할 요청 본문
	 * @param path        요청 대상 경로
	 * @param method      사용할 HTTP 메서드
	 * @param headers     요청 헤더 맵
	 * @param queryParams 쿼리 매개변수 맵
	 * @return 주어진 입력 데이터를 포함하는 EnrichedRequestDTO 인스턴스
	 */
	private EnrichedRequestDTO<Object> createEnrichedRequest(
		Object body, String path, String method,
		Map<String, String> headers, Map<String, String> queryParams) {

		EnrichedRequestDTO<Object> enrichedRequest = new EnrichedRequestDTO<>();
		enrichedRequest.setBody(body);
		enrichedRequest.setPath(path);
		enrichedRequest.setMethod(method);
		enrichedRequest.setHeaders(headers);
		enrichedRequest.setQueryParams(queryParams);

		return enrichedRequest;
	}

	/**
	 * 지정된 대상 서비스로 서비스 메시지를 전송하고 비동기적으로 응답을 기다립니다.
	 * 서비스 메시지 페이로드를 구성하고 관련 정보를 로깅하며,
	 * RabbitMQ를 통해 메시지를 전송하고 지정된 타임아웃으로 해당 응답을 수신합니다.
	 *
	 * @param builder         ServiceMessageDTO 객체 구성을 위한 빌더
	 * @param enrichedRequest 본문, 경로, 메서드, 헤더, 쿼리 매개변수를 포함하는 보강된 요청 상세 정보
	 * @param correlationId   메시지와 응답을 연관짓는데 사용되는 고유 식별자
	 * @param targetService   메시지가 전송될 대상 서비스의 이름
	 * @param operation       메시지와 관련된 작업 또는 동작
	 * @return 성공적으로 수신 시 응답 메시지를 방출하거나 작업 실패 시 오류를 방출하는 Mono<ServiceMessageDTO<?>>
	 */
	private Mono<ServiceMessageDTO<?>> sendAndAwaitResponse(
		ServiceMessageDTO.ServiceMessageDTOBuilder<Object> builder,
		EnrichedRequestDTO<Object> enrichedRequest,
		String correlationId,
		String targetService,
		String operation) {

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
					log.error("서비스 응답 타임아웃: correlationId={}, ", correlationId);
					return Mono.error(e);
				});
	}
}