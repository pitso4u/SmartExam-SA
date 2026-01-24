"use client";

import React from 'react';
import { Package, BarChart3, Users, Settings, Activity, Database, ChevronRight } from 'lucide-react';
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

function SidebarItem({ icon, label, active = false }: SidebarItemProps) {
    return (
        <div
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
        </div>
    );
}

export default function Sidebar() {
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
                    <SidebarItem icon={<BarChart3 className="w-5 h-5" />} label="Dashboard" active />
                    <SidebarItem icon={<Package className="w-5 h-5" />} label="Marketplace Packs" />
                    <SidebarItem icon={<Users className="w-5 h-5" />} label="Community Creators" />
                    <SidebarItem icon={<Activity className="w-5 h-5" />} label="Verification Log" />
                </nav>
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
