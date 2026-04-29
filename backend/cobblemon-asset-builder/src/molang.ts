/**
 * Évaluateur MoLang minimal — couvre ce qui suffit pour les animations
 * d'idle Cobblemon (math.sin/cos en degrés, q.anim_time, arithmétique).
 *
 * Les identifiers inconnus (q.is_holding_item, v.foo, etc.) sont remplacés
 * par 0, avec un warning au premier usage.
 */

const warned = new Set<string>();

const HELPERS = {
    __sin: (deg: number) => Math.sin((deg * Math.PI) / 180),
    __cos: (deg: number) => Math.cos((deg * Math.PI) / 180),
    __tan: (deg: number) => Math.tan((deg * Math.PI) / 180),
    __asin: (x: number) => (Math.asin(x) * 180) / Math.PI,
    __acos: (x: number) => (Math.acos(x) * 180) / Math.PI,
    __atan: (x: number) => (Math.atan(x) * 180) / Math.PI,
    __atan2: (y: number, x: number) => (Math.atan2(y, x) * 180) / Math.PI,
    __mod: (a: number, b: number) => ((a % b) + b) % b,
    __clamp: (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v)),
    __lerp: (a: number, b: number, t: number) => a + (b - a) * t,
};

const HELPER_NAMES = Object.keys(HELPERS);
const HELPER_VALUES = Object.values(HELPERS);

const cache = new Map<string, (t: number) => number>();

function compile(rawExpr: string): (t: number) => number {
    let s = rawExpr.toLowerCase().trim();

    // q.anim_time / query.anim_time → __t
    s = s.replace(/\b(?:q|query)\.anim_time\b/g, '__t');

    // Math functions → helpers (degré-based) ou Math.*
    s = s.replace(/\bmath\.sin\b/g, '__sin');
    s = s.replace(/\bmath\.cos\b/g, '__cos');
    s = s.replace(/\bmath\.tan\b/g, '__tan');
    s = s.replace(/\bmath\.asin\b/g, '__asin');
    s = s.replace(/\bmath\.acos\b/g, '__acos');
    s = s.replace(/\bmath\.atan2\b/g, '__atan2');
    s = s.replace(/\bmath\.atan\b/g, '__atan');
    s = s.replace(/\bmath\.mod\b/g, '__mod');
    s = s.replace(/\bmath\.clamp\b/g, '__clamp');
    s = s.replace(/\bmath\.lerp\b/g, '__lerp');
    s = s.replace(/\bmath\.abs\b/g, 'Math.abs');
    s = s.replace(/\bmath\.sqrt\b/g, 'Math.sqrt');
    s = s.replace(/\bmath\.pow\b/g, 'Math.pow');
    s = s.replace(/\bmath\.exp\b/g, 'Math.exp');
    s = s.replace(/\bmath\.ln\b/g, 'Math.log');
    s = s.replace(/\bmath\.floor\b/g, 'Math.floor');
    s = s.replace(/\bmath\.ceil\b/g, 'Math.ceil');
    s = s.replace(/\bmath\.round\b/g, 'Math.round');
    s = s.replace(/\bmath\.min\b/g, 'Math.min');
    s = s.replace(/\bmath\.max\b/g, 'Math.max');
    s = s.replace(/\bmath\.pi\b/g, 'Math.PI');

    // Variables inconnues q.foo / v.foo / variable.foo / temp.foo / t.foo → 0
    s = s.replace(/\b(?:q|query|v|variable|t|temp|c|context)\.[a-z0-9_]+/g, (m) => {
        if (!warned.has(m)) {
            console.warn(`[molang] unknown identifier ${m} → 0`);
            warned.add(m);
        }
        return '(0)';
    });

    const body = `return (${s});`;
    const compiled = new Function('__t', ...HELPER_NAMES, body) as (
        t: number,
        ...helpers: unknown[]
    ) => number;

    return (t: number) => compiled(t, ...HELPER_VALUES);
}

/**
 * Évalue une valeur MoLang. Retourne 0 en cas d'erreur (avec warning).
 * `expr` peut être un nombre direct (passe-plat) ou une string MoLang.
 */
export function evalAt(expr: number | string, animTime: number): number {
    if (typeof expr === 'number') return expr;
    if (typeof expr !== 'string') return 0;

    let fn = cache.get(expr);
    if (!fn) {
        try {
            fn = compile(expr);
        } catch (err) {
            console.warn(`[molang] compile error on "${expr}":`, (err as Error).message);
            fn = () => 0;
        }
        cache.set(expr, fn);
    }
    try {
        const r = fn(animTime);
        return Number.isFinite(r) ? r : 0;
    } catch {
        return 0;
    }
}
