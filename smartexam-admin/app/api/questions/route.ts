import { NextResponse } from 'next/server';
import { db } from '@/lib/firebase-admin';
import { Question } from '@/types';

export async function POST(req: Request) {
    try {
        const question: Question = await req.json();

        // CAPS Validation Logic
        if (!question.marks || question.marks <= 0) {
            return NextResponse.json({ error: 'Mark allocation is required and must be > 0' }, { status: 400 });
        }

        if (!['RECALL', 'UNDERSTANDING', 'APPLICATION', 'EVALUATION'].includes(question.cognitiveLevel)) {
            return NextResponse.json({ error: 'Invalid or missing CAPS cognitive level' }, { status: 400 });
        }

        if (!question.capsTopicId) {
            return NextResponse.json({ error: 'CAPS Topic ID is required' }, { status: 400 });
        }

        // Production check: Versioning
        question.version = (question.version || 0) + 1;
        question.createdAt = Date.now();

        const docRef = await db.collection('questions').add(question);

        return NextResponse.json({ id: docRef.id, ...question }, { status: 201 });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
