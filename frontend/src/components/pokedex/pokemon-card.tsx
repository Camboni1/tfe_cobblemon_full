import Link from 'next/link';
import Image from 'next/image';
import { ROUTES } from '@/lib/constants/routes';
import type { PokemonListItem } from '@/types/api/pokemon.types';

interface PokemonCardProps {
    pokemon: PokemonListItem;
}

function formatDexNumber(n: number): string {
    return `N° ${String(n).padStart(4, '0')}`;
}

/**
 * Tuile d'un Pokémon dans la grille du Pokédex.
 * Style Pokémon HOME (GuideBook) : card blanche arrondie, ombre douce,
 * image centrée, numéro national au-dessus du nom.
 */
export function PokemonCard({ pokemon }: PokemonCardProps) {
    const fallbackSprite =
        `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.nationalDexNumber}.png`;
    const spriteSrc = pokemon.homeSprites.defaultUrl ?? fallbackSprite;

    const cardClassName = pokemon.implemented
        ? 'home-pokemon-card'
        : 'home-pokemon-card home-pokemon-card-empty';

    return (
        <Link href={ROUTES.pokemon(pokemon.slug)} className={cardClassName}>
            <div className="home-pokemon-card-image">
                {pokemon.implemented ? (
                    <Image
                        src={spriteSrc}
                        alt={pokemon.displayName}
                        width={96}
                        height={96}
                        unoptimized
                    />
                ) : (
                    <div className="home-pokemon-card-placeholder">?</div>
                )}
            </div>

            <span className="home-pokemon-card-dex">
                {formatDexNumber(pokemon.nationalDexNumber)}
            </span>

            <span className="home-pokemon-card-name">
                {pokemon.displayName}
            </span>
        </Link>
    );
}
