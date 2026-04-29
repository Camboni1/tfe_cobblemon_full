import { env } from '@/lib/config/env';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

interface FetchOptions {
    method?: HttpMethod;
    body?: unknown;
    params?: Record<string, string | number | boolean | undefined | null>;
}

function buildUrl(path: string, params?: FetchOptions['params']): string {
    const url = new URL(`${env.apiBaseUrl}${path}`);
    if (params) {
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                url.searchParams.set(key, String(value));
            }
        });
    }
    return url.toString();
}

/**
 * Spring Boot 3.3+ sérialise les `Page<T>` sous la forme :
 *   { content: [...], page: { number, size, totalElements, totalPages } }
 *
 * Notre type frontend `SpringPage<T>` attend le format plat historique :
 *   { content, totalElements, totalPages, number, size, first, last }
 *
 * Cet adapter détecte la forme imbriquée et la remet à plat, de façon
 * transparente pour tous les endpoints paginés (pokemon, biomes, items, etc.).
 */
interface NestedSpringPage<T> {
    content: T[];
    page: {
        number: number;
        size: number;
        totalElements: number;
        totalPages: number;
    };
}

function isNestedSpringPage(data: unknown): data is NestedSpringPage<unknown> {
    if (!data || typeof data !== 'object') return false;
    const obj = data as Record<string, unknown>;
    if (!Array.isArray(obj.content)) return false;
    if (!obj.page || typeof obj.page !== 'object') return false;
    const page = obj.page as Record<string, unknown>;
    return (
        typeof page.number === 'number' &&
        typeof page.totalElements === 'number' &&
        typeof page.totalPages === 'number'
    );
}

function flattenSpringPage<T>(data: NestedSpringPage<T>) {
    const { content, page } = data;
    return {
        content,
        totalElements: page.totalElements,
        totalPages: page.totalPages,
        number: page.number,
        size: page.size,
        first: page.number === 0,
        last: page.totalPages === 0 || page.number >= page.totalPages - 1,
    };
}

export async function apiFetch<T>(path: string, options: FetchOptions = {}): Promise<T> {
    const { method = 'GET', body, params } = options;

    const response = await fetch(buildUrl(path, params), {
        method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
        const error = await response.text().catch(() => 'Erreur inconnue');
        throw new Error(`API ${method} ${path} → ${response.status}: ${error}`);
    }

    // 204 No Content
    if (response.status === 204) {
        return undefined as T;
    }

    const data: unknown = await response.json();

    // Adapter automatique Spring Boot 3.3+ → format plat
    if (isNestedSpringPage(data)) {
        return flattenSpringPage(data) as T;
    }

    return data as T;
}
