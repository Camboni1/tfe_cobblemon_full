package be.loic.tfe_cobblemon.spawn.entity;

import be.loic.tfe_cobblemon.spawn.converter.SpawnConditionTokenSideConverter;
import be.loic.tfe_cobblemon.spawn.converter.SpawnConditionTokenTypeConverter;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenSide;
import be.loic.tfe_cobblemon.spawn.enums.SpawnConditionTokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "spawn_condition_token",
        indexes = {
                @Index(name = "idx_spawn_condition_token_type_value", columnList = "token_type, token_value"),
                @Index(name = "idx_spawn_condition_token_condition_id", columnList = "spawn_condition_id")
        }
)
public class SpawnConditionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "spawn_condition_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spawn_condition_token_condition")
    )
    private SpawnCondition spawnCondition;

    @Convert(converter = SpawnConditionTokenSideConverter.class)
    @Column(name = "side", nullable = false, length = 16)
    private SpawnConditionTokenSide side;

    @Convert(converter = SpawnConditionTokenTypeConverter.class)
    @Column(name = "token_type", nullable = false, length = 24)
    private SpawnConditionTokenType tokenType;

    @Column(name = "token_value", nullable = false, length = 80)
    private String tokenValue;

    @Column(name = "is_tag", nullable = false)
    private boolean isTag = false;

}
