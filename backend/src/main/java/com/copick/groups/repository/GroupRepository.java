package com.copick.groups.repository;

import com.copick.groups.entity.Group;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    Optional<Group> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    @Modifying
    @Query("DELETE FROM Group g WHERE g.groupId = :id")
    int deleteGroupById(@Param("id") Integer id);
}