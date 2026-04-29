'use client';

import dynamic from 'next/dynamic';
import { useEffect, useState } from 'react';

const PokemonModelViewer = dynamic(
    () => import('./pokemon-model-viewer').then((m) => m.PokemonModelViewer),
    { ssr: false, loading: () => <Pending /> },
);

interface PokemonModel3DStageProps {
    modelUrl: string;
    textureUrl: string;
    fallback: React.ReactNode;
    className?: string;
}

export function PokemonModel3DStage({
                                        modelUrl,
                                        textureUrl,
                                        fallback,
                                        className,
                                    }: PokemonModel3DStageProps) {
    // Probe the URLs once: if either is unreachable, fall back to 2D immediately.
    const [ready, setReady] = useState<'pending' | 'ok' | 'error'>('pending');

    useEffect(() => {
        let cancelled = false;
        setReady('pending');
        Promise.all([
            fetch(modelUrl, { method: 'HEAD' }),
            fetch(textureUrl, { method: 'HEAD' }),
        ])
            .then(([m, t]) => {
                if (cancelled) return;
                setReady(m.ok && t.ok ? 'ok' : 'error');
            })
            .catch(() => {
                if (!cancelled) setReady('error');
            });

        return () => {
            cancelled = true;
        };
    }, [modelUrl, textureUrl]);

    if (ready === 'error') return <>{fallback}</>;
    if (ready === 'pending') return <Pending />;

    return (
        <PokemonModelViewer
            modelUrl={modelUrl}
            textureUrl={textureUrl}
            className={className}
        />
    );
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