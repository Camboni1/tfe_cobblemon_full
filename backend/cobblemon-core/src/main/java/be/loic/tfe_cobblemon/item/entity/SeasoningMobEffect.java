package be.loic.tfe_cobblemon.item.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "seasoning_mob_effect",
        indexes = {
                @Index(name = "idx_seasoning_mob_effect_seasoning_id", columnList = "seasoning_id")
        }
)
public class SeasoningMobEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "seasoning_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_seasoning_mob_effect_seasoning")
    )
    private Seasoning seasoning;

    @Column(name = "effect_id", nullable = false, length = 40)
    private String effectId;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "amplifier", nullable = false)
    private Short amplifier = 0;

    @Column(name = "ambient", nullable = false)
    private Boolean ambient = false;

    @Column(name = "visible", nullable = false)
    private Boolean visible = true;

    @Column(name = "show_icon", nullable = false)
    private Boolean showIcon = true;
}