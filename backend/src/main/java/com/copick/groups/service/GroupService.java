package com.copick.groups.service;

import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.groups.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * Создает новую группу и делает создателя админом
     */
    @Transactional
    public Group createGroup(String name, Short ageRestriction, Integer creatorProfileId) {
        Group group = new Group();
        group.setName(name);
        group.setAgeRestriction(ageRestriction);
        // Генерируем короткий уникальный код приглашения (например, 8 символов)
        group.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        group = groupRepository.save(group);

        // Добавляем создателя как админа
        GroupMember member = new GroupMember();
        member.setGroupId(group.getGroupId());
        member.setProfileId(creatorProfileId);
        member.setRole("owner");
        groupMemberRepository.save(member);

        return group;
    }

    /**
     * Вступление в группу по коду
     */
    @Transactional
    public Group joinGroup(String inviteCode, Integer profileId) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Группа с таким кодом не найдена"));

        // Проверка, не состоит ли уже пользователь в группе
        if (groupMemberRepository.existsByGroupIdAndProfileId(group.getGroupId(), profileId)) {
            throw new RuntimeException("Вы уже состоите в этой группе");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(group.getGroupId());
        member.setProfileId(profileId);
        member.setRole("member");
        groupMemberRepository.save(member);

        return group;
    }
}