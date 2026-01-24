"use client";

import React, { useEffect, useState } from 'react';
import { Package, BarChart3, Users, Activity, Search, ArrowUpRight, ArrowDownRight, Filter } from 'lucide-react';
import PackList from '@/components/PackList';
import Sidebar from '@/components/Sidebar';
import TopNav from '@/components/TopNav';
import { QuestionPack } from '@/types';

export default function DashboardPage() {
    const [stats, setStats] = useState({ totalQuestions: 0, activePacks: 0, totalRevenue: 0 });
    const [packs, setPacks] = useState<QuestionPack[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function fetchData() {
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
        }
        fetchData();
    }, []);

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
                                <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-700 hover:bg-slate-50 transition-colors shadow-sm">
                                    <Filter className="w-4 h-4" />
                                    <span>Last 30 Days</span>
                                </button>
                                <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-700 hover:bg-slate-50 transition-colors shadow-sm text-primary-600 border-primary-100 bg-primary-50/30">
                                    <Activity className="w-4 h-4" />
                                    <span>Live Monitoring</span>
                                </button>
                            </div>
                        </div>

                        {error && (
                            <div className="mb-8 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center gap-3 animate-pulse shadow-sm shadow-red-100">
                                <Activity className="w-5 h-5" />
                                <span className="font-bold text-sm">{error}</span>
                            </div>
                        )}

                        {/* Premium Stats Grid */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                            <StatCard
                                title="Live Questions"
                                value={stats?.totalQuestions?.toLocaleString() ?? "0"}
                                icon={<Search />}
                                trend="+12.5%"
                                trendUp={true}
                                color="blue"
                            />
                            <StatCard
                                title="Question Packs"
                                value={stats?.activePacks?.toLocaleString() ?? "0"}
                                icon={<Package />}
                                trend="+4.2%"
                                trendUp={true}
                                color="purple"
                            />
                            <StatCard
                                title="Total Revenue"
                                value={`R ${stats?.totalRevenue?.toLocaleString() ?? "0"}`}
                                icon={<BarChart3 />}
                                trend="-2.1%"
                                trendUp={false}
                                color="emerald"
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
                            {loading ? (
                                <div className="py-20 text-center">
                                    <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                                    <p className="text-slate-400 font-bold text-sm tracking-widest uppercase">Loading Inventory...</p>
                                </div>
                            ) : (
                                <PackList packs={packs} onEdit={() => { }} />
                            )}
                        </div>
                    </section>
                </main>
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
    color: 'blue' | 'purple' | 'emerald'
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
