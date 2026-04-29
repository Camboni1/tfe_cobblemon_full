import Link from 'next/link';
import { ROUTES } from '@/lib/constants/routes';

export default function PokemonNotFound() {
    return (
        <div className="text-center py-20">
            <p className="text-5xl mb-4">❓</p>
            <h2 className="text-xl font-bold text-white">Pokémon introuvable</h2>
            <p className="mt-2 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                Ce Pokémon n'existe pas dans la base de données.
            </p>
            <Link
                href={ROUTES.pokedex}
                className="mt-5 inline-block px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium transition-colors"
            >
                Retour au Pokédex
            </Link>
        </div>
    );
}