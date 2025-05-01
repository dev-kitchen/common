package com.linkedout.common.exception;

/**
 * 사용자가 접근 권한이 없는 리소스에 접근을 시도할 때 발생하는 예외를 나타냅니다.
 * 이 예외는 HTTP 403 Forbidden 상태 코드와 연관되어 있습니다.
 * <p>
 * 이 클래스는 {@code BaseException}을 확장하여 특정 리소스에 대한
 * 접근권한 제한 상황을 처리할 수 있도록 사용자 정의 오류 메시지를 제공합니다.
 */
public class ForbiddenException extends BaseException {

	/**
	 * 접근 권한이 없는 리소스에 대한 요청이 있을 때 발생하는 예외를 생성합니다.
	 * 이 예외는 HTTP 403 Forbidden 상태 코드와 함께 사용됩니다.
	 *
	 * @param message 예외와 관련된 사용자 정의 오류 메시지
	 */
	public ForbiddenException(String message) {
		super(message, 403);
	}
}
