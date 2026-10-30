package com.copick.preferences.service;

import com.copick.preferences.dto.PreferenceResponseDto;
import com.copick.preferences.dto.PreferenceUpdateDto;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.entity.PreferenceHistory;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import com.copick.preferences.exception.GenreNotFoundException;
import com.copick.preferences.exception.InvalidWeightException;
import com.copick.preferences.repository.GenreRepository;
import com.copick.preferences.repository.PreferenceHistoryRepository;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.ProfileNotFoundException;
import com.copick.profiles.repository.ProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final Double WEIGHT_MIN = -1.0;
    private static final Double WEIGHT_MAX = 1.0;

    private final ProfilePreferenceRepository preferenceRepository;
    private final PreferenceHistoryRepository historyRepository;
    private final ProfileRepository profileRepository;
    private final GenreRepository genreRepository;

    @Transactional
    public List<PreferenceResponseDto> updatePreferences(Integer profileId, List<PreferenceUpdateDto> updates) {
        if (!profileRepository.existsById(profileId)) {
            throw new ProfileNotFoundException(profileId);
        }

        if (updates == null || updates.isEmpty()) {
            return getPreferences(profileId);
        }

        for (PreferenceUpdateDto dto : updates) {
            validateWeight(dto.weight());
        }

        Set<Integer> requestedGenreIds = updates.stream()
            .map(PreferenceUpdateDto::genreId)
            .collect(Collectors.toSet());

        Map<Integer, Genre> existingGenres = genreRepository.findAllById(requestedGenreIds).stream()
            .collect(Collectors.toMap(Genre::getGenreId, Function.identity()));

        for (Integer genreId : requestedGenreIds) {
            if (!existingGenres.containsKey(genreId)) {
                throw new GenreNotFoundException(genreId);
            }
        }

        Profile profileRef = profileRepository.getReferenceById(profileId);

        for (PreferenceUpdateDto dto : updates) {
            ProfilePreferenceId id = new ProfilePreferenceId(profileId, dto.genreId());
            Genre genre = existingGenres.get(dto.genreId());

            ProfilePreference pref = preferenceRepository.findById(id)
                .orElseGet(() -> {
                    ProfilePreference newPref = new ProfilePreference();
                    newPref.setId(id);
                    newPref.setProfile(profileRef);
                    newPref.setGenre(genre);
                    return newPref;
                });

            Double oldWeight = pref.getWeight();

            if (!Objects.equals(oldWeight, dto.weight())) {
                pref.setWeight(dto.weight());
                preferenceRepository.save(pref);

                PreferenceHistory history = new PreferenceHistory();
                history.setProfile(pref.getProfile());
                history.setGenre(pref.getGenre());
                history.setOldWeight(oldWeight);
                history.setNewWeight(dto.weight());
                historyRepository.save(history);

                log.info("Обновлено предпочтение: profile={}, genre={}, old={}, new={}",
                    profileId, dto.genreId(), oldWeight, dto.weight());
            }
        }

        return getPreferences(profileId);
    }

    /** Получает все предпочтения пользователя с названиями жанров */
    @Transactional(readOnly = true)
    public List<PreferenceResponseDto> getPreferences(Integer profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new ProfileNotFoundException(profileId);
        }
        return preferenceRepository.findByProfileIdWithGenre(profileId)
            .stream()
            .map(PreferenceResponseDto::fromEntity)
            .toList();
    }

    /** Удаляет конкретное предпочтение пользователя */
    @Transactional
    public void deletePreference(Integer profileId, Integer genreId) {
        if (!profileRepository.existsById(profileId)) {
            throw new ProfileNotFoundException(profileId);
        }
        preferenceRepository.deleteByProfileIdAndGenreId(profileId, genreId);
        log.info("Удалено предпочтение: profileId={}, genreId={}", profileId, genreId);
    }

    /** Удаляет все предпочтения пользователя */
    @Transactional
    public void deleteAllPreferences(Integer profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new ProfileNotFoundException(profileId);
        }
        preferenceRepository.deleteAllByProfileId(profileId);
        log.info("Удалены все предпочтения для profileId={}", profileId);
    }

    /** Возвращает историю изменений предпочтений пользователя */
    @Transactional(readOnly = true)
    public List<PreferenceHistory> getPreferenceHistory(Integer profileId) {
        return historyRepository.findByProfileIdOrderByChangedAtDesc(profileId);
    }

    private void validateWeight(Double weight) {
        if (weight == null) {
            throw new InvalidWeightException();
        }
        if (weight.compareTo(WEIGHT_MIN) < 0 || weight.compareTo(WEIGHT_MAX) > 0) {
            throw new InvalidWeightException(weight);
        }
    }
}