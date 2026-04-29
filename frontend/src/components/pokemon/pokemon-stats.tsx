import { STAT_CONFIG } from '@/lib/utils/pokemon';
import type { PokemonForm } from '@/types/api/pokemon.types';

interface PokemonStatsProps {
    form: PokemonForm;
}

const MAX_STAT = 255;

export function PokemonStats({ form }: PokemonStatsProps) {
    const total =
        form.baseHp +
        form.baseAttack +
        form.baseDefense +
        form.baseSpecialAttack +
        form.baseSpecialDefense +
        form.baseSpeed;

    return (
        <div
            className="rounded-xl border p-6 space-y-4"
            style={{ backgroundColor: 'var(--color-bg-card)', borderColor: 'var(--color-border)' }}
        >
            <h2 className="text-lg font-semibold text-white">Statistiques de base</h2>

            <div className="space-y-3">
                {STAT_CONFIG.map(({ key, label, color }) => {
                    const value = form[key] as number;
                    const pct = Math.round((value / MAX_STAT) * 100);
                    return (
                        <div key={key} className="flex items-center gap-3">
              <span
                  className="text-xs w-20 flex-shrink-0 text-right"
                  style={{ color: 'var(--color-text-secondary)' }}
              >
                {label}
              </span>
                            <span className="text-sm font-mono font-semibold text-white w-8 flex-shrink-0">
                {value}
              </span>
                            <div className="flex-1 h-2 rounded-full bg-white/10 overflow-hidden">
                                <div
                                    className={`h-full rounded-full transition-all duration-500 ${color}`}
                                    style={{ width: `${pct}%` }}
                                />
                            </div>
                        </div>
                    );
                })}
            </div>

            <div className="pt-2 border-t flex justify-between text-sm"
                 style={{ borderColor: 'var(--color-border)' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>Total</span>
                <span className="font-bold text-white">{total}</span>
            </div>
        </div>
    );
}