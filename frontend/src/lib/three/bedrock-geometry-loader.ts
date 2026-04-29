import * as THREE from 'three';

interface BoxFaceUV {
    uv: [number, number];
    uv_size: [number, number];
}

interface BedrockCube {
    origin: [number, number, number];
    size: [number, number, number];
    uv:
        | [number, number]
        | {
        north?: BoxFaceUV;
        south?: BoxFaceUV;
        east?: BoxFaceUV;
        west?: BoxFaceUV;
        up?: BoxFaceUV;
        down?: BoxFaceUV;
    };
    inflate?: number;
    mirror?: boolean;
    rotation?: [number, number, number];
    pivot?: [number, number, number];
}

interface BedrockBone {
    name: string;
    parent?: string;
    pivot?: [number, number, number];
    rotation?: [number, number, number];
    cubes?: BedrockCube[];
    mirror?: boolean;
}

interface BedrockGeometry {
    description: {
        identifier: string;
        texture_width: number;
        texture_height: number;
    };
    bones?: BedrockBone[];
}

interface BedrockGeoFile {
    format_version: string;
    'minecraft:geometry': BedrockGeometry[];
}

export interface LoadedBedrockModel {
    root: THREE.Group;
    /** Bounding box for auto-centering. */
    box: THREE.Box3;
    dispose: () => void;
}

export async function loadBedrockModel(
    modelUrl: string,
    textureUrl: string,
): Promise<LoadedBedrockModel> {
    const [geoData, texture] = await Promise.all([
        fetch(modelUrl).then((r) => {
            if (!r.ok) throw new Error(`Failed to fetch model: ${r.status}`);
            return r.json() as Promise<BedrockGeoFile>;
        }),
        loadTexture(textureUrl),
    ]);

    const geos = geoData['minecraft:geometry'];
    if (!geos || geos.length === 0) {
        throw new Error('Invalid bedrock geometry: no minecraft:geometry entry');
    }
    const geo = pickPrimaryGeometry(geos);
    console.debug(
        '[bedrock] available geometries:',
        geos.map((g) => g.description.identifier),
        '→ picked:',
        geo.description.identifier,
    );

    const texW = geo.description.texture_width;
    const texH = geo.description.texture_height;

    const material = new THREE.MeshStandardMaterial({
        map: texture,
        transparent: true,
        alphaTest: 0.05,
        side: THREE.DoubleSide,
        roughness: 1,
        metalness: 0,
    });

    const root = new THREE.Group();
    const boneGroups = new Map<string, THREE.Group>();
    const bonesByName = new Map<string, BedrockBone>();

    for (const bone of geo.bones ?? []) {
        bonesByName.set(bone.name, bone);
    }

    // First pass: create groups with absolute pivots
    for (const bone of geo.bones ?? []) {
        const group = new THREE.Group();
        group.name = bone.name;
        boneGroups.set(bone.name, group);
    }

    // Second pass: attach to parent (relative position) + cubes
    for (const bone of geo.bones ?? []) {
        const group = boneGroups.get(bone.name)!;
        const pivot = bone.pivot ?? [0, 0, 0];

        if (bone.parent && boneGroups.has(bone.parent)) {
            const parentBone = bonesByName.get(bone.parent);
            const parentPivot = parentBone?.pivot ?? [0, 0, 0];
            group.position.set(
                pivot[0] - parentPivot[0],
                pivot[1] - parentPivot[1],
                pivot[2] - parentPivot[2],
            );
            boneGroups.get(bone.parent)!.add(group);
        } else {
            group.position.set(pivot[0], pivot[1], pivot[2]);
            root.add(group);
        }

        if (bone.rotation) {
            group.rotation.set(
                THREE.MathUtils.degToRad(bone.rotation[0]),
                THREE.MathUtils.degToRad(bone.rotation[1]),
                THREE.MathUtils.degToRad(bone.rotation[2]),
            );
        }

        for (const cube of bone.cubes ?? []) {
            const inflate = cube.inflate ?? 0;
            const sx = cube.size[0] + inflate * 2;
            const sy = cube.size[1] + inflate * 2;
            const sz = cube.size[2] + inflate * 2;

            const ox = cube.origin[0] - inflate;
            const oy = cube.origin[1] - inflate;
            const oz = cube.origin[2] - inflate;

            // Skip degenerate cubes (some models use 0-size cubes for locators)
            if (sx <= 0 || sy <= 0 || sz <= 0) continue;

            const geometry = new THREE.BoxGeometry(sx, sy, sz);

            if (Array.isArray(cube.uv)) {
                applyBoxUV(
                    geometry,
                    cube.uv[0],
                    cube.uv[1],
                    cube.size[0],
                    cube.size[1],
                    cube.size[2],
                    texW,
                    texH,
                    cube.mirror ?? bone.mirror ?? false,
                );
            } else if (cube.uv && typeof cube.uv === 'object') {
                applyPerFaceUV(geometry, cube.uv, texW, texH);
            }

            const mesh = new THREE.Mesh(geometry, material);

            if (cube.rotation && cube.pivot) {
                // Cube has its own pivot: wrap in a group rotated around the cube's pivot
                const cubePivot = cube.pivot;
                const cubeGroup = new THREE.Group();
                cubeGroup.position.set(
                    cubePivot[0] - pivot[0],
                    cubePivot[1] - pivot[1],
                    cubePivot[2] - pivot[2],
                );
                cubeGroup.rotation.set(
                    THREE.MathUtils.degToRad(cube.rotation[0]),
                    THREE.MathUtils.degToRad(cube.rotation[1]),
                    THREE.MathUtils.degToRad(cube.rotation[2]),
                );
                mesh.position.set(
                    ox + sx / 2 - cubePivot[0],
                    oy + sy / 2 - cubePivot[1],
                    oz + sz / 2 - cubePivot[2],
                );
                cubeGroup.add(mesh);
                group.add(cubeGroup);
            } else {
                mesh.position.set(
                    ox + sx / 2 - pivot[0],
                    oy + sy / 2 - pivot[1],
                    oz + sz / 2 - pivot[2],
                );
                group.add(mesh);
            }
        }
    }
    // Bedrock: la face "avant" du modèle est en -Z (face nord).
// Three.js: la caméra par défaut regarde +Z → -Z, donc elle voit la face +Z = arrière du modèle.
// On rotate de 180° autour de Y pour que l'avant Bedrock face la caméra.
    root.rotation.y = Math.PI;

    // Compute bounding box for auto-centering / framing
    const box = new THREE.Box3().setFromObject(root);

    return {
        root,
        box,
        dispose: () => {
            root.traverse((obj) => {
                if (obj instanceof THREE.Mesh) {
                    obj.geometry.dispose();
                }
            });
            material.dispose();
            texture.dispose();
        },
    };
}

function loadTexture(url: string): Promise<THREE.Texture> {
    return new Promise((resolve, reject) => {
        new THREE.TextureLoader().load(
            url,
            (tex) => {
                tex.magFilter = THREE.NearestFilter;
                tex.minFilter = THREE.NearestFilter;
                tex.colorSpace = THREE.SRGBColorSpace;
                tex.generateMipmaps = false;
                resolve(tex);
            },
            undefined,
            (err) => reject(err),
        );
    });
}

/**
 * Bedrock unfolded box UV layout (origin top-left, Y grows downward):
 *
 *      +--+--+----------+
 *      |UP|DN|          |
 *   +--+--+--+----------+
 *   |E |N |W |S         |   E,W of size (d,h) ; N,S of size (w,h)
 *   +--+--+--+----------+
 *
 * Three.js BoxGeometry face order: 0=+X, 1=-X, 2=+Y, 3=-Y, 4=+Z, 5=-Z
 * Mapping: +X=east, -X=west, +Y=up, -Y=down, +Z=south, -Z=north
 */
function applyBoxUV(
    geometry: THREE.BoxGeometry,
    u: number,
    v: number,
    w: number,
    h: number,
    d: number,
    texW: number,
    texH: number,
    mirror: boolean,
) {
    // East face (+X) — when mirrored, swap east/west tiles
    setFaceUV(geometry, 0, u, v + d, d, h, texW, texH, mirror);
    // West face (-X)
    setFaceUV(geometry, 1, u + d + w, v + d, d, h, texW, texH, mirror);
    // Up face (+Y) — flipped vertically per Minecraft convention
    setFaceUV(geometry, 2, u + d, v, w, d, texW, texH, false, true);
    // Down face (-Y) — flipped vertically
    setFaceUV(geometry, 3, u + d + w, v, w, d, texW, texH, false, true);
    // South face (+Z)
    setFaceUV(geometry, 4, u + 2 * d + w, v + d, w, h, texW, texH, mirror);
    // North face (-Z)
    setFaceUV(geometry, 5, u + d, v + d, w, h, texW, texH, mirror);
}

function applyPerFaceUV(
    geometry: THREE.BoxGeometry,
    uv: { [k: string]: BoxFaceUV | undefined },
    texW: number,
    texH: number,
) {
    const east = uv.east;
    const west = uv.west;
    const up = uv.up;
    const down = uv.down;
    const south = uv.south;
    const north = uv.north;

    if (east) setFaceUVRaw(geometry, 0, east.uv[0], east.uv[1], east.uv_size[0], east.uv_size[1], texW, texH);
    if (west) setFaceUVRaw(geometry, 1, west.uv[0], west.uv[1], west.uv_size[0], west.uv_size[1], texW, texH);
    if (up) setFaceUVRaw(geometry, 2, up.uv[0], up.uv[1], up.uv_size[0], up.uv_size[1], texW, texH);
    if (down) setFaceUVRaw(geometry, 3, down.uv[0], down.uv[1], down.uv_size[0], down.uv_size[1], texW, texH);
    if (south) setFaceUVRaw(geometry, 4, south.uv[0], south.uv[1], south.uv_size[0], south.uv_size[1], texW, texH);
    if (north) setFaceUVRaw(geometry, 5, north.uv[0], north.uv[1], north.uv_size[0], north.uv_size[1], texW, texH);
}

function setFaceUV(
    geometry: THREE.BoxGeometry,
    faceIdx: number,
    x: number,
    y: number,
    w: number,
    h: number,
    texW: number,
    texH: number,
    mirrorX: boolean,
    mirrorY = false,
) {
    setFaceUVRaw(geometry, faceIdx, x, y, w, h, texW, texH, mirrorX, mirrorY);
}

function setFaceUVRaw(
    geometry: THREE.BoxGeometry,
    faceIdx: number,
    x: number,
    y: number,
    w: number,
    h: number,
    texW: number,
    texH: number,
    mirrorX = false,
    mirrorY = false,
) {
    const uvAttr = geometry.attributes.uv as THREE.BufferAttribute;

    let u1 = x / texW;
    let u2 = (x + w) / texW;
    let v1 = 1 - y / texH; // top edge in three.js space
    let v2 = 1 - (y + h) / texH; // bottom edge

    if (mirrorX) [u1, u2] = [u2, u1];
    if (mirrorY) [v1, v2] = [v2, v1];

    const offset = faceIdx * 4;
    // BoxGeometry uv vertex order: 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
    uvAttr.setXY(offset + 0, u1, v1);
    uvAttr.setXY(offset + 1, u2, v1);
    uvAttr.setXY(offset + 2, u1, v2);
    uvAttr.setXY(offset + 3, u2, v2);
    uvAttr.needsUpdate = true;
}

/**
 * Sélectionne la géométrie "canonique" parmi plusieurs poses.
 * Préfère l'identifier le plus court (= sans suffixe de pose),
 * puis trie par ordre alphabétique pour la stabilité.
 */
function pickPrimaryGeometry(geos: BedrockGeometry[]): BedrockGeometry {
    if (geos.length === 1) return geos[0];

    const sorted = [...geos].sort((a, b) => {
        const idA = a.description.identifier;
        const idB = b.description.identifier;
        const dotsA = (idA.match(/\./g) ?? []).length;
        const dotsB = (idB.match(/\./g) ?? []).length;
        if (dotsA !== dotsB) return dotsA - dotsB; // moins de points = plus canonique
        return idA.length - idB.length;
    });
    return sorted[0];
}