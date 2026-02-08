
import React, { useState, useEffect } from 'react';
import { X, Search, Check, AlertCircle, Calculator, Sparkles } from 'lucide-react';
import { Question, QuestionPack } from '@/types';
import AIQuestionGenerator from './AIQuestionGenerator';

interface CreatePackModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export default function CreatePackModal({ isOpen, onClose, onSuccess }: CreatePackModalProps) {
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [fetchingQuestions, setFetchingQuestions] = useState(false);
    const [questions, setQuestions] = useState<Question[]>([]);
    const [selectedQuestions, setSelectedQuestions] = useState<Question[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');

    // AI Modal State
    const [isAiModalOpen, setIsAiModalOpen] = useState(false);

    const [formData, setFormData] = useState<Partial<QuestionPack>>({
        title: '',
        description: '',
        subject: 'Mathematics',
        grade: 10,
        term: 1,
        priceCents: 4999, // R49.99 default
        capsStrand: '',
        isPublished: false
    });

    useEffect(() => {
        if (isOpen && step === 2 && questions.length === 0) {
            fetchQuestions();
        }
    }, [isOpen, step]);

    // Reset when closed
    useEffect(() => {
        if (!isOpen) {
            setStep(1);
            setFormData({
                title: '',
                description: '',
                subject: 'Mathematics', // Default
                grade: 10,
                term: 1,
                priceCents: 4999,
                capsStrand: '',
                isPublished: false
            });
            setSelectedQuestions([]);
            setError(null);
            setIsAiModalOpen(false);
        }
    }, [isOpen]);

    const fetchQuestions = async () => {
        setFetchingQuestions(true);
        setError(null);
        try {
            const res = await fetch('/api/questions');
            const data = await res.json();
            if (data.error) throw new Error(data.error);
            setQuestions(data);
        } catch (err: any) {
            setError(err.message || 'Failed to fetch questions');
        } finally {
            setFetchingQuestions(false);
        }
    };

    const handleToggleQuestion = (question: Question) => {
        if (!question.id) return;

        const isSelected = selectedQuestions.find(q => q.id === question.id);
        if (isSelected) {
            setSelectedQuestions(prev => prev.filter(q => q.id !== question.id));
        } else {
            setSelectedQuestions(prev => [...prev, question]);
        }
    };

    const handleAiQuestionsGenerated = (newQuestions: Question[]) => {
        // Add new questions to the local list so they appear immediately
        setQuestions(prev => [...newQuestions, ...prev]);

        // Auto-select them
        setSelectedQuestions(prev => [...prev, ...newQuestions]);
    };

    const calculateTotals = () => {
        const totalMarks = selectedQuestions.reduce((sum, q) => sum + (q.marks || 0), 0);
        return {
            count: selectedQuestions.length,
            marks: totalMarks
        };
    };

    const handleSubmit = async () => {
        if (!formData.title || !formData.description || !formData.subject || !formData.capsStrand) {
            setError("Please fill in all required fields");
            return;
        }

        if (selectedQuestions.length === 0) {
            setError("Please select at least one question");
            return;
        }

        setLoading(true);
        setError(null);

        const totals = calculateTotals();

        const packData: Partial<QuestionPack> = {
            ...formData,
            questionCount: totals.count,
            totalMarks: totals.marks,
            questionIds: selectedQuestions.map(q => q.id!).filter(Boolean),
            createdAt: Date.now(),
            version: 1,
            isPublished: false
        };

        try {
            const res = await fetch('/api/packs', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(packData)
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to create pack');

            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.message || 'Failed to create pack');
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    const filteredQuestions = questions.filter(q =>
        q.questionText?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        q.subject?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        q.capsTopicId?.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const totals = calculateTotals();

    return (
        <>
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
                <div className="bg-white rounded-[2rem] w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
                    {/* Header */}
                    <div className="px-8 py-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                        <div>
                            <h2 className="text-xl font-black text-slate-900 tracking-tight">Create Question Pack</h2>
                            <p className="text-sm text-slate-500 font-medium mt-0.5">Step {step} of 2: {step === 1 ? 'Pack Details' : 'Select Questions'}</p>
                        </div>
                        <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-xl text-slate-400 hover:text-slate-600 transition-colors">
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Content */}
                    <div className="flex-1 overflow-y-auto p-8">
                        {error && (
                            <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center gap-3">
                                <AlertCircle className="w-5 h-5" />
                                <span className="font-bold text-sm">{error}</span>
                            </div>
                        )}

                        {step === 1 ? (
                            <div className="space-y-6">
                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Title</label>
                                        <input
                                            type="text"
                                            placeholder="e.g. Grade 10 Math Term 1 Prep"
                                            className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all"
                                            value={formData.title}
                                            onChange={e => setFormData({ ...formData, title: e.target.value })}
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Pricing (Cents)</label>
                                        <input
                                            type="number"
                                            className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all"
                                            value={formData.priceCents}
                                            onChange={e => setFormData({ ...formData, priceCents: Number(e.target.value) })}
                                        />
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Description</label>
                                    <textarea
                                        className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-medium text-slate-700 h-24 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all resize-none"
                                        placeholder="Describe what's in this pack..."
                                        value={formData.description}
                                        onChange={e => setFormData({ ...formData, description: e.target.value })}
                                    ></textarea>
                                </div>

                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Subject</label>
                                        <select
                                            className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all"
                                            value={formData.subject}
                                            onChange={e => setFormData({ ...formData, subject: e.target.value })}
                                        >
                                            <option value="Mathematics">Mathematics</option>
                                            <option value="Physical Sciences">Physical Sciences</option>
                                            <option value="Life Sciences">Life Sciences</option>
                                            <option value="Accounting">Accounting</option>
                                            <option value="English">English</option>
                                            <option value="Business Studies">Business Studies</option>
                                        </select>
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">CAPS Strand</label>
                                        <input
                                            type="text"
                                            placeholder="e.g. Algebra & Equations"
                                            className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all"
                                            value={formData.capsStrand}
                                            onChange={e => setFormData({ ...formData, capsStrand: e.target.value })}
                                        />
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Grade</label>
                                        <select
                                            className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all"
                                            value={formData.grade}
                                            onChange={e => setFormData({ ...formData, grade: Number(e.target.value) })}
                                        >
                                            <option value={10}>Grade 10</option>
                                            <option value={11}>Grade 11</option>
                                            <option value={12}>Grade 12</option>
                                        </select>
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Term</label>
                                        <select
                                            className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all"
                                            value={formData.term}
                                            onChange={e => setFormData({ ...formData, term: Number(e.target.value) })}
                                        >
                                            <option value={1}>Term 1</option>
                                            <option value={2}>Term 2</option>
                                            <option value={3}>Term 3</option>
                                            <option value={4}>Term 4</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="h-full flex flex-col">
                                {/* Stats Bar & AI Action */}
                                <div className="flex gap-4 mb-6">
                                    <div className="flex-1 bg-primary-50 border border-primary-100 rounded-xl p-4 flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center text-primary-600">
                                                <Calculator className="w-5 h-5" />
                                            </div>
                                            <div>
                                                <div className="text-[10px] font-black uppercase text-primary-400 tracking-widest">Selected</div>
                                                <div className="font-black text-primary-900">{totals.count} Questions</div>
                                            </div>
                                        </div>
                                        <div className="text-right">
                                            <div className="text-[10px] font-black uppercase text-primary-400 tracking-widest">Total Value</div>
                                            <div className="font-black text-primary-900">{totals.marks} Marks</div>
                                        </div>
                                    </div>

                                    <button
                                        onClick={() => setIsAiModalOpen(true)}
                                        className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-violet-600 text-white rounded-xl font-bold shadow-lg shadow-indigo-200 hover:-translate-y-0.5 transition-all flex flex-col items-center justify-center shrink-0 min-w-[100px]"
                                    >
                                        <Sparkles className="w-5 h-5 mb-1" />
                                        <span className="text-[10px] uppercase tracking-widest">AI Generate</span>
                                    </button>
                                </div>

                                {/* Search */}
                                <div className="relative mb-4">
                                    <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                                    <input
                                        type="text"
                                        placeholder="Search questions by text or topic..."
                                        className="w-full pl-12 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 transition-all"
                                        value={searchQuery}
                                        onChange={e => setSearchQuery(e.target.value)}
                                    />
                                </div>

                                {/* List */}
                                <div className="flex-1 overflow-y-auto space-y-2 pr-2">
                                    {fetchingQuestions ? (
                                        <div className="text-center py-10 text-slate-400 font-medium">Loading questions...</div>
                                    ) : filteredQuestions.length === 0 ? (
                                        <div className="text-center py-10 text-slate-400 font-medium">No questions found. Use AI to generate some!</div>
                                    ) : (
                                        filteredQuestions.map(q => {
                                            const isSelected = selectedQuestions.some(sq => sq.id === q.id);
                                            return (
                                                <div
                                                    key={q.id}
                                                    onClick={() => handleToggleQuestion(q)}
                                                    className={`p-4 rounded-xl border cursor-pointer transition-all flex items-start gap-4 ${isSelected ? 'bg-primary-50 border-primary-200' : 'bg-white border-slate-100 hover:border-slate-300'}`}
                                                >
                                                    <div className={`w-6 h-6 rounded-md border-2 flex items-center justify-center flex-shrink-0 transition-colors ${isSelected ? 'bg-primary-500 border-primary-500 text-white' : 'border-slate-300'}`}>
                                                        {isSelected && <Check className="w-4 h-4" />}
                                                    </div>
                                                    <div className="flex-1">
                                                        <p className="text-sm font-bold text-slate-700 line-clamp-2">{q.questionText}</p>
                                                        <div className="flex gap-2 mt-2">
                                                            <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-500 uppercase">{q.subject}</span>
                                                            <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-500 uppercase">{q.marks} Marks</span>
                                                            <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-500 uppercase">{q.cognitiveLevel}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            );
                                        })
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Footer */}
                    <div className="px-8 py-6 border-t border-slate-100 bg-slate-50/50 flex justify-end gap-3">
                        <button
                            onClick={step === 1 ? onClose : () => setStep(1)}
                            className="px-6 py-2.5 font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-200/50 rounded-xl transition-colors"
                            disabled={loading}
                        >
                            {step === 1 ? 'Cancel' : 'Back'}
                        </button>
                        <button
                            onClick={step === 1 ? () => setStep(2) : handleSubmit}
                            disabled={loading || (step === 2 && selectedQuestions.length === 0)}
                            className="px-6 py-2.5 bg-slate-900 text-white font-bold rounded-xl hover:bg-slate-800 transition-all shadow-lg shadow-slate-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                        >
                            {loading ? 'Creating...' : step === 1 ? 'Next: Add Questions' : 'Create Pack'}
                        </button>
                    </div>
                </div>
            </div>

            {/* AI Modal */}
            {isAiModalOpen && (
                <AIQuestionGenerator
                    onClose={() => setIsAiModalOpen(false)}
                    onQuestionsGenerated={handleAiQuestionsGenerated}
                    prefillGrade={formData.grade || 10}
                    prefillSubject={formData.subject || 'Mathematics'}
                />
            )}
        </>
    );
}
