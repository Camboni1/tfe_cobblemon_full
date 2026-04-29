package be.loic.tfe_cobblemon.spawn.entity;

import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.spawn.converter.SpawnBucketConverter;
import be.loic.tfe_cobblemon.spawn.converter.SpawnTypeConverter;
import be.loic.tfe_cobblemon.spawn.converter.SpawnablePositionTypeConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnBucket;
import be.loic.tfe_cobblemon.spawn.enums.SpawnType;
import be.loic.tfe_cobblemon.spawn.enums.SpawnablePositionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "spawn_rule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_spawn_rule_external_id_per_file",
                        columnNames = {"spawn_source_file_id", "external_id"}
                )
        },
        indexes = {
                @Index(name = "idx_spawn_rule_pokemon", columnList = "pokemon_id"),
                @Index(name = "idx_spawn_rule_form", columnList = "pokemon_form_id"),
                @Index(name = "idx_spawn_rule_bucket", columnList = "bucket"),
                @Index(name = "idx_spawn_rule_position_type", columnList = "spawnable_position_type"),
                @Index(name = "idx_spawn_rule_source_file_id", columnList = "spawn_source_file_id")
        }
)
public class SpawnRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "spawn_source_file_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spawn_rule_source_file")
    )
    private SpawnSourceFile spawnSourceFile;

    @Column(name = "external_id", nullable = false, length = 40)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pokemon_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spawn_rule_pokemon")
    )
    private Pokemon pokemon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pokemon_form_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_spawn_rule_pokemon_form")
    )
    private PokemonForm pokemonForm;

    @Column(name = "form_selector", length = 160)
    private String formSelector;

    @Column(name = "target_expression", nullable = false, length = 120)
    private String targetExpression;

    @Convert(converter = SpawnTypeConverter.class)
    @Column(name = "spawn_type", nullable = false, length = 20)
    private SpawnType spawnType;

    @Convert(converter = SpawnablePositionTypeConverter.class)
    @Column(name = "spawnable_position_type", nullable = false, length = 20)
    private SpawnablePositionType spawnablePositionType;

    @Convert(converter = SpawnBucketConverter.class)
    @Column(name = "bucket", nullable = false, length = 20)
    private SpawnBucket bucket;

    @Column(name = "level_min", nullable = false)
    private Short levelMin;

    @Column(name = "level_max", nullable = false)
    private Short levelMax;

    @Column(name = "weight", nullable = false, precision = 8, scale = 3)
    private BigDecimal weight;

    @Column(name = "max_herd_size")
    private Short maxHerdSize;

    @Column(name = "min_distance_between_spawns", precision = 6, scale = 2)
    private BigDecimal minDistanceBetweenSpawns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weight_multiplier_json", columnDefinition = "jsonb")
    private String weightMultiplierJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weight_multipliers_json", columnDefinition = "jsonb")
    private String weightMultipliersJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "herdable_pokemon_json", columnDefinition = "jsonb")
    private String herdablePokemonJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "spawn_rule_preset",
            joinColumns = @JoinColumn(
                    name = "spawn_rule_id",
                    foreignKey = @ForeignKey(name = "fk_spawn_rule_preset_rule")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "spawn_preset_id",
                    foreignKey = @ForeignKey(name = "fk_spawn_rule_preset_preset")
            )
    )
    private Set<SpawnPreset> presets = new LinkedHashSet<>();

    @OneToOne(mappedBy = "spawnRule", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private SpawnCondition spawnCondition;

    public void setSpawnCondition(SpawnCondition spawnCondition) {
        if (this.spawnCondition != null) {
            this.spawnCondition.setSpawnRule(null);
        }

        this.spawnCondition = spawnCondition;

        if (spawnCondition != null && spawnCondition.getSpawnRule() != this) {
            spawnCondition.setSpawnRule(this);
        }
    }
}