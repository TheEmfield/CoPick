package com.copick.recommendations.repository;

import com.copick.recommendations.entity.RecommendationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationSnapshotRepository extends JpaRepository<RecommendationSnapshot, Integer> {
    Optional<RecommendationSnapshot> findByGroupGroupId(Integer groupId);

    @Modifying
    void deleteByGroupGroupId(Integer groupId);
}