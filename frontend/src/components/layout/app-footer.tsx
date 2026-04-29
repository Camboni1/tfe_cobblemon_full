export function AppFooter() {
    return (
        <footer className="px-4 pb-4 pt-2">
            <div className="mx-auto max-w-[1500px]">
                <div className="pokedex-panel px-4 py-3">
                    <div
                        className="relative z-10 flex flex-col gap-2 text-xs sm:flex-row sm:items-center sm:justify-between"
                        style={{ color: 'var(--color-text-secondary)' }}
                    >
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="pokedex-chip">Cobblemon 1.6.1</span>
                            <span className="pokedex-chip">Web Interface</span>
                        </div>

                        <p>Indexed field guide for Cobblemon data</p>
                    </div>
                </div>
            </div>
        </footer>
    );
}
