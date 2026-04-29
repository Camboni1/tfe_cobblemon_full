package be.loic.tfe_cobblemon.spawn.entity;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "spawn_preset",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_spawn_preset_version_code",
                        columnNames = {"dataset_version_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_spawn_preset_dataset_version_id", columnList = "dataset_version_id")
        }
)
public class SpawnPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "dataset_version_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spawn_preset_dataset_version")
    )
    private DatasetVersion datasetVersion;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "source_file", nullable = false, length = 64)
    private String sourceFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_json", columnDefinition = "jsonb")
    private String conditionJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anticondition_json", columnDefinition = "jsonb")
    private String anticonditionJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;

    @ManyToMany(mappedBy = "presets", fetch = FetchType.LAZY)
    private Set<SpawnRule> spawnRules = new LinkedHashSet<>();

}
