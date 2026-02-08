"use client";

import React, { useEffect, useState } from 'react';
import { Users, UserCheck, Award, Star, TrendingUp, Shield, Clock, BookOpen } from 'lucide-react';
import Sidebar from '@/components/Sidebar';
import TopNav from '@/components/TopNav';

interface Creator {
    id: string;
    name: string;
    email: string;
    avatar?: string;
    joinDate: number;
    totalPacks: number;
    totalQuestions: number;
    averageRating: number;
    totalDownloads: number;
    status: 'active' | 'inactive' | 'suspended';
    lastActive: number;
    verified: boolean;
}

export default function CommunityCreatorsPage() {
    const [creators, setCreators] = useState<Creator[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<'all' | 'active' | 'verified'>('all');

    useEffect(() => {
        fetchCreators();
    }, []);

    const fetchCreators = async () => {
        try {
            const res = await fetch('/api/users?role=creator&sortBy=totalPacks&sortOrder=desc&limit=50');
            const data = await res.json();
            
            if (data.error) {
                throw new Error(data.error);
            }
            
            setCreators(data);
        } catch (err: any) {
            setError(err.message || 'Failed to fetch creators');
        } finally {
            setLoading(false);
        }
    };

    const filteredCreators = creators.filter(creator => {
        switch (filter) {
            case 'active':
                return creator.status === 'active';
            case 'verified':
                return creator.verified;
            default:
                return true;
        }
    });

    const stats = {
        totalCreators: creators.length,
        activeCreators: creators.filter(c => c.status === 'active').length,
        verifiedCreators: creators.filter(c => c.verified).length,
        totalPacks: creators.reduce((sum, c) => sum + c.totalPacks, 0),
        totalDownloads: creators.reduce((sum, c) => sum + c.totalDownloads, 0)
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
                                <h1 className="text-3xl font-black text-slate-900 tracking-tight">Community Creators</h1>
                                <p className="text-slate-500 font-medium mt-1">Manage and support our content creator community</p>
                            </div>
                        </div>

                        {error && (
                            <div className="mb-8 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl flex items-center gap-3 animate-pulse shadow-sm shadow-red-100">
                                <Users className="w-5 h-5" />
                                <span className="font-bold text-sm">{error}</span>
                            </div>
                        )}

                        {/* Creator Stats */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
                            <CreatorStatCard
                                title="Total Creators"
                                value={stats.totalCreators.toString()}
                                icon={<Users />}
                                color="blue"
                            />
                            <CreatorStatCard
                                title="Active Creators"
                                value={stats.activeCreators.toString()}
                                icon={<UserCheck />}
                                color="green"
                            />
                            <CreatorStatCard
                                title="Verified Creators"
                                value={stats.verifiedCreators.toString()}
                                icon={<Shield />}
                                color="purple"
                            />
                            <CreatorStatCard
                                title="Total Packs"
                                value={stats.totalPacks.toString()}
                                icon={<BookOpen />}
                                color="orange"
                            />
                            <CreatorStatCard
                                title="Total Downloads"
                                value={stats.totalDownloads.toLocaleString()}
                                icon={<TrendingUp />}
                                color="emerald"
                            />
                        </div>
                    </section>

                    {/* Creators List */}
                    <section className="bg-white rounded-[2.5rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-200/50 overflow-hidden">
                        <div className="px-8 py-7 border-b border-slate-100 flex justify-between items-center bg-gradient-to-r from-slate-50/50 to-white">
                            <div>
                                <h2 className="text-xl font-black text-slate-900 tracking-tight">Creator Directory</h2>
                                <p className="text-sm text-slate-500 font-medium mt-0.5">All registered content creators and their contributions</p>
                            </div>

                            {/* Filter Tabs */}
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
                                    All ({creators.length})
                                </button>
                                <button
                                    onClick={() => setFilter('active')}
                                    className={cn(
                                        "px-4 py-2 text-sm font-bold rounded-xl transition-all",
                                        filter === 'active'
                                            ? "bg-white text-slate-900 shadow-sm"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                    )}
                                >
                                    Active ({stats.activeCreators})
                                </button>
                                <button
                                    onClick={() => setFilter('verified')}
                                    className={cn(
                                        "px-4 py-2 text-sm font-bold rounded-xl transition-all",
                                        filter === 'verified'
                                            ? "bg-white text-slate-900 shadow-sm"
                                            : "text-slate-600 hover:text-slate-900 hover:bg-white/50"
                                    )}
                                >
                                    Verified ({stats.verifiedCreators})
                                </button>
                            </div>
                        </div>

                        <div className="p-2">
                            {loading ? (
                                <div className="py-20 text-center">
                                    <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                                    <p className="text-slate-400 font-bold text-sm tracking-widest uppercase">Loading Creators...</p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {filteredCreators.map((creator) => (
                                        <CreatorCard key={creator.id} creator={creator} />
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

function CreatorCard({ creator }: { creator: Creator }) {
    const getStatusColor = (status: string) => {
        switch (status) {
            case 'active': return 'bg-emerald-50 border-emerald-200 text-emerald-700';
            case 'inactive': return 'bg-slate-50 border-slate-200 text-slate-600';
            case 'suspended': return 'bg-red-50 border-red-200 text-red-700';
            default: return 'bg-slate-50 border-slate-200 text-slate-600';
        }
    };

    const formatDate = (timestamp: number) => {
        return new Date(timestamp).toLocaleDateString('en-ZA', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    const getTimeAgo = (timestamp: number) => {
        const now = Date.now();
        const diff = now - timestamp;
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (days === 0) return 'Today';
        if (days === 1) return 'Yesterday';
        if (days < 7) return `${days} days ago`;
        if (days < 30) return `${Math.floor(days / 7)} weeks ago`;
        return `${Math.floor(days / 30)} months ago`;
    };

    return (
        <div className="bg-white border border-slate-100 rounded-2xl p-6 hover:shadow-md transition-all">
            <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
                        {creator.name.split(' ').map(n => n[0]).join('')}
                    </div>
                    <div>
                        <div className="flex items-center gap-2 mb-1">
                            <h3 className="text-lg font-bold text-slate-900">{creator.name}</h3>
                            {creator.verified && (
                                <Shield className="w-4 h-4 text-blue-500" />
                            )}
                        </div>
                        <p className="text-sm text-slate-500">{creator.email}</p>
                        <p className="text-xs text-slate-400">Joined {formatDate(creator.joinDate)}</p>
                    </div>
                </div>

                <div className="text-right">
                    <span className={`px-3 py-1 rounded-full text-xs font-bold border ${getStatusColor(creator.status)}`}>
                        {creator.status}
                    </span>
                    <p className="text-xs text-slate-400 mt-1">Active {getTimeAgo(creator.lastActive)}</p>
                </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-4">
                <div className="text-center">
                    <div className="text-2xl font-black text-slate-900">{creator.totalPacks}</div>
                    <div className="text-xs text-slate-500 uppercase tracking-wider">Packs</div>
                </div>
                <div className="text-center">
                    <div className="text-2xl font-black text-slate-900">{creator.totalQuestions}</div>
                    <div className="text-xs text-slate-500 uppercase tracking-wider">Questions</div>
                </div>
                <div className="text-center">
                    <div className="text-2xl font-black text-slate-900">{creator.totalDownloads.toLocaleString()}</div>
                    <div className="text-xs text-slate-500 uppercase tracking-wider">Downloads</div>
                </div>
                <div className="text-center">
                    <div className="text-2xl font-black text-slate-900">{creator.averageRating.toFixed(1)}</div>
                    <div className="text-xs text-slate-500 uppercase tracking-wider">Avg Rating</div>
                </div>
                <div className="text-center">
                    <div className="flex items-center justify-center gap-1">
                        <Star className="w-4 h-4 text-yellow-500 fill-current" />
                        <span className="text-lg font-black text-slate-900">{creator.averageRating.toFixed(1)}</span>
                    </div>
                    <div className="text-xs text-slate-500 uppercase tracking-wider">Rating</div>
                </div>
            </div>

            <div className="flex justify-end gap-2">
                <button className="px-4 py-2 text-sm font-bold text-slate-600 hover:text-slate-900 hover:bg-slate-50 rounded-xl transition-colors">
                    View Profile
                </button>
                <button className="px-4 py-2 text-sm font-bold text-primary-600 hover:text-primary-700 hover:bg-primary-50 rounded-xl transition-colors">
                    Contact Creator
                </button>
            </div>
        </div>
    );
}

function CreatorStatCard({ title, value, icon, color }: {
    title: string,
    value: string,
    icon: React.ReactNode,
    color: string
}) {
    const colors = {
        blue: 'bg-blue-50 text-blue-600 border-blue-100',
        green: 'bg-green-50 text-green-600 border-green-100',
        purple: 'bg-purple-50 text-purple-600 border-purple-100',
        orange: 'bg-orange-50 text-orange-600 border-orange-100',
        emerald: 'bg-emerald-50 text-emerald-600 border-emerald-100'
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100">
            <div className="flex items-center gap-3 mb-3">
                <div className={`p-3 rounded-xl text-white ${color === 'blue' ? 'bg-blue-500' : color === 'green' ? 'bg-green-500' : color === 'purple' ? 'bg-purple-500' : color === 'orange' ? 'bg-orange-500' : 'bg-emerald-500'}`}>
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
