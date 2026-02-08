"use client";

import React, { useEffect, useState } from 'react';
import { Package, BarChart3, Users, Activity, Search, ArrowUpRight, ArrowDownRight, Filter, TrendingUp, Clock, Target, Star, Zap } from 'lucide-react';
import PackList from '@/components/PackList';
import Sidebar from '@/components/Sidebar';
import TopNav from '@/components/TopNav';
import CreatePackModal from '@/components/CreatePackModal';
import ViewEditPackModal from '@/components/ViewEditPackModal';
import { QuestionPack } from '@/types';

export default function DashboardPage() {
    const [stats, setStats] = useState({
        // Basic metrics
        totalQuestions: 0,
        activePacks: 0,
        totalRevenue: 0,

        // User metrics
        totalUsers: 0,
        activeUsers: 0,
        userGrowth: 0,

        // Pack metrics
        publishedPacks: 0,
        draftPacks: 0,
        averagePackPrice: 0,

        // Activity metrics
        recentQuestions: 0,
        recentPacks: 0,

        // Engagement metrics
        engagement: {
            dailyActiveUsers: 0,
            averageSessionDuration: 0,
            packDownloadRate: 0,
            questionAttemptRate: 0
        },

        // Performance indicators
        conversionRate: 0,
        retentionRate: 0,
        satisfactionScore: 0
    });
    const [packs, setPacks] = useState<QuestionPack[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isViewEditModalOpen, setIsViewEditModalOpen] = useState(false);
    const [selectedPack, setSelectedPack] = useState<QuestionPack | null>(null);
    const [modalMode, setModalMode] = useState<'view' | 'edit'>('view');
    const [packFilter, setPackFilter] = useState<'all' | 'published' | 'draft'>('all');
    const [processingId, setProcessingId] = useState<string | null>(null);

    const fetchData = async () => {
        try {
            const [statsRes, packsRes] = await Promise.all([
                fetch('/api/stats'),
                fetch('/api/packs')
            ]);

            const statsData = await statsRes.json();
            const packsData = await packsRes.json();

            if (statsData.error) {
                setError(statsData.error);
            } else {
                setStats(statsData);
            }

            if (!packsData.error) {
                setPacks(packsData);
            }
        } catch (error) {
            console.error("Failed to fetch dashboard data:", error);
            setError("Connection failed. Is the server running?");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleDelete = async (packId: string) => {
        if (!confirm('Are you sure you want to delete this pack?')) return;
        setProcessingId(packId);
        try {
            const res = await fetch(`/api/packs/${packId}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Failed to delete');
            await fetchData(); // Refresh list and stats
        } catch (err) {
            alert('Failed to delete pack');
        } finally {
            setProcessingId(null);
        }
    };

    const handleTogglePublish = async (pack: QuestionPack) => {
        if (!pack.id) return;
        setProcessingId(pack.id);
        try {
            const res = await fetch(`/api/packs/${pack.id}`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ isPublished: !pack.isPublished })
            });
            if (!res.ok) throw new Error('Failed to update status');
            await fetchData();
        } catch (err) {
            alert('Failed to update pack status');
        } finally {
            setProcessingId(null);
        }
    };

    const handleView = (pack: QuestionPack) => {
        setSelectedPack(pack);
        setModalMode('view');
        setIsViewEditModalOpen(true);
    };

    const handleEdit = (pack: QuestionPack) => {
        setSelectedPack(pack);
        setModalMode('edit');
        setIsViewEditModalOpen(true);
    };

    const filteredPacks = packs.filter(pack => {
        switch (packFilter) {
            case 'published':
                return pack.isPublished;
            case 'draft':
                return !pack.isPublished;
            default:
                return true;
        }
    });

    return (
        <div className="flex bg-slate-50 min-h-screen font-sans selection:bg-primary-100 selection:text-primary-900">
            <Sidebar />

            <div className="flex-1 flex flex-col min-w-0">
                <TopNav />

                <main className="p-10 max-w-[1600px] w-full mx-auto space-y-10 ml-72">
                    <section>
                        <div className="flex justify-between items-end mb-8">
                            <div>
                                <h1 className="text-3xl font-black text-slate-900 tracking-tight">Portal Overview</h1>
                                <p className="text-slate-500 font-medium mt-1">Real-time marketplace monitoring & CAPS validation</p>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    onClick={() => setIsCreateModalOpen(true)}
                                    className="flex items-center gap-2 px-6 py-2 bg-slate-900 text-white rounded-xl text-sm font-bold hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200"
                                >
                                    <Package className="w-4 h-4" />
                                    <span>Create Pack</span>
                                </button>
                                <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-700 hover:bg-slate-50 transition-colors shadow-sm">
                                    <Filter className="w-4 h-4" />
                                    <span>Last 30 Days</span>
                                </button>
                            </div>
                        </div>

                        {error && (
                            <div className="mb-8 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center gap-3 animate-pulse shadow-sm shadow-red-100">
                                <Activity className="w-5 h-5" />
                                <span className="font-bold text-sm">{error}</span>
                            </div>
                        )}

                        {/* Comprehensive Analytics Grid */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                            {/* Revenue & Financial */}
                            <StatCard
                                title="Total Revenue"
                                value={`R ${stats?.totalRevenue?.toLocaleString() ?? "0"}`}
                                icon={<BarChart3 />}
                                trend="+8.2%"
                                trendUp={true}
                                color="emerald"
                            />
                            <StatCard
                                title="Avg Pack Price"
                                value={`R ${(stats?.averagePackPrice ?? 0).toFixed(2)}`}
                                icon={<Target />}
                                trend="+3.1%"
                                trendUp={true}
                                color="blue"
                            />
                            <StatCard
                                title="Conversion Rate"
                                value={`${(stats?.conversionRate ?? 0).toFixed(1)}%`}
                                icon={<TrendingUp />}
                                trend="+0.8%"
                                trendUp={true}
                                color="purple"
                            />

                            {/* User Metrics */}
                            <StatCard
                                title="Total Users"
                                value={stats?.totalUsers?.toLocaleString() ?? "0"}
                                icon={<Users />}
                                trend={`${(stats?.userGrowth ?? 0).toFixed(1)}%`}
                                trendUp={true}
                                color="indigo"
                            />
                            <StatCard
                                title="Active Users (30d)"
                                value={stats?.activeUsers?.toLocaleString() ?? "0"}
                                icon={<Activity />}
                                trend="+15.3%"
                                trendUp={true}
                                color="cyan"
                            />
                            <StatCard
                                title="Daily Active"
                                value={stats?.engagement?.dailyActiveUsers?.toLocaleString() ?? "0"}
                                icon={<Zap />}
                                trend="+12.7%"
                                trendUp={true}
                                color="orange"
                            />

                            {/* Content Metrics */}
                            <StatCard
                                title="Total Questions"
                                value={stats?.totalQuestions?.toLocaleString() ?? "0"}
                                icon={<Search />}
                                trend="+22.1%"
                                trendUp={true}
                                color="slate"
                            />
                            <StatCard
                                title="Published Packs"
                                value={stats?.publishedPacks?.toLocaleString() ?? "0"}
                                icon={<Package />}
                                trend="+9.4%"
                                trendUp={true}
                                color="teal"
                            />

                            {/* Engagement Metrics */}
                            <StatCard
                                title="Session Duration"
                                value={`${stats?.engagement?.averageSessionDuration ?? 0}m`}
                                icon={<Clock />}
                                trend="+5.2%"
                                trendUp={true}
                                color="pink"
                            />
                            <StatCard
                                title="User Retention"
                                value={`${(stats?.retentionRate ?? 0).toFixed(1)}%`}
                                icon={<Star />}
                                trend="+2.3%"
                                trendUp={true}
                                color="yellow"
                            />
                        </div>

                        {/* Activity Summary Cards */}
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                            <ActivityCard
                                title="Recent Activity (7 days)"
                                metrics={[
                                    { label: "New Questions", value: stats?.recentQuestions ?? 0, color: "blue" },
                                    { label: "New Packs", value: stats?.recentPacks ?? 0, color: "green" }
                                ]}
                            />
                            <ActivityCard
                                title="Pack Status"
                                metrics={[
                                    { label: "Published", value: stats?.publishedPacks ?? 0, color: "emerald" },
                                    { label: "Draft", value: stats?.draftPacks ?? 0, color: "amber" }
                                ]}
                            />
                            <ActivityCard
                                title="Engagement Rates"
                                metrics={[
                                    { label: "Pack Downloads", value: `${((stats?.engagement?.packDownloadRate ?? 0) * 100).toFixed(0)}%`, color: "purple" },
                                    { label: "Question Attempts", value: `${((stats?.engagement?.questionAttemptRate ?? 0) * 100).toFixed(0)}%`, color: "indigo" }
                                ]}
                            />
                        </div>
                    </section>

                    {/* Inventory Section */}
                    <section className="bg-white rounded-[2.5rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-200/50 overflow-hidden">
                        <div className="px-8 py-7 border-b border-slate-100 flex justify-between items-center bg-gradient-to-r from-slate-50/50 to-white">
                            <div>
                                <h2 className="text-xl font-black text-slate-900 tracking-tight">Marketplace Inventory</h2>
                                <p className="text-sm text-slate-500 font-medium mt-0.5">Manage and verify community-contributed content</p>
                            </div>
                            <div className="flex items-center gap-2 text-[10px] font-black text-primary-600 bg-primary-50 px-3 py-1.5 rounded-full uppercase tracking-widest border border-primary-100/50">
                                <span className="w-1.5 h-1.5 bg-primary-500 rounded-full animate-pulse"></span>
                                CAPS COMPLIANT
                            </div>
                        </div>

                        <div className="p-2">
                            {/* Filter Tabs */}
                            <div className="flex items-center gap-1 mb-6 bg-slate-100/50 p-1 rounded-2xl">
                                <button
                                    onClick={() => setPackFilter('all')}
                                    className={cn(
                                        "px-4 py-2 text-sm font-bold rounded-xl transition-all flex items-center gap-2",
                                        packFilter === 'all'
                                            ? "bg-white text-slate-900 shadow-sm"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                    )}
                                >
                                    All Packs
                                    <span className="px-2 py-0.5 bg-slate-200 text-slate-700 rounded-full text-xs font-black">
                                        {packs.length}
                                    </span>
                                </button>
                                <button
                                    onClick={() => setPackFilter('published')}
                                    className={cn(
                                        "px-4 py-2 text-sm font-bold rounded-xl transition-all flex items-center gap-2",
                                        packFilter === 'published'
                                            ? "bg-white text-slate-900 shadow-sm"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                    )}
                                >
                                    Published
                                    <span className="px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-full text-xs font-black">
                                        {packs.filter(p => p.isPublished).length}
                                    </span>
                                </button>
                                <button
                                    onClick={() => setPackFilter('draft')}
                                    className={cn(
                                        "px-4 py-2 text-sm font-bold rounded-xl transition-all flex items-center gap-2",
                                        packFilter === 'draft'
                                            ? "bg-white text-slate-900 shadow-sm"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                    )}
                                >
                                    Draft
                                    <span className="px-2 py-0.5 bg-amber-100 text-amber-700 rounded-full text-xs font-black">
                                        {packs.filter(p => !p.isPublished).length}
                                    </span>
                                </button>
                            </div>

                            {loading ? (
                                <div className="py-20 text-center">
                                    <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                                    <p className="text-slate-400 font-bold text-sm tracking-widest uppercase">Loading Inventory...</p>
                                </div>
                            ) : (
                                <PackList
                                    packs={filteredPacks}
                                    onEdit={handleEdit}
                                    onView={handleView}
                                    onDelete={handleDelete}
                                    onTogglePublish={handleTogglePublish}
                                />
                            )}
                        </div>
                    </section>
                </main>
            </div>

            <CreatePackModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onSuccess={() => {
                    fetchData();
                    // Optional: Show success toast
                }}
            />

            <ViewEditPackModal
                isOpen={isViewEditModalOpen}
                onClose={() => setIsViewEditModalOpen(false)}
                onSuccess={() => {
                    fetchData();
                }}
                pack={selectedPack}
                mode={modalMode}
            />
        </div>
    );
}

function ActivityCard({ title, metrics }: {
    title: string,
    metrics: { label: string, value: string | number, color: string }[]
}) {
    const colorMap = {
        blue: 'bg-blue-50 border-blue-200 text-blue-700',
        green: 'bg-green-50 border-green-200 text-green-700',
        emerald: 'bg-emerald-50 border-emerald-200 text-emerald-700',
        amber: 'bg-amber-50 border-amber-200 text-amber-700',
        purple: 'bg-purple-50 border-purple-200 text-purple-700',
        indigo: 'bg-indigo-50 border-indigo-200 text-indigo-700'
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900 mb-4">{title}</h3>
            <div className="space-y-3">
                {metrics.map((metric, index) => (
                    <div key={index} className="flex items-center justify-between">
                        <span className="text-sm text-slate-600">{metric.label}</span>
                        <span className={`px-3 py-1 rounded-full text-sm font-bold border ${colorMap[metric.color as keyof typeof colorMap]}`}>
                            {typeof metric.value === 'number' ? metric.value.toLocaleString() : metric.value}
                        </span>
                    </div>
                ))}
            </div>
        </div>
    );
}

function StatCard({ title, value, icon, trend, trendUp, color }: {
    title: string,
    value: string,
    icon: React.ReactNode,
    trend: string,
    trendUp: boolean,
    color: 'blue' | 'purple' | 'emerald' | 'indigo' | 'cyan' | 'orange' | 'slate' | 'teal' | 'pink' | 'yellow'
}) {
    const colors = {
        blue: {
            bg: 'bg-blue-50',
            text: 'text-blue-600',
            icon: 'bg-blue-500',
            border: 'border-blue-100'
        },
        purple: {
            bg: 'bg-purple-50',
            text: 'text-purple-600',
            icon: 'bg-purple-500',
            border: 'border-purple-100'
        },
        emerald: {
            bg: 'bg-emerald-50',
            text: 'text-emerald-600',
            icon: 'bg-emerald-500',
            border: 'border-emerald-100'
        },
        indigo: {
            bg: 'bg-indigo-50',
            text: 'text-indigo-600',
            icon: 'bg-indigo-500',
            border: 'border-indigo-100'
        },
        cyan: {
            bg: 'bg-cyan-50',
            text: 'text-cyan-600',
            icon: 'bg-cyan-500',
            border: 'border-cyan-100'
        },
        orange: {
            bg: 'bg-orange-50',
            text: 'text-orange-600',
            icon: 'bg-orange-500',
            border: 'border-orange-100'
        },
        slate: {
            bg: 'bg-slate-50',
            text: 'text-slate-600',
            icon: 'bg-slate-500',
            border: 'border-slate-100'
        },
        teal: {
            bg: 'bg-teal-50',
            text: 'text-teal-600',
            icon: 'bg-teal-500',
            border: 'border-teal-100'
        },
        pink: {
            bg: 'bg-pink-50',
            text: 'text-pink-600',
            icon: 'bg-pink-500',
            border: 'border-pink-100'
        },
        yellow: {
            bg: 'bg-yellow-50',
            text: 'text-yellow-600',
            icon: 'bg-yellow-500',
            border: 'border-yellow-100'
        }
    };

    const currentScale = colors[color];

    return (
        <div className="bg-white p-8 rounded-[2rem] shadow-[0_8px_30px_rgb(0,0,0,0.02)] border border-slate-100 hover:shadow-[0_20px_40px_rgb(0,0,0,0.06)] hover:-translate-y-1 transition-all duration-300 group">
            <div className="flex items-start justify-between mb-6">
                <div className={cn("p-4 rounded-2xl text-white shadow-lg transition-transform group-hover:scale-110 duration-300", currentScale.icon)}>
                    {React.cloneElement(icon as React.ReactElement, { className: 'w-6 h-6' })}
                </div>
                <div className={cn(
                    "flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold border",
                    trendUp ? "text-emerald-600 bg-emerald-50 border-emerald-100" : "text-red-600 bg-red-50 border-red-100"
                )}>
                    {trendUp ? <ArrowUpRight className="w-3.5 h-3.5" /> : <ArrowDownRight className="w-3.5 h-3.5" />}
                    {trend}
                </div>
            </div>
            <div>
                <p className="text-xs font-black text-slate-400 uppercase tracking-[0.15em] mb-1">{title}</p>
                <p className="text-4xl font-black text-slate-900 tracking-tight">{value}</p>
            </div>

            <div className="mt-6 pt-6 border-t border-slate-50 flex items-center justify-between">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Across all regions</span>
                <button className="text-[10px] font-black text-primary-500 uppercase tracking-widest hover:text-primary-700 transition-colors">View Details</button>
            </div>
        </div>
    );
}

function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
