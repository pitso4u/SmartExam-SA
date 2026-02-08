import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/firebase-admin';

export async function DELETE(
    req: Request,
    { params }: { params: { id: string } }
) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured' }, { status: 503 });
    }

    try {
        const { id } = params;
        await db.collection('question_packs').doc(id).delete();
        return NextResponse.json({ success: true });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}

export async function PATCH(
    req: Request,
    { params }: { params: { id: string } }
) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured' }, { status: 503 });
    }

    try {
        const { id } = params;
        const updates = await req.json();

        // Prevent updating immutable fields if necessary, for now allow all
        await db.collection('question_packs').doc(id).update(updates);

        return NextResponse.json({ success: true, id, ...updates });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
