import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/firebase-admin';
import { QuestionPack, Question } from '@/types';

export async function GET() {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured. Please set environment variables.' }, { status: 503 });
    }
    try {
        const packsSnapshot = await db!.collection('question_packs')
            .orderBy('createdAt', 'desc')
            .get();

        const packs = packsSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        }));

        return NextResponse.json(packs);
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}

export async function POST(req: Request) {
    if (!isConfigured || !db) {
        return NextResponse.json({ error: 'Firebase Admin not configured. Please set environment variables.' }, { status: 503 });
    }
    try {
        const pack: QuestionPack = await req.json();

        // 1. Fetch all questions to validate total marks
        const questionRefs = pack.questionIds.map(id => db!.collection('questions').doc(id).get());
        const questionDocs = await Promise.all(questionRefs);

        let calculatedTotalMarks = 0;
        questionDocs.forEach(doc => {
            if (doc.exists) {
                calculatedTotalMarks += (doc.data() as Question).marks;
            }
        });

        // 2. Strict Weighting Validation
        if (calculatedTotalMarks !== pack.totalMarks) {
            return NextResponse.json({
                error: `Total marks mismatch. Expected sum: ${calculatedTotalMarks}, Given: ${pack.totalMarks}`
            }, { status: 400 });
        }

        // 3. CAPS Alignment Check
        if (!pack.subject || !pack.grade || !pack.term) {
            return NextResponse.json({ error: 'Subject, Grade, and Term are required for CAPS compliance' }, { status: 400 });
        }

        pack.createdAt = Date.now();
        pack.version = 1;
        pack.isPublished = false;

        const docRef = await db!.collection('question_packs').add(pack);

        return NextResponse.json({ id: docRef.id, ...pack }, { status: 201 });
    } catch (error: any) {
        return NextResponse.json({ error: error.message }, { status: 500 });
    }
}
