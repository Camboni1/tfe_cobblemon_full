// Miroir de la Page<T> de Spring Data
export interface SpringPage<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;   // page courante (0-based)
    size: number;
    first: boolean;
    last: boolean;
}