import Link from 'next/link';
import { ROUTES } from '@/lib/constants/routes';
import { MainNav } from './main-nav';

/**
 * Header principal style Pokémon HOME : card blanche arrondie, logo à gauche,
 * navigation en pilules à droite. Sticky en haut de page.
 */
export function AppHeader() {
    return (
        <header className="sticky top-0 z-50 px-3 pt-4 sm:px-5">
            <div className="mx-auto max-w-[1180px]">
                <div
                    className="home-panel flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
                    style={{ padding: '0.75rem 1.1rem' }}
                >
                    <Link href={ROUTES.home} className="flex items-center gap-2.5">
                        <LogoBadge />
                        <span
                            className="text-lg font-bold tracking-tight"
                            style={{ color: 'var(--color-primary-strong)' }}
                        >
                            Cobblemon<span style={{ color: 'var(--color-text-primary)' }}>Dex</span>
                        </span>
                    </Link>

                    <MainNav />
                </div>
            </div>
        </header>
    );
}

/**
 * Petit logo circulaire turquoise qui évoque (sans imiter) une Pokéball.
 */
function LogoBadge() {
    return (
        <span
            aria-hidden="true"
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 34,
                height: 34,
                borderRadius: 999,
                background: 'var(--color-primary)',
                color: 'white',
                boxShadow: '0 2px 6px rgba(62, 186, 160, 0.35)',
            }}
        >
            <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
            >
                <circle cx="12" cy="12" r="9" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <circle cx="12" cy="12" r="2.5" fill="currentColor" stroke="none" />
            </svg>
        </span>
    );
}
