package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AlreadyMemberException extends DomainException {

    public AlreadyMemberException(Integer groupId, Integer profileId) {
        super(
            "Профиль " + profileId + " уже состоит в группе " + groupId,
            HttpStatus.CONFLICT,
            "ALREADY_MEMBER"
        );
    }
}