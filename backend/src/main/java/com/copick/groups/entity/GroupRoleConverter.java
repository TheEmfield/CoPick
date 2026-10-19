package com.copick.groups.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GroupRoleConverter implements AttributeConverter<GroupRole, String> {

    @Override
    public String convertToDatabaseColumn(GroupRole role) {
        return role == null ? null : role.name().toLowerCase();
    }

    @Override
    public GroupRole convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : GroupRole.valueOf(dbValue.toUpperCase());
    }
}
