'use client';

import { Suspense, useEffect, useMemo, useRef, useState } from 'react';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import * as THREE from 'three';
import { loadBedrockModel, type LoadedBedrockModel } from '@/lib/three/bedrock-geometry-loader';

interface PokemonModelViewerProps {
    modelUrl: string;
    textureUrl: string;
    autoRotate?: boolean;
    className?: string;
}

export function PokemonModelViewer({
                                       modelUrl,
                                       textureUrl,
                                       autoRotate = false,
                                       className,
                                   }: PokemonModelViewerProps) {
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
                camera={{ fov: 30, near: 0.1, far: 5000, position: [0, 0, 50] }}
                dpr={[1, 2]}
                gl={{ antialias: true, alpha: true }}
            >
                <ambientLight intensity={0.9} />
                <directionalLight position={[10, 20, 15]} intensity={1.0} />
                <directionalLight position={[-10, -5, -10]} intensity={0.35} />

                <Suspense fallback={null}>
                    <BedrockModel
                        modelUrl={modelUrl}
                        textureUrl={textureUrl}
                        autoRotate={autoRotate}
                    />
                </Suspense>
            </Canvas>
        </div>
    );
}

function BedrockModel({
                          modelUrl,
                          textureUrl,
                          autoRotate,
                      }: {
    modelUrl: string;
    textureUrl: string;
    autoRotate: boolean;
}) {
    const [loaded, setLoaded] = useState<LoadedBedrockModel | null>(null);
    const groupRef = useRef<THREE.Group>(null);
    const controlsRef = useRef<any>(null);
    const { camera, size } = useThree();

    useEffect(() => {
        let cancelled = false;
        setLoaded(null);

        loadBedrockModel(modelUrl, textureUrl)
            .then((m) => {
                if (cancelled) {
                    m.dispose();
                    return;
                }
                setLoaded(m);
            })
            .catch((err) => {
                console.error('Failed to load Bedrock model:', err);
            });

        return () => {
            cancelled = true;
        };
    }, [modelUrl, textureUrl]);

    useEffect(() => {
        return () => {
            loaded?.dispose();
        };
    }, [loaded]);

    const framing = useMemo(() => {
        if (!loaded) return null;

        const box = new THREE.Box3().setFromObject(loaded.root);
        const center = new THREE.Vector3();
        const sizeVec = new THREE.Vector3();
        box.getCenter(center);
        box.getSize(sizeVec);

        // On centre X et Z, mais on shift le modèle vers le bas pour qu'il
        // apparaisse dans la moitié basse du viewport (au-dessus du nom).
        const verticalShift = sizeVec.y * 0.25;
        const offset = new THREE.Vector3(
            -center.x,
            -center.y - verticalShift,
            -center.z,
        );

        const persp = camera as THREE.PerspectiveCamera;
        const aspect = size.width / size.height;
        const fovV = THREE.MathUtils.degToRad(persp.fov);
        const fovH = 2 * Math.atan(Math.tan(fovV / 2) * aspect);

        const halfMaxDim = Math.max(sizeVec.x, sizeVec.y, sizeVec.z) / 2;
        const distV = halfMaxDim / Math.tan(fovV / 2);
        const distH = halfMaxDim / Math.tan(fovH / 2);
        const distance = Math.max(distV, distH) * 1.7; // un poil plus de marge pour compenser le shift

        return { offset, distance, modelHeight: sizeVec.y };
    }, [loaded, camera, size.width, size.height]);

    useEffect(() => {
        if (!framing || !controlsRef.current) return;

        // Vue de face, légèrement en hauteur pour donner du volume
        camera.position.set(0, 0, framing.distance);
        camera.near = framing.distance / 100;
        camera.far = framing.distance * 100;
        (camera as THREE.PerspectiveCamera).updateProjectionMatrix();

        controlsRef.current.target.set(0, 0, 0);
        controlsRef.current.minDistance = framing.distance * 0.4;
        controlsRef.current.maxDistance = framing.distance * 4;
        controlsRef.current.update();
    }, [framing, camera]);

    useFrame((_, delta) => {
        if (autoRotate && groupRef.current) {
            groupRef.current.rotation.y += delta * 0.25;
        }
    });

    return (
        <>
            <OrbitControls
                ref={controlsRef}
                enablePan={false}
                makeDefault
            />
            {loaded && framing && (
                <group ref={groupRef}>
                    <primitive object={loaded.root} position={framing.offset} />
                </group>
            )}
        </>
    );
}