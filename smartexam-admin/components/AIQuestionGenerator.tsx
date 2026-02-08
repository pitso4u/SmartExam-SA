
import React, { useState, useEffect } from 'react';
import { X, Sparkles, Check, ChevronRight, Save, Wand2 } from 'lucide-react';
import { Question } from '@/types';

interface AIQuestionGeneratorProps {
    onClose: () => void;
    onQuestionsGenerated: (questions: Question[]) => void;
    prefillGrade: number;
    prefillSubject: string;
}

export default function AIQuestionGenerator({ onClose, onQuestionsGenerated, prefillGrade, prefillSubject }: AIQuestionGeneratorProps) {
    const [step, setStep] = useState(1); // 1: Input, 2: Review
    const [loading, setLoading] = useState(false);
    const [retrySeconds, setRetrySeconds] = useState(0);

    useEffect(() => {
        if (retrySeconds > 0) {
            const timer = setTimeout(() => setRetrySeconds(s => s - 1), 1000);
            return () => clearTimeout(timer);
        }
    }, [retrySeconds]);

    // Inputs
    const [topic, setTopic] = useState('');
    const [count, setCount] = useState(5);
    const [difficulty, setDifficulty] = useState('Mixed');
    const [model, setModel] = useState('google/gemini-2.0-flash-lite-preview-02-05:free');

    // Results
    const [generatedQuestions, setGeneratedQuestions] = useState<Question[]>([]);
    const [selectedIndices, setSelectedIndices] = useState<number[]>([]);

    const handleGenerate = async () => {
        if (!topic) return;

        setLoading(true);
        setRetrySeconds(0);
        try {
            const res = await fetch('/api/ai/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    topic,
                    grade: prefillGrade,
                    subject: prefillSubject,
                    count,
                    difficulty,
                    model
                })
            });

            const data = await res.json();

            if (res.status === 429) {
                // Try to extract seconds from error message or default to 60
                const match = data.error?.match(/retry in (\d+)/i);
                setRetrySeconds(match ? parseInt(match[1]) + 5 : 60);
                throw new Error("Rate limit exceeded. Please wait...");
            }

            if (data.error) throw new Error(data.error);

            setGeneratedQuestions(data);
            // Auto-select all by default
            setSelectedIndices(data.map((_: any, i: number) => i));
            setStep(2);
        } catch (err: any) {
            if (err.message !== "Rate limit exceeded. Please wait...") {
                alert('Generation failed: ' + err.message);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        setLoading(true);
        const questionsToSave = generatedQuestions.filter((_, i) => selectedIndices.includes(i));

        try {
            // Save each question to Firestore via API
            const savedQuestions = await Promise.all(questionsToSave.map(async (q) => {
                const res = await fetch('/api/questions', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(q)
                });
                const saved = await res.json();
                return { ...q, id: saved.id };
            }));

            onQuestionsGenerated(savedQuestions);
            onClose();
        } catch (err: any) {
            alert('Failed to save questions: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const toggleSelection = (index: number) => {
        if (selectedIndices.includes(index)) {
            setSelectedIndices(prev => prev.filter(i => i !== index));
        } else {
            setSelectedIndices(prev => [...prev, index]);
        }
    };

    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
            <div className="bg-white rounded-[2rem] w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden border border-primary-100">

                {/* Header */}
                <div className="px-8 py-6 border-b border-slate-100 bg-gradient-to-r from-primary-50 to-white flex justify-between items-center">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-gradient-to-br from-primary-400 to-indigo-500 rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary-200">
                            <Sparkles className="w-5 h-5" />
                        </div>
                        <div>
                            <h2 className="text-xl font-black text-slate-900 tracking-tight">AI Generator</h2>
                            <p className="text-xs text-slate-500 font-bold uppercase tracking-wider">Powered by Gemini Pro</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-xl text-slate-400 hover:text-slate-600 transition-colors">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-8">
                    {step === 1 ? (
                        <div className="space-y-6">
                            <div>
                                <label className="text-xs font-black text-slate-500 uppercase tracking-widest mb-2 block">Context</label>
                                <div className="flex gap-2">
                                    <span className="px-3 py-1 bg-slate-100 rounded-lg text-xs font-bold text-slate-600 uppercase">{prefillSubject}</span>
                                    <span className="px-3 py-1 bg-slate-100 rounded-lg text-xs font-bold text-slate-600 uppercase">Grade {prefillGrade}</span>
                                </div>
                            </div>

                            <div className="space-y-2">
                                <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Topic / Content Strand</label>
                                <input
                                    type="text"
                                    autoFocus
                                    placeholder="e.g. Linear Equations, Ancient Egypt, Photosynthesis..."
                                    className="w-full px-4 py-3.5 bg-slate-50 border-2 border-slate-200 rounded-xl font-bold text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-primary-500 focus:ring-4 focus:ring-primary-100 transition-all"
                                    value={topic}
                                    onChange={e => setTopic(e.target.value)}
                                />
                                <p className="text-xs text-slate-400 font-medium ml-1">Be specific for better results</p>
                            </div>

                            <div className="grid grid-cols-2 gap-6">
                                <div className="space-y-2">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Model</label>
                                    <select
                                        className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:border-primary-500 transition-all"
                                        value={model}
                                        onChange={e => setModel(e.target.value)}
                                    >
                                        <optgroup label="Free (Recommended)">
                                            <option value="google/gemini-2.0-flash-lite-preview-02-05:free">Gemini 2.0 Flash Lite (Free)</option>
                                            <option value="google/gemini-2.0-pro-exp-02-05:free">Gemini 2.0 Pro (Free)</option>
                                            <option value="deepseek/deepseek-r1-distill-llama-70b:free">DeepSeek R1 (Free)</option>
                                        </optgroup>
                                        <optgroup label="Premium (Credits Required)">
                                            <option value="x-ai/grok-beta">Grok Beta</option>
                                            <option value="moonshot-ai/moonshot-v1-8k">Kimi (Moonshot)</option>
                                            <option value="openai/gpt-4o-mini">GPT-4o Mini</option>
                                        </optgroup>
                                    </select>
                                </div>
                                <div className="space-y-2">
                                    {/* Spacer or future input */}
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-6">
                                <div className="space-y-2">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Question Count</label>
                                    <select
                                        className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:border-primary-500 transition-all"
                                        value={count}
                                        onChange={e => setCount(Number(e.target.value))}
                                    >
                                        <option value={3}>3 Questions</option>
                                        <option value={5}>5 Questions</option>
                                        <option value={10}>10 Questions</option>
                                    </select>
                                </div>
                                <div className="space-y-2">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Difficulty</label>
                                    <select
                                        className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:border-primary-500 transition-all"
                                        value={difficulty}
                                        onChange={e => setDifficulty(e.target.value)}
                                    >
                                        <option value="Mixed">Mixed Levels</option>
                                        <option value="Easy">Entry Level</option>
                                        <option value="Medium">Standard Grade</option>
                                        <option value="Hard">Advanced / Extension</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="space-y-4">
                            <div className="flex justify-between items-center mb-4">
                                <h3 className="font-bold text-slate-900">Review Generated Questions</h3>
                                <div className="text-xs font-bold text-slate-500 uppercase">{selectedIndices.length} Selected</div>
                            </div>

                            {generatedQuestions.map((q, i) => {
                                const isSelected = selectedIndices.includes(i);
                                return (
                                    <div
                                        key={i}
                                        onClick={() => toggleSelection(i)}
                                        className={`p-4 rounded-xl border-2 transition-all cursor-pointer group ${isSelected ? 'border-primary-500 bg-primary-50/30' : 'border-slate-100 bg-white hover:border-slate-200'}`}
                                    >
                                        <div className="flex items-start gap-4">
                                            <div className={`w-6 h-6 rounded-lg flex items-center justify-center flex-shrink-0 transition-colors ${isSelected ? 'bg-primary-500 text-white' : 'bg-slate-100 text-slate-300'}`}>
                                                <Check className="w-3.5 h-3.5" />
                                            </div>
                                            <div className="flex-1 space-y-2">
                                                <p className="font-bold text-slate-800 text-sm leading-relaxed">{q.questionText}</p>

                                                {/* Options Preview for MC/Text */}
                                                {q.content?.options && (
                                                    <div className="grid grid-cols-2 gap-2 mt-2">
                                                        {q.content.options.map((opt: string, idx: number) => (
                                                            <div key={idx} className={`text-xs px-2 py-1.5 rounded border ${opt === q.content.correctAnswer ? 'bg-emerald-50 border-emerald-200 text-emerald-700 font-bold' : 'bg-slate-50 border-slate-100 text-slate-500'}`}>
                                                                {opt}
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}

                                                <div className="flex gap-2">
                                                    <span className="text-[10px] font-bold text-primary-600 bg-primary-50 px-2 py-1 rounded uppercase tracking-wider">{q.cognitiveLevel}</span>
                                                    <span className="text-[10px] font-bold text-slate-500 bg-slate-100 px-2 py-1 rounded uppercase tracking-wider">{q.marks} Marks</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="px-8 py-6 border-t border-slate-100 bg-slate-50/50 flex justify-end gap-3">
                    {step === 1 ? (
                        <>
                            <button onClick={onClose} className="px-6 py-3 font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-200/50 rounded-xl transition-colors">Cancel</button>
                            <button
                                onClick={handleGenerate}
                                disabled={loading || !topic || retrySeconds > 0}
                                className={`px-8 py-3 font-bold rounded-xl transition-all flex items-center gap-2 ${retrySeconds > 0 ? 'bg-orange-100 text-orange-600' : 'bg-gradient-to-r from-primary-600 to-indigo-600 text-white hover:shadow-lg hover:shadow-primary-200 hover:-translate-y-0.5'} disabled:opacity-50 disabled:cursor-not-allowed`}
                            >
                                {loading ? (
                                    <>
                                        <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                                        <span>Generating...</span>
                                    </>
                                ) : retrySeconds > 0 ? (
                                    <span>Retry in {retrySeconds}s</span>
                                ) : (
                                    <>
                                        <Wand2 className="w-4 h-4" />
                                        <span>Generate Questions</span>
                                    </>
                                )}
                            </button>
                        </>
                    ) : (
                        <>
                            <button onClick={() => setStep(1)} className="px-6 py-3 font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-200/50 rounded-xl transition-colors">Back</button>
                            <button
                                onClick={handleSave}
                                disabled={loading || selectedIndices.length === 0}
                                className="px-8 py-3 bg-slate-900 text-white font-bold rounded-xl hover:bg-slate-800 transition-all flex items-center gap-2 disabled:opacity-50"
                            >
                                {loading ? 'Saving...' : `Import ${selectedIndices.length} Questions`}
                                <ChevronRight className="w-4 h-4" />
                            </button>
                        </>
                    )}
                </div>
            </div>
        </div >
    );
}
