import { getBucketConfig } from '@/lib/utils/pokemon';
import type { SpawnRule } from '@/types/api/spawn.types';

interface SpawnCardProps {
    spawn: SpawnRule;
}

function formatPositionType(type: string): string {
    const map: Record<string, string> = {
        LAND: '🌿 Terre',
        WATER: '💧 Eau',
        UNDERGROUND: '⛏️ Souterrain',
        AIR: '🌤️ Air',
        LAVA: '🌋 Lave',
    };
    return map[type] ?? type;
}

function formatSpawnType(type: string): string {
    const map: Record<string, string> = {
        NATURAL: 'Naturel',
        FISHING: 'Pêche',
        HEADBUTT: 'Cognade',
    };
    return map[type] ?? type;
}

export function SpawnCard({ spawn }: SpawnCardProps) {
    const bucket = getBucketConfig(spawn.bucket);

    return (
        <div
            className="rounded-xl border p-4 space-y-3"
            style={{ backgroundColor: 'var(--color-bg-card)', borderColor: 'var(--color-border)' }}
        >
            {/* En-tête */}
            <div className="flex items-start justify-between gap-2">
                <div className="space-y-1">
          <span
              className={`inline-block text-xs font-semibold px-2 py-0.5 rounded-full text-white ${bucket.className}`}
          >
            {bucket.label}
          </span>
                    {spawn.formCode && (
                        <p className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>
                            Forme : {spawn.formCode}
                        </p>
                    )}
                </div>
                <span className="text-xs px-2 py-1 rounded-lg bg-white/5"
                      style={{ color: 'var(--color-text-secondary)' }}>
          {formatSpawnType(spawn.spawnType)}
        </span>
            </div>

            {/* Infos spawn */}
            <div className="grid grid-cols-2 gap-2 text-sm">
                <InfoRow label="Position" value={formatPositionType(spawn.spawnablePositionType)} />
                <InfoRow label="Niveaux" value={`${spawn.levelMin} – ${spawn.levelMax}`} />
                {spawn.weight != null && (
                    <InfoRow label="Poids" value={String(spawn.weight)} />
                )}
                {spawn.maxHerdSize != null && (
                    <InfoRow label="Taille groupe" value={`max. ${spawn.maxHerdSize}`} />
                )}
            </div>

            {/* Expression de condition (biomes, tags...) */}
            {spawn.targetExpression && (
                <div className="pt-2 border-t" style={{ borderColor: 'var(--color-border)' }}>
                    <p className="text-xs mb-1.5" style={{ color: 'var(--color-text-secondary)' }}>
                        Conditions
                    </p>
                    <p className="text-xs font-mono text-white/70 break-all leading-relaxed">
                        {spawn.targetExpression}
                    </p>
                </div>
            )}

            {/* Fichier source */}
            <p className="text-xs truncate" style={{ color: 'var(--color-text-secondary)' }}>
                Source : {spawn.sourceFilename}
            </p>
        </div>
    );
}

function InfoRow({ label, value }: { label: string; value: string }) {
    return (
        <div>
      <span className="block text-xs" style={{ color: 'var(--color-text-secondary)' }}>
        {label}
      </span>
            <span className="text-white font-medium">{value}</span>
        </div>
    );
}