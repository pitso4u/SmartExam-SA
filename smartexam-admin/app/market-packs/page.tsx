"use client";

import React, { useEffect, useState } from 'react';
import { Package, BarChart3, Users, Activity, Search, ArrowUpRight, ArrowDownRight, Filter, TrendingUp, Star, DollarSign } from 'lucide-react';
import PackList from '@/components/PackList';
import Sidebar from '@/components/Sidebar';
import TopNav from '@/components/TopNav';
import CreatePackModal from '@/components/CreatePackModal';
import ViewEditPackModal from '@/components/ViewEditPackModal';
import { QuestionPack } from '@/types';

export default function MarketPacksPage() {
    const [packs, setPacks] = useState<QuestionPack[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isViewEditModalOpen, setIsViewEditModalOpen] = useState(false);
    const [selectedPack, setSelectedPack] = useState<QuestionPack | null>(null);
    const [modalMode, setModalMode] = useState<'view' | 'edit'>('view');
    const [packFilter, setPackFilter] = useState<'all' | 'published' | 'draft'>('all');
    const [processingId, setProcessingId] = useState<string | null>(null);
    const [marketStats, setMarketStats] = useState({
        totalRevenue: 0,
        totalDownloads: 0,
        averageRating: 0,
        topPerformingPack: null as QuestionPack | null
    });

    const fetchData = async () => {
        try {
            const [packsRes, statsRes] = await Promise.all([
                fetch('/api/packs'),
                fetch('/api/stats')
            ]);

            const packsData = await packsRes.json();
            const statsData = await statsRes.json();

            if (!packsData.error) {
                setPacks(packsData);

                // Calculate market-specific stats from real data
                const publishedPacks = packsData.filter((p: QuestionPack) => p.isPublished);
                const totalRevenue = publishedPacks.reduce((sum: number, pack: QuestionPack) => {
                    // Use actual revenue data if available, otherwise estimate
                    const actualRevenue = pack.totalRevenue || 0;
                    return sum + actualRevenue;
                }, 0);
                const averageRating = publishedPacks.length > 0 
                    ? publishedPacks.reduce((sum: number, pack: QuestionPack) => sum + (pack.averageRating || 0), 0) / publishedPacks.length
                    : 0;
                const topPack = publishedPacks.reduce((top: QuestionPack | null, pack: QuestionPack) =>
                    !top || (pack.questionCount * pack.totalMarks) > (top.questionCount * top.totalMarks) ? pack : top, null
                );

                setMarketStats({
                    totalRevenue,
                    totalDownloads: statsData.totalDownloads || 0,
                    averageRating,
                    topPerformingPack: topPack
                });
            }

            if (statsData.error) {
                setError(statsData.error);
            }
        } catch (error) {
            console.error("Failed to fetch market packs data:", error);
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
            await fetchData();
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
                                <h1 className="text-3xl font-black text-slate-900 tracking-tight">Marketplace Packs</h1>
                                <p className="text-slate-500 font-medium mt-1">Comprehensive marketplace management and analytics</p>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    onClick={() => setIsCreateModalOpen(true)}
                                    className="flex items-center gap-2 px-6 py-2 bg-slate-900 text-white rounded-xl text-sm font-bold hover:bg-slate-800 transition-colors shadow-lg shadow-slate-200"
                                >
                                    <Package className="w-4 h-4" />
                                    <span>Create Pack</span>
                                </button>
                            </div>
                        </div>

                        {error && (
                            <div className="mb-8 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center gap-3 animate-pulse shadow-sm shadow-red-100">
                                <Activity className="w-5 h-5" />
                                <span className="font-bold text-sm">{error}</span>
                            </div>
                        )}

                        {/* Market Analytics */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                            <MarketStatCard
                                title="Market Revenue"
                                value={`R ${marketStats.totalRevenue.toLocaleString()}`}
                                icon={<DollarSign />}
                                trend="+12.3%"
                                trendUp={true}
                                color="emerald"
                            />
                            <MarketStatCard
                                title="Total Downloads"
                                value={marketStats.totalDownloads.toLocaleString()}
                                icon={<Package />}
                                trend="+8.7%"
                                trendUp={true}
                                color="blue"
                            />
                            <MarketStatCard
                                title="Avg Rating"
                                value={marketStats.averageRating.toFixed(1)}
                                icon={<Star />}
                                trend="+0.3"
                                trendUp={true}
                                color="yellow"
                            />
                            <MarketStatCard
                                title="Published Packs"
                                value={packs.filter(p => p.isPublished).length.toString()}
                                icon={<TrendingUp />}
                                trend="+5.2%"
                                trendUp={true}
                                color="purple"
                            />
                        </div>

                        {/* Top Performing Pack Highlight */}
                        {marketStats.topPerformingPack && (
                            <div className="bg-gradient-to-r from-primary-50 to-indigo-50 border border-primary-100 rounded-2xl p-6 mb-8">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 bg-primary-100 rounded-2xl flex items-center justify-center">
                                        <Star className="w-6 h-6 text-primary-600" />
                                    </div>
                                    <div>
                                        <h3 className="text-lg font-bold text-slate-900">Top Performing Pack</h3>
                                        <p className="text-primary-700 font-medium">{marketStats.topPerformingPack.title}</p>
                                        <p className="text-sm text-slate-600">{marketStats.topPerformingPack.questionCount} questions • Grade {marketStats.topPerformingPack.grade} • {marketStats.topPerformingPack.subject}</p>
                                    </div>
                                </div>
                            </div>
                        )}
                    </section>

                    {/* Marketplace Inventory */}
                    <section className="bg-white rounded-[2.5rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-200/50 overflow-hidden">
                        <div className="px-8 py-7 border-b border-slate-100 flex justify-between items-center bg-gradient-to-r from-slate-50/50 to-white">
                            <div>
                                <h2 className="text-xl font-black text-slate-900 tracking-tight">Pack Inventory</h2>
                                <p className="text-sm text-slate-500 font-medium mt-0.5">All marketplace packs and their performance</p>
                            </div>
                            <div className="flex items-center gap-2 text-[10px] font-black text-primary-600 bg-primary-50 px-3 py-1.5 rounded-full uppercase tracking-widest border border-primary-100/50">
                                <span className="w-1.5 h-1.5 bg-primary-500 rounded-full animate-pulse"></span>
                                LIVE MARKETPLACE
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
                                    <p className="text-slate-400 font-bold text-sm tracking-widest uppercase">Loading Marketplace...</p>
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

function MarketStatCard({ title, value, icon, trend, trendUp, color }: {
    title: string,
    value: string,
    icon: React.ReactNode,
    trend: string,
    trendUp: boolean,
    color: string
}) {
    const colors = {
        emerald: {
            bg: 'bg-emerald-50',
            text: 'text-emerald-600',
            icon: 'bg-emerald-500',
            border: 'border-emerald-100'
        },
        blue: {
            bg: 'bg-blue-50',
            text: 'text-blue-600',
            icon: 'bg-blue-500',
            border: 'border-blue-100'
        },
        yellow: {
            bg: 'bg-yellow-50',
            text: 'text-yellow-600',
            icon: 'bg-yellow-500',
            border: 'border-yellow-100'
        },
        purple: {
            bg: 'bg-purple-50',
            text: 'text-purple-600',
            icon: 'bg-purple-500',
            border: 'border-purple-100'
        }
    };

    const currentColor = colors[color as keyof typeof colors];

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 hover:shadow-lg transition-all">
            <div className="flex items-start justify-between mb-4">
                <div className={cn("p-3 rounded-xl text-white shadow-lg", currentColor.icon)}>
                    {React.cloneElement(icon as React.ReactElement, { className: 'w-5 h-5' })}
                </div>
                <div className={cn(
                    "flex items-center gap-1 px-2 py-1 rounded-full text-xs font-bold border",
                    trendUp ? "text-emerald-600 bg-emerald-50 border-emerald-100" : "text-red-600 bg-red-50 border-red-100"
                )}>
                    {trendUp ? <ArrowUpRight className="w-3 h-3" /> : <ArrowDownRight className="w-3 h-3" />}
                    {trend}
                </div>
            </div>
            <div>
                <p className="text-sm font-bold text-slate-400 uppercase tracking-[0.15em] mb-1">{title}</p>
                <p className="text-3xl font-black text-slate-900 tracking-tight">{value}</p>
            </div>
        </div>
    );
}

function cn(...inputs: any[]) {
    return inputs.filter(Boolean).join(' ');
}
