package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class GroupNotFoundException extends DomainException {
    public GroupNotFoundException(Integer groupId) {
        super("Группа с id=" + groupId + " не найдена", HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND");
    }

    public GroupNotFoundException(String inviteCode) {
        super(
            "Группа с invite-кодом '" + inviteCode + "' не найдена",
            HttpStatus.NOT_FOUND,
            "GROUP_NOT_FOUND");
    }
}