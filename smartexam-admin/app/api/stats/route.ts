import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/firebase-admin';

export async function GET() {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured. Please set environment variables.' }, { status: 503 });
    }
    try {
        const questionsSnapshot = await db.collection('questions').count().get();
        const packsSnapshot = await db.collection('question_packs').count().get();

        // Simulating revenue calculation from stripe/purchases if available
        // For now, using a placeholder logic or fetching from a 'stats' collection
        const statsDoc = await db.collection('platform_stats').doc('summary').get();
        const revenue = statsDoc.exists ? statsDoc.data()?.totalRevenueCents || 0 : 0;

        return NextResponse.json({
            totalQuestions: questionsSnapshot.data().count,
            activePacks: packsSnapshot.data().count,
            totalRevenue: revenue / 100 // Convert cents to R or major currency
        });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
