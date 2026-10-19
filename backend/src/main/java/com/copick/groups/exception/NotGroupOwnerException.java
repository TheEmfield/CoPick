package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class NotGroupOwnerException extends DomainException {
    public NotGroupOwnerException(Integer groupId, Integer profileId) {
        super(
            "Профиль " + profileId + " не является владельцем группы " + groupId,
            HttpStatus.FORBIDDEN,
            "NOT_GROUP_OWNER");
    }
}
