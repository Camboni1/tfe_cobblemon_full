package be.loic.tfe_cobblemon.pokemon.specification;

import be.loic.tfe_cobblemon.pokemon.entity.Pokemon;
import org.springframework.data.jpa.domain.Specification;

public final class PokemonSpecification {

    private PokemonSpecification() {
    }

    public static Specification<Pokemon> hasDatasetVersion(Long datasetVersionId) {
        return (root, query, cb) -> cb.equal(root.get("datasetVersion").get("id"), datasetVersionId);
    }

    public static Specification<Pokemon> containsSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String likeValue = "%" + search.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("slug")), likeValue),
                    cb.like(cb.lower(root.get("displayName")), likeValue)
            );
        };
    }

    public static Specification<Pokemon> hasGeneration(String generationCode) {
        return (root, query, cb) -> {
            if (generationCode == null || generationCode.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(cb.lower(root.get("generationCode")), generationCode.trim().toLowerCase());
        };
    }

    public static Specification<Pokemon> isImplemented(Boolean implemented) {
        return (root, query, cb) -> implemented == null
                ? cb.conjunction()
                : cb.equal(root.get("implemented"), implemented);
    }
}