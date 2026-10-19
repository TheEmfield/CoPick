package com.copick.groups.service;

import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupMemberId;
import com.copick.groups.entity.GroupRole;
import com.copick.groups.exception.AlreadyMemberException;
import com.copick.groups.exception.GroupNotFoundException;
import com.copick.groups.exception.InvalidAgeRestrictionException;
import com.copick.groups.exception.InvalidGroupNameException;
import com.copick.groups.exception.InviteCodeGenerationException;
import com.copick.groups.exception.MemberNotFoundException;
import com.copick.groups.exception.NotGroupOwnerException;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.groups.repository.GroupRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.repository.ProfileRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private static final int INVITE_CODE_LENGTH = 8;
    private static final int MAX_INVITE_CODE_ATTEMPTS = 5;

    private static final int GROUP_NAME_MAX_LENGTH = 64;
    private static final short AGE_RESTRICTION_MIN = 0;
    private static final short AGE_RESTRICTION_MAX = 21;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ProfileRepository profileRepository;

    /** Создает новую группу и делает создателя владельцем */
    @Transactional
    public Group createGroup(String name, Short ageRestriction, Integer creatorProfileId) {
        validateGroupName(name);
        validateAgeRestriction(ageRestriction);

        Group group = new Group();
        group.setName(name);
        group.setAgeRestriction(ageRestriction);
        group.setInviteCode(generateUniqueInviteCode());

        group = groupRepository.save(group);

        Profile creatorRef = profileRepository.getReferenceById(creatorProfileId);

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setProfile(creatorRef);
        member.setRole(GroupRole.OWNER);
        groupMemberRepository.save(member);

        log.info("Создана группа: groupId={}, owner={}", group.getGroupId(), creatorProfileId);

        return group;
    }

    /** Вступление в группу по коду */
    @Transactional
    public Group joinGroup(String inviteCode, Integer profileId) {
        Group group =
            groupRepository
                .findByInviteCode(inviteCode)
                .orElseThrow(() -> new GroupNotFoundException(inviteCode));

        if (groupMemberRepository.existsById(new GroupMemberId(group.getGroupId(), profileId))) {
          throw new AlreadyMemberException(group.getGroupId(), profileId);
        }

        Profile profileRef = profileRepository.getReferenceById(profileId);

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setProfile(profileRef);
        member.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(member);

        log.info("Профиль {} вступил в группу {}", profileId, group.getGroupId());

        return group;
    }

    /** Возвращает метаданные группы по ID. */
    @Transactional(readOnly = true)
    public Group getGroupById(Integer groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    /**
    * Возвращает членства пользователя с группами. Возвращаются GroupMember (а не
    * Group), потому что caller'у нужна роль (OWNER/MEMBER) для каждой группы. Сам объект Group
    * доступен через member.getGroup().
    */
    @Transactional(readOnly = true)
    public List<GroupMember> getGroupsByProfileId(Integer profileId) {
        return groupMemberRepository.findByIdProfileId(profileId);
    }

    /**
    * Возвращает участников группы с профилями.
    *
    * @return список членств (profile + role)
    */
    @Transactional(readOnly = true)
    public List<GroupMember> getMembers(Integer groupId) {
        getGroupById(groupId);
        return groupMemberRepository.findByIdGroupId(groupId);
    }

    /** Возвращает роль пользователя в группе. */
    @Transactional(readOnly = true)
    public GroupRole getMemberRole(Integer groupId, Integer profileId) {
        return findMember(groupId, profileId).getRole();
    }

    /**
    * Удаляет группу.
    *
    * @param ownerId профиль, инициирующий удаление — должен быть OWNER
    */
    @Transactional
    public void deleteGroup(Integer groupId, Integer ownerId) {
        Group group = getGroupById(groupId);
        requireOwner(groupId, ownerId);

        groupRepository.delete(group);

        log.info("Удалена группа: groupId={}, by profile={}", groupId, ownerId);
    }

    /** Передаёт владение группой другому участнику. */
    @Transactional
    public void transferOwnership(Integer groupId, Integer currentOwnerId, Integer newOwnerId) {
        requireOwner(groupId, currentOwnerId);

        if (currentOwnerId.equals(newOwnerId)) {
            return;
        }

        GroupMember currentOwner = findMember(groupId, currentOwnerId);
        GroupMember newOwner = findMember(groupId, newOwnerId);

        currentOwner.setRole(GroupRole.MEMBER);
        newOwner.setRole(GroupRole.OWNER);

        groupMemberRepository.save(currentOwner);
        groupMemberRepository.save(newOwner);

        log.info("Передано владение группой {}: {} → {}", groupId, currentOwnerId, newOwnerId);
    }

    /**
    * Удаляет участника из группы
    *
    * @param memberProfileId удаляемый участник — не может быть самим владельцем
    */
    @Transactional
    public void kickMember(Integer groupId, Integer ownerId, Integer memberProfileId) {
        requireOwner(groupId, ownerId);

        if (ownerId.equals(memberProfileId)) {
            throw new NotGroupOwnerException(groupId, memberProfileId);
        }

        GroupMember member = findMember(groupId, memberProfileId);
        groupMemberRepository.delete(member);

        log.info("Участник {} удалён из группы {} владельцем {}", memberProfileId, groupId, ownerId);
    }

    /** Генерирует новый invite-код для группы (старый становится недействительным). */
    @Transactional
    public Group regenerateInviteCode(Integer groupId, Integer ownerId) {
        Group group = getGroupById(groupId);
        requireOwner(groupId, ownerId);

        group.setInviteCode(generateUniqueInviteCode());
        group = groupRepository.save(group);

        log.info("Invite-код группы {} регенерирован владельцем {}", groupId, ownerId);

        return group;
    }

    /** Обновляет название группы. */
    @Transactional
    public Group updateName(Integer groupId, Integer ownerId, String newName) {
        Group group = getGroupById(groupId);
        requireOwner(groupId, ownerId);

        validateGroupName(newName);

        group.setName(newName);
        group = groupRepository.save(group);

        log.info("Название группы {} обновлено владельцем {}", groupId, ownerId);

        return group;
    }

    /** Обновляет возрастное ограничение группы. null означает снятие ограничения */
    @Transactional
    public Group updateAgeRestriction(Integer groupId, Integer ownerId, Short newAgeRestriction) {
        Group group = getGroupById(groupId);
        requireOwner(groupId, ownerId);

        validateAgeRestriction(newAgeRestriction);

        group.setAgeRestriction(newAgeRestriction);
        group = groupRepository.save(group);

        log.info(
            "Возрастное ограничение группы {} обновлено: {} (by {})",
            groupId,
            newAgeRestriction,
            ownerId);

        return group;
    }

    /**
    * Проверяет, что profileId — владелец группы.
    *
    * @throws NotGroupOwnerException если профиль не состоит в группе или не владелец
    */
    private void requireOwner(Integer groupId, Integer profileId) {
        GroupMember member =
            groupMemberRepository
                .findByIdGroupIdAndIdProfileId(groupId, profileId)
                .orElseThrow(() -> new NotGroupOwnerException(groupId, profileId));

        if (member.getRole() != GroupRole.OWNER) {
            throw new NotGroupOwnerException(groupId, profileId);
        }
    }

    /** Находит членство
     *
     * @throws MemberNotFoundException при отсутствии
     */
    private GroupMember findMember(Integer groupId, Integer profileId) {
        return groupMemberRepository
            .findByIdGroupIdAndIdProfileId(groupId, profileId)
            .orElseThrow(() -> new MemberNotFoundException(groupId, profileId));
    }

    /** Генерирует invite-код с защитой от коллизий. */
    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
            String code = UUID.randomUUID().toString().substring(0, INVITE_CODE_LENGTH).toUpperCase();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new InviteCodeGenerationException(MAX_INVITE_CODE_ATTEMPTS);
    }

    private void validateGroupName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidGroupNameException("Название группы не может быть пустым");
        }
        if (name.length() > GROUP_NAME_MAX_LENGTH) {
            throw new InvalidGroupNameException(
                "Название группы не может быть длиннее " + GROUP_NAME_MAX_LENGTH + " символов");
        }
    }

    private void validateAgeRestriction(Short age) {
        if (age == null) {
            // null = "без ограничения" — валидное значение
            return;
        }
        if (age < AGE_RESTRICTION_MIN || age > AGE_RESTRICTION_MAX) {
            throw new InvalidAgeRestrictionException(age);
        }
    }
}
