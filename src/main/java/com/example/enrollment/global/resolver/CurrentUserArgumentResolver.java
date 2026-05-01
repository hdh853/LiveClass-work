package com.example.enrollment.global.resolver;

import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(UserInfo.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        String userIdHeader = webRequest.getHeader(HEADER_USER_ID);
        String userRoleHeader = webRequest.getHeader(HEADER_USER_ROLE);

        if (!StringUtils.hasText(userIdHeader) || !StringUtils.hasText(userRoleHeader)) {
            throw new BusinessException(ErrorCode.MISSING_USER_INFO);
        }

        try {
            Long userId = Long.parseLong(userIdHeader);
            UserInfo.UserRole role = UserInfo.UserRole.valueOf(userRoleHeader.toUpperCase());
            return new UserInfo(userId, role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.MISSING_USER_INFO,
                    "유효하지 않은 사용자 정보입니다. X-User-Id는 숫자, X-User-Role은 CREATOR 또는 STUDENT여야 합니다.");
        }
    }
}
