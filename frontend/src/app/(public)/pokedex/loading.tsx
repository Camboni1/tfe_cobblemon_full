export default function PokedexLoading() {
    return (
        <div className="space-y-6">
            <div className="h-10 w-48 rounded-lg bg-white/5 animate-pulse" />
            <div className="h-12 w-full rounded-lg bg-white/5 animate-pulse" />
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                {Array.from({ length: 24 }).map((_, i) => (
                    <div key={i} className="h-44 rounded-xl bg-white/5 animate-pulse" />
                ))}
            </div>
        </div>
    );
}