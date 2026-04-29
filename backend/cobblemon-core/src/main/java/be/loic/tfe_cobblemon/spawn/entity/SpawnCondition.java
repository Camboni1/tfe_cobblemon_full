package be.loic.tfe_cobblemon.spawn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "spawn_condition",
        uniqueConstraints = {
        @UniqueConstraint(name = "uq_spawn_condition_rule", columnNames = "spawn_rule_id")
        },
        indexes = {
                @Index(name = "idx_spawn_condition_time_range", columnList = "time_range"),
                @Index(name = "idx_spawn_condition_moon_phase", columnList = "moon_phase"),
                @Index(name = "idx_spawn_condition_rule_id", columnList = "spawn_rule_id")
        }
)
public class SpawnCondition {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(
                name = "spawn_rule_id",
                nullable = false,
                unique = true,
                foreignKey = @ForeignKey(name = "fk_spawn_condition_rule")
        )
        private SpawnRule spawnRule;

        @Column(name = "can_see_sky")
        private Boolean canSeeSky;

        @Column(name = "is_raining")
        private Boolean isRaining;

        @Column(name = "is_thundering")
        private Boolean isThundering;

        @Column(name = "is_slime_chunk")
        private Boolean isSlimeChunk;

        @Column(name = "min_x")
        private Integer minX;

        @Column(name = "max_x")
        private Integer maxX;

        @Column(name = "min_y")
        private Short minY;

        @Column(name = "max_y")
        private Short maxY;

        @Column(name = "min_light")
        private Short minLight;

        @Column(name = "max_light")
        private Short maxLight;

        @Column(name = "min_sky_light")
        private Short minSkyLight;

        @Column(name = "max_sky_light")
        private Short maxSkyLight;

        @Column(name = "min_lure_level")
        private Short minLureLevel;

        @Column(name = "max_lure_level")
        private Short maxLureLevel;

        @Column(name = "moon_phase", length = 16)
        private String moonPhase;

        @Column(name = "time_range", length = 16)
        private String timeRange;

        @Column(name = "rod_type", length = 30)
        private String rodType;

        @Column(name = "bait_item_expression", length = 64)
        private String baitItemExpression;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "condition_json", columnDefinition = "jsonb")
        private String conditionJson;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "anticondition_json", columnDefinition = "jsonb")
        private String anticonditionJson;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "effective_condition_json", columnDefinition = "jsonb")
        private String effectiveConditionJson;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "effective_anticondition_json", columnDefinition = "jsonb")
        private String effectiveAnticonditionJson;

        @OneToMany(mappedBy = "spawnCondition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
        private List<SpawnConditionToken> tokens = new ArrayList<>();

}
