package be.loic.tfe_cobblemon.pokemon.entity;

import be.loic.tfe_cobblemon.dataset.entity.DatasetVersion;
import be.loic.tfe_cobblemon.spawn.entity.SpawnRule;
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
        name = "pokemon",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pokemon_version_slug", columnNames = {"dataset_version_id", "slug"}),
                @UniqueConstraint(name = "uq_pokemon_version_dex", columnNames = {"dataset_version_id", "national_dex_number"})
        },
        indexes = {
                @Index(name = "idx_pokemon_slug", columnList = "slug"),
                @Index(name = "idx_pokemon_dex", columnList = "national_dex_number"),
                @Index(name = "idx_pokemon_dataset_version_id", columnList = "dataset_version_id")
        }
)
public class Pokemon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_version_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pokemon_dataset_version"))
    private DatasetVersion datasetVersion;

    @Column(name = "slug", nullable = false, length = 50)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "national_dex_number", nullable = false)
    private Short nationalDexNumber;

    @Column(name = "generation_code", nullable = false, length = 12)
    private String generationCode;

    @Column(name = "implemented", nullable = false)
    private Boolean implemented = true;

    @Column(name = "source_file", nullable = false, length = 64)
    private String sourceFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;

    @OneToMany(mappedBy = "pokemon", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private List<PokemonForm> forms = new ArrayList<>();

    @OneToMany(mappedBy = "pokemon", fetch = FetchType.LAZY)
    private List<SpawnRule> spawnRules = new ArrayList<>();

    public void addForm(PokemonForm form) {
        this.forms.add(form);
        form.setPokemon(this);
    }

    public void removeForm(PokemonForm form) {
        this.forms.remove(form);
        form.setPokemon(null);
    }
}