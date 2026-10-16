package com.copick.preferences.repository;

import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfilePreferenceRepository extends JpaRepository<ProfilePreference, ProfilePreferenceId> {
    Optional<ProfilePreference> findByProfileIdAndGenreId(Integer profileId, Integer genreId);
    List<ProfilePreference> findByProfileId(Integer profileId);
    List<ProfilePreference> findByGenreId(Integer genreId);
}