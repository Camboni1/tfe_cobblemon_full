package be.loic.tfe_cobblemon.common.translation.repository;

import be.loic.tfe_cobblemon.common.translation.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

    @Query("select t from Translation t where t.locale = :locale")
    List<Translation> findAllByLocale(String locale);
    @Modifying
    @Query(value = """
    INSERT INTO translation (key, locale, value)
    VALUES (:key, :locale, :value)
    ON CONFLICT (key, locale) DO UPDATE SET value = EXCLUDED.value
""", nativeQuery = true)
    void upsert(
            @Param("key") String key,
            @Param("locale") String locale,
            @Param("value") String value
    );
}