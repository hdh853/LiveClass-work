package com.example.enrollment.global.resolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 사용하여 현재 사용자 정보를 주입받습니다.
 * HTTP 헤더의 X-User-Id, X-User-Role 값을 기반으로 UserInfo를 생성합니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
