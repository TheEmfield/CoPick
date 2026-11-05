package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CannotKickOwnerException extends DomainException {

    public CannotKickOwnerException(Integer groupId, Integer profileId) {
        super(
                "Нельзя исключить владельца группы через этот эндпоинт. Для выхода используйте 'Покинуть группу'.",
                HttpStatus.BAD_REQUEST,
                "CANNOT_KICK_OWNER"
        );
    }
}