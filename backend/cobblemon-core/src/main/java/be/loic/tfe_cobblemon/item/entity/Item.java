package be.loic.tfe_cobblemon.item.entity;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.drop.entity.PokemonDrop;
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
        name = "item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_item_version_namespaced_id",
                        columnNames = {"dataset_version_id", "namespaced_id"}
                )
        },
        indexes = {
                @Index(name = "idx_item_namespace_id", columnList = "namespaced_id"),
                @Index(name = "idx_item_dataset_version_id", columnList = "dataset_version_id"),
                @Index(name = "idx_item_generated_placeholder", columnList = "generated_placeholder")
        }
)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "dataset_version_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_dataset_version")
    )
    private DatasetVersion datasetVersion;

    @Column(name = "namespaced_id", nullable = false, length = 128)
    private String namespacedId;

    @Column(name = "namespace", nullable = false, length = 24)
    private String namespace;

    @Column(name = "path", nullable = false, length = 128)
    private String path;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(name = "generated_placeholder", nullable = false)
    private boolean generatedPlaceholder = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private String rawJson;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private List<PokemonDrop> pokemonDrops = new ArrayList<>();

    @OneToOne(mappedBy = "item", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Seasoning seasoning;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    private List<BaitEffect> baitEffects = new ArrayList<>();
}