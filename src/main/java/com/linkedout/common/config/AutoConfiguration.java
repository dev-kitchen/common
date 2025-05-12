package com.linkedout.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ApiMessageResponseHandler;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageClient;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.util.JsonUtils;
import com.linkedout.common.util.PayloadConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

/**
 * 이 자동 설정 클래스는 common 라이브러리를 사용하는 다른 서비스에서 필요한 빈들을 자동으로 등록합니다.
 * 서비스에서 LinkedOut 의존성을 추가하면 자동으로 필요한 빈들이 주입됩니다.
 * <p>
 * 주요 제공 기능:
 * - JSON 유틸리티: 객체와 JSON 간 변환
 * - 에러 응답 처리: 표준화된 에러 응답 생성
 * - 서비스 간 메시징: 서비스 간 비동기 통신을 위한 컴포넌트
 * <p>
 * 모든 빈들은 의존성 주입을 위해 자동으로 등록되며, 별도의 설정 없이 바로 사용할 수 있습니다.
 */
@Configuration
public class AutoConfiguration {
	/**
	 * JSON 처리를 위한 JsonUtils 빈을 생성하고 제공합니다.
	 * JsonUtils 클래스는 Java 객체와 JSON 문자열 간의 변환을 단순화합니다.
	 * 이 빈은 JSON 직렬화 및 역직렬화를 위해 제공된 ObjectMapper 인스턴스를 사용합니다.
	 *
	 * @param objectMapper JSON 작업에 사용될 ObjectMapper 인스턴스
	 * @return 지정된 ObjectMapper로 구성된 JsonUtils 인스턴스
	 */
	@Bean
	public JsonUtils jsonUtils(ObjectMapper objectMapper) {
		return new JsonUtils(objectMapper);
	}

	/**
	 * HTTP와 메시징 사용 사례 모두에 대한 오류 응답의 생성을 단순화하고 표준화하는
	 * ErrorResponseBuilder의 인스턴스를 제공합니다. 이 메서드는 상세하고 구조화된 오류
	 * 메시지 포맷팅을 위해 제공된 ObjectMapper와 통합됩니다.
	 *
	 * @param objectMapper 오류 응답을 JSON 형식으로 직렬화하는 데 사용되는 ObjectMapper 인스턴스
	 * @return 지정된 ObjectMapper로 구성된 새로운 ErrorResponseBuilder 인스턴스
	 */
	@Bean
	public ErrorResponseBuilder errorResponseBuilder(ObjectMapper objectMapper) {
		return new ErrorResponseBuilder(objectMapper);
	}

	/**
	 * API 특정 메시징 응답을 처리하기 위한 ApiMessageResponseHandler의 인스턴스를 제공합니다.
	 * 이 핸들러는 Redis와 통합되며 JSON 처리를 위해 제공된 ObjectMapper를 사용합니다.
	 *
	 * @param reactiveRedisTemplate Redis 데이터 저장소와 상호 작용하기 위한 ReactiveRedisTemplate 인스턴스
	 * @param connectionFactory     Redis 연결을 관리하기 위한 ReactiveRedisConnectionFactory
	 * @param objectMapper          JSON 직렬화 및 역직렬화를 위한 ObjectMapper 인스턴스
	 * @return 지정된 매개변수로 구성된 새로운 ApiMessageResponseHandler 인스턴스
	 */
	@Bean
	public ApiMessageResponseHandler apiMessageResponseHandler(
		ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
		ReactiveRedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper) {
		return new ApiMessageResponseHandler(reactiveRedisTemplate,
			connectionFactory,
			objectMapper);
	}

	/**
	 * Redis와 Jackson ObjectMapper를 통합하여 서비스 간 메시징 응답을 처리하는
	 * ServiceMessageResponseHandler 타입의 빈을 제공합니다. 이 핸들러는 응답 채널 관리,
	 * 메시지 직렬화 및 서비스와 관련된 메시지 흐름 조정을 담당합니다.
	 *
	 * @param reactiveRedisTemplate 기본 데이터 저장소로 Redis와 상호 작용하기 위한 ReactiveRedisTemplate 인스턴스
	 * @param connectionFactory     Redis 서버에 대한 연결을 관리하기 위한 ReactiveRedisConnectionFactory
	 * @param objectMapper          JSON 직렬화 및 역직렬화를 용이하게 하는 ObjectMapper 인스턴스
	 * @param serviceIdentifier     서비스 이름과 같은 현재 서비스에 대한 세부 정보를 제공하는 ServiceIdentifier 인스턴스
	 * @return 메시징 응답을 처리하도록 구성된 ServiceMessageResponseHandler 인스턴스
	 */
	@Bean
	public ServiceMessageResponseHandler serviceMessageResponseHandler(
		ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
		ReactiveRedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper,
		ServiceIdentifier serviceIdentifier) {
		return new ServiceMessageResponseHandler(reactiveRedisTemplate, connectionFactory, objectMapper, serviceIdentifier);
	}

	/**
	 * RabbitMQ와 ObjectMapper를 사용한 JSON 직렬화/역직렬화를 통해 서비스 간 통신을 담당하는
	 * ServiceMessageClient용 빈을 제공합니다. 이 클라이언트는 메시지 응답을 처리하기 위해
	 * ServiceMessageResponseHandler와 통합되며 메시지 라우팅 및 서비스 식별을 위해
	 * ServiceIdentifier를 사용합니다.
	 *
	 * @param rabbitTemplate                RabbitMQ에 메시지를 보내는 데 사용되는 RabbitTemplate 인스턴스
	 * @param objectMapper                  JSON 직렬화 및 역직렬화에 사용되는 ObjectMapper 인스턴스
	 * @param serviceMessageResponseHandler 응답 채널을 관리하고 다른 서비스의 응답을 기다리는 핸들러
	 * @param serviceIdentifier             서비스 이름 및 응답 라우팅 구성과 같은 현재 서비스에 대한 세부 정보를 제공하는 서비스 식별자
	 * @return 제공된 컴포넌트로 구성된 ServiceMessageClient 인스턴스
	 */
	@Bean
	public ServiceMessageClient serviceMessageClient(
		RabbitTemplate rabbitTemplate,
		ObjectMapper objectMapper,
		ServiceMessageResponseHandler serviceMessageResponseHandler,
		ServiceIdentifier serviceIdentifier) {
		return new ServiceMessageClient(rabbitTemplate, objectMapper, serviceMessageResponseHandler, serviceIdentifier);
	}

	@Bean
	@ConditionalOnMissingBean // 이미 ModelMapper 빈이 등록되어 있지 않은 경우에만 등록
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();

		modelMapper.getConfiguration()
			.setMatchingStrategy(MatchingStrategies.STRICT)
			.setFieldMatchingEnabled(true)
			.setSkipNullEnabled(true)
			.setFieldAccessLevel(AccessLevel.PRIVATE);

		return modelMapper;
	}

	@Bean
	@ConditionalOnMissingBean
	public PayloadConverter payloadConverter(ObjectMapper objectMapper) {
		return new PayloadConverter(objectMapper);
	}
}
