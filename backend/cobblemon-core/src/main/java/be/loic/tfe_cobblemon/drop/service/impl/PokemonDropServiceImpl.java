package be.loic.tfe_cobblemon.drop.service.impl;

import be.loic.tfe_cobblemon.common.exception.ResourceNotFoundException;
import be.loic.tfe_cobblemon.dataset.service.DatasetVersionService;
import be.loic.tfe_cobblemon.drop.dto.PokemonDropResponse;
import be.loic.tfe_cobblemon.drop.entity.PokemonDrop;
import be.loic.tfe_cobblemon.drop.repository.PokemonDropRepository;
import be.loic.tfe_cobblemon.drop.service.PokemonDropService;
import be.loic.tfe_cobblemon.drop.service.command.CreatePokemonDropCommand;
import be.loic.tfe_cobblemon.drop.service.command.UpdatePokemonDropCommand;
import be.loic.tfe_cobblemon.item.entity.Item;
import be.loic.tfe_cobblemon.item.repository.ItemRepository;
import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import be.loic.tfe_cobblemon.pokemon.entity.PokemonForm;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonFormRepository;
import be.loic.tfe_cobblemon.pokemon.repository.PokemonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokemonDropServiceImpl implements PokemonDropService {

    private final PokemonDropRepository pokemonDropRepository;
    private final PokemonRepository pokemonRepository;
    private final PokemonFormRepository pokemonFormRepository;
    private final ItemRepository itemRepository;
    private final DatasetVersionService datasetVersionService;

    @Override
    @Transactional
    public PokemonDropResponse create(String pokemonSlug, String formCode,
                                      CreatePokemonDropCommand command) {
        Long datasetVersionId = datasetVersionService.getActiveDatasetVersionId();

        Pokemon pokemon = pokemonRepository.findByDatasetVersionIdAndSlug(datasetVersionId, pokemonSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Pokémon introuvable : " + pokemonSlug));

        PokemonForm form = pokemonFormRepository.findByPokemonIdAndCode(pokemon.getId(), formCode)
                .orElseThrow(() -> new ResourceNotFoundException("Forme introuvable : " + formCode));

        Item item = itemRepository.findByDatasetVersionIdAndNamespacedId(
                        datasetVersionId, command.itemNamespacedId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item introuvable : " + command.itemNamespacedId()));

        PokemonDrop drop = new PokemonDrop();
        drop.setPokemonForm(form);
        drop.setItem(item);
        drop.setQuantityMin(command.quantityMin());
        drop.setQuantityMax(command.quantityMax());
        drop.setPercentage(command.percentage());
        drop.setDropPoolAmountMin(command.dropPoolAmountMin());
        drop.setDropPoolAmountMax(command.dropPoolAmountMax());
        drop.setRawJson("{}");

        PokemonDrop saved = pokemonDropRepository.save(drop);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PokemonDropResponse update(Long dropId, UpdatePokemonDropCommand command) {
        PokemonDrop drop = pokemonDropRepository.findById(dropId)
                .orElseThrow(() -> new ResourceNotFoundException("Drop introuvable : " + dropId));

        drop.setQuantityMin(command.quantityMin());
        drop.setQuantityMax(command.quantityMax());
        drop.setPercentage(command.percentage());
        drop.setDropPoolAmountMin(command.dropPoolAmountMin());
        drop.setDropPoolAmountMax(command.dropPoolAmountMax());

        return toResponse(pokemonDropRepository.save(drop));
    }

    @Override
    @Transactional
    public void delete(Long dropId) {
        if (!pokemonDropRepository.existsById(dropId)) {
            throw new ResourceNotFoundException("Drop introuvable : " + dropId);
        }
        pokemonDropRepository.deleteById(dropId);
    }

    private PokemonDropResponse toResponse(PokemonDrop drop) {
        return new PokemonDropResponse(
                drop.getId(),
                drop.getItem().getNamespacedId(),
                drop.getItem().getDisplayName(),
                drop.getItem().isGeneratedPlaceholder(),
                drop.getQuantityMin(),
                drop.getQuantityMax(),
                drop.getPercentage(),
                drop.getDropPoolAmountMin(),
                drop.getDropPoolAmountMax()
        );
    }
}