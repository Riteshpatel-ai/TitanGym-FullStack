import { useMemo, useState } from "react";
import axios from "../../api/axios";

const quickPrompts = [
    "What should I buy for home workouts?",
    "Recommend a beginner strength stack.",
    "Which products help recovery after leg day?",
];

function CoachChatPanel() {
    const [messages, setMessages] = useState([
        {
            role: "assistant",
            content:
                "Hi, I am Coach AI. Ask me which gear fits your workout, compare products, or check what is in stock.",
            recommendations: [],
        },
    ]);
    const [input, setInput] = useState("");
    const [loading, setLoading] = useState(false);

    const token = useMemo(() => localStorage.getItem("authToken"), []);

    const sendMessage = async (messageText) => {
        const trimmedMessage = messageText.trim();
        if (!trimmedMessage || loading) {
            return;
        }

        setLoading(true);
        setMessages((prev) => [
            ...prev,
            { role: "user", content: trimmedMessage, recommendations: [] },
        ]);

        try {
            const response = await axios.post(
                "/api/v1/coach/chat",
                { message: trimmedMessage },
                {
                    headers: token
                        ? {
                              Authorization: `Bearer ${token}`,
                          }
                        : undefined,
                }
            );

            setMessages((prev) => [
                ...prev,
                {
                    role: "assistant",
                    content: response.data.reply,
                    recommendations: response.data.recommendations || [],
                },
            ]);
            setInput("");
        } catch (err) {
            const fallback =
                err.response?.data?.error ||
                "Coach AI is temporarily unavailable. Please try again in a moment.";
            setMessages((prev) => [
                ...prev,
                {
                    role: "assistant",
                    content: fallback,
                    recommendations: [],
                },
            ]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <section
            id="coach-ai"
            className="w-full max-w-2xl mx-auto rounded-3xl border border-slate-200 bg-white/95 shadow-[0_20px_60px_rgba(15,23,42,0.14)] backdrop-blur dark:border-slate-700 dark:bg-slate-900/95"
        >
            <div className="flex items-center gap-4 border-b border-slate-200 px-6 py-5 dark:border-slate-700">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-lime-400 text-xl font-bold text-slate-900">
                    C
                </div>
                <div>
                    <h3 className="text-xl font-semibold text-slate-900 dark:text-white">Coach AI</h3>
                    <p className="text-sm text-emerald-600 dark:text-emerald-400">
                        Online with your product catalog
                    </p>
                </div>
            </div>

            <div className="space-y-4 px-4 py-5 sm:px-6">
                {messages.map((message, index) => (
                    <div
                        key={`${message.role}-${index}`}
                        className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}
                    >
                        <div
                            className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-6 ${
                                message.role === "user"
                                    ? "bg-slate-900 text-white"
                                    : "bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-100"
                            }`}
                        >
                            <p>{message.content}</p>
                            {message.recommendations?.length > 0 && (
                                <div className="mt-3 flex flex-wrap gap-2">
                                    {message.recommendations.map((recommendation) => (
                                        <span
                                            key={recommendation}
                                            className="rounded-full bg-lime-100 px-3 py-1 text-xs font-semibold text-lime-900 dark:bg-lime-300/20 dark:text-lime-200"
                                        >
                                            {recommendation}
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>

            <div className="border-t border-slate-200 px-4 py-4 dark:border-slate-700 sm:px-6">
                <div className="mb-3 flex flex-wrap gap-2">
                    {quickPrompts.map((prompt) => (
                        <button
                            key={prompt}
                            type="button"
                            onClick={() => sendMessage(prompt)}
                            className="rounded-full border border-lime-200 bg-lime-50 px-3 py-1 text-xs font-medium text-lime-900 transition hover:bg-lime-100 dark:border-lime-400/30 dark:bg-lime-400/10 dark:text-lime-100"
                        >
                            {prompt}
                        </button>
                    ))}
                </div>

                <form
                    className="flex items-center gap-3"
                    onSubmit={(event) => {
                        event.preventDefault();
                        sendMessage(input);
                    }}
                >
                    <input
                        value={input}
                        onChange={(event) => setInput(event.target.value)}
                        placeholder="Ask about gym products..."
                        className="h-12 flex-1 rounded-2xl border border-slate-300 bg-white px-4 text-slate-900 outline-none transition focus:border-lime-400 focus:ring-2 focus:ring-lime-200 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    />
                    <button
                        type="submit"
                        disabled={loading}
                        className="h-12 rounded-2xl bg-lime-400 px-5 font-semibold text-slate-900 transition hover:bg-lime-300 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {loading ? "..." : "Ask"}
                    </button>
                </form>
            </div>
        </section>
    );
}

export default CoachChatPanel;
