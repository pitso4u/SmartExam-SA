import React, { useState, useEffect } from 'react';
import { X, Search, Check, AlertCircle, Calculator, Sparkles, Eye, Edit3 } from 'lucide-react';
import { Question, QuestionPack } from '@/types';
import AIQuestionGenerator from './AIQuestionGenerator';

interface ViewEditPackModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    pack: QuestionPack | null;
    mode: 'view' | 'edit';
}

export default function ViewEditPackModal({ isOpen, onClose, onSuccess, pack, mode }: ViewEditPackModalProps) {
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [fetchingQuestions, setFetchingQuestions] = useState(false);
    const [questions, setQuestions] = useState<Question[]>([]);
    const [selectedQuestions, setSelectedQuestions] = useState<Question[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [isAiModalOpen, setIsAiModalOpen] = useState(false);

    const [formData, setFormData] = useState<Partial<QuestionPack>>({
        title: '',
        description: '',
        subject: 'Mathematics',
        grade: 10,
        term: 1,
        priceCents: 4999,
        capsStrand: '',
        isPublished: false
    });

    const isViewMode = mode === 'view';

    useEffect(() => {
        if (isOpen && pack) {
            setFormData({ ...pack });
            fetchPackQuestions();
            setStep(1);
            setError(null);
        }
    }, [isOpen, pack]);

    const fetchPackQuestions = async () => {
        if (!pack?.questionIds?.length) return;
        setFetchingQuestions(true);
        try {
            // Fetch specific questions for this pack
            const res = await fetch(`/api/questions?ids=${pack.questionIds.join(',')}`);
            const packQuestions = await res.json();
            
            if (packQuestions.error) {
                throw new Error(packQuestions.error);
            }
            
            setQuestions(packQuestions);
            setSelectedQuestions(packQuestions);
        } catch (err: any) {
            setError('Failed to load questions: ' + (err.message || err.toString()));
        } finally {
            setFetchingQuestions(false);
        }
    };

    // Navigation Handler
    const handleNext = () => {
        if (step === 1) {
            // If moving to step 2 and in edit mode, you might want to fetch ALL
            // available questions to allow adding new ones.
            if (!isViewMode) fetchAllQuestions();
            setStep(2);
        }
    };

    const calculateTotals = () => {
        if (!selectedQuestions.length) return { count: 0, marks: 0 };
        const totalMarks = selectedQuestions.reduce((sum, q) => sum + (q.marks || 0), 0);
        return { count: selectedQuestions.length, marks: totalMarks };
    };

    const totals = calculateTotals();

    const handleToggleQuestion = (question: Question) => {
        if (mode === 'view') return; // No selection changes in view mode

        if (!question.id) return;

        const isSelected = selectedQuestions.find(q => q.id === question.id);
        if (isSelected) {
            setSelectedQuestions(prev => prev.filter(q => q.id !== question.id));
        } else {
            setSelectedQuestions(prev => [...prev, question]);
        }
    };

    const handleAiQuestionsGenerated = (newQuestions: Question[]) => {
        if (mode === 'view') return;

        // Add new questions to the local list so they appear immediately
        setQuestions(prev => [...newQuestions, ...prev]);

        // Auto-select them
        setSelectedQuestions(prev => [...prev, ...newQuestions]);
    };

    const handleSubmit = async () => {
        if (mode === 'view') return;

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
            id: pack?.id,
            questionCount: totals.count,
            totalMarks: totals.marks,
            questionIds: selectedQuestions.map(q => q.id!).filter(Boolean),
            createdAt: pack?.createdAt || Date.now(),
            version: (pack?.version || 0) + 1,
        };

        try {
            const res = await fetch(`/api/packs/${pack?.id}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(packData)
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to update pack');

            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.message || 'Failed to update pack');
        } finally {
            setLoading(false);
        }
    };

    const fetchAllQuestions = async () => {
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

    if (!isOpen || !pack) return null;

    return (
        <>
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
                <div className="bg-white rounded-[2rem] w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">

                    {/* Header */}
                    <div className="px-8 py-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
                        <div>
                            <h2 className="text-xl font-black text-slate-900 tracking-tight">
                                {isViewMode ? 'View' : 'Edit'} Question Pack
                            </h2>
                            <p className="text-sm text-slate-500 font-medium mt-0.5">
                                {pack?.title} - Step {step} of 2: {step === 1 ? 'Pack Details' : 'Questions'}
                                {isViewMode && ' (Read-only)'}
                            </p>
                        </div>
                        <div className="flex items-center gap-2">
                            {isViewMode ? (
                                <Eye className="w-5 h-5 text-slate-400" />
                            ) : (
                                <Edit3 className="w-5 h-5 text-slate-400" />
                            )}
                            <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-xl text-slate-400 hover:text-slate-600 transition-colors">
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto p-8">
                        {/* Step 1: Form Fields */}
                        {step === 1 && (
                            <div className="space-y-6">
                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Title</label>
                                        <input
                                            type="text"
                                            placeholder="e.g. Grade 10 Math Term 1 Prep"
                                            className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                            value={formData.title}
                                            onChange={isViewMode ? undefined : e => setFormData({ ...formData, title: e.target.value })}
                                            readOnly={isViewMode}
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Pricing (Cents)</label>
                                        <input
                                            type="number"
                                            className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                            value={formData.priceCents}
                                            onChange={isViewMode ? undefined : e => setFormData({ ...formData, priceCents: Number(e.target.value) })}
                                            readOnly={isViewMode}
                                        />
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Description</label>
                                    <textarea
                                        className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-medium text-slate-700 h-24 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all resize-none ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                        placeholder="Describe what's in this pack..."
                                        value={formData.description}
                                        onChange={isViewMode ? undefined : e => setFormData({ ...formData, description: e.target.value })}
                                        readOnly={isViewMode}
                                    ></textarea>
                                </div>

                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Subject</label>
                                        <select
                                            className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                            value={formData.subject}
                                            onChange={isViewMode ? undefined : e => setFormData({ ...formData, subject: e.target.value })}
                                            disabled={isViewMode}
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
                                            className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                            value={formData.capsStrand}
                                            onChange={isViewMode ? undefined : e => setFormData({ ...formData, capsStrand: e.target.value })}
                                            readOnly={isViewMode}
                                        />
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Grade</label>
                                        <select
                                            className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                            value={formData.grade}
                                            onChange={isViewMode ? undefined : e => setFormData({ ...formData, grade: Number(e.target.value) })}
                                            disabled={isViewMode}
                                        >
                                            <option value={10}>Grade 10</option>
                                            <option value={11}>Grade 11</option>
                                            <option value={12}>Grade 12</option>
                                        </select>
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Term</label>
                                        <select
                                            className={`w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all ${isViewMode ? 'cursor-not-allowed opacity-60' : ''}`}
                                            value={formData.term}
                                            onChange={isViewMode ? undefined : e => setFormData({ ...formData, term: Number(e.target.value) })}
                                            disabled={isViewMode}
                                        >
                                            <option value={1}>Term 1</option>
                                            <option value={2}>Term 2</option>
                                            <option value={3}>Term 3</option>
                                            <option value={4}>Term 4</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Step 2: Question Selection */}
                        {step === 2 && (
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

                                    {!isViewMode && (
                                        <button
                                            onClick={() => setIsAiModalOpen(true)}
                                            className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-violet-600 text-white rounded-xl font-bold shadow-lg shadow-indigo-200 hover:-translate-y-0.5 transition-all flex flex-col items-center justify-center shrink-0 min-w-[100px]"
                                        >
                                            <Sparkles className="w-5 h-5 mb-1" />
                                            <span className="text-[10px] uppercase tracking-widest">AI Generate</span>
                                        </button>
                                    )}
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
                                    ) : questions.length === 0 ? (
                                        <div className="text-center py-10 text-slate-400 font-medium">
                                            {searchQuery ? 'No questions found matching your search.' : 'No questions available.'}
                                        </div>
                                    ) : (
                                        questions
                                            .filter(q =>
                                                q.questionText?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                                                q.subject?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                                                q.capsTopicId?.toLowerCase().includes(searchQuery.toLowerCase())
                                            )
                                            .map(q => {
                                                const isSelected = selectedQuestions.some(sq => sq.id === q.id);
                                                return (
                                                    <div
                                                        key={q.id}
                                                        onClick={() => handleToggleQuestion(q)}
                                                        className={`p-4 rounded-xl border cursor-pointer transition-all flex items-start gap-4 ${
                                                            isSelected
                                                                ? 'bg-primary-50 border-primary-200'
                                                                : 'bg-white border-slate-100 hover:border-slate-300'
                                                        } ${isViewMode ? 'cursor-default' : ''}`}
                                                    >
                                                        {!isViewMode && (
                                                            <div className={`w-6 h-6 rounded-md border-2 flex items-center justify-center flex-shrink-0 transition-colors ${isSelected ? 'bg-primary-500 border-primary-500 text-white' : 'border-slate-300'}`}>
                                                                {isSelected && <Check className="w-4 h-4" />}
                                                            </div>
                                                        )}
                                                        <div className="flex-1">
                                                            <p className="text-sm font-bold text-slate-700 line-clamp-2">{q.questionText}</p>
                                                            <div className="flex gap-2 mt-2">
                                                                <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-500 uppercase">{q.subject}</span>
                                                                <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-500 uppercase">{q.marks} Marks</span>
                                                                <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-bold text-slate-500 uppercase">{q.cognitiveLevel}</span>
                                                            </div>
                                                            
                                                            {/* Question Content Display */}
                                                            <div className="mt-3 space-y-2">
                                                                {/* Multiple Choice Options */}
                                                                {q.content?.options && Array.isArray(q.content.options) && (
                                                                    <div className="space-y-2">
                                                                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Options:</div>
                                                                        <div className="grid grid-cols-1 gap-2">
                                                                            {q.content.options.map((option: string, idx: number) => {
                                                                                const isCorrect = option === q.content.correctAnswer;
                                                                                return (
                                                                                    <div
                                                                                        key={idx}
                                                                                        className={`p-3 rounded-lg border-2 text-sm font-medium ${
                                                                                            isCorrect
                                                                                                ? 'bg-emerald-50 border-emerald-300 text-emerald-800'
                                                                                                : 'bg-slate-50 border-slate-200 text-slate-700'
                                                                                        }`}
                                                                                    >
                                                                                        <div className="flex items-center gap-3">
                                                                                            <div className={`w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold ${
                                                                                                isCorrect
                                                                                                    ? 'bg-emerald-500 text-white'
                                                                                                    : 'bg-slate-300 text-slate-600'
                                                                                            }`}>
                                                                                                {String.fromCharCode(65 + idx)}
                                                                                            </div>
                                                                                            <span>{option}</span>
                                                                                            {isCorrect && (
                                                                                                <div className="ml-auto flex items-center gap-1 text-emerald-600">
                                                                                                    <Check className="w-4 h-4" />
                                                                                                    <span className="text-xs font-bold">Correct Answer</span>
                                                                                                </div>
                                                                                            )}
                                                                                        </div>
                                                                                    </div>
                                                                                );
                                                                            })}
                                                                        </div>
                                                                    </div>
                                                                )}

                                                                {/* True/False Answer */}
                                                                {q.type === 'TRUE_FALSE' && q.content?.correctAnswer && (
                                                                    <div className="space-y-2">
                                                                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Answer:</div>
                                                                        <div className={`p-3 rounded-lg border-2 font-bold text-center ${
                                                                            q.content.correctAnswer === 'true'
                                                                                ? 'bg-emerald-50 border-emerald-300 text-emerald-800'
                                                                                : 'bg-red-50 border-red-300 text-red-800'
                                                                        }`}>
                                                                            {q.content.correctAnswer === 'true' ? 'TRUE' : 'FALSE'}
                                                                        </div>
                                                                    </div>
                                                                )}

                                                                {/* Other Question Types */}
                                                                {q.type === 'FILL_IN_BLANKS' && q.content?.blanks && Array.isArray(q.content.blanks) && (
                                                                    <div className="space-y-2">
                                                                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Answers:</div>
                                                                        <div className="space-y-2">
                                                                            {q.content.blanks.map((blank: any, idx: number) => (
                                                                                <div key={idx} className="p-2 bg-blue-50 border border-blue-200 rounded text-sm">
                                                                                    <span className="font-bold text-blue-800">Blank {idx + 1}:</span> {blank.answer}
                                                                                </div>
                                                                            ))}
                                                                        </div>
                                                                    </div>
                                                                )}

                                                                {/* Match Columns */}
                                                                {q.type === 'MATCH_COLUMNS' && q.content?.pairs && Array.isArray(q.content.pairs) && (
                                                                    <div className="space-y-2">
                                                                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Column Matches:</div>
                                                                        <div className="grid grid-cols-2 gap-4">
                                                                            {q.content.pairs.map((pair: any, idx: number) => (
                                                                                <div key={idx} className="space-y-1">
                                                                                    <div className="p-2 bg-slate-100 rounded text-xs font-bold text-slate-600">
                                                                                        {pair.left}
                                                                                    </div>
                                                                                    <div className="p-2 bg-emerald-50 border border-emerald-200 rounded text-xs text-emerald-800">
                                                                                        → {pair.right}
                                                                                    </div>
                                                                                </div>
                                                                            ))}
                                                                        </div>
                                                                    </div>
                                                                )}

                                                                {/* Image-based questions */}
                                                                {q.type === 'IMAGE_BASED' && q.content?.imageDescription && (
                                                                    <div className="space-y-2">
                                                                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Image Description:</div>
                                                                        <div className="p-3 bg-slate-50 border border-slate-200 rounded text-sm">
                                                                            {q.content.imageDescription}
                                                                        </div>
                                                                    </div>
                                                                )}
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

                    {/* Footer - Added for Navigation */}
                    <div className="px-8 py-6 border-t border-slate-100 bg-slate-50 flex justify-between items-center">
                        <button
                            onClick={step === 1 ? onClose : () => setStep(1)}
                            className="px-6 py-3 rounded-xl font-bold text-slate-600 hover:bg-slate-200 transition-all"
                        >
                            {step === 1 ? 'Cancel' : 'Back to Details'}
                        </button>

                        <div className="flex gap-3">
                            {step === 1 ? (
                                <button
                                    onClick={handleNext}
                                    className="px-8 py-3 bg-slate-900 text-white rounded-xl font-bold hover:bg-slate-800 transition-all"
                                >
                                    Next: Manage Questions
                                </button>
                            ) : (
                                !isViewMode && (
                                    <button
                                        onClick={handleSubmit}
                                        disabled={loading}
                                        className="px-8 py-3 bg-primary-600 text-white rounded-xl font-bold hover:bg-primary-700 transition-all disabled:opacity-50"
                                    >
                                        {loading ? 'Saving...' : 'Update Question Pack'}
                                    </button>
                                )
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* AI Generator Modal */}
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
