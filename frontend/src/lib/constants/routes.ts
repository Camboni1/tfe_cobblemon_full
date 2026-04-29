export const ROUTES = {
    home: '/',
    pokedex: '/pokedex',
    pokemon: (slug: string) => `/pokedex/${slug}`,
    biomes: '/biomes',
    items: '/items',
    admin: {
        root: '/admin',
        pokemon: '/admin/pokemon',
        pokemonNew: '/admin/pokemon/new',
        pokemonEdit: (slug: string) => `/admin/pokemon/${slug}`,
        translations: '/admin/translations',
    },
} as const;