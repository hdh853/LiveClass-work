package com.example.enrollment.global.resolver;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfo {

    private final Long id;
    private final UserRole role;

    public boolean isCreator() {
        return this.role == UserRole.CREATOR;
    }

    public boolean isStudent() {
        return this.role == UserRole.STUDENT;
    }

    public enum UserRole {
        CREATOR, STUDENT
    }
}
