import Link from 'next/link';
import { ROUTES } from '@/lib/constants/routes';

export default function NotFound() {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center text-center px-4"
             style={{ backgroundColor: 'var(--color-bg-base)' }}
        >
            <p className="text-8xl font-bold text-red-600">404</p>
            <h1 className="mt-4 text-2xl font-bold text-white">Page introuvable</h1>
            <p className="mt-2 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                Cette page n'existe pas ou a été déplacée.
            </p>
            <Link
                href={ROUTES.home}
                className="mt-6 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium transition-colors"
            >
                Retour à l'accueil
            </Link>
        </div>
    );
}