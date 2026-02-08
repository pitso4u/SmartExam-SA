
import { NextResponse } from 'next/server';
import OpenAI from 'openai';

// Initialize OpenAI client pointing to OpenRouter
const openai = new OpenAI({
    baseURL: "https://openrouter.ai/api/v1",
    apiKey: process.env.OPENROUTER_API_KEY,
    defaultHeaders: {
        "HTTP-Referer": "https://smartexam-sa.com", // Optional, for OpenRouter rankings
        "X-Title": "SmartExam SA",
    }
});

export async function POST(req: Request) {
    if (!process.env.OPENROUTER_API_KEY) {
        return NextResponse.json({ error: 'OpenRouter API Key not configured' }, { status: 503 });
    }

    try {
        const { topic, grade, subject, count, difficulty, model } = await req.json();

        // Validate inputs
        if (!topic || !grade || !subject) {
            return NextResponse.json({ error: 'Missing required fields' }, { status: 400 });
        }

        // Default to a good free model if none specified
        // Options: "google/gemini-2.0-flash-exp:free", "mistralai/mistral-7b-instruct:free", "deepseek/deepseek-chat"
        const selectedModel = model || "google/gemini-2.0-flash-lite-preview-02-05:free";

        const systemPrompt = `
            You are an expert South African teacher creating CAPS-aligned exam questions.
            Return ONLY a valid JSON array of objects. Do not wrap in markdown code blocks.
            
            Structure for each question:
            {
                "capsTopicId": "${topic}", 
                "subject": "${subject}",
                "grade": ${grade},
                "term": 1,
                "type": "MULTIPLE_CHOICE", 
                "cognitiveLevel": "UNDERSTANDING", 
                "marks": 2,
                "questionText": "The actual question text?",
                "content": {
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswer": "Option A"
                },
                "version": 1,
                "createdAt": ${Date.now()}
            }
        `;

        const userPrompt = `
            Generate ${count || 5} unique exam questions for:
            Subject: ${subject}
            Grade: ${grade}
            Topic: ${topic}
            Difficulty: ${difficulty || 'Mixed'}

            Vary the "cognitiveLevel" between "RECALL", "UNDERSTANDING", "APPLICATION", "EVALUATION".
            Vary the "marks" appropriate to the question difficulty.
            Make the questions challenging and relevant to the South African curriculum.
        `;

        const completion = await openai.chat.completions.create({
            model: selectedModel,
            messages: [
                { role: "system", content: systemPrompt },
                { role: "user", content: userPrompt }
            ],
            response_format: { type: "json_object" } // Some OpenRouter providers support this, others ignore it. We'll try to parse regardless.
        });

        const text = completion.choices[0].message.content || "[]";

        // Basic clean up if model adds markdown
        const cleanedText = text.replace(/```json/g, '').replace(/```/g, '').trim();

        // Handle cases where model wraps response in { "questions": [...] } vs just [...]
        let questions = JSON.parse(cleanedText);
        if (!Array.isArray(questions) && questions.questions) {
            questions = questions.questions;
        }

        if (!Array.isArray(questions)) {
            // Fallback: try to find the array in the text if simple parse failed logic above
            throw new Error("AI did not return an array of questions.");
        }

        return NextResponse.json(questions);

    } catch (error: any) {
        console.error("AI Generation Error:", error);

        const status = error.status || error.response?.status || 500;

        // Pass through rate limits or other OpenRouter specific errors
        return NextResponse.json({
            error: error.message || 'AI Generation Failed',
            details: error.error // OpenRouter error details
        }, { status });
    }
}
