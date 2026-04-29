import Link from 'next/link';
import { ROUTES } from '@/lib/constants/routes';

export default function HomePage() {
    return (
        <div className="space-y-16">

            {/* Hero */}
            <section className="text-center py-16 space-y-6">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium border"
                     style={{ borderColor: 'var(--color-border)', color: 'var(--color-text-secondary)' }}
                >
                    Cobblemon 1.6.1
                </div>

                <h1 className="text-5xl font-bold text-white leading-tight">
                    Cobblemon<span className="text-red-500">Dex</span>
                </h1>

                <p className="text-lg max-w-xl mx-auto" style={{ color: 'var(--color-text-secondary)' }}>
                    Trouve facilement où et comment spawn chaque Pokémon dans Cobblemon —
                    biomes, conditions, formes et drops réunis en un seul endroit.
                </p>

                <Link
                    href={ROUTES.pokedex}
                    className="inline-block px-8 py-3 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium transition-colors"
                >
                    Ouvrir le Pokédex
                </Link>
            </section>

            {/* Raccourcis */}
            <section className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {[
                    {
                        href: ROUTES.pokedex,
                        emoji: '📖',
                        title: 'Pokédex',
                        desc: 'Tous les Pokémon, avec leurs formes, stats, spawns et drops.',
                    },
                    {
                        href: ROUTES.biomes,
                        emoji: '🌍',
                        title: 'Biomes',
                        desc: 'Explore un biome et découvre quels Pokémon s\'y trouvent.',
                    },
                    {
                        href: ROUTES.items,
                        emoji: '🎒',
                        title: 'Items',
                        desc: 'Liste complète des items droppés par les Pokémon.',
                    },
                ].map((card) => (
                    <Link
                        key={card.href}
                        href={card.href}
                        className="group block rounded-xl border p-6 transition-all hover:-translate-y-1 hover:border-red-500/50"
                        style={{ backgroundColor: 'var(--color-bg-card)', borderColor: 'var(--color-border)' }}
                    >
                        <div className="text-3xl mb-3">{card.emoji}</div>
                        <h2 className="text-lg font-semibold text-white mb-1">{card.title}</h2>
                        <p className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                            {card.desc}
                        </p>
                    </Link>
                ))}
            </section>

        </div>
    );
}