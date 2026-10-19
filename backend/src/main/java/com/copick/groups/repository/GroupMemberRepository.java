package com.copick.groups.repository;

import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupMemberId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    // Возвращает список ID пользователей, состоящих в группе.
    @Query("select gm.id.profileId from GroupMember gm where gm.id.groupId = :groupId")
    List<Integer> findProfileIdsByGroupId(@Param("groupId") Integer groupId);

    // Возвращает список ID групп, в которых состоит пользователь.
    @Query("select gm.id.groupId from GroupMember gm where gm.id.profileId = :profileId")
    List<Integer> findGroupIdsByProfileId(@Param("profileId") Integer profileId);

    // Возвращает участников группы с профилями.
    @EntityGraph(attributePaths = "profile")
    List<GroupMember> findByIdGroupId(Integer groupId);

    // Возвращает членства пользователя с группами.
    // Используется для "мои группы с ролью" одним запросом.
    @EntityGraph(attributePaths = "group")
    List<GroupMember> findByIdProfileId(Integer profileId);

    // Точечный поиск конкретного членства — для проверки роли, kick, transferOwnership.
    Optional<GroupMember> findByIdGroupIdAndIdProfileId(Integer groupId, Integer profileId);
}