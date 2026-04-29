import { AppFooter } from './app-footer';
import { AppHeader } from './app-header';

interface AppShellProps {
    children: React.ReactNode;
}

export function AppShell({ children }: AppShellProps) {
    return (
        <div className="relative min-h-screen overflow-x-clip">
            <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
                <div
                    className="absolute left-[-14%] top-20 hidden h-[34rem] w-[60vw] xl:block"
                    style={{
                        clipPath: 'polygon(0 0, 100% 18%, 100% 82%, 0 100%)',
                        background:
                            'linear-gradient(90deg, rgba(120,214,255,0.24), rgba(120,214,255,0.08) 54%, transparent 100%)',
                    }}
                />

                <div
                    className="absolute left-6 top-24 h-[50vh] w-[48vw] max-w-[620px]"
                    style={{
                        background:
                            'radial-gradient(circle at 25% 35%, rgba(116,228,255,0.18), transparent 55%)',
                    }}
                />
            </div>

            <div className="relative z-10 flex min-h-screen flex-col">
                <AppHeader />
                <main className="mx-auto flex w-full max-w-[1500px] flex-1 px-4 pb-8 pt-5 sm:px-6 lg:px-8">
                    <div className="w-full">{children}</div>
                </main>
                <AppFooter />
            </div>
        </div>
    );
}
