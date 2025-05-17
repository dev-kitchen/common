package com.linkedout.common.util;

import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 복잡한 리액티브 {@link Mono} 스트림의 가독성을 향상시키기 위해 체인 메서드 패턴을 제공합니다.
 * 이 클래스는 {@code Mono} 인스턴스를 캡슐화하고 체인 가능한 API를 제공하여
 * 기본 {@code Mono}에 대한 변환, 오류 처리 및 기타 작업을 적용할 수 있습니다.
 * <p>
 *
 * @param <T> 기본 {@code Mono}에 포함된 값의 타입
 */
public class MonoPipe<T> {
	private final Mono<T> mono;

	/**
	 * 주어진 {@code Mono}를 사용하여 {@code MonoPipe}의 새 인스턴스를 생성합니다.
	 *
	 * @param mono {@code MonoPipe} 내에서 래핑되고 처리될 리액티브 {@code Mono} 인스턴스
	 */
	private MonoPipe(Mono<T> mono) {
		this.mono = mono;
	}

	/**
	 * 일반 값을 사용하여 리액티브 파이프라인을 시작할 때 사용됩니다.
	 * 제공된 값을 방출하는 {@code Mono}를 포함하는 새로운 {@code MonoPipe} 인스턴스를 생성합니다.
	 * 예를 들어 non-reactive 컴포넌트와의 통합 시에 {@code Mono} 기반 처리를 구성하고자 할 때 활용할 수 있습니다.
	 * <p>
	 * 사용 예시:
	 * <pre>{@code
	 * MonoPipe<String> pipe = MonoPipe.of("안녕하세요");
	 * }</pre>
	 *
	 * @param <U>   {@code MonoPipe} 내에 캡슐화될 값의 타입
	 * @param value {@code Mono}로 래핑되어 {@code MonoPipe} 내에 캡슐화될 값
	 * @return 제공된 값의 {@code Mono}를 캡슐화하는 새로운 {@code MonoPipe} 인스턴스
	 */
	public static <U> MonoPipe<U> of(U value) {
		return new MonoPipe<>(Mono.just(value));
	}

	/**
	 * 제공된 {@code Mono}를 캡슐화하는 새로운 {@code MonoPipe} 인스턴스를 생성합니다.
	 *
	 * @param <U>  제공된 Mono에 포함된 값의 타입
	 * @param mono 캡슐화할 {@code Mono} 인스턴스
	 * @return 제공된 {@code Mono}를 포함하는 새로운 {@code MonoPipe} 인스턴스
	 */
	public static <U> MonoPipe<U> ofMono(Mono<U> mono) {
		return new MonoPipe<>(mono);
	}

	/**
	 * {@code MonoPipe}에 포함된 현재 값에 변환을 적용하고
	 * 변환 결과를 포함하는 새로운 {@code MonoPipe}를 반환합니다.
	 * 변환은 제공된 매핑 함수에 의해 정의됩니다.
	 *
	 * @param <R>    매핑 함수를 적용한 후의 결과 값 타입
	 * @param mapper 현재 {@code T} 타입의 값을 {@code R} 타입의 새로운 값으로 변환하는 함수
	 * @return {@code R} 타입의 변환된 값을 포함하는 새로운 {@code MonoPipe}
	 */
	public <R> MonoPipe<R> then(Function<? super T, ? extends R> mapper) {
		return new MonoPipe<>(mono.map(mapper));
	}

	/**
	 * 각 방출된 요소에 비동기 함수를 적용하고 결과 Publisher를 단일 Mono로 평면화하여
	 * MonoPipe에 캡슐화된 기본 Mono를 변환합니다.
	 *
	 * @param <R>    변환 후 결과 요소의 타입
	 * @param mapper {@code T} 타입의 방출된 요소를 {@code R} 타입의 Mono로 변환하는 함수
	 * @return 변환된 Mono를 캡슐화하는 새로운 MonoPipe 인스턴스
	 */
	public <R> MonoPipe<R> thenFlatMap(Function<? super T, ? extends Mono<? extends R>> mapper) {
		return new MonoPipe<>(mono.flatMap(mapper));
	}


	/**
	 * 기본 {@code Mono} 스트림에서 발생하는 오류를 지정된 오류 소비자를 실행하여 처리합니다.
	 *
	 * @param errorConsumer 오류 발생 시 {@code Throwable}을 처리하는 {@code Consumer}
	 * @return 오류 처리가 적용된 새로운 {@code MonoPipe<T>} 인스턴스
	 */
	public MonoPipe<T> handleError(Consumer<Throwable> errorConsumer) {
		return new MonoPipe<>(mono.doOnError(errorConsumer));
	}

	/**
	 * 기본 {@code Mono}가 비어 있을 때의 경우를 처리하는 메커니즘을 제공합니다.
	 * 이 메서드는 현재 {@code Mono}가 비어 있을 때 주어진 {@code emptySupplier}가 생성한 Mono로 전환합니다.
	 *
	 * @param emptySupplier 현재 {@code Mono}가 비어 있을 때 사용할 대체 {@code Mono}를 제공하는 {@code Supplier}
	 * @return 원래 값 또는 비어 있을 경우 대체 {@code Mono} 값을 포함하는 새로운 {@code MonoPipe}
	 */
	public MonoPipe<T> handleEmpty(Supplier<Mono<T>> emptySupplier) {
		return new MonoPipe<>(mono.switchIfEmpty(Mono.defer(emptySupplier)));
	}

	/**
	 * {@code MonoPipe}가 캡슐화한 기본 {@code Mono} 인스턴스를 반환하여 파이프라인 체인을 종료합니다.
	 * 이 메서드는 일반적으로 파이프라인의 마지막 단계에서 호출되어 최종 결과물을 가져옵니다.
	 *
	 * @return 모든 변환과 처리가 적용된 최종 {@code Mono} 인스턴스
	 */
	public Mono<T> result() {
		return mono;
	}
}
