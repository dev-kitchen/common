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

import java.util.UUID;

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
                    .mapNotNull(response -> {
                        try {
                            if (response.getError() != null) {
                                throw new BadRequestException(response.getError());
                            }

                            return objectMapper.convertValue(response.getPayload(), responseType);
                        } catch (Exception e) {
                            log.error("응답 변환 오류: {}", e.getMessage(), e);
                            throw new InternalServerErrorException("응답 변환 중 오류 발생");
                        }
                    });

        } catch (Exception e) {
            log.error("메시지 전송 오류: {}", e.getMessage(), e);
            return Mono.error(new InternalServerErrorException("메시지 전송 중 오류 발생"));
        }
    }
}
