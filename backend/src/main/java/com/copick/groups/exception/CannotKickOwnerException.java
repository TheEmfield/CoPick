package com.copick.groups.exception;

public class CannotKickOwnerException extends RuntimeException {
    public CannotKickOwnerException(Integer groupId, Integer profileId) {
        super("Нельзя кикнуть владельца группы (groupId=" + groupId + ", profileId=" + profileId + ")");
    }
}