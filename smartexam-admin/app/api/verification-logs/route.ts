import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/firebase-admin';

export async function GET(req: Request) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured' }, { status: 503 });
    }

    try {
        const { searchParams } = new URL(req.url);
        const limit = parseInt(searchParams.get('limit') || '50');
        const status = searchParams.get('status');
        const type = searchParams.get('type');
        const sortBy = searchParams.get('sortBy') || 'submittedAt';
        const sortOrder = searchParams.get('sortOrder') || 'desc';
        const startAfter = searchParams.get('startAfter');

        let query: any = db.collection('verification_logs');

        // Filter by status if specified
        if (status) {
            query = query.where('status', '==', status);
        }

        // Filter by type if specified
        if (type) {
            query = query.where('type', '==', type);
        }

        // Add sorting
        query = query.orderBy(sortBy, sortOrder as 'asc' | 'desc');

        // Add pagination cursor if provided
        if (startAfter) {
            query = query.startAfter(startAfter);
        }

        // Apply limit
        query = query.limit(limit);

        const snapshot = await query.get();
        const logs = snapshot.docs.map((doc: any) => ({
            id: doc.id,
            ...doc.data()
        }));

        // Get last document for pagination
        const lastDoc = snapshot.docs[snapshot.docs.length - 1];
        const hasMore = snapshot.docs.length === limit;

        return NextResponse.json({
            logs,
            pagination: {
                hasMore,
                lastDocId: lastDoc?.id || null
            }
        });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}

export async function POST(req: Request) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured' }, { status: 503 });
    }

    try {
        const logData = await req.json();

        // Validate required fields
        if (!logData.type || !logData.title || !logData.submittedBy) {
            return NextResponse.json({ 
                error: 'Missing required fields: type, title, submittedBy' 
            }, { status: 400 });
        }

        // Add timestamps and default values
        const verificationLog = {
            ...logData,
            submittedAt: Date.now(),
            status: logData.status || 'pending',
            priority: logData.priority || 'medium',
            createdAt: Date.now(),
            updatedAt: Date.now()
        };

        const docRef = await db.collection('verification_logs').add(verificationLog);
        
        return NextResponse.json({
            id: docRef.id,
            ...verificationLog
        }, { status: 201 });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}

export async function PATCH(req: Request) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured' }, { status: 503 });
    }

    try {
        const { id, ...updateData } = await req.json();

        if (!id) {
            return NextResponse.json({ error: 'Log ID is required' }, { status: 400 });
        }

        // Add update timestamp
        updateData.updatedAt = Date.now();

        await db.collection('verification_logs').doc(id).update(updateData);

        return NextResponse.json({ 
            id, 
            ...updateData,
            message: 'Verification log updated successfully' 
        });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
