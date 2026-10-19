package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends DomainException {
    public MemberNotFoundException(Integer groupId, Integer profileId) {
        super(
            "Профиль " + profileId + " не состоит в группе " + groupId,
            HttpStatus.NOT_FOUND,
            "MEMBER_NOT_FOUND");
    }
}
