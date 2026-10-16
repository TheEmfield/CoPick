package com.copick.preferences.repository;

import com.copick.preferences.entity.PreferenceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreferenceHistoryRepository extends JpaRepository<PreferenceHistory, Integer> {
    List<PreferenceHistory> findByProfileIdOrderByChangedAtDesc(Integer profileId);
}