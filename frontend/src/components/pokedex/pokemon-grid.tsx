import { PokemonCard } from './pokemon-card';
import type { PokemonListItem } from '@/types/api/pokemon.types';

interface PokemonGridProps {
    pokemon: PokemonListItem[];
}

/**
 * Grille responsive de Pokémon (style Pokémon HOME GuideBook).
 *
 * La densité est gérée en CSS via `.home-pokemon-grid` :
 * `repeat(auto-fill, minmax(110–130px, 1fr))` selon le breakpoint.
 * Résultat : la grille s'adapte seule à la largeur disponible,
 * sans dépendre des classes Tailwind `grid-cols-*`.
 */
export function PokemonGrid({ pokemon }: PokemonGridProps) {
    if (pokemon.length === 0) {
        return (
            <div className="home-panel home-empty-state">
                <p className="home-empty-title">Aucun Pokémon trouvé</p>
                <p className="home-empty-desc">
                    Essaie d&apos;ajuster tes filtres ou ta recherche.
                </p>
            </div>
        );
    }

    return (
        <div className="home-pokemon-grid home-fade-in">
            {pokemon.map((p) => (
                <PokemonCard key={p.id} pokemon={p} />
            ))}
        </div>
    );
}
