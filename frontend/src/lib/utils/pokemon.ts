export function getSpriteUrl(dexNumber: number): string {
    return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${dexNumber}.png`;
}

export function formatDexNumber(n: number): string {
    return `#${String(n).padStart(4, '0')}`;
}

export function formatGeneration(code: string): string {
    const map: Record<string, string> = {
        generation1: 'Génération I',
        generation2: 'Génération II',
        generation3: 'Génération III',
        generation4: 'Génération IV',
        generation5: 'Génération V',
        generation6: 'Génération VI',
        generation7: 'Génération VII',
        generation8: 'Génération VIII',
        generation9: 'Génération IX',
    };
    return map[code] ?? code;
}

export const BUCKET_CONFIG = {
    COMMON:     { label: 'Commun',      className: 'bg-green-600' },
    UNCOMMON:   { label: 'Peu commun',  className: 'bg-blue-600' },
    RARE:       { label: 'Rare',        className: 'bg-purple-600' },
    ULTRA_RARE: { label: 'Ultra rare',  className: 'bg-orange-500' },
} as const;

export function getBucketConfig(bucket: string) {
    return BUCKET_CONFIG[bucket as keyof typeof BUCKET_CONFIG]
        ?? { label: bucket, className: 'bg-gray-600' };
}

export const STAT_CONFIG = [
    { key: 'baseHp',             label: 'PV',       color: 'bg-red-500' },
    { key: 'baseAttack',         label: 'Attaque',  color: 'bg-orange-500' },
    { key: 'baseDefense',        label: 'Défense',  color: 'bg-yellow-500' },
    { key: 'baseSpecialAttack',  label: 'Atq. Spé', color: 'bg-blue-500' },
    { key: 'baseSpecialDefense', label: 'Déf. Spé', color: 'bg-green-500' },
    { key: 'baseSpeed',          label: 'Vitesse',  color: 'bg-pink-500' },
] as const;