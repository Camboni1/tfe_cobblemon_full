'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ROUTES } from '@/lib/constants/routes';

const NAV_LINKS = [
    { label: 'Pokédex', href: ROUTES.pokedex },
    { label: 'Biomes',  href: ROUTES.biomes },
    { label: 'Items',   href: ROUTES.items },
];

/**
 * Navigation principale en pilules (style HOME).
 * L'état actif est rempli en turquoise plein.
 */
export function MainNav() {
    const pathname = usePathname();

    return (
        <nav className="flex items-center gap-2 overflow-x-auto">
            {NAV_LINKS.map((link) => {
                const isActive =
                    pathname === link.href || pathname.startsWith(`${link.href}/`);

                return (
                    <Link
                        key={link.href}
                        href={link.href}
                        className={
                            isActive
                                ? 'home-toggle home-toggle-active'
                                : 'home-toggle'
                        }
                    >
                        {link.label}
                    </Link>
                );
            })}
        </nav>
    );
}
