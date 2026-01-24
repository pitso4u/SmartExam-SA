export type CognitiveLevel = 'RECALL' | 'UNDERSTANDING' | 'APPLICATION' | 'EVALUATION';

export type QuestionType =
    | 'MULTIPLE_CHOICE'
    | 'TRUE_FALSE'
    | 'MATCH_COLUMNS'
    | 'FILL_IN_BLANKS'
    | 'CHOOSE_FROM_TABLE'
    | 'IMAGE_LABELING'
    | 'IMAGE_BASED';

export interface Question {
    id?: string;
    capsTopicId: string;
    subject: string;
    grade: number;
    term: number;
    type: QuestionType;
    cognitiveLevel: CognitiveLevel;
    marks: number;
    questionText: string;
    content: any; // Flexible structure per type
    imagePath?: string;
    tags?: string[];
    version: number;
    createdAt: number;
}

export interface QuestionPack {
    id?: string;
    title: string;
    description: string;
    subject: string;
    grade: number;
    term: number;
    totalMarks: number;
    questionCount: number;
    questionIds: string[];
    priceCents: number;
    capsStrand: string;
    isPublished: boolean;
    version: number;
    createdAt: number;
}
