package be.loic.tfe_cobblemon.spawn.entity;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
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
@Table(
        name = "spawn_source_file",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_spawn_source_file_dataset_version_id",
                        columnNames = { "dataset_version_id", "filename"}
                        )
        },
        indexes = {
                @Index( name = "idx_spawn_source_file_dataset_version_id", columnList = "dataset_version_id")
        }
)
public class SpawnSourceFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "dataset_version_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spawn_source_file_dataset_version")
            )
    private DatasetVersion datasetVersion;

    @Column(name = "filename", nullable = false, length = 64)
    private String filename;

    @Column(name = "comment_text", columnDefinition = "text")
    private String commentText;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "needed_installed_mods_json", columnDefinition = "jsonb")
    private String neededInstalledModsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "needed_uninstalled_mods_json", columnDefinition = "jsonb")
    private String neededUninstalledModsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;

    @OneToMany(mappedBy = "spawnSourceFile", fetch = FetchType.LAZY)
    private List<SpawnRule> spawnRules = new ArrayList<>();

}
