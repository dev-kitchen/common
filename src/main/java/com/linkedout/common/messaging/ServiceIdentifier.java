package com.linkedout.common.messaging;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ServiceIdentifier 클래스는 라우팅 키와 큐 이름과 같은 서비스별 식별 정보를 제공합니다.
 * 애플리케이션 구성에서 서비스 이름을 가져와서 이러한 식별자를 동적으로 생성하는 데
 * 사용합니다.
 * <p>
 * 이 클래스는 Spring 컨텍스트 내에서 관리되는 빈으로 사용할 수 있도록 Spring Component로
 * 표시됩니다.
 */
@Getter
@Component
public class ServiceIdentifier {
	/**
	 * 이 식별자와 연관된 서비스의 이름을 나타냅니다.
	 * 이 값은 일반적으로 서비스 인스턴스의 컨텍스트에 맞게 동적으로 조정되도록
	 * 애플리케이션의 구성 또는 속성 파일에서 로드됩니다.
	 * <p>
	 * serviceName은 메시징 또는 기타 서비스 관련 요구사항에 대한 라우팅 키와
	 * 큐 이름을 구성하는 기본 식별자로 사용됩니다.
	 */
	private final String serviceName;

	/**
	 * 새로운 ServiceIdentifier 인스턴스를 생성하고 애플리케이션 구성에서 가져온
	 * 서비스 이름으로 초기화합니다.
	 *
	 * @param serviceName 애플리케이션 구성을 통해 주입되는 서비스 이름
	 */
	public ServiceIdentifier(@Value("${service.name}") String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * 서비스 리스너의 라우팅 키를 생성하고 반환합니다.
	 * 라우팅 키는 서비스 이름을 사용하여 동적으로 구성됩니다.
	 *
	 * @return 서비스 리스너의 라우팅 키를 나타내는 문자열
	 */
	public String getResponseRoutingKey() {
		return serviceName + ".listener.routing.key";
	}

	/**
	 * 서비스별 응답 큐의 이름을 생성하고 반환합니다.
	 * 응답 큐 이름은 서비스 이름을 사용하여 동적으로 구성됩니다.
	 *
	 * @return "<serviceName>.service.listener" 형식으로 동적 구성된 응답 큐 이름
	 */
	public String getResponseQueueName() {
		return serviceName + ".service.listener";
	}
}
