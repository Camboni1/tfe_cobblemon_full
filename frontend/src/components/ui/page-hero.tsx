import type { ReactNode } from 'react';
import { Panel } from '@/components/ui/panel';
import { cn } from '@/lib/utils/cn';

interface HeroStat {
    label: string;
    value: string;
}

interface PageHeroProps {
    eyebrow?: string;
    title: ReactNode;
    description?: ReactNode;
    actions?: ReactNode;
    aside?: ReactNode;
    stats?: HeroStat[];
    className?: string;
}

export function PageHero({
                             eyebrow,
                             title,
                             description,
                             actions,
                             aside,
                             stats = [],
                             className,
                         }: PageHeroProps) {
    return (
        <Panel inset={false} className={cn('overflow-hidden', className)}>
            <div
                className="pointer-events-none absolute inset-0"
                style={{
                    background:
                        'radial-gradient(circle at top left, rgba(230,57,70,0.16), transparent 34%), radial-gradient(circle at 88% 18%, rgba(59,130,246,0.08), transparent 22%)',
                }}
            />

            <div className="relative grid gap-8 p-6 sm:p-8 xl:grid-cols-[minmax(0,1fr)_320px]">
                <div className="space-y-6">
                    {eyebrow && (
                        <p
                            className="text-xs font-semibold uppercase tracking-[0.32em]"
                            style={{ color: 'var(--color-text-secondary)' }}
                        >
                            {eyebrow}
                        </p>
                    )}

                    <div className="space-y-3">
                        <h1 className="text-4xl font-black tracking-tight text-white sm:text-5xl">
                            {title}
                        </h1>

                        {description && (
                            <div
                                className="max-w-2xl text-base leading-7"
                                style={{ color: 'var(--color-text-secondary)' }}
                            >
                                {description}
                            </div>
                        )}
                    </div>

                    {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
                </div>

                {(stats.length > 0 || aside) && (
                    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
                        {stats.map((stat) => (
                            <div
                                key={stat.label}
                                className="rounded-2xl border p-4"
                                style={{
                                    backgroundColor: 'rgba(255,255,255,0.03)',
                                    borderColor: 'rgba(255,255,255,0.08)',
                                }}
                            >
                                <p
                                    className="text-[11px] font-semibold uppercase tracking-[0.24em]"
                                    style={{ color: 'var(--color-text-secondary)' }}
                                >
                                    {stat.label}
                                </p>
                                <p className="mt-2 text-lg font-bold text-white">{stat.value}</p>
                            </div>
                        ))}

                        {aside && (
                            <div
                                className="rounded-2xl border p-4"
                                style={{
                                    backgroundColor: 'rgba(255,255,255,0.03)',
                                    borderColor: 'rgba(255,255,255,0.08)',
                                }}
                            >
                                {aside}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </Panel>
    );
}
