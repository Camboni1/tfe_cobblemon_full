package be.loic.tfe_cobblemon.drop.entity;

import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
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
        name = "pokemon_drop",
        indexes = {
                @Index(name = "idx_pokemon_drop_form_id", columnList = "pokemon_form_id"),
                @Index(name = "idx_pokemon_drop_item_id", columnList = "item_id")
        }
)
public class PokemonDrop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pokemon_form_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pokemon_drop_form")
    )
    private PokemonForm pokemonForm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pokemon_drop_item")
    )
    private Item item;

    @Column(name = "drop_pool_amount_min")
    private Short dropPoolAmountMin;

    @Column(name = "drop_pool_amount_max")
    private Short dropPoolAmountMax;

    @Column(name = "quantity_min")
    private Short quantityMin;

    @Column(name = "quantity_max")
    private Short quantityMax;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, columnDefinition = "jsonb")
    private String rawJson;
}
