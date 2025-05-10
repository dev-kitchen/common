package com.linkedout.common.messaging;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ServiceIdentifier {
	// 서비스 이름 (예: "auth-service", "account-service" 등)
	private final String serviceName;

	public ServiceIdentifier(@Value("${service.name}") String serviceName) {
		this.serviceName = serviceName;
	}

	// 서비스의 라우팅 키 생성
	public String getResponseRoutingKey() {
		return serviceName + ".listener.routing.key";
	}

	public String getResponseQueueName() {
		return serviceName + ".service.listener";
	}
}