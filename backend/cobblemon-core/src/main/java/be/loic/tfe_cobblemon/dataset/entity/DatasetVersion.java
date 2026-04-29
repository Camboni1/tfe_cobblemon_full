package be.loic.tfe_cobblemon.dataset.entity;

import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.spawn.entity.SpawnPreset;
import be.loic.tfe_cobblemon.spawn.entity.SpawnSourceFile;
import be.loic.tfe_cobblemon.item.entity.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "dataset_version",
        uniqueConstraints = {
        @UniqueConstraint(name = "uq_dataset_version_code", columnNames = "code")
        }
)
public class DatasetVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Column(name = "label", nullable = false, length = 32)
    private String label;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "imported_at", nullable = false)
    private OffsetDateTime importedAt;

    @OneToMany(mappedBy = "datasetVersion", fetch = FetchType.LAZY)
    private List<Pokemon> pokemons = new ArrayList<>();

    @OneToMany(mappedBy = "datasetVersion", fetch = FetchType.LAZY)
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "datasetVersion", fetch = FetchType.LAZY)
    private List<SpawnPreset> spawnPresets = new ArrayList<>();

    @OneToMany(mappedBy = "datasetVersion", fetch = FetchType.LAZY)
    private List<SpawnSourceFile> spawnSourceFiles = new ArrayList<>();

}
