package com.copick.groups.controller;

import com.copick.groups.dto.GroupCreateRequest;
import com.copick.groups.dto.GroupDetailsDto;
import com.copick.groups.dto.GroupJoinRequest;
import com.copick.groups.dto.GroupListItemDto;
import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupRole;
import com.copick.groups.repository.GroupMemberRepository;
import com.copick.groups.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;

    // Конструкторная инъекция, как у напарника
    public GroupController(GroupService groupService, GroupMemberRepository groupMemberRepository) {
        this.groupService = groupService;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * 1. Список всех групп текущего профиля
     */
    @GetMapping
    public ResponseEntity<List<GroupListItemDto>> getMyGroups(
            @AuthenticationPrincipal Integer currentProfileId) {

        List<GroupMember> memberships = groupService.getGroupsByProfileId(currentProfileId);

        List<GroupListItemDto> result = memberships.stream().map(m -> {
            // Получаем количество участников группы
            int memberCount = groupMemberRepository.findProfileIdsByGroupId(m.getGroup().getGroupId()).size();

            return new GroupListItemDto(
                    m.getGroup().getGroupId(),
                    m.getGroup().getName(),
                    m.getGroup().getInviteCode(),
                    m.getRole().name().toLowerCase(), // Конвертер уже делает это в БД, но для JSON явно приводим к lowerCase
                    memberCount
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * 2. Создать группу
     */
    @PostMapping
    public ResponseEntity<Group> createGroup(
            @AuthenticationPrincipal Integer currentProfileId,
            @RequestBody GroupCreateRequest request) {

        Group createdGroup = groupService.createGroup(
                request.name(),
                request.ageRestriction(),
                currentProfileId
        );

        return ResponseEntity.ok(createdGroup);
    }

    /**
     * 3. Вступить по invite_code
     */
    @PostMapping("/join")
    public ResponseEntity<Group> joinGroup(
            @AuthenticationPrincipal Integer currentProfileId,
            @RequestBody GroupJoinRequest request) {

        Group joinedGroup = groupService.joinGroup(request.inviteCode(), currentProfileId);
        return ResponseEntity.ok(joinedGroup);
    }

    /**
     * 4. Вся инфа по группе
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupDetailsDto> getGroupDetails(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {
        groupService.getMemberRole(id, currentProfileId);

        Group group = groupService.getGroupById(id);
        List<GroupMember> members = groupService.getMembers(id);

        List<GroupDetailsDto.MemberDto> memberDtos = members.stream().map(m ->
                new GroupDetailsDto.MemberDto(
                        m.getProfile().getProfileId(),
                        m.getProfile().getNickname(),
                        m.getRole().name().toLowerCase()
                )
        ).toList();

        GroupDetailsDto response = new GroupDetailsDto(
                group.getGroupId(),
                group.getName(),
                group.getInviteCode(),
                group.getAgeRestriction(),
                group.getCreatedAt(),
                memberDtos
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 5. Удалить группу (только owner)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.deleteGroup(id, currentProfileId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * 6. Кикнуть участника (только owner)
     */
    @DeleteMapping("/{id}/members/{memberProfileId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable Integer id,
            @PathVariable Integer memberProfileId,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.kickMember(id, currentProfileId, memberProfileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 7. Обновить invite-код (только owner)
     */
    @PostMapping("/{id}/regenerate-invite")
    public ResponseEntity<Map<String, String>> regenerateInviteCode(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        Group updatedGroup = groupService.regenerateInviteCode(id, currentProfileId);
        return ResponseEntity.ok(Map.of("inviteCode", updatedGroup.getInviteCode()));
    }

    /**
     * 8. Покинуть группу
     */
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.leaveGroup(id, currentProfileId);
        throw new UnsupportedOperationException("Метод leaveGroup нужно добавить в GroupService");
    }
}
