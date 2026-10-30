package com.copick.preferences.repository;

import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilePreferenceRepository
    extends JpaRepository<ProfilePreference, ProfilePreferenceId> {
    @Query("SELECT p FROM ProfilePreference p JOIN FETCH p.genre WHERE p.id.profileId = :profileId ORDER BY p.id.genreId ASC")
    List<ProfilePreference> findByProfileIdWithGenre(@Param("profileId") Integer profileId);

    @Query("SELECT p FROM ProfilePreference p WHERE p.id.genreId = :genreId")
    List<ProfilePreference> findByGenreId(@Param("genreId") Integer genreId);

    @Modifying
    @Query("DELETE FROM ProfilePreference p WHERE p.id.profileId = :profileId")
    int deleteAllByProfileId(@Param("profileId") Integer profileId);

    @Modifying
    @Query("DELETE FROM ProfilePreference p WHERE p.id.profileId = :profileId AND p.id.genreId = :genreId")
    int deleteByProfileIdAndGenreId(@Param("profileId") Integer profileId, @Param("genreId") Integer genreId);
}