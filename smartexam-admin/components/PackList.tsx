import React from 'react';
import { QuestionPack } from '@/types';
import { Edit3, Eye, MoreVertical, BookOpen, Target, CreditCard } from 'lucide-react';

interface PackListProps {
    packs: QuestionPack[];
    onEdit: (pack: QuestionPack) => void;
    onView: (pack: QuestionPack) => void;
    onDelete: (packId: string) => void;
    onTogglePublish: (pack: QuestionPack) => void;
}

export default function PackList({ packs, onEdit, onView, onDelete, onTogglePublish }: PackListProps) {
    return (
        <div className="overflow-hidden">
            <table className="w-full text-left">
                <thead>
                    <tr className="border-b border-slate-100">
                        <th className="px-6 py-5 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">Pack Details</th>
                        <th className="px-6 py-5 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">CAPS Validation</th>
                        <th className="px-6 py-5 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">Pricing</th>
                        <th className="px-6 py-5 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">Status</th>
                        <th className="px-6 py-5 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] text-right">Actions</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                    {packs.map((pack) => (
                        <tr key={pack.id} className="group hover:bg-slate-50/50 transition-colors">
                            <td className="px-6 py-6">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400 group-hover:bg-primary-100 group-hover:text-primary-600 transition-colors">
                                        <BookOpen className="w-6 h-6" />
                                    </div>
                                    <div>
                                        <div className="font-bold text-slate-900 group-hover:text-primary-700 transition-colors">{pack.title}</div>
                                        <div className="flex items-center gap-2 mt-1">
                                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{pack.questionCount} Questions</span>
                                            <span className="w-1 h-1 bg-slate-300 rounded-full"></span>
                                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{pack.totalMarks} Marks</span>
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td className="px-6 py-6">
                                <div className="space-y-1.5">
                                    <div className="flex items-center gap-2">
                                        <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-black text-slate-600 uppercase tracking-tight">Grade {pack.grade}</span>
                                        <span className="px-2 py-0.5 bg-slate-100 rounded text-[10px] font-black text-slate-600 uppercase tracking-tight">Term {pack.term}</span>
                                    </div>
                                    <div className="text-xs font-bold text-slate-500 flex items-center gap-1">
                                        <Target className="w-3 h-3" />
                                        {pack.capsStrand}
                                    </div>
                                </div>
                            </td>
                            <td className="px-6 py-6">
                                <div className="flex items-center gap-2">
                                    <CreditCard className="w-4 h-4 text-slate-400" />
                                    <span className="text-sm font-black text-slate-900">R {(pack.priceCents / 100).toFixed(2)}</span>
                                </div>
                            </td>
                            <td className="px-6 py-6">
                                <button
                                    onClick={() => onTogglePublish(pack)}
                                    className={cn(
                                        "inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider hover:opacity-80 transition-opacity",
                                        pack.isPublished
                                            ? 'bg-emerald-50 text-emerald-600 border border-emerald-100'
                                            : 'bg-amber-50 text-amber-600 border border-amber-100'
                                    )}>
                                    <span className={cn("w-1.5 h-1.5 rounded-full", pack.isPublished ? "bg-emerald-500" : "bg-amber-500")}></span>
                                    {pack.isPublished ? 'Published' : 'Draft'}
                                </button>
                            </td>
                            <td className="px-6 py-6 text-right">
                                <div className="flex items-center justify-end gap-2">
                                    <button
                                        onClick={() => onEdit(pack)}
                                        className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all"
                                        title="Edit Pack"
                                    >
                                        <Edit3 className="w-4 h-4" />
                                    </button>
                                    <button
                                        onClick={() => onView(pack)}
                                        className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all"
                                        title="View Pack"
                                    >
                                        <Eye className="w-4 h-4" />
                                    </button>
                                    <button
                                        onClick={() => pack.id && onDelete(pack.id)}
                                        className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all"
                                        title="Delete Pack"
                                    >
                                        <MoreVertical className="w-4 h-4" />
                                    </button>
                                </div>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}
