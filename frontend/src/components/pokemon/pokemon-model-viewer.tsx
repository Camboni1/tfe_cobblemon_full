'use client';
import { Canvas } from '@react-three/fiber';
import { useLoader } from '@react-three/fiber';
import { Bounds, OrbitControls, useAnimations, useGLTF } from '@react-three/drei';
import { Suspense, useEffect, useMemo, useRef } from 'react';
import {
    DoubleSide,
    LinearMipMapLinearFilter,
    NearestFilter,
    Object3D,
    SRGBColorSpace,
    Texture,
    TextureLoader,
    type Material,
    type Mesh,
} from 'three';

type TexturedMaterial = Material & {
    map?: Texture | null;
    transparent?: boolean;
    alphaTest?: number;
    side?: number;
    needsUpdate?: boolean;
};

function PokemonGltf({ url, textureUrl }: { url: string; textureUrl?: string | null }) {
    return textureUrl ? (
        <PokemonGltfWithTexture url={url} textureUrl={textureUrl} />
    ) : (
        <PokemonGltfScene url={url} />
    );
}

function PokemonGltfWithTexture({ url, textureUrl }: { url: string; textureUrl: string }) {
    const texture = useLoader(TextureLoader, textureUrl);
    const gltfTexture = useMemo(() => {
        const clone = texture.clone();
        clone.flipY = false;
        clone.colorSpace = SRGBColorSpace;
        clone.magFilter = NearestFilter;
        clone.minFilter = LinearMipMapLinearFilter;
        clone.needsUpdate = true;
        return clone;
    }, [texture]);

    useEffect(() => {
        return () => {
            gltfTexture.dispose();
        };
    }, [gltfTexture]);

    return <PokemonGltfScene url={url} texture={gltfTexture} />;
}

function PokemonGltfScene({ url, texture }: { url: string; texture?: Texture }) {
    const { scene, animations } = useGLTF(url);
    const ref = useRef<Object3D>(null);
    const { actions, names } = useAnimations(animations, ref);
    const clonedScene = useMemo(() => {
        const clone = scene.clone(true);
        clone.traverse((object) => {
            const mesh = object as Mesh;
            if (!mesh.isMesh) return;

            if (Array.isArray(mesh.material)) {
                mesh.material = mesh.material.map((material) => material.clone());
            } else if (mesh.material) {
                mesh.material = mesh.material.clone();
            }

            const materials = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
            for (const material of materials) {
                const texturedMaterial = material as TexturedMaterial;
                if (!('map' in texturedMaterial)) continue;
                if (texture) texturedMaterial.map = texture;
                texturedMaterial.transparent = true;
                texturedMaterial.alphaTest = 0.5;
                texturedMaterial.side = DoubleSide;
                texturedMaterial.needsUpdate = true;
            }
        });
        return clone;
    }, [scene, texture]);

    useEffect(() => {
        const action = names[0] ? actions[names[0]] : null;
        action?.reset().play();

        return () => {
            action?.stop();
        };
    }, [actions, names]);

    return <primitive ref={ref} object={clonedScene} />;
}

interface PokemonModelViewerProps {
    url: string;
    textureUrl?: string | null;
    className?: string;
}

export function PokemonModelViewer({ url, textureUrl, className }: PokemonModelViewerProps) {
    const assetKey = `${url}::${textureUrl ?? ''}`;

    return (
        <div
            className={className}
            style={{
                width: '100%',
                height: '100%',
                minHeight: 320,
                position: 'relative',
                borderRadius: 12,
                overflow: 'hidden',
                background:
                    'radial-gradient(ellipse at center, rgba(255,255,255,0.06) 0%, rgba(0,0,0,0) 70%)',
            }}
        >
            <Canvas
                camera={{ fov: 30, position: [0, 14, 50], near: 0.1, far: 5000 }}
                dpr={[1, 2]}
                gl={{ antialias: true, alpha: true }}
            >
                <ambientLight intensity={0.9} />
                <directionalLight position={[10, 20, 15]} intensity={1.0} />
                <directionalLight position={[-10, -5, -10]} intensity={0.35} />
                <OrbitControls makeDefault enablePan={false} />
                <Suspense fallback={null}>
                    <Bounds key={assetKey} fit clip observe margin={1.25}>
                        <PokemonGltf key={assetKey} url={url} textureUrl={textureUrl} />
                    </Bounds>
                </Suspense>
            </Canvas>
        </div>
    );
}
