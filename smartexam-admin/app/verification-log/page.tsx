"use client";

import React, { useEffect, useState } from 'react';
import { Activity, CheckCircle, XCircle, Clock, AlertTriangle, Shield, Search, Filter } from 'lucide-react';
import Sidebar from '@/components/Sidebar';
import TopNav from '@/components/TopNav';

interface VerificationLog {
    id: string;
    type: 'pack_verification' | 'question_review' | 'creator_verification' | 'content_flag';
    status: 'pending' | 'approved' | 'rejected' | 'flagged';
    title: string;
    description: string;
    submittedBy: string;
    submittedAt: number;
    reviewedBy?: string;
    reviewedAt?: number;
    notes?: string;
    priority: 'low' | 'medium' | 'high' | 'urgent';
}

export default function VerificationLogPage() {
    const [logs, setLogs] = useState<VerificationLog[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<'all' | 'pending' | 'approved' | 'rejected'>('all');
    const [typeFilter, setTypeFilter] = useState<'all' | 'pack_verification' | 'question_review' | 'creator_verification' | 'content_flag'>('all');

    useEffect(() => {
        fetchVerificationLogs();
    }, []);

    const fetchVerificationLogs = async () => {
        try {
            const res = await fetch('/api/verification-logs?sortBy=submittedAt&sortOrder=desc&limit=50');
            const data = await res.json();
            
            if (data.error) {
                throw new Error(data.error);
            }
            
            setLogs(data.logs || []);
        } catch (err: any) {
            setError(err.message || 'Failed to fetch verification logs');
        } finally {
            setLoading(false);
        }
    };

    const filteredLogs = logs.filter(log => {
        const statusMatch = filter === 'all' || log.status === filter;
        const typeMatch = typeFilter === 'all' || log.type === typeFilter;
        return statusMatch && typeMatch;
    });

    const stats = {
        total: logs.length,
        pending: logs.filter(l => l.status === 'pending').length,
        approved: logs.filter(l => l.status === 'approved').length,
        rejected: logs.filter(l => l.status === 'rejected').length,
        urgent: logs.filter(l => l.priority === 'urgent').length
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'pending': return <Clock className="w-5 h-5 text-yellow-500" />;
            case 'approved': return <CheckCircle className="w-5 h-5 text-green-500" />;
            case 'rejected': return <XCircle className="w-5 h-5 text-red-500" />;
            case 'flagged': return <AlertTriangle className="w-5 h-5 text-orange-500" />;
            default: return <Clock className="w-5 h-5 text-slate-500" />;
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'pending': return 'bg-yellow-50 border-yellow-200 text-yellow-800';
            case 'approved': return 'bg-green-50 border-green-200 text-green-800';
            case 'rejected': return 'bg-red-50 border-red-200 text-red-800';
            case 'flagged': return 'bg-orange-50 border-orange-200 text-orange-800';
            default: return 'bg-slate-50 border-slate-200 text-slate-800';
        }
    };

    const getPriorityColor = (priority: string) => {
        switch (priority) {
            case 'urgent': return 'bg-red-100 text-red-800 border-red-200';
            case 'high': return 'bg-orange-100 text-orange-800 border-orange-200';
            case 'medium': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
            case 'low': return 'bg-green-100 text-green-800 border-green-200';
            default: return 'bg-slate-100 text-slate-800 border-slate-200';
        }
    };

    const getTypeLabel = (type: string) => {
        switch (type) {
            case 'pack_verification': return 'Pack Verification';
            case 'question_review': return 'Question Review';
            case 'creator_verification': return 'Creator Verification';
            case 'content_flag': return 'Content Flag';
            default: return type;
        }
    };

    const formatDate = (timestamp: number) => {
        return new Date(timestamp).toLocaleString('en-ZA', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <div className="flex bg-slate-50 min-h-screen font-sans selection:bg-primary-100 selection:text-primary-900">
            <Sidebar />

            <div className="flex-1 flex flex-col min-w-0">
                <TopNav />

                <main className="p-10 max-w-[1600px] w-full mx-auto space-y-10 ml-72">
                    <section>
                        <div className="flex justify-between items-end mb-8">
                            <div>
                                <h1 className="text-3xl font-black text-slate-900 tracking-tight">Verification Log</h1>
                                <p className="text-slate-500 font-medium mt-1">Track all content verification and approval activities</p>
                            </div>
                        </div>

                        {error && (
                            <div className="mb-8 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center gap-3 animate-pulse shadow-sm shadow-red-100">
                                <Activity className="w-5 h-5" />
                                <span className="font-bold text-sm">{error}</span>
                            </div>
                        )}

                        {/* Verification Stats */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
                            <VerificationStatCard
                                title="Total Reviews"
                                value={stats.total.toString()}
                                icon={<Activity />}
                                color="blue"
                            />
                            <VerificationStatCard
                                title="Pending"
                                value={stats.pending.toString()}
                                icon={<Clock />}
                                color="yellow"
                            />
                            <VerificationStatCard
                                title="Approved"
                                value={stats.approved.toString()}
                                icon={<CheckCircle />}
                                color="green"
                            />
                            <VerificationStatCard
                                title="Rejected"
                                value={stats.rejected.toString()}
                                icon={<XCircle />}
                                color="red"
                            />
                            <VerificationStatCard
                                title="Urgent"
                                value={stats.urgent.toString()}
                                icon={<AlertTriangle />}
                                color="orange"
                            />
                        </div>
                    </section>

                    {/* Verification Log */}
                    <section className="bg-white rounded-[2.5rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-200/50 overflow-hidden">
                        <div className="px-8 py-7 border-b border-slate-100 flex justify-between items-center bg-gradient-to-r from-slate-50/50 to-white">
                            <div>
                                <h2 className="text-xl font-black text-slate-900 tracking-tight">Review Activity</h2>
                                <p className="text-sm text-slate-500 font-medium mt-0.5">All verification requests and their current status</p>
                            </div>

                            {/* Filters */}
                            <div className="flex items-center gap-4">
                                <select
                                    value={typeFilter}
                                    onChange={(e) => setTypeFilter(e.target.value as any)}
                                    className="px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-800 focus:outline-none focus:border-primary-500"
                                >
                                    <option value="all">All Types</option>
                                    <option value="pack_verification">Pack Verification</option>
                                    <option value="question_review">Question Review</option>
                                    <option value="creator_verification">Creator Verification</option>
                                    <option value="content_flag">Content Flag</option>
                                </select>

                                <div className="flex items-center gap-1 bg-slate-100/50 p-1 rounded-2xl">
                                    <button
                                        onClick={() => setFilter('all')}
                                        className={cn(
                                            "px-4 py-2 text-sm font-bold rounded-xl transition-all",
                                            filter === 'all'
                                                ? "bg-white text-slate-900 shadow-sm"
                                                : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                        )}
                                    >
                                        All ({stats.total})
                                    </button>
                                    <button
                                        onClick={() => setFilter('pending')}
                                        className={cn(
                                            "px-4 py-2 text-sm font-bold rounded-xl transition-all",
                                            filter === 'pending'
                                                ? "bg-white text-slate-900 shadow-sm"
                                                : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                        )}
                                    >
                                        Pending ({stats.pending})
                                    </button>
                                    <button
                                        onClick={() => setFilter('approved')}
                                        className={cn(
                                            "px-4 py-2 text-sm font-bold rounded-xl transition-all",
                                            filter === 'approved'
                                                ? "bg-white text-slate-900 shadow-sm"
                                                : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                        )}
                                    >
                                        Approved ({stats.approved})
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div className="p-2">
                            {loading ? (
                                <div className="py-20 text-center">
                                    <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                                    <p className="text-slate-400 font-bold text-sm tracking-widest uppercase">Loading Verification Logs...</p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {filteredLogs.map((log) => (
                                        <div key={log.id} className="bg-white border border-slate-100 rounded-2xl p-6 hover:shadow-md transition-all">
                                            <div className="flex items-start justify-between mb-4">
                                                <div className="flex items-start gap-4">
                                                    {getStatusIcon(log.status)}
                                                    <div className="flex-1">
                                                        <div className="flex items-center gap-2 mb-2">
                                                            <h3 className="text-lg font-bold text-slate-900">{log.title}</h3>
                                                            <span className={`px-2 py-1 rounded-full text-xs font-bold border ${getStatusColor(log.status)}`}>
                                                                {log.status}
                                                            </span>
                                                            <span className={`px-2 py-1 rounded-full text-xs font-bold border ${getPriorityColor(log.priority)}`}>
                                                                {log.priority}
                                                            </span>
                                                        </div>
                                                        <p className="text-sm text-slate-600 mb-2">{log.description}</p>
                                                        <p className="text-xs text-slate-500">
                                                            <span className="font-medium">Type:</span> {getTypeLabel(log.type)} •
                                                            <span className="font-medium"> Submitted by:</span> {log.submittedBy} •
                                                            <span className="font-medium"> Submitted:</span> {formatDate(log.submittedAt)}
                                                        </p>
                                                        {log.reviewedBy && log.reviewedAt && (
                                                            <p className="text-xs text-slate-500 mt-1">
                                                                <span className="font-medium">Reviewed by:</span> {log.reviewedBy} •
                                                                <span className="font-medium"> Reviewed:</span> {formatDate(log.reviewedAt)}
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>

                                                <div className="text-right">
                                                    {log.status === 'pending' && (
                                                        <div className="space-y-2">
                                                            <button className="px-4 py-2 bg-green-500 text-white text-sm font-bold rounded-xl hover:bg-green-600 transition-colors">
                                                                Approve
                                                            </button>
                                                            <button className="px-4 py-2 bg-red-500 text-white text-sm font-bold rounded-xl hover:bg-red-600 transition-colors block">
                                                                Reject
                                                            </button>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>

                                            {log.notes && (
                                                <div className="mt-4 p-3 bg-slate-50 border border-slate-200 rounded-xl">
                                                    <p className="text-sm text-slate-700">
                                                        <span className="font-bold text-slate-900">Review Notes:</span> {log.notes}
                                                    </p>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </section>
                </main>
            </div>
        </div>
    );
}

function VerificationStatCard({ title, value, icon, color }: {
    title: string,
    value: string,
    icon: React.ReactNode,
    color: string
}) {
    const colors = {
        blue: 'bg-blue-50 text-blue-600 border-blue-100',
        yellow: 'bg-yellow-50 text-yellow-600 border-yellow-100',
        green: 'bg-green-50 text-green-600 border-green-100',
        red: 'bg-red-50 text-red-600 border-red-100',
        orange: 'bg-orange-50 text-orange-600 border-orange-100'
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
            <div className="flex items-center gap-3 mb-3">
                <div className={`p-3 rounded-xl text-white ${color === 'blue' ? 'bg-blue-500' : color === 'yellow' ? 'bg-yellow-500' : color === 'green' ? 'bg-green-500' : color === 'red' ? 'bg-red-500' : 'bg-orange-500'}`}>
                    {React.cloneElement(icon as React.ReactElement, { className: 'w-5 h-5' })}
                </div>
                <div>
                    <p className="text-sm font-bold text-slate-400 uppercase tracking-[0.15em]">{title}</p>
                    <p className="text-2xl font-black text-slate-900">{value}</p>
                </div>
            </div>
        </div>
    );
}

function cn(...inputs: any[]) {
    return inputs.filter(Boolean).join(' ');
}
