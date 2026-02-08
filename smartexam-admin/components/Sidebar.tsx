"use client";

import React, { useState, useEffect } from 'react';
import { Package, BarChart3, Users, Settings, Activity, Database, ChevronRight, FileText, Download, UserCheck } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

interface SidebarItemProps {
    icon: React.ReactNode;
    label: string;
    active?: boolean;
    href?: string;
}

function SidebarItem({ icon, label, active = false, href = "#" }: SidebarItemProps) {
    return (
        <a
            href={href}
            className={cn(
                "flex items-center gap-3 px-4 py-3 rounded-xl cursor-pointer transition-all duration-200 group",
                active
                    ? "bg-primary-600 text-white shadow-lg shadow-primary-900/20"
                    : "text-slate-400 hover:bg-slate-800 hover:text-slate-100"
            )}
        >
            <div className={cn(
                "transition-transform duration-200",
                active ? "scale-110" : "group-hover:scale-110"
            )}>
                {icon}
            </div>
            <span className="font-medium text-sm tracking-wide flex-1">{label}</span>
            {active && <div className="w-1.5 h-1.5 bg-white rounded-full" />}
            {!active && <ChevronRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />}
        </a>
    );
}

export default function Sidebar() {
    const [userStats, setUserStats] = useState({
        totalUsers: 0,
        activeUsers: 0,
        totalInstalls: 0
    });

    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchUserStats = async () => {
            try {
                const res = await fetch('/api/users');
                const data = await res.json();
                if (!data.error) {
                    setUserStats(data);
                }
            } catch (error) {
                console.error('Failed to fetch user stats:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchUserStats();
    }, []);
    return (
        <aside className="w-72 bg-slate-900 text-white h-screen flex flex-col fixed left-0 top-0 z-20 border-r border-slate-800 shadow-2xl">
            <div className="p-8">
                <div className="flex items-center gap-3 mb-10">
                    <div className="bg-primary-500 p-2.5 rounded-2xl shadow-inner rotate-3 group-hover:rotate-0 transition-transform">
                        <Database className="w-7 h-7 text-white" />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold tracking-tight text-white leading-none">SmartExam</h2>
                        <p className="text-[10px] font-bold text-primary-400 uppercase tracking-[0.2em] mt-1">Admin Portal</p>
                    </div>
                </div>

                <nav className="space-y-2">
                    <SidebarItem icon={<BarChart3 className="w-5 h-5" />} label="Dashboard" active href="/" />
                    <SidebarItem icon={<Package className="w-5 h-5" />} label="Marketplace Packs" href="/market-packs" />
                    <SidebarItem icon={<Users className="w-5 h-5" />} label="Community Creators" href="/community-creators" />
                    <SidebarItem icon={<Activity className="w-5 h-5" />} label="Verification Log" href="/verification-log" />
                    <SidebarItem icon={<FileText className="w-5 h-5" />} label="Terms & Privacy" href="/terms" />
                </nav>

                {/* User Statistics Section */}
                <div className="mt-8 p-4 bg-slate-800/40 rounded-2xl border border-slate-700/50">
                    <div className="flex items-center gap-2 mb-4">
                        <Download className="w-5 h-5 text-primary-400" />
                        <h3 className="text-sm font-bold text-white">App Analytics</h3>
                    </div>

                    <div className="space-y-3">
                        {/* Total Installs */}
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <Download className="w-4 h-4 text-slate-400" />
                                <span className="text-xs text-slate-300">Total Installs</span>
                            </div>
                            <span className="text-sm font-bold text-white">
                                {loading ? '...' : userStats.totalInstalls.toLocaleString()}
                            </span>
                        </div>

                        {/* Active Users */}
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <UserCheck className="w-4 h-4 text-emerald-400" />
                                <span className="text-xs text-slate-300">Active Users (30d)</span>
                            </div>
                            <span className="text-sm font-bold text-emerald-400">
                                {loading ? '...' : userStats.activeUsers.toLocaleString()}
                            </span>
                        </div>

                        {/* Total Users */}
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <Users className="w-4 h-4 text-blue-400" />
                                <span className="text-xs text-slate-300">Registered Users</span>
                            </div>
                            <span className="text-sm font-bold text-blue-400">
                                {loading ? '...' : userStats.totalUsers.toLocaleString()}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <div className="mt-auto p-8 border-t border-slate-800/50 bg-slate-900/50 backdrop-blur-sm">
                <SidebarItem icon={<Settings className="w-5 h-5" />} label="Portal Settings" />

                <div className="mt-6 flex items-center gap-3 p-3 bg-slate-800/40 rounded-2xl border border-slate-700/50">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center font-bold text-white shadow-lg">
                        AD
                    </div>
                    <div className="flex-1 min-w-0">
                        <p className="text-xs font-bold text-white truncate">Admin User</p>
                        <p className="text-[10px] text-slate-500 truncate">admin@smartexam.co.za</p>
                    </div>
                </div>
            </div>
        </aside>
    );
}
