import { Document, type Primitive } from '@gltf-transform/core';
import type { BedrockBone, BedrockCube, BedrockGeometry } from './bedrock-types.js';
import { eulerXYZToQuat, type SampledAnimation } from './animation-baker.js';

/**
 * Convertit une géométrie Bedrock + sa texture en un Document gltf-transform.
 *
 * Convention :
 *   - Chaque bone Bedrock devient un Node glTF, avec hiérarchie parent/enfant.
 *   - Chaque cube devient une Primitive attachée au Mesh du bone.
 *   - Les vertices d'un cube sont calculés en repère LOCAL au bone
 *     (origin - pivot), donc pas besoin de translation cube-level.
 *   - Le squelette est enveloppé dans un Node racine tourné de 180° sur Y
 *     pour que la "face avant" Bedrock (face nord, -Z) regarde la caméra
 *     three.js par défaut (qui regarde -Z depuis +Z).
 *
 * Limitations v1 (à étendre plus tard) :
 *   - Rotations cube-level appliquées autour du pivot du cube.
 *   - UVs : on supporte le format box-UV [u,v] et le format per-face.
 */
export function buildGltfDocument(
    geo: BedrockGeometry,
    textureBytes: Uint8Array,
    options: {
        modelName: string;
        flipFront?: boolean; // défaut true (180° sur Y)
        animation?: SampledAnimation;
    },
): Document {
    const { modelName, flipFront = true, animation } = options;
    const texW = geo.description.texture_width;
    const texH = geo.description.texture_height;

    const doc = new Document();
    doc.createBuffer();
    const scene = doc.createScene(modelName);

    // Texture + Material partagés par TOUTES les primitives du modèle
    const texture = doc
        .createTexture(`${modelName}_tex`)
        .setMimeType('image/png')
        .setImage(textureBytes);

    const material = doc
        .createMaterial(`${modelName}_mat`)
        .setBaseColorTexture(texture)
        .setAlphaMode('MASK')
        .setAlphaCutoff(0.5)
        .setDoubleSided(true)
        .setMetallicFactor(0)
        .setRoughnessFactor(1);

    // Filtrage NEAREST pour le rendu pixel-art Minecraft (samplers glTF)
    const texInfo = material.getBaseColorTextureInfo();
    if (texInfo) {
        texInfo.setMagFilter(9728); // NEAREST
        texInfo.setMinFilter(9728); // NEAREST
    }

    // Index des bones par nom
    const bonesByName = new Map<string, BedrockBone>();
    for (const b of geo.bones ?? []) bonesByName.set(b.name, b);

    // Crée un Node glTF par bone
    const nodesByName = new Map<string, ReturnType<typeof doc.createNode>>();
    for (const bone of geo.bones ?? []) {
        nodesByName.set(bone.name, doc.createNode(bone.name));
    }

    // Racine du modèle : on y attache tous les bones sans parent.
    // On lui applique l'éventuelle rotation 180° pour faire face à la caméra.
    const root = doc.createNode(modelName);
    if (flipFront) {
        // Quaternion (0, 1, 0, 0) = 180° autour de Y
        root.setRotation([0, 1, 0, 0]);
    }
    scene.addChild(root);

    // Hiérarchie + transforms locales
    for (const bone of geo.bones ?? []) {
        const node = nodesByName.get(bone.name)!;
        const pivot = bone.pivot ?? [0, 0, 0];

        if (bone.parent && nodesByName.has(bone.parent)) {
            const parentBone = bonesByName.get(bone.parent);
            const parentPivot = parentBone?.pivot ?? [0, 0, 0];
            node.setTranslation([
                pivot[0] - parentPivot[0],
                pivot[1] - parentPivot[1],
                pivot[2] - parentPivot[2],
            ]);
            nodesByName.get(bone.parent)!.addChild(node);
        } else {
            node.setTranslation([pivot[0], pivot[1], pivot[2]]);
            root.addChild(node);
        }

        // Rotation de repos (rare en Cobblemon mais à appliquer si non-zéro)
        if (bone.rotation && (bone.rotation[0] || bone.rotation[1] || bone.rotation[2])) {
            node.setRotation(eulerXYZToQuat(-bone.rotation[0], bone.rotation[1], bone.rotation[2]));
        }

        // Cubes du bone → Primitives groupés dans un Mesh par bone
        const cubes = bone.cubes ?? [];
        if (cubes.length === 0) continue;

        const mesh = doc.createMesh(`${bone.name}_mesh`);
        for (const cube of cubes) {
            const prim = buildCubePrimitive(doc, cube, bone, texW, texH, material);
            if (prim) mesh.addPrimitive(prim);
        }
        node.setMesh(mesh);
    }

    // ===== Animation : channels pour chaque (bone, path) =====
    if (animation && animation.tracks.size > 0) {
        const buffer = doc.getRoot().listBuffers()[0];
        const gltfAnim = doc.createAnimation(animation.name);

        for (const [boneName, tracks] of animation.tracks.entries()) {
            const node = nodesByName.get(boneName);
            if (!node) continue; // bone animé pas dans la geo : on ignore

            // Pour les translations, on ajoute le delta animé à la translation
            // de repos (le node a déjà pivot - parentPivot setté).
            const baseTranslation = node.getTranslation() ?? [0, 0, 0];

            for (const track of tracks) {
                let outputValues = track.values;

                // Translation : on additionne le baseTranslation à chaque sample
                if (track.path === 'translation') {
                    const out = new Float32Array(track.values.length);
                    for (let i = 0; i < track.values.length; i += 3) {
                        out[i + 0] = baseTranslation[0] + track.values[i + 0];
                        out[i + 1] = baseTranslation[1] + track.values[i + 1];
                        out[i + 2] = baseTranslation[2] + track.values[i + 2];
                    }
                    outputValues = out;
                }

                const inputAcc = doc
                    .createAccessor(`${boneName}_${track.path}_in`)
                    .setType('SCALAR')
                    .setArray(new Float32Array(track.times))
                    .setBuffer(buffer);

                const outputAcc = doc
                    .createAccessor(`${boneName}_${track.path}_out`)
                    .setType(track.path === 'rotation' ? 'VEC4' : 'VEC3')
                    .setArray(new Float32Array(outputValues))
                    .setBuffer(buffer);

                const sampler = doc
                    .createAnimationSampler()
                    .setInput(inputAcc)
                    .setOutput(outputAcc)
                    .setInterpolation('LINEAR');

                const channel = doc
                    .createAnimationChannel()
                    .setTargetNode(node)
                    .setTargetPath(track.path)
                    .setSampler(sampler);

                gltfAnim.addSampler(sampler).addChannel(channel);
            }
        }
    }

    return doc;
}

function buildCubePrimitive(
    doc: Document,
    cube: BedrockCube,
    bone: BedrockBone,
    texW: number,
    texH: number,
    material: ReturnType<typeof doc.createMaterial>,
): Primitive | null {
    const inflate = cube.inflate ?? 0;
    const sx = cube.size[0] + inflate * 2;
    const sy = cube.size[1] + inflate * 2;
    const sz = cube.size[2] + inflate * 2;
    const flatAxes = [sx <= 0, sy <= 0, sz <= 0].filter(Boolean).length;
    if (sx < 0 || sy < 0 || sz < 0 || flatAxes > 1) return null;

    const ox = cube.origin[0] - inflate;
    const oy = cube.origin[1] - inflate;
    const oz = cube.origin[2] - inflate;
    const pivot = bone.pivot ?? [0, 0, 0];

    // 8 coins du cube en repère LOCAL au bone (i.e., origin - pivot)
    const x0 = ox - pivot[0],
        y0 = oy - pivot[1],
        z0 = oz - pivot[2];
    const x1 = x0 + sx,
        y1 = y0 + sy,
        z1 = z0 + sz;

    // Vertices : 4 par face (24 au total) — duplications pour avoir des UVs/normales propres par face
    const positions: number[] = [];
    const normals: number[] = [];
    const uvs: number[] = [];
    const indices: number[] = [];

    const mirror = cube.mirror ?? bone.mirror ?? false;
    const isBoxUV = Array.isArray(cube.uv);

    // Helper : ajoute une face de 4 vertices (CCW vu de l'extérieur) + 2 triangles
    function addFace(
        v00: [number, number, number],
        v10: [number, number, number],
        v11: [number, number, number],
        v01: [number, number, number],
        normal: [number, number, number],
        uv00: [number, number],
        uv10: [number, number],
        uv11: [number, number],
        uv01: [number, number],
    ) {
        const baseIdx = positions.length / 3;
        positions.push(...v00, ...v10, ...v11, ...v01);
        for (let i = 0; i < 4; i++) normals.push(...normal);
        uvs.push(...uv00, ...uv10, ...uv11, ...uv01);
        indices.push(baseIdx, baseIdx + 1, baseIdx + 2, baseIdx, baseIdx + 2, baseIdx + 3);
    }

    // === UVs : calcul selon le format ===
    // glTF (et Bedrock) : V=0 en haut de la texture, V=1 en bas.
    // Donc on garde Y descendant. Pas de "1 - v" comme côté three.js.
    const uvCoords = isBoxUV
        ? boxUVCoords(cube.uv as [number, number], cube.size, texW, texH)
        : perFaceUVCoords(
              cube.uv as Exclude<typeof cube.uv, [number, number]>,
              texW,
              texH,
          );

    // Application du miroir : on swap horizontalement les UV des faces N/S/E/W (pas U/D)
    if (mirror) mirrorBoxUVs(uvCoords);

    if (flatAxes === 1) {
        // Bedrock uses zero-size cubes as flat planes for details like wing membranes.
        if (sx <= 0) {
            addFace(
                [x1, y0, z0],
                [x1, y0, z1],
                [x1, y1, z1],
                [x1, y1, z0],
                [1, 0, 0],
                ...uvCoords.east,
            );
        } else if (sy <= 0) {
            addFace(
                [x0, y1, z0],
                [x1, y1, z0],
                [x1, y1, z1],
                [x0, y1, z1],
                [0, 1, 0],
                ...uvCoords.up,
            );
        } else {
            addFace(
                [x0, y0, z0],
                [x1, y0, z0],
                [x1, y1, z0],
                [x0, y1, z0],
                [0, 0, -1],
                ...uvCoords.north,
            );
        }
    } else {

    // Faces — ordre des vertices CCW vu de l'extérieur :
    // East (+X)
    addFace(
        [x1, y0, z0],
        [x1, y0, z1],
        [x1, y1, z1],
        [x1, y1, z0],
        [1, 0, 0],
        ...uvCoords.east,
    );
    // West (-X)
    addFace(
        [x0, y0, z1],
        [x0, y0, z0],
        [x0, y1, z0],
        [x0, y1, z1],
        [-1, 0, 0],
        ...uvCoords.west,
    );
    // Up (+Y)
    addFace(
        [x0, y1, z0],
        [x1, y1, z0],
        [x1, y1, z1],
        [x0, y1, z1],
        [0, 1, 0],
        ...uvCoords.up,
    );
    // Down (-Y)
    addFace(
        [x0, y0, z1],
        [x1, y0, z1],
        [x1, y0, z0],
        [x0, y0, z0],
        [0, -1, 0],
        ...uvCoords.down,
    );
    // South (+Z) — Bedrock = arrière du modèle
    addFace(
        [x1, y0, z1],
        [x0, y0, z1],
        [x0, y1, z1],
        [x1, y1, z1],
        [0, 0, 1],
        ...uvCoords.south,
    );
    // North (-Z) — Bedrock = avant du modèle
    addFace(
        [x0, y0, z0],
        [x1, y0, z0],
        [x1, y1, z0],
        [x0, y1, z0],
        [0, 0, -1],
        ...uvCoords.north,
    );

    }

    const cubeRotation = cube.rotation ?? [0, 0, 0];
    if (cubeRotation[0] || cubeRotation[1] || cubeRotation[2]) {
        const cubePivot = cube.pivot ?? pivot;
        const localPivot: [number, number, number] = [
            cubePivot[0] - pivot[0],
            cubePivot[1] - pivot[1],
            cubePivot[2] - pivot[2],
        ];
        const quat = eulerXYZToQuat(cubeRotation[0], cubeRotation[1], cubeRotation[2]);

        for (let i = 0; i < positions.length; i += 3) {
            const rotated = rotatePointAroundPivot(
                [positions[i + 0], positions[i + 1], positions[i + 2]],
                localPivot,
                quat,
            );
            positions[i + 0] = rotated[0];
            positions[i + 1] = rotated[1];
            positions[i + 2] = rotated[2];
        }

        for (let i = 0; i < normals.length; i += 3) {
            const rotated = rotateVec3(
                [normals[i + 0], normals[i + 1], normals[i + 2]],
                quat,
            );
            normals[i + 0] = rotated[0];
            normals[i + 1] = rotated[1];
            normals[i + 2] = rotated[2];
        }
    }

    const buffer = doc.getRoot().listBuffers()[0];

    const positionAcc = doc
        .createAccessor()
        .setType('VEC3')
        .setArray(new Float32Array(positions))
        .setBuffer(buffer);
    const normalAcc = doc
        .createAccessor()
        .setType('VEC3')
        .setArray(new Float32Array(normals))
        .setBuffer(buffer);
    const uvAcc = doc
        .createAccessor()
        .setType('VEC2')
        .setArray(new Float32Array(uvs))
        .setBuffer(buffer);
    const indexAcc = doc
        .createAccessor()
        .setType('SCALAR')
        .setArray(new Uint16Array(indices))
        .setBuffer(buffer);

    return doc
        .createPrimitive()
        .setAttribute('POSITION', positionAcc)
        .setAttribute('NORMAL', normalAcc)
        .setAttribute('TEXCOORD_0', uvAcc)
        .setIndices(indexAcc)
        .setMaterial(material);
}

/**
 * Représentation des UV par face. Chaque tuple = (uv00, uv10, uv11, uv01)
 * dans le sens trigonométrique (CCW), V croît vers le bas (convention glTF).
 */
type FaceUVs = [
    [number, number],
    [number, number],
    [number, number],
    [number, number],
];

interface CubeUVs {
    east: FaceUVs;
    west: FaceUVs;
    up: FaceUVs;
    down: FaceUVs;
    south: FaceUVs;
    north: FaceUVs;
}

/**
 * Disposition Bedrock canonique du déroulé "box UV" :
 *
 *      +--+--+----------+
 *      |UP|DN|          |
 *   +--+--+--+----------+
 *   |E |N |W |S         |
 *   +--+--+--+----------+
 *
 *   East:  (u0,         v0+d), size (d, h)
 *   North: (u0+d,       v0+d), size (w, h)
 *   West:  (u0+d+w,     v0+d), size (d, h)
 *   South: (u0+2d+w,    v0+d), size (w, h)
 *   Up:    (u0+d,       v0  ), size (w, d)
 *   Down:  (u0+d+w,     v0  ), size (w, d)
 */
function boxUVCoords(
    uv: [number, number],
    size: [number, number, number],
    texW: number,
    texH: number,
): CubeUVs {
    const [u0, v0] = uv;
    const [w, h, d] = size;

    const tile = (x: number, y: number, w: number, h: number): FaceUVs => {
        const u1 = x / texW;
        const u2 = (x + w) / texW;
        const v1 = y / texH;
        const v2 = (y + h) / texH;
        // Ordre CCW vu de l'extérieur, V vers le bas en glTF :
        // (uv00 = bas-gauche, uv10 = bas-droit, uv11 = haut-droit, uv01 = haut-gauche)
        return [
            [u1, v2],
            [u2, v2],
            [u2, v1],
            [u1, v1],
        ];
    };

    return {
        east: tile(u0, v0 + d, d, h),
        north: tile(u0 + d, v0 + d, w, h),
        west: tile(u0 + d + w, v0 + d, d, h),
        south: tile(u0 + 2 * d + w, v0 + d, w, h),
        up: tile(u0 + d, v0, w, d),
        down: tile(u0 + d + w, v0, w, d),
    };
}

function perFaceUVCoords(
    uv: Exclude<BedrockCube['uv'], [number, number]>,
    texW: number,
    texH: number,
): CubeUVs {
    const empty: FaceUVs = [
        [0, 0],
        [0, 0],
        [0, 0],
        [0, 0],
    ];

    const make = (face?: { uv: [number, number]; uv_size: [number, number] }): FaceUVs => {
        if (!face) return empty;
        const [x, y] = face.uv;
        const [w, h] = face.uv_size;
        const u1 = x / texW;
        const u2 = (x + w) / texW;
        const v1 = y / texH;
        const v2 = (y + h) / texH;
        return [
            [u1, v2],
            [u2, v2],
            [u2, v1],
            [u1, v1],
        ];
    };

    return {
        east: make(uv.east),
        west: make(uv.west),
        up: make(uv.up),
        down: make(uv.down),
        south: make(uv.south),
        north: make(uv.north),
    };
}

function mirrorBoxUVs(uvs: CubeUVs) {
    for (const face of ['east', 'west', 'south', 'north'] as const) {
        const [a, b, c, d] = uvs[face];
        // Swap horizontalement
        uvs[face] = [b, a, d, c];
    }
}

function rotatePointAroundPivot(
    point: [number, number, number],
    pivot: [number, number, number],
    quat: [number, number, number, number],
): [number, number, number] {
    const local: [number, number, number] = [
        point[0] - pivot[0],
        point[1] - pivot[1],
        point[2] - pivot[2],
    ];
    const rotated = rotateVec3(local, quat);
    return [
        rotated[0] + pivot[0],
        rotated[1] + pivot[1],
        rotated[2] + pivot[2],
    ];
}

function rotateVec3(
    vec: [number, number, number],
    quat: [number, number, number, number],
): [number, number, number] {
    const [x, y, z] = vec;
    const [qx, qy, qz, qw] = quat;

    const uvx = qy * z - qz * y;
    const uvy = qz * x - qx * z;
    const uvz = qx * y - qy * x;

    const uuvx = qy * uvz - qz * uvy;
    const uuvy = qz * uvx - qx * uvz;
    const uuvz = qx * uvy - qy * uvx;

    return [
        x + 2 * (qw * uvx + uuvx),
        y + 2 * (qw * uvy + uuvy),
        z + 2 * (qw * uvz + uuvz),
    ];
}
