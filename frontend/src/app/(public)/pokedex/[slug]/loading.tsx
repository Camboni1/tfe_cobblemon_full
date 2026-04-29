export default function PokemonDetailLoading() {
    return (
        <div className="space-y-6">
            <div className="h-6 w-40 rounded bg-white/5 animate-pulse" />
            <div className="h-48 rounded-2xl bg-white/5 animate-pulse" />
            <div className="h-32 rounded-xl bg-white/5 animate-pulse" />
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="h-64 rounded-xl bg-white/5 animate-pulse" />
                <div className="h-64 rounded-xl bg-white/5 animate-pulse" />
            </div>
            <div className="h-80 rounded-xl bg-white/5 animate-pulse" />
        </div>
    );
}
