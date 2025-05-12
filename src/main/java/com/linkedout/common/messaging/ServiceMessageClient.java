package com.linkedout.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ServiceMessageDTO;
import com.linkedout.common.exception.BadRequestException;
import com.linkedout.common.exception.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ 메시징을 통해  마이크로서비스간 상호작용하는 클라이언트 서비스입니다.
 * 이 클래스는 다른 서비스로 서비스 메시지를 전송하고 응답을 비동기식으로 처리하는 기능을 제공합니다.
 */
@Service
@Slf4j
public class ServiceMessageClient {

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;
	private final ServiceMessageResponseHandler serviceMessageResponseHandler;
	private final ServiceIdentifier serviceIdentifier;

	public ServiceMessageClient(
		RabbitTemplate rabbitTemplate,
		ObjectMapper objectMapper,
		ServiceMessageResponseHandler serviceMessageResponseHandler,
		ServiceIdentifier serviceIdentifier) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
		this.serviceMessageResponseHandler = serviceMessageResponseHandler;
		this.serviceIdentifier = serviceIdentifier;
	}

	/**
	 * 대상 서비스로 메시지를 전송하고 비동기 응답을 기다립니다.
	 * 이 메서드는 제공된 데이터로 서비스 메시지를 구성하고, RabbitMQ를 사용하여
	 * 메시지를 전송한 후, 응답을 처리하여 원하는 페이로드 타입을 반환합니다.
	 *
	 * @param <T>           요청 페이로드의 타입
	 * @param <R>           응답 페이로드의 타입
	 * @param targetService 메시지를 전송할 대상 서비스의 식별자
	 * @param operation     대상 서비스가 실행해야 할 작업 이름
	 * @param requestData   서비스 메시지의 일부로 전송될 데이터 페이로드
	 * @param responseType  응답 페이로드 타입의 클래스
	 * @return 성공 시 타입 R의 응답 페이로드를 방출하는 Mono.
	 * 응답 과정이나 처리 중에 오류가 발생하면 적절한 오류가 방출됩니다.
	 */
	public <T, R> Mono<R> sendMessage(String targetService, String operation, T requestData, Class<R> responseType) {
		String correlationId = UUID.randomUUID().toString();

		// 요청 메시지 생성
		ServiceMessageDTO<T> message = ServiceMessageDTO.<T>builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation)
			.replyTo(serviceIdentifier.getResponseRoutingKey())
			.payload(requestData)
			.build();

		try {
			// 응답 대기 설정
			Mono<ServiceMessageDTO<?>> responseMono = serviceMessageResponseHandler.awaitResponse(correlationId);

			// 요청 메시지 발송
			String targetRoutingKey = targetService + ".consumer.routing.key";
			log.debug("서비스 메시지 발송: target={}, operation={}, correlationId={}",
				targetService, operation, correlationId);

			rabbitTemplate.convertAndSend(
				RabbitMQConstants.SERVICE_EXCHANGE,
				targetRoutingKey,
				message,
				msg -> {
					msg.getMessageProperties().setCorrelationId(correlationId);
					msg.getMessageProperties().setReplyTo(serviceIdentifier.getResponseRoutingKey());
					return msg;
				});

			// 응답 변환 및 반환
			return responseMono
				.timeout(Duration.ofSeconds(30))
				.onErrorResume(TimeoutException.class, e -> {
					log.error("서비스 응답 타임아웃: correlationId={}, targetService={}, operation={}",
						correlationId, targetService, operation);
					return Mono.error(new InternalServerErrorException("서비스 응답 타임아웃 발생"));
				})
				.handle((response, sink) -> {
					try {
						if (response.getError() != null) {
							sink.error(new BadRequestException(response.getError()));
							return;
						}

						sink.next(objectMapper.convertValue(response.getPayload(), responseType));
					} catch (Exception e) {
						log.error("응답 변환 오류: {}", e.getMessage(), e);
						sink.error(new InternalServerErrorException("응답 변환 중 오류 발생"));
					}
				});

		} catch (Exception e) {
			log.error("메시지 전송 오류: {}", e.getMessage(), e);
			return Mono.error(new InternalServerErrorException("메시지 전송 중 오류 발생"));
		}
	}
}
