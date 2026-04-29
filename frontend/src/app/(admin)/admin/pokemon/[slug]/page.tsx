type PageProps = {
    params: {
        slug: string;
    };
};

export default function AdminPokemonDetailPage({ params }: PageProps) {
    return (
        <main>
            <h1>Administration Pokémon</h1>
            <p>Pokémon : {params.slug}</p>
        </main>
    );
}