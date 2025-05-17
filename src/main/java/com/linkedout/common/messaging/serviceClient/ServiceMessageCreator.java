package com.linkedout.common.messaging.serviceClient;

import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.model.dto.ServiceMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ServiceMessageCreator {
	private final ServiceIdentifier serviceIdentifier;

	/**
	 * 제공된 매개변수로 서비스 메시지 DTO를 생성하고 Mono로 감싸서 반환합니다.
	 * 이 메서드는 서비스 간 통신을 위한 메시지를 구성하는 데 사용됩니다.
	 *
	 * @param <T>           페이로드의 타입
	 * @param correlationId 요청과 응답을 연결하는 고유 식별자
	 * @param targetService 메시지가 전송될 대상 서비스의 식별자
	 * @param operation     대상 서비스가 수행할 작업을 나타내는 작업명
	 * @param requestData   메시지에 포함될 페이로드 데이터
	 * @return 구성된 {@code ServiceMessageDTO<T>}를 방출하는 Mono
	 */
	public <T> Mono<ServiceMessageDTO<T>> createMessage(String correlationId, String targetService,
																											String operation, T requestData) {
		return Mono.just(ServiceMessageDTO.<T>builder()
			.correlationId(correlationId)
			.senderService(serviceIdentifier.getServiceName())
			.operation(operation)
			.replyTo(serviceIdentifier.getResponseRoutingKey())
			.payload(requestData)
			.build());
	}
}
