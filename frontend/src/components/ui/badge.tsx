import { cn } from '@/lib/utils/cn';

interface TypeBadgeProps {
    type: string;
    className?: string;
    /**
     * Affiche le label en français (défaut) ou laisse la valeur brute.
     */
    raw?: boolean;
}

/**
 * Libellés français officiels des 18 types Pokémon.
 */
const TYPE_LABELS_FR: Record<string, string> = {
    normal:   'Normal',
    fire:     'Feu',
    water:    'Eau',
    grass:    'Plante',
    electric: 'Électrik',
    ice:      'Glace',
    fighting: 'Combat',
    poison:   'Poison',
    ground:   'Sol',
    flying:   'Vol',
    psychic:  'Psy',
    bug:      'Insecte',
    rock:     'Roche',
    ghost:    'Spectre',
    dragon:   'Dragon',
    dark:     'Ténèbres',
    steel:    'Acier',
    fairy:    'Fée',
};

/**
 * Badge de type Pokémon avec couleur officielle.
 * Style Pokémon HOME : pastille arrondie, couleurs d'origine Nintendo.
 */
export function TypeBadge({ type, className, raw = false }: TypeBadgeProps) {
    const key = type.toLowerCase();
    const label = raw ? type : (TYPE_LABELS_FR[key] ?? type);

    return (
        <span className={cn('home-type-badge', `home-type-${key}`, className)}>
            {label}
        </span>
    );
}
