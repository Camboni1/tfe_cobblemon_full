'use client';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, useAnimations, useGLTF } from '@react-three/drei';
import { useEffect, useRef } from 'react';
import type { Object3D } from 'three';

function PokemonGltf({ url }: { url: string }) {
    const { scene, animations } = useGLTF(url);
    const ref = useRef<Object3D>(null);
    const { actions, names } = useAnimations(animations, ref);

    useEffect(() => {
        if (names[0]) actions[names[0]]?.reset().play();
    }, [actions, names]);

    return <primitive ref={ref} object={scene} />;
}

export function PokemonModelViewer({ url }: { url: string }) {
    return (
        <Canvas camera={{ fov: 30, position: [0, 0, 5] }}>
            <ambientLight intensity={0.8} />
            <directionalLight position={[5, 10, 5]} intensity={1} />
            <PokemonGltf url={url} />
            <OrbitControls makeDefault enablePan={false} />
        </Canvas>
    );
}