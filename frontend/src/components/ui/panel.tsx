import type { CSSProperties, HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/utils/cn';

type PanelTone = 'default' | 'accent' | 'soft';

interface PanelProps extends HTMLAttributes<HTMLDivElement> {
    children: ReactNode;
    tone?: PanelTone;
    inset?: boolean;
}

const toneStyles: Record<PanelTone, CSSProperties> = {
    default: {
        backgroundColor: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
    },
    accent: {
        backgroundColor: 'var(--color-bg-card)',
        borderColor: 'var(--color-border-strong)',
    },
    soft: {
        backgroundColor: 'var(--color-bg-soft)',
        borderColor: 'rgba(255,255,255,0.08)',
    },
};

export function Panel({
                          children,
                          className,
                          tone = 'default',
                          inset = true,
                          style,
                          ...props
                      }: PanelProps) {
    return (
        <div
            className={cn(
                'relative overflow-hidden rounded-[var(--radius-panel)] border shadow-[var(--shadow-panel)]',
                className
            )}
            style={{ ...toneStyles[tone], ...style }}
            {...props}
        >
            <div
                className="pointer-events-none absolute inset-x-0 top-0 h-px"
                style={{
                    background:
                        'linear-gradient(90deg, rgba(230,57,70,0.92) 0%, rgba(230,57,70,0.20) 34%, transparent 100%)',
                }}
            />
            <div className={cn('relative', inset && 'p-6 sm:p-8')}>{children}</div>
        </div>
    );
}