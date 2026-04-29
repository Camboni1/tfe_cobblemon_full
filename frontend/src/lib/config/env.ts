const publicApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const internalApiBaseUrl = process.env.API_INTERNAL_URL ?? publicApiBaseUrl;

export const env = {
    apiBaseUrl: typeof window === 'undefined' ? internalApiBaseUrl : publicApiBaseUrl,
} as const;
