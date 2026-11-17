package com.copick.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupMemberId;
import com.copick.groups.entity.GroupRole;
import com.copick.groups.exception.AlreadyMemberException;
import com.copick.groups.exception.CannotKickOwnerException;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService: жизненный цикл групп и участников")
class GroupServiceTest {

    private static final Integer OWNER_ID = 9;
    private static final Integer OTHER_MEMBER_ID = 2;
    private static final Integer GROUP_ID = 1;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, groupMemberRepository, profileRepository);
    }

    private static Group group(int id, String inviteCode) {
        Group group = new Group();
        group.setGroupId(id);
        group.setName("Кино по пятницам");
        group.setInviteCode(inviteCode);
        group.setAgeRestriction((short) 16);
        return group;
    }

    private static GroupMember member(int groupId, int profileId, GroupRole role, String joinedAt) {
        GroupMember member = new GroupMember();
        member.setId(new GroupMemberId(groupId, profileId));
        member.setGroup(group(groupId, "ABCD1234"));
        Profile profile = new Profile();
        profile.setProfileId(profileId);
        profile.setNickname("user" + profileId);
        member.setProfile(profile);
        member.setRole(role);
        member.setJoinedAt(OffsetDateTime.parse(joinedAt));
        return member;
    }

    private void stubOwner() {
        when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OWNER_ID))
                .thenReturn(Optional.of(member(GROUP_ID, OWNER_ID, GroupRole.OWNER, "2026-01-01T10:00:00Z")));
    }

    private void stubGroupExists() {
        when(groupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(group(GROUP_ID, "ABCD1234")));
    }

    @Nested
    @DisplayName("createGroup: создание группы")
    class CreateGroup {

        @Test
        @DisplayName("создаёт группу и делает создателя владельцем")
        void createsGroupAndMakesCreatorOwner() {
            when(groupRepository.existsByInviteCode(anyString())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
                Group saved = invocation.getArgument(0);
                saved.setGroupId(GROUP_ID);
                return saved;
            });

            Group created = groupService.createGroup("Кино по пятницам", (short) 16, OWNER_ID);

            assertThat(created.getGroupId()).isEqualTo(GROUP_ID);
            assertThat(created.getName()).isEqualTo("Кино по пятницам");
            assertThat(created.getInviteCode()).matches("[0-9A-F]{8}");

            ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
            verify(groupMemberRepository).save(memberCaptor.capture());
            GroupMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getRole()).isEqualTo(GroupRole.OWNER);
            assertThat(savedMember.getId())
                    .isEqualTo(new GroupMemberId(GROUP_ID, OWNER_ID));
        }

        @Test
        @DisplayName("возрастное ограничение null допустимо (без ограничения)")
        void nullAgeRestrictionIsAllowed() {
            when(groupRepository.existsByInviteCode(anyString())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
                Group saved = invocation.getArgument(0);
                saved.setGroupId(GROUP_ID);
                return saved;
            });

            Group created = groupService.createGroup("Без ограничений", null, OWNER_ID);

            assertThat(created.getAgeRestriction()).isNull();
            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("пустое или слишком длинное название отклоняется")
        void rejectsInvalidGroupName() {
            assertThatThrownBy(() -> groupService.createGroup("   ", null, OWNER_ID))
                    .isInstanceOf(InvalidGroupNameException.class);

            assertThatThrownBy(() -> groupService.createGroup(null, null, OWNER_ID))
                    .isInstanceOf(InvalidGroupNameException.class);

            assertThatThrownBy(() -> groupService.createGroup("а".repeat(65), null, OWNER_ID))
                    .isInstanceOf(InvalidGroupNameException.class);

            verify(groupRepository, never()).save(any(Group.class));
        }

        @Test
        @DisplayName("возрастное ограничение вне [0, 21] отклоняется")
        void rejectsInvalidAgeRestriction() {
            assertThatThrownBy(() -> groupService.createGroup("Группа", (short) -1, OWNER_ID))
                    .isInstanceOf(InvalidAgeRestrictionException.class);

            assertThatThrownBy(() -> groupService.createGroup("Группа", (short) 22, OWNER_ID))
                    .isInstanceOf(InvalidAgeRestrictionException.class);

            verify(groupRepository, never()).save(any(Group.class));
        }

        @Test
        @DisplayName("если уникальный invite-код не сгенерирован за 5 попыток -> исключение")
        void throwsWhenInviteCodeGenerationFails() {
            when(groupRepository.existsByInviteCode(anyString())).thenReturn(true);

            assertThatThrownBy(() -> groupService.createGroup("Группа", null, OWNER_ID))
                    .isInstanceOf(InviteCodeGenerationException.class);

            verify(groupRepository, times(5)).existsByInviteCode(anyString());
            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Nested
    @DisplayName("joinGroup: вступление по invite-коду")
    class JoinGroup {

        @Test
        @DisplayName("успешное вступление с ролью MEMBER")
        void joinsGroupAsMember() {
            Group group = group(GROUP_ID, "ABCD1234");
            when(groupRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(group));
            when(groupMemberRepository.existsById(new GroupMemberId(GROUP_ID, 5))).thenReturn(false);

            Group joined = groupService.joinGroup("ABCD1234", 5);

            assertThat(joined).isSameAs(group);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(groupMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(GroupRole.MEMBER);
            assertThat(captor.getValue().getId()).isEqualTo(new GroupMemberId(GROUP_ID, 5));
        }

        @Test
        @DisplayName("неизвестный invite-код -> GROUP_NOT_FOUND")
        void unknownInviteCodeThrows() {
            when(groupRepository.findByInviteCode("NOPE1234")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.joinGroup("NOPE1234", 5))
                    .isInstanceOf(GroupNotFoundException.class);

            verify(groupMemberRepository, never()).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("повторное вступление -> ALREADY_MEMBER")
        void alreadyMemberThrows() {
            when(groupRepository.findByInviteCode("ABCD1234"))
                    .thenReturn(Optional.of(group(GROUP_ID, "ABCD1234")));
            when(groupMemberRepository.existsById(new GroupMemberId(GROUP_ID, 5))).thenReturn(true);

            assertThatThrownBy(() -> groupService.joinGroup("ABCD1234", 5))
                    .isInstanceOf(AlreadyMemberException.class);

            verify(groupMemberRepository, never()).save(any(GroupMember.class));
        }
    }

    @Nested
    @DisplayName("Чтение групп и участников")
    class ReadOperations {

        @Test
        @DisplayName("getGroupById возвращает группу или GROUP_NOT_FOUND")
        void getGroupByIdFindsOrThrows() {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group(GROUP_ID, "ABCD1234")));
            assertThat(groupService.getGroupById(GROUP_ID).getGroupId()).isEqualTo(GROUP_ID);

            when(groupRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> groupService.getGroupById(999))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("getMembers сначала проверяет существование группы")
        void getMembersChecksGroupExists() {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.getMembers(GROUP_ID))
                    .isInstanceOf(GroupNotFoundException.class);

            verify(groupMemberRepository, never()).findByIdGroupId(GROUP_ID);
        }

        @Test
        @DisplayName("getMembers возвращает участников группы")
        void getMembersReturnsMembers() {
            stubGroupExists();
            List<GroupMember> members = List.of(
                    member(GROUP_ID, OWNER_ID, GroupRole.OWNER, "2026-01-01T10:00:00Z"),
                    member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z"));
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(members);

            assertThat(groupService.getMembers(GROUP_ID)).isEqualTo(members);
        }

        @Test
        @DisplayName("getMemberRole возвращает роль участника")
        void getMemberRoleReturnsRole() {
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.of(member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z")));

            assertThat(groupService.getMemberRole(GROUP_ID, 5)).isEqualTo(GroupRole.MEMBER);
        }

        @Test
        @DisplayName("getMemberRole для не-участника -> MEMBER_NOT_FOUND")
        void getMemberRoleForNonMemberThrows() {
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.getMemberRole(GROUP_ID, 5))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Операции, доступные только владельцу")
    class OwnerOperations {

        @Test
        @DisplayName("deleteGroup удаляет группу владельцем")
        void deleteGroupByOwner() {
            Group group = group(GROUP_ID, "ABCD1234");
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
            stubOwner();

            groupService.deleteGroup(GROUP_ID, OWNER_ID);

            verify(groupRepository).deleteGroupById(GROUP_ID);
        }

        @Test
        @DisplayName("deleteGroup не-владельцем -> NOT_GROUP_OWNER")
        void deleteGroupByNonOwnerThrows() {
            when(groupRepository.findById(GROUP_ID))
                    .thenReturn(Optional.of(group(GROUP_ID, "ABCD1234")));
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.of(member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z")));

            assertThatThrownBy(() -> groupService.deleteGroup(GROUP_ID, 5))
                    .isInstanceOf(NotGroupOwnerException.class);

            verify(groupRepository, never()).deleteGroupById(GROUP_ID);
        }

        @Test
        @DisplayName("deleteGroup несуществующей группы -> GROUP_NOT_FOUND")
        void deleteMissingGroupThrows() {
            when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.deleteGroup(GROUP_ID, OWNER_ID))
                    .isInstanceOf(GroupNotFoundException.class);
        }

        @Test
        @DisplayName("transferOwnership меняет роли владельца и нового владельца")
        void transferOwnershipSwapsRoles() {
            GroupMember currentOwner = member(GROUP_ID, OWNER_ID, GroupRole.OWNER, "2026-01-01T10:00:00Z");
            GroupMember newOwner = member(GROUP_ID, OTHER_MEMBER_ID, GroupRole.MEMBER, "2026-01-05T10:00:00Z");
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OWNER_ID))
                    .thenReturn(Optional.of(currentOwner));
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OTHER_MEMBER_ID))
                    .thenReturn(Optional.of(newOwner));

            groupService.transferOwnership(GROUP_ID, OWNER_ID, OTHER_MEMBER_ID);

            assertThat(currentOwner.getRole()).isEqualTo(GroupRole.MEMBER);
            assertThat(newOwner.getRole()).isEqualTo(GroupRole.OWNER);
            verify(groupMemberRepository).save(currentOwner);
            verify(groupMemberRepository).save(newOwner);
        }

        @Test
        @DisplayName("transferOwnership не-владельцем -> NOT_GROUP_OWNER")
        void transferOwnershipByNonOwnerThrows() {
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.of(member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z")));

            assertThatThrownBy(() -> groupService.transferOwnership(GROUP_ID, 5, OTHER_MEMBER_ID))
                    .isInstanceOf(NotGroupOwnerException.class);
        }

        @Test
        @DisplayName("transferOwnership несуществующему участнику -> MEMBER_NOT_FOUND")
        void transferOwnershipToNonMemberThrows() {
            GroupMember currentOwner = member(GROUP_ID, OWNER_ID, GroupRole.OWNER, "2026-01-01T10:00:00Z");
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OWNER_ID))
                    .thenReturn(Optional.of(currentOwner));
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OTHER_MEMBER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.transferOwnership(GROUP_ID, OWNER_ID, OTHER_MEMBER_ID))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("regenerateInviteCode выдаёт новый код")
        void regenerateInviteCodeProducesNewOne() {
            stubGroupExists();
            stubOwner();
            when(groupRepository.existsByInviteCode(anyString())).thenReturn(false);
            when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Group updated = groupService.regenerateInviteCode(GROUP_ID, OWNER_ID);

            assertThat(updated.getInviteCode()).isNotEqualTo("ABCD1234").matches("[0-9A-F]{8}");
        }

        @Test
        @DisplayName("regenerateInviteCode не-владельцем -> NOT_GROUP_OWNER")
        void regenerateInviteCodeByNonOwnerThrows() {
            stubGroupExists();
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.of(member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z")));

            assertThatThrownBy(() -> groupService.regenerateInviteCode(GROUP_ID, 5))
                    .isInstanceOf(NotGroupOwnerException.class);

            verify(groupRepository, never()).save(any(Group.class));
        }

        @Test
        @DisplayName("updateName меняет название")
        void updateNameChangesName() {
            stubGroupExists();
            stubOwner();
            when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Group updated = groupService.updateName(GROUP_ID, OWNER_ID, "Новое название");

            assertThat(updated.getName()).isEqualTo("Новое название");
        }

        @Test
        @DisplayName("updateName с некорректным названием -> INVALID_GROUP_NAME")
        void updateNameWithInvalidNameThrows() {
            stubGroupExists();
            stubOwner();

            assertThatThrownBy(() -> groupService.updateName(GROUP_ID, OWNER_ID, "  "))
                    .isInstanceOf(InvalidGroupNameException.class);

            verify(groupRepository, never()).save(any(Group.class));
        }

        @Test
        @DisplayName("updateAgeRestriction меняет ограничение, null снимает его")
        void updateAgeRestrictionSetsAndClears() {
            stubGroupExists();
            stubOwner();
            when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(groupService.updateAgeRestriction(GROUP_ID, OWNER_ID, (short) 18).getAgeRestriction())
                    .isEqualTo((short) 18);
            assertThat(groupService.updateAgeRestriction(GROUP_ID, OWNER_ID, null).getAgeRestriction())
                    .isNull();
        }

        @Test
        @DisplayName("updateAgeRestriction вне [0, 21] -> INVALID_AGE_RESTRICTION")
        void updateAgeRestrictionWithInvalidValueThrows() {
            stubGroupExists();
            stubOwner();

            assertThatThrownBy(() -> groupService.updateAgeRestriction(GROUP_ID, OWNER_ID, (short) 30))
                    .isInstanceOf(InvalidAgeRestrictionException.class);

            verify(groupRepository, never()).save(any(Group.class));
        }
    }

    @Nested
    @DisplayName("kickMember и leaveGroup")
    class KickAndLeave {

        @Test
        @DisplayName("kickMember удаляет участника владельцем")
        void kickMemberRemovesMember() {
            stubOwner();
            GroupMember target = member(GROUP_ID, OTHER_MEMBER_ID, GroupRole.MEMBER, "2026-01-05T10:00:00Z");
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OTHER_MEMBER_ID))
                    .thenReturn(Optional.of(target));

            groupService.kickMember(GROUP_ID, OWNER_ID, OTHER_MEMBER_ID);

            verify(groupMemberRepository).delete(target);
        }

        @Test
        @DisplayName("владелец не может кикнуть сам себя")
        void ownerCannotKickHimself() {
            stubOwner();

            assertThatThrownBy(() -> groupService.kickMember(GROUP_ID, OWNER_ID, OWNER_ID))
                    .isInstanceOf(CannotKickOwnerException.class);

            verify(groupMemberRepository, never()).delete(any(GroupMember.class));
        }

        @Test
        @DisplayName("kickMember не-владельцем -> NOT_GROUP_OWNER")
        void kickByNonOwnerThrows() {
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.of(member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z")));

            assertThatThrownBy(() -> groupService.kickMember(GROUP_ID, 5, OTHER_MEMBER_ID))
                    .isInstanceOf(NotGroupOwnerException.class);
        }

        @Test
        @DisplayName("kick несуществующего участника -> MEMBER_NOT_FOUND")
        void kickUnknownMemberThrows() {
            stubOwner();
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OTHER_MEMBER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.kickMember(GROUP_ID, OWNER_ID, OTHER_MEMBER_ID))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("участник покидает группу без передачи владения")
        void memberLeavesWithoutOwnershipTransfer() {
            GroupMember leaver = member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z");
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.of(leaver));

            groupService.leaveGroup(GROUP_ID, 5);

            verify(groupMemberRepository).delete(leaver);
            verify(groupMemberRepository).flush();
            verify(groupMemberRepository, never()).save(any(GroupMember.class));
            verify(groupRepository, never()).deleteById(any(Integer.class));
        }

        @Test
        @DisplayName("владелец уходит -> владение переходит старейшему участнику")
        void ownerLeavesAndOldestMemberBecomesOwner() {
            GroupMember owner = member(GROUP_ID, OWNER_ID, GroupRole.OWNER, "2026-01-01T10:00:00Z");
            GroupMember earlier = member(GROUP_ID, 3, GroupRole.MEMBER, "2026-01-02T10:00:00Z");
            GroupMember later = member(GROUP_ID, 4, GroupRole.MEMBER, "2026-01-09T10:00:00Z");
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OWNER_ID))
                    .thenReturn(Optional.of(owner));
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(List.of(later, earlier));

            groupService.leaveGroup(GROUP_ID, OWNER_ID);

            verify(groupMemberRepository).delete(owner);
            assertThat(earlier.getRole()).isEqualTo(GroupRole.OWNER);
            assertThat(later.getRole()).isEqualTo(GroupRole.MEMBER);
            verify(groupMemberRepository).save(earlier);
            verify(groupRepository, never()).deleteById(any(Integer.class));
        }

        @Test
        @DisplayName("последний участник уходит -> группа удаляется")
        void lastMemberLeavingDeletesGroup() {
            GroupMember owner = member(GROUP_ID, OWNER_ID, GroupRole.OWNER, "2026-01-01T10:00:00Z");
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, OWNER_ID))
                    .thenReturn(Optional.of(owner));
            when(groupMemberRepository.findByIdGroupId(GROUP_ID)).thenReturn(List.of());

            groupService.leaveGroup(GROUP_ID, OWNER_ID);

            verify(groupRepository).deleteById(GROUP_ID);
            verify(groupMemberRepository, never()).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("уход не-участника -> MEMBER_NOT_FOUND")
        void nonMemberLeaveThrows() {
            when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> groupService.leaveGroup(GROUP_ID, 5))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Test
    @DisplayName("requireMember проходит для участника и падает для чужого")
    void requireMemberChecksMembership() {
        when(groupMemberRepository.findByIdGroupIdAndIdProfileId(GROUP_ID, 5))
                .thenReturn(Optional.of(member(GROUP_ID, 5, GroupRole.MEMBER, "2026-01-05T10:00:00Z")))
                .thenReturn(Optional.empty());

        groupService.requireMember(GROUP_ID, 5); // не падает

        assertThatThrownBy(() -> groupService.requireMember(GROUP_ID, 5))
                .isInstanceOf(MemberNotFoundException.class);
    }
}
