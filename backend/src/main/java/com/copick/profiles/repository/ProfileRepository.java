package com.copick.profiles.repository;

import com.copick.profiles.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    // Для register()
    boolean existsByNickname(String nickname);

    // Для login()
    Optional<Profile> findByNickname(String nickname);
}
