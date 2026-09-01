import { Link } from "react-router-dom";
import CoachChatPanel from "../components/CoachAI/CoachChatPanel";

function Home() {
    return (
        <main className="min-h-screen bg-gradient-to-br from-lime-50 via-white to-slate-100 px-4 py-8 text-slate-900 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950 dark:text-white">
            <div className="mx-auto grid max-w-7xl gap-8 lg:grid-cols-[1.05fr_1fr]">
                <section className="flex flex-col justify-center space-y-6 rounded-[2rem] border border-slate-200 bg-white/80 p-8 shadow-[0_20px_60px_rgba(15,23,42,0.08)] backdrop-blur dark:border-slate-800 dark:bg-slate-900/80 lg:p-12">
                    <div className="inline-flex w-fit rounded-full bg-lime-100 px-4 py-2 text-sm font-semibold tracking-[0.28em] text-lime-800 dark:bg-lime-400/10 dark:text-lime-300">
                        POWERED BY GROQ
                    </div>
                    <div className="space-y-4">
                        <h1 className="max-w-xl text-5xl font-black leading-[0.95] tracking-tight sm:text-6xl">
                            Your personal gym gear coach.
                        </h1>
                        <p className="max-w-lg text-lg leading-8 text-slate-600 dark:text-slate-300">
                            TitanGym is a gym-only store for strength, recovery, nutrition, and everyday
                            training essentials — with Coach AI to help you choose the right gear fast.
                        </p>
                    </div>

                    <div className="flex flex-wrap gap-3">
                        <Link
                            to="/products"
                            className="rounded-full bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-700 dark:bg-lime-400 dark:text-slate-900 dark:hover:bg-lime-300"
                        >
                            Shop gym products
                        </Link>
                        <Link
                            to="/coach-ai"
                            className="rounded-full border border-slate-300 px-6 py-3 text-sm font-semibold text-slate-800 transition hover:border-lime-400 hover:text-lime-700 dark:border-slate-700 dark:text-slate-200 dark:hover:text-lime-300"
                        >
                            Ask Coach AI
                        </Link>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-3">
                        {[
                            "Strength essentials",
                            "Recovery must-haves",
                            "Nutrition picks",
                        ].map((item) => (
                            <div
                                key={item}
                                className="rounded-2xl border border-lime-200 bg-lime-50 px-4 py-4 text-sm font-semibold text-lime-900 dark:border-lime-400/20 dark:bg-lime-400/10 dark:text-lime-100"
                            >
                                {item}
                            </div>
                        ))}
                    </div>
                </section>

                <CoachChatPanel />
            </div>
        </main>
    );
}

export default Home;