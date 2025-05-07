package com.linkedout.common.messaging;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ResponseRegistry {
	private ResponseRegistry() {
		throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
	}

	// static 필드로 맵 관리
	private static final Map<String, Sinks.One<Object>> RESPONSE_HANDLERS = new ConcurrentHashMap<>();

	// static 메서드로 접근
	public static void registerHandler(String correlationId, Sinks.One<Object> sink) {
		RESPONSE_HANDLERS.put(correlationId, sink);
	}

	public static void completeResponse(String correlationId, Object response) {
		Sinks.One<Object> sink = RESPONSE_HANDLERS.get(correlationId);
		if (sink != null) {
			sink.tryEmitValue(response);
		} else {
			log.warn("응답 핸들러를 찾을 수 없음: {}", correlationId);
		}
	}

	public static void completeWithError(String correlationId, Throwable error) {
		Sinks.One<Object> sink = RESPONSE_HANDLERS.get(correlationId);
		if (sink != null) {
			sink.tryEmitError(error);
		} else {
			log.warn("응답 핸들러를 찾을 수 없음: {}", correlationId);
		}
	}

	public static void removeHandler(String correlationId) {
		RESPONSE_HANDLERS.remove(correlationId);
	}
}
