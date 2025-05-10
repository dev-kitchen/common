package com.linkedout.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ApiMessageResponseHandler;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageClient;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.util.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class AutoConfiguration {
	@Bean
	public JsonUtils jsonUtils(ObjectMapper objectMapper) {
		return new JsonUtils(objectMapper);
	}

	@Bean
	public ErrorResponseBuilder errorResponseBuilder(ObjectMapper objectMapper) {
		return new ErrorResponseBuilder(objectMapper);
	}

	@Bean
	public ApiMessageResponseHandler apiMessageResponseHandler(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
																														 ReactiveRedisConnectionFactory connectionFactory,
																														 ObjectMapper objectMapper) {
		return new ApiMessageResponseHandler(reactiveRedisTemplate,
			connectionFactory,
			objectMapper);
	}

	@Bean
	public ServiceMessageResponseHandler serviceMessageResponseHandler(ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
																																		 ReactiveRedisConnectionFactory connectionFactory,
																																		 ObjectMapper objectMapper,
																																		 ServiceIdentifier serviceIdentifier) {
		return new ServiceMessageResponseHandler(reactiveRedisTemplate, connectionFactory, objectMapper, serviceIdentifier);
	}

	@Bean
	public ServiceMessageClient serviceMessageClient(RabbitTemplate rabbitTemplate,
																									 ObjectMapper objectMapper,
																									 ServiceMessageResponseHandler serviceMessageResponseHandler,
																									 ServiceIdentifier serviceIdentifier) {
		return new ServiceMessageClient(rabbitTemplate, objectMapper, serviceMessageResponseHandler, serviceIdentifier);
	}
}
