package be.loic.tfe_cobblemon.common.translation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "translation",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_translation_key_locale",
        columnNames = {"key", "locale"}
    )
)
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", nullable = false, length = 80)
    private String key;

    @Column(name = "locale", nullable = false, length = 5)
    private String locale;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")  // ← était length = 255
    private String value;
}