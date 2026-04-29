interface PaginationProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

export function PokemonPagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
    if (totalPages <= 1) return null;

    const pages = Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
        if (totalPages <= 7) return i;
        if (currentPage < 4) return i;
        if (currentPage > totalPages - 4) return totalPages - 7 + i;
        return currentPage - 3 + i;
    });

    const btnBase =
        'w-9 h-9 rounded-lg text-sm font-medium transition-colors flex items-center justify-center';

    return (
        <div className="flex items-center justify-center gap-1 mt-8">
            <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className={`${btnBase} disabled:opacity-30 disabled:cursor-not-allowed`}
                style={{ backgroundColor: 'var(--color-bg-card)', color: 'var(--color-text-secondary)' }}
            >
                ‹
            </button>

            {pages.map((page) => (
                <button
                    key={page}
                    onClick={() => onPageChange(page)}
                    className={btnBase}
                    style={
                        page === currentPage
                            ? { backgroundColor: '#dc2626', color: 'white' }
                            : { backgroundColor: 'var(--color-bg-card)', color: 'var(--color-text-secondary)' }
                    }
                >
                    {page + 1}
                </button>
            ))}

            <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                className={`${btnBase} disabled:opacity-30 disabled:cursor-not-allowed`}
                style={{ backgroundColor: 'var(--color-bg-card)', color: 'var(--color-text-secondary)' }}
            >
                ›
            </button>
        </div>
    );
}