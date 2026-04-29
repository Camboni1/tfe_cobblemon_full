import type { Metadata } from 'next';
import {Nunito} from 'next/font/google';
import './globals.css';
import { Providers } from './providers';

const nunito = Nunito({ subsets: ['latin'], weight: ['400', '500', '600', '700'] });

export const metadata: Metadata = {
    title: 'Cobblemon Pokédex',
    description: 'Trouvez facilement où et comment spawn chaque Pokémon dans Cobblemon.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="fr">
        <body className={nunito.className}>
        <Providers>{children}</Providers>
        </body>
        </html>
    );
}