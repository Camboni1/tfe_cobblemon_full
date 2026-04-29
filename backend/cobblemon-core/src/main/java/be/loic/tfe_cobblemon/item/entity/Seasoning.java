package be.loic.tfe_cobblemon.item.entity;

import jakarta.persistence.*;
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
        name = "seasoning",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_seasoning_name", columnNames = "item_id")
        },
        indexes = {
                @Index(name = "idx_seasoning_item_id", columnList = "item_id")
        }
)
public class Seasoning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "item_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_seasoning_item")
    )
    private Item item;

    @Column(name = "colour", nullable = false, length = 20)
    private String colour;

    @Column(name = "food_hunger")
    private Short foodHunger;

    @Column(name = "food_saturation", precision = 4, scale = 1)
    private BigDecimal foodSaturation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;

    @OneToMany(mappedBy = "seasoning", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeasoningMobEffect> mobEffects = new ArrayList<>();
}
