import { NextResponse } from 'next/server';
import { db } from '@/lib/firebase-admin';
import * as admin from 'firebase-admin';

export async function POST(req: Request) {
    try {
        const { sessionId, userId, packId } = await req.json();

        // 1. Production: Verify Stripe Session Status
        // const session = await stripe.checkout.sessions.retrieve(sessionId);
        // if (session.payment_status !== 'paid') throw new Error('Payment not verified');

        // 2. Grant Access: Update User profile in Firestore
        const userRef = db.collection('users').doc(userId);
        await userRef.update({
            purchasedPacks: admin.firestore.FieldValue.arrayUnion(packId)
        });

        // 3. Record Revenue Metric
        await db.collection('revenue_logs').add({
            packId,
            userId,
            amountCents: 4900, // Derived from pack price in production
            createdAt: Date.now()
        });

        return NextResponse.json({ success: true });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
