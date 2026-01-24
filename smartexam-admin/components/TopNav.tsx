"use client";

import React from 'react';
import { Search, Bell, Plus, HelpCircle } from 'lucide-react';

export default function TopNav() {
    return (
        <header className="h-20 bg-white/80 backdrop-blur-md border-b border-slate-200/60 px-8 sticky top-0 z-10 flex justify-between items-center ml-72">
            <div className="flex-1 max-w-xl">
                <div className="relative group">
                    <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-primary-500 transition-colors" />
                    <input
                        type="text"
                        placeholder="Search for question packs, creators, or logs..."
                        className="w-full pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all placeholder:text-slate-400"
                    />
                </div>
            </div>

            <div className="flex items-center gap-4">
                <button className="p-2.5 text-slate-500 hover:bg-slate-50 rounded-xl transition-colors relative">
                    <Bell className="w-5 h-5" />
                    <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 border-2 border-white rounded-full"></span>
                </button>
                <button className="p-2.5 text-slate-500 hover:bg-slate-50 rounded-xl transition-colors">
                    <HelpCircle className="w-5 h-5" />
                </button>

                <div className="h-8 w-[1px] bg-slate-200 mx-2"></div>

                <button className="flex items-center gap-2 bg-slate-900 text-white px-5 py-2.5 rounded-xl hover:bg-slate-800 transition-all font-semibold shadow-xl shadow-slate-900/10 active:scale-95">
                    <Plus className="w-4 h-4" />
                    <span className="text-sm">Publish Pack</span>
                </button>
            </div>
        </header>
    );
}
