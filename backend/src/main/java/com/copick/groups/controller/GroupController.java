package com.copick.groups.controller;

import com.copick.groups.dto.*;
import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupRole;
import com.copick.groups.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /** Список всех групп текущего профиля */
    @GetMapping
    public ResponseEntity<List<GroupListItemDto>> getMyGroups(
            @AuthenticationPrincipal Integer currentProfileId) {

        List<GroupListItemDto> result = groupService.getGroupsByProfileId(currentProfileId).stream()
                .map(m -> new GroupListItemDto(
                        m.getGroup().getGroupId(),
                        m.getGroup().getName(),
                        m.getGroup().getInviteCode(),
                        m.getRole().name().toLowerCase(),
                        groupService.countMembers(m.getGroup().getGroupId())))
                .toList();

        return ResponseEntity.ok(result);
    }

    /** Создать группу */
    @PostMapping
    public ResponseEntity<GroupDetailsDto> createGroup(
            @AuthenticationPrincipal Integer currentProfileId,
            @Valid @RequestBody GroupCreateRequest request) {

        Group group = groupService.createGroup(
                request.name(), request.ageRestriction(), currentProfileId);

        return ResponseEntity.ok(toDetailsDto(group));
    }

    /** Вступить по invite_code */
    @PostMapping("/join")
    public ResponseEntity<GroupDetailsDto> joinGroup(
            @AuthenticationPrincipal Integer currentProfileId,
            @Valid @RequestBody GroupJoinRequest request) {

        Group group = groupService.joinGroup(request.inviteCode(), currentProfileId);
        return ResponseEntity.ok(toDetailsDto(group));
    }

    /** Вся инфа по группе (только для участников) */
    @GetMapping("/{id}")
    public ResponseEntity<GroupDetailsDto> getGroupDetails(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.requireMember(id, currentProfileId);
        return ResponseEntity.ok(toDetailsDto(groupService.getGroupById(id)));
    }

    /** Удалить группу (только owner) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.deleteGroup(id, currentProfileId);
        return ResponseEntity.noContent().build();
    }

    /** Кикнуть участника (только owner) */
    @DeleteMapping("/{id}/members/{memberProfileId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable Integer id,
            @PathVariable Integer memberProfileId,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.kickMember(id, currentProfileId, memberProfileId);
        return ResponseEntity.noContent().build();
    }

    /** Обновить invite-код (только owner) */
    @PostMapping("/{id}/regenerate-invite")
    public ResponseEntity<Map<String, String>> regenerateInviteCode(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        Group group = groupService.regenerateInviteCode(id, currentProfileId);
        return ResponseEntity.ok(Map.of("inviteCode", group.getInviteCode()));
    }

    /** Покинуть группу */
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer currentProfileId) {

        groupService.leaveGroup(id, currentProfileId);
        return ResponseEntity.noContent().build();
    }

    // ---------- mapping ----------

    private GroupDetailsDto toDetailsDto(Group group) {
        List<GroupMember> members = groupService.getMembers(group.getGroupId());

        List<GroupDetailsDto.MemberDto> memberDtos = members.stream()
                .map(m -> new GroupDetailsDto.MemberDto(
                        m.getProfile().getProfileId(),
                        m.getProfile().getNickname(),
                        m.getRole().name().toLowerCase()))
                .toList();

        Integer ownerId = members.stream()
                .filter(m -> m.getRole() == GroupRole.OWNER)
                .map(m -> m.getProfile().getProfileId())
                .findFirst()
                .orElse(null);

        return new GroupDetailsDto(
                group.getGroupId(),
                group.getName(),
                group.getInviteCode(),
                group.getAgeRestriction(),
                ownerId,
                group.getCreatedAt(),
                memberDtos);
    }
}