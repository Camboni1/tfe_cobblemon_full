'use client';

import dynamic from 'next/dynamic';
import { useEffect, useState } from 'react';

const PokemonModelViewer = dynamic(
    () => import('./pokemon-model-viewer').then((m) => m.PokemonModelViewer),
    { ssr: false, loading: () => <Pending /> },
);

interface PokemonModel3DStageProps {
    /** URL du fichier .glb produit par cobblemon-asset-builder. */
    url: string;
    /** Texture Cobblemon à appliquer au modèle, par exemple shiny. */
    textureUrl?: string | null;
    fallback: React.ReactNode;
    className?: string;
}

export function PokemonModel3DStage({ url, textureUrl, fallback, className }: PokemonModel3DStageProps) {
    // Ping le glb une fois : si introuvable (404), retombe sur le sprite 2D.
    const [probe, setProbe] = useState<{ url: string; ready: 'ok' | 'error' } | null>(null);
    const ready = probe?.url === url ? probe.ready : 'pending';
    const viewerKey = url;

    useEffect(() => {
        let cancelled = false;
        fetch(url, { method: 'HEAD' })
            .then((res) => {
                if (cancelled) return;
                setProbe({ url, ready: res.ok ? 'ok' : 'error' });
            })
            .catch(() => {
                if (!cancelled) setProbe({ url, ready: 'error' });
            });

        return () => {
            cancelled = true;
        };
    }, [url]);

    if (ready === 'error') return <>{fallback}</>;
    if (ready === 'pending') return <Pending />;

    return <PokemonModelViewer key={viewerKey} url={url} textureUrl={textureUrl} className={className} />;
}

function Pending() {
    return (
        <div
            className="flex items-center justify-center w-full h-full"
            style={{ minHeight: 320, color: 'var(--color-text-secondary)' }}
        >
            <span className="animate-pulse text-sm">Chargement du modèle 3D…</span>
        </div>
    );
}
