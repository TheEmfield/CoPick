package com.copick.groups.repository;

import com.copick.groups.entity.GroupMember;
import com.copick.groups.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    // Проверяет, состоит ли пользователь в группе
    boolean existsByGroupIdAndProfileId(Integer groupId, Integer profileId);

    // Возвращает список ID пользователей, состоящих в группе (нужно для пересчета group_vibes)
    List<Integer> findProfileIdByGroupId(Integer groupId);

    // Возвращает список ID групп, в которых состоит пользователь
    List<Integer> findGroupIdByProfileId(Integer profileId);
}