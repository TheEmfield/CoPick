package com.copick.preferences.repository;

import com.copick.preferences.entity.PreferenceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PreferenceHistoryRepository extends JpaRepository<PreferenceHistory, Integer> {
  @Query(
      "select h from PreferenceHistory h "
          + "where h.profile.profileId = :profileId "
          + "order by h.changedAt desc")
  List<PreferenceHistory> findByProfileIdOrderByChangedAtDesc(
      @Param("profileId") Integer profileId);
}