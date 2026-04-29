package be.loic.tfe_cobblemon.item.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "bait_effect",
        indexes = {
                @Index(name = "idx_bait_effect_type_subcategory", columnList = "effect_type, subcategory"),
                @Index(name = "idx_bait_effect_item_id", columnList = "item_id")
        }
)
public class BaitEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bait_effect_item")
    )
    private Item item;

    @Column(name = "effect_type", nullable = false, length = 40)
    private String effectType;

    @Column(name = "subcategory", length = 40)
    private String subcategory;

    @Column(name = "chance", nullable = false, precision = 4, scale = 2)
    private BigDecimal chance;

    @Column(name = "value", precision = 8, scale = 4)
    private BigDecimal value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;
}
