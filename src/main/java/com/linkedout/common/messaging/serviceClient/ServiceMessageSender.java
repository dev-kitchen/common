package com.linkedout.common.messaging.serviceClient;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceMessageSender {
	private final RabbitTemplate rabbitTemplate;
	private final ServiceIdentifier serviceIdentifier;

	/**
	 * RabbitMQ를 사용하여 지정된 대상 서비스로 서비스 메시지를 전송합니다.
	 *
	 * @param <T>           서비스 메시지에 포함된 페이로드의 타입
	 * @param message       {@link ServiceMessageDTO}에 캡슐화된 전송할 서비스 메시지
	 * @param targetService 메시지가 라우팅될 대상 서비스의 식별자
	 * @param correlationId 요청과 응답을 연결하고 추적하기 위한 고유 상관 ID
	 * @return 메시지가 성공적으로 전송되면 {@code true}를 방출하고, 문제가 발생하면 에러와 함께 완료되는 {@link Mono}
	 */
	public <T> Mono<Boolean> send(ServiceMessageDTO<T> message, String targetService, String correlationId) {
		String targetRoutingKey = targetService + ".consumer.routing.key";

		return Mono.fromCallable(() -> {
			rabbitTemplate.convertAndSend(
				RabbitMQConstants.SERVICE_EXCHANGE,
				targetRoutingKey,
				message,
				msg -> {
					msg.getMessageProperties().setCorrelationId(correlationId);
					msg.getMessageProperties().setReplyTo(serviceIdentifier.getResponseRoutingKey());
					return msg;
				});

			log.debug("서비스 메시지 발송: target={}, operation={}, correlationId={}",
				targetService, message.getOperation(), correlationId);
			return true;
		});
	}
}
