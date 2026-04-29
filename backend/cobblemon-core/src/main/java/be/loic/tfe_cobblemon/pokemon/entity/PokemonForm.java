package be.loic.tfe_cobblemon.pokemon.entity;


import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
import jakarta.persistence.*;
import be.loic.tfe_cobblemon.drop.entity.PokemonDrop;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "pokemon_form",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pokemon_form_code", columnNames = {"pokemon_id", "code"}),
                @UniqueConstraint(name = "uq_pokemon_form_id_pokemon", columnNames = {"id", "pokemon_id"})
        },
        indexes = {
                @Index(name = "idx_pokemon_form_default", columnList = "pokemon_id, is_default"),
                @Index(name = "idx_pokemon_form_pokemon_id", columnList = "pokemon_id")
        }
)
public class PokemonForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pokemon_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pokemon_form_pokemon"))
    private Pokemon pokemon;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "battle_only", nullable = false)
    private Boolean battleOnly = false;

    @Column(name = "primary_type", nullable = false, length = 16)
    private String primaryType;

    @Column(name = "secondary_type", length = 16)
    private String secondaryType;

    @Column(name = "home_form_id")
    private Integer homeFormId;

    @Column(name = "male_ratio", precision = 4, scale = 3)
    private BigDecimal maleRatio;

    @Column(name = "height_dm")
    private Integer heightDm;

    @Column(name = "weight_hg")
    private Integer weightHg;

    @Column(name = "catch_rate")
    private Short catchRate;

    @Column(name = "base_experience_yield")
    private Short baseExperienceYield;

    @Column(name = "experience_group", length = 20)
    private String experienceGroup;

    @Column(name = "egg_cycles")
    private Short eggCycles;

    @Column(name = "base_friendship")
    private Short baseFriendship;

    @Column(name = "base_scale", precision = 5, scale = 3)
    private BigDecimal baseScale;

    @Column(name = "base_hp", nullable = false)
    private Short baseHp;

    @Column(name = "base_attack", nullable = false)
    private Short baseAttack;

    @Column(name = "base_defense", nullable = false)
    private Short baseDefense;

    @Column(name = "base_special_attack", nullable = false)
    private Short baseSpecialAttack;

    @Column(name = "base_special_defense", nullable = false)
    private Short baseSpecialDefense;

    @Column(name = "base_speed", nullable = false)
    private Short baseSpeed;

    @Column(name = "ev_hp", nullable = false)
    private Short evHp = 0;

    @Column(name = "ev_attack", nullable = false)
    private Short evAttack = 0;

    @Column(name = "ev_defense", nullable = false)
    private Short evDefense = 0;

    @Column(name = "ev_special_attack", nullable = false)
    private Short evSpecialAttack = 0;

    @Column(name = "ev_special_defense", nullable = false)
    private Short evSpecialDefense = 0;

    @Column(name = "ev_speed", nullable = false)
    private Short evSpeed = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aspects_json", columnDefinition = "jsonb")
    private String aspectsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels_json", columnDefinition = "jsonb")
    private String labelsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;

    @OneToMany(mappedBy = "pokemonForm", fetch = FetchType.LAZY)
    private List<SpawnRule> spawnRules = new ArrayList<>();

    @OneToMany(mappedBy = "pokemonForm", fetch = FetchType.LAZY)
    private List<PokemonDrop> drops = new ArrayList<>();

}