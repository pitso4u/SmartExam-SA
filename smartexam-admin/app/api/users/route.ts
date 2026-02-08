import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/firebase-admin';

export async function GET(req: Request) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured' }, { status: 503 });
    }

    try {
        const { searchParams } = new URL(req.url);
        const role = searchParams.get('role');
        const limit = parseInt(searchParams.get('limit') || '50');
        const sortBy = searchParams.get('sortBy') || 'createdAt';
        const sortOrder = searchParams.get('sortOrder') || 'desc';
        const statsOnly = searchParams.get('stats') === 'true';

        // If only stats are requested, return aggregated data
        if (statsOnly) {
            const usersSnapshot = await db.collection('users').count().get();
            const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);
            const activeUsersQuery = await db.collection('users')
                .where('lastLoginAt', '>', thirtyDaysAgo)
                .count()
                .get();

            return NextResponse.json({
                totalUsers: usersSnapshot.data().count,
                activeUsers: activeUsersQuery.data().count,
                totalInstalls: usersSnapshot.data().count,
                lastUpdated: Date.now()
            });
        }

        // Return detailed user data with filtering
        let query: any = db.collection('users');

        // Filter by role if specified
        if (role) {
            query = query.where('role', '==', role);
        }

        // Add sorting
        query = query.orderBy(sortBy, sortOrder as 'asc' | 'desc');

        // Apply limit
        query = query.limit(limit);

        const snapshot = await query.get();
        const users = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        }));

        return NextResponse.json(users);
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
