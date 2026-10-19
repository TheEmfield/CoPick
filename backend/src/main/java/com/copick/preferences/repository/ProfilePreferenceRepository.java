package com.copick.preferences.repository;

import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilePreferenceRepository
    extends JpaRepository<ProfilePreference, ProfilePreferenceId> {
    @Query("select p from ProfilePreference p where p.id.profileId = :profileId")
    List<ProfilePreference> findByProfileId(@Param("profileId") Integer profileId);

    @Query("select p from ProfilePreference p where p.id.genreId = :genreId")
    List<ProfilePreference> findByGenreId(@Param("genreId") Integer genreId);
    long deleteByProfileId(Integer profileId);
}