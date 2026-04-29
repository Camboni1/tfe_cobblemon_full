export type SpawnType = 'NATURAL' | 'FISHING' | 'HEADBUTT' | string;
export type SpawnablePositionType = 'LAND' | 'WATER' | 'UNDERGROUND' | 'AIR' | string;
export type SpawnBucket = 'COMMON' | 'UNCOMMON' | 'RARE' | 'ULTRA_RARE' | string;

export interface SpawnRule {
    id: number;
    externalId: string;
    pokemonSlug: string;
    formCode: string | null;
    targetExpression: string | null;
    spawnType: SpawnType;
    spawnablePositionType: SpawnablePositionType;
    bucket: SpawnBucket;
    levelMin: number;
    levelMax: number;
    weight: number;
    maxHerdSize: number | null;
    sourceFilename: string;
}