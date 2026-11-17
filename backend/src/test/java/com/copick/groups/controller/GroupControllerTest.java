package com.copick.groups.controller;

import static com.copick.testsupport.ControllerTestSupport.authenticateAs;
import static com.copick.testsupport.ControllerTestSupport.clearAuthentication;
import static com.copick.testsupport.ControllerTestSupport.standaloneWithPrincipal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.copick.exception.GlobalExceptionHandler;
import com.copick.groups.entity.Group;
import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupMemberId;
import com.copick.groups.entity.GroupRole;
import com.copick.groups.exception.GroupNotFoundException;
import com.copick.groups.exception.NotGroupOwnerException;
import com.copick.groups.service.GroupService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupController: REST /api/v1/groups")
class GroupControllerTest {

    private static final Integer PROFILE_ID = 7;

    @Mock
    private GroupService groupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneWithPrincipal(new GroupController(groupService), new GlobalExceptionHandler());
        authenticateAs(PROFILE_ID);
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    private static Group group(int id, String name, String inviteCode) {
        Group group = new Group();
        group.setGroupId(id);
        group.setName(name);
        group.setInviteCode(inviteCode);
        group.setAgeRestriction((short) 16);
        group.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        return group;
    }

    private static GroupMember member(Group group, int profileId, GroupRole role) {
        GroupMember member = new GroupMember();
        member.setId(new GroupMemberId(group.getGroupId(), profileId));
        member.setGroup(group);
        member.setProfile(new com.copick.profiles.entity.Profile());
        member.getProfile().setProfileId(profileId);
        member.getProfile().setNickname("user" + profileId);
        member.setRole(role);
        member.setJoinedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        return member;
    }

    @Test
    @DisplayName("GET / -> список групп текущего профиля с ролью и числом участников")
    void getMyGroupsReturnsListItems() throws Exception {
        Group group = group(1, "Кино по пятницам", "ABCD1234");
        GroupMember membership = member(group, PROFILE_ID, GroupRole.OWNER);

        when(groupService.getGroupsByProfileId(PROFILE_ID)).thenReturn(List.of(membership));
        when(groupService.countMembers(1)).thenReturn(3);

        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(1))
                .andExpect(jsonPath("$[0].name").value("Кино по пятницам"))
                .andExpect(jsonPath("$[0].inviteCode").value("ABCD1234"))
                .andExpect(jsonPath("$[0].role").value("owner"))
                .andExpect(jsonPath("$[0].memberCount").value(3));
    }

    @Test
    @DisplayName("POST / -> создание группы")
    void createGroupReturnsDetails() throws Exception {
        Group group = group(1, "Новая группа", "ABCD1234");
        when(groupService.createGroup("Новая группа", (short) 16, PROFILE_ID)).thenReturn(group);
        when(groupService.getMembers(1)).thenReturn(List.of(member(group, PROFILE_ID, GroupRole.OWNER)));

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Новая группа", "ageRestriction": 16}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(1))
                .andExpect(jsonPath("$.name").value("Новая группа"))
                .andExpect(jsonPath("$.inviteCode").value("ABCD1234"))
                .andExpect(jsonPath("$.ageRestriction").value(16))
                .andExpect(jsonPath("$.ownerId").value(PROFILE_ID))
                .andExpect(jsonPath("$.members[0].role").value("owner"));
    }

    @Test
    @DisplayName("POST /join -> вступление по invite-коду")
    void joinGroupReturnsDetails() throws Exception {
        Group group = group(1, "Кино по пятницам", "ABCD1234");
        when(groupService.joinGroup("ABCD1234", PROFILE_ID)).thenReturn(group);
        when(groupService.getMembers(1))
                .thenReturn(List.of(
                        member(group, 9, GroupRole.OWNER),
                        member(group, PROFILE_ID, GroupRole.MEMBER)));

        mockMvc.perform(post("/api/v1/groups/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode": "ABCD1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(1))
                .andExpect(jsonPath("$.ownerId").value(9))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[1].role").value("member"));
    }

    @Test
    @DisplayName("GET /{id} -> детали группы для участника")
    void getGroupDetailsReturnsDetails() throws Exception {
        Group group = group(1, "Кино по пятницам", "ABCD1234");
        when(groupService.getGroupById(1)).thenReturn(group);
        when(groupService.getMembers(1)).thenReturn(List.of(member(group, PROFILE_ID, GroupRole.OWNER)));

        mockMvc.perform(get("/api/v1/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Кино по пятницам"))
                .andExpect(jsonPath("$.members[0].nickname").value("user" + PROFILE_ID));

        verify(groupService).requireMember(1, PROFILE_ID);
    }

    @Test
    @DisplayName("GET /{id} несуществующей группы -> 404 GROUP_NOT_FOUND")
    void getMissingGroupReturns404() throws Exception {
        when(groupService.getGroupById(1)).thenThrow(new GroupNotFoundException(1));

        mockMvc.perform(get("/api/v1/groups/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GROUP_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /{id} -> 204 без тела")
    void deleteGroupReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/1"))
                .andExpect(status().isNoContent());

        verify(groupService).deleteGroup(1, PROFILE_ID);
    }

    @Test
    @DisplayName("DELETE /{id} не-владельцем -> 403 NOT_GROUP_OWNER")
    void deleteGroupByNonOwnerReturns403() throws Exception {
        org.mockito.Mockito.doThrow(new NotGroupOwnerException(1, PROFILE_ID))
                .when(groupService).deleteGroup(1, PROFILE_ID);

        mockMvc.perform(delete("/api/v1/groups/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_GROUP_OWNER"));
    }

    @Test
    @DisplayName("DELETE /{id}/members/{memberProfileId} -> 204")
    void kickMemberReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/1/members/5"))
                .andExpect(status().isNoContent());

        verify(groupService).kickMember(1, PROFILE_ID, 5);
    }

    @Test
    @DisplayName("POST /{id}/regenerate-invite -> новый код")
    void regenerateInviteCodeReturnsNewCode() throws Exception {
        Group group = group(1, "Кино по пятницам", "NEWCODE9");
        when(groupService.regenerateInviteCode(1, PROFILE_ID)).thenReturn(group);

        mockMvc.perform(post("/api/v1/groups/1/regenerate-invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value("NEWCODE9"));
    }

    @Test
    @DisplayName("POST /{id}/leave -> 204")
    void leaveGroupReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/groups/1/leave"))
                .andExpect(status().isNoContent());

        verify(groupService).leaveGroup(1, PROFILE_ID);
    }
}
