package com.copick.preferences.service;

import com.copick.preferences.dto.PreferenceUpdateDto;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.entity.PreferenceHistory;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import com.copick.preferences.exception.InvalidWeightException;
import com.copick.preferences.repository.GenreRepository;
import com.copick.preferences.repository.PreferenceHistoryRepository;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.repository.ProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final BigDecimal WEIGHT_MIN = BigDecimal.ONE.negate(); // -1
    private static final BigDecimal WEIGHT_MAX = BigDecimal.ONE; //  1

    private final ProfilePreferenceRepository preferenceRepository;
    private final PreferenceHistoryRepository historyRepository;
    private final ProfileRepository profileRepository;
    private final GenreRepository genreRepository;

    @Transactional
    public void updatePreferences(Integer profileId, List<PreferenceUpdateDto> updates) {
        for (PreferenceUpdateDto dto : updates) {
            validateWeight(dto.getWeight());
        }

        Profile profileRef = profileRepository.getReferenceById(profileId);

        for (PreferenceUpdateDto dto : updates) {
            ProfilePreferenceId id = new ProfilePreferenceId(profileId, dto.getGenreId());

            ProfilePreference pref = preferenceRepository
                .findById(id)
                .orElseGet(() -> {
                    ProfilePreference newPref = new ProfilePreference();
                    newPref.setProfile(profileRef);
                    newPref.setGenre(genreRepository.getReferenceById(dto.getGenreId()));
                    return newPref;
                });

            BigDecimal oldWeight = pref.getWeight();

            pref.setWeight(dto.getWeight());
            preferenceRepository.save(pref);

            PreferenceHistory history = new PreferenceHistory();
            history.setProfile(pref.getProfile());
            history.setGenre(pref.getGenre());
            history.setOldWeight(oldWeight);
            history.setNewWeight(dto.getWeight());
            historyRepository.save(history);

            log.info("Обновлено предпочтение: profile={}, genre={}, old={}, new={}",
                    profileId, dto.getGenreId(), oldWeight, dto.getWeight());
        }
    }

    /** Получает все предпочтения пользователя */
    @Transactional(readOnly = true)
    public List<ProfilePreference> getPreferences(Integer profileId) {
        return preferenceRepository.findByProfileId(profileId);
    }

    /** Сбрасывает конкретное предпочтение пользователя. */
    @Transactional
    public void resetPreference(Integer profileId, Integer genreId) {
        ProfilePreferenceId id = new ProfilePreferenceId(profileId, genreId);

        Optional<ProfilePreference> existing = preferenceRepository.findById(id);
        if (existing.isEmpty()) {
            return;
        }

        ProfilePreference pref = existing.get();
        BigDecimal oldWeight = pref.getWeight();

        PreferenceHistory history = new PreferenceHistory();
        history.setProfile(pref.getProfile());
        history.setGenre(pref.getGenre());
        history.setOldWeight(oldWeight);
        history.setNewWeight(null);
        historyRepository.save(history);

        preferenceRepository.delete(pref);

        log.info("Сброшено предпочтение: profile={}, genre={}, old={}", profileId, genreId, oldWeight);
    }

    /**
    * Сбрасывает все предпочтения пользователя.
    *
    * @return количество удалённых предпочтений
    */
    @Transactional
    public int resetAllPreferences(Integer profileId) {
        List<ProfilePreference> prefs = preferenceRepository.findByProfileId(profileId);

        if (prefs.isEmpty()) {
            return 0;
        }

        for (ProfilePreference pref : prefs) {
            BigDecimal oldWeight = pref.getWeight();

            PreferenceHistory history = new PreferenceHistory();
            history.setProfile(pref.getProfile());
            history.setGenre(pref.getGenre());
            history.setOldWeight(oldWeight);
            history.setNewWeight(null);
            historyRepository.save(history);

            preferenceRepository.delete(pref);
        }

        log.info("Сброшены все предпочтения профиля {}: {} шт.", profileId, prefs.size());

        return prefs.size();
    }

    /** Возвращает историю изменений предпочтений пользователя, отсортированную от новых к старым. */
    @Transactional(readOnly = true)
    public List<PreferenceHistory> getPreferenceHistory(Integer profileId) {
        return historyRepository.findByProfileIdOrderByChangedAtDesc(profileId);
    }

    private void validateWeight(BigDecimal weight) {
        if (weight == null) {
            throw new InvalidWeightException();
        }
        if (weight.compareTo(WEIGHT_MIN) < 0 || weight.compareTo(WEIGHT_MAX) > 0) {
            throw new InvalidWeightException(weight);
        }
    }
}