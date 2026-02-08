import { Question, QuestionPack } from '@/types';

// Mathematics Topic Bank (Grades 4-7)
const mathTopics = {
  4: {
    "Numbers, Operations & Relationships": [
      "Place value (up to 6-digit numbers)",
      "Comparing and ordering numbers",
      "Addition and subtraction",
      "Multiplication (2–3 digit × 1 digit)",
      "Division (sharing and grouping)",
      "Fractions (halves, quarters, thirds)",
      "Number patterns"
    ],
    "Patterns, Functions & Algebra": [
      "Numeric patterns",
      "Input-output tables",
      "Simple algebraic rules"
    ],
    "Space & Shape (Geometry)": [
      "2D shapes (properties)",
      "3D objects",
      "Symmetry"
    ],
    "Measurement": [
      "Length",
      "Mass",
      "Capacity",
      "Time",
      "Perimeter"
    ],
    "Data Handling": [
      "Pictographs",
      "Bar graphs",
      "Reading tables"
    ]
  },
  5: {
    "Numbers, Operations & Relationships": [
      "Place value (up to millions)",
      "Addition and subtraction (large numbers)",
      "Multiplication (3-digit × 2-digit)",
      "Division (long division basics)",
      "Common fractions (equivalent fractions)",
      "Decimal fractions (tenths, hundredths)"
    ],
    "Patterns, Functions & Algebra": [
      "Numeric patterns",
      "Geometric patterns",
      "Function rules"
    ],
    "Space & Shape": [
      "Angles",
      "Triangles and quadrilaterals",
      "3D objects (nets)"
    ],
    "Measurement": [
      "Area",
      "Perimeter",
      "Time calculations"
    ],
    "Data Handling": [
      "Bar graphs",
      "Line graphs",
      "Interpreting data"
    ]
  },
  6: {
    "Numbers, Operations & Relationships": [
      "Whole number operations",
      "Common fractions (add/subtract)",
      "Decimal fractions",
      "Percentage (basic)",
      "Ratio"
    ],
    "Patterns, Functions & Algebra": [
      "Numeric sequences",
      "Input-output rules",
      "Simple algebraic expressions"
    ],
    "Space & Shape": [
      "Angles (measuring and classifying)",
      "Transformation (reflection, rotation)"
    ],
    "Measurement": [
      "Area of rectangles and triangles",
      "Volume (cubes)",
      "Temperature"
    ],
    "Data Handling": [
      "Line graphs",
      "Pie charts",
      "Mean (average)"
    ]
  },
  7: {
    "Numbers, Operations & Relationships": [
      "Integers",
      "Fractions (operations)",
      "Decimal operations",
      "Percentage calculations",
      "Ratio and rate"
    ],
    "Patterns, Functions & Algebra": [
      "Algebraic expressions",
      "Substitution",
      "Linear number patterns"
    ],
    "Space & Shape": [
      "Geometry terminology",
      "Angle relationships",
      "2D geometry properties"
    ],
    "Measurement": [
      "Area of composite shapes",
      "Surface area",
      "Volume"
    ],
    "Data Handling": [
      "Statistical measures (mean, median, mode)",
      "Graph interpretation",
      "Probability (basic)"
    ]
  }
};

// Question types available
const questionTypes = [
  'MULTIPLE_CHOICE',
  'TRUE_FALSE',
  'MATCH_COLUMNS',
  'FILL_IN_BLANKS'
] as const;

// Cognitive levels
const cognitiveLevels = ['RECALL', 'UNDERSTANDING', 'APPLICATION', 'EVALUATION'] as const;

// Difficulties
const difficulties = ['Easy', 'Medium', 'Hard'] as const;

// Function to generate a question
function generateQuestion(
  grade: number,
  strand: string,
  topic: string,
  type: typeof questionTypes[number],
  cognitiveLevel: typeof cognitiveLevels[number],
  marks: number,
  difficulty: typeof difficulties[number]
): Omit<Question, 'id' | 'createdAt' | 'version' | 'packId' | 'isFromMarketplace'> {
  const { questionText, content } = generateQuestionContent(grade, strand, topic, type);

  return {
    subject: 'Mathematics',
    grade,
    topic,
    capsTopicId: topic,
    type,
    cognitiveLevel,
    marks,
    difficulty,
    questionText,
    content,
    tags: [topic, strand]
  };
}

// Generate question text and content based on strand and topic
function generateQuestionContent(grade: number, strand: string, topic: string, type: string): { questionText: string, content: Record<string, string> } {
  let questionText = '';
  let content: Record<string, string> = {};

  if (strand.includes('Numbers')) {
    ({ questionText, content } = generateNumbersQuestion(grade, topic, type));
  } else if (strand.includes('Patterns')) {
    ({ questionText, content } = generatePatternsQuestion(grade, topic, type));
  } else if (strand.includes('Space') || strand.includes('Geometry')) {
    ({ questionText, content } = generateSpaceQuestion(grade, topic, type));
  } else if (strand.includes('Measurement')) {
    ({ questionText, content } = generateMeasurementQuestion(grade, topic, type));
  } else if (strand.includes('Data')) {
    ({ questionText, content } = generateDataQuestion(grade, topic, type));
  } else {
    // Default
    questionText = `Sample ${type} question on ${topic} for Grade ${grade}`;
    content = generateContent(type);
  }

  return { questionText, content };
}

// Numbers strand questions
function generateNumbersQuestion(grade: number, topic: string, type: string): { questionText: string, content: Record<string, string> } {
  let questionText = '';
  let content: Record<string, string> = {};

  if (topic.includes('Place value')) {
    const maxDigits = grade === 4 ? 6 : grade === 5 ? 7 : 8;
    const num = Math.floor(Math.random() * Math.pow(10, maxDigits)) + Math.pow(10, maxDigits - 1);
    const digitPos = Math.floor(Math.random() * maxDigits);
    const digit = num.toString().padStart(maxDigits, '0')[digitPos];
    questionText = `What is the place value of the digit ${digit} in ${num}?`;

    if (type === 'MULTIPLE_CHOICE') {
      const places = ['Units', 'Tens', 'Hundreds', 'Thousands', 'Ten thousands', 'Hundred thousands', 'Millions', 'Ten millions'];
      const correctIndex = maxDigits - 1 - digitPos;
      content = {
        options: JSON.stringify(places.slice(0, maxDigits)),
        answer: String.fromCharCode(65 + correctIndex)
      };
    } else if (type === 'FILL_IN_BLANKS') {
      content = { answer: ['Units', 'Tens', 'Hundreds', 'Thousands', 'Ten thousands', 'Hundred thousands', 'Millions', 'Ten millions'][maxDigits - 1 - digitPos] };
    }
  } else if (topic.includes('Addition') || topic.includes('Subtraction')) {
    const a = Math.floor(Math.random() * 10000) + 100;
    const b = Math.floor(Math.random() * 1000) + 10;
    const op = topic.includes('Addition') ? '+' : '-';
    const answer = op === '+' ? a + b : a - b;
    questionText = `Calculate: ${a} ${op} ${b}`;

    if (type === 'FILL_IN_BLANKS') {
      content = { answer: answer.toString() };
    } else if (type === 'MULTIPLE_CHOICE') {
      const options = [answer, answer + 10, answer - 5, answer + 20];
      content = {
        options: JSON.stringify(options.map(String)),
        answer: 'A'
      };
    }
  } else if (topic.includes('Multiplication')) {
    const a = Math.floor(Math.random() * 100) + 10;
    const b = Math.floor(Math.random() * 10) + 1;
    const answer = a * b;
    questionText = `Calculate: ${a} × ${b}`;

    if (type === 'FILL_IN_BLANKS') {
      content = { answer: answer.toString() };
    }
  } else if (topic.includes('Division')) {
    const answer = Math.floor(Math.random() * 50) + 10;
    const b = Math.floor(Math.random() * 10) + 2;
    const a = answer * b;
    questionText = `Calculate: ${a} ÷ ${b}`;

    if (type === 'FILL_IN_BLANKS') {
      content = { answer: answer.toString() };
    }
  } else if (topic.includes('Fractions')) {
    if (topic.includes('equivalent')) {
      const frac = ['1/2', '1/3', '1/4', '2/4', '3/6'];
      const correct = '2/4';
      questionText = `Which fraction is equivalent to 1/2?`;
      if (type === 'MULTIPLE_CHOICE') {
        content = {
          options: JSON.stringify(frac),
          answer: 'D' // 2/4 is index 3
        };
      }
    } else {
      questionText = `Simplify the fraction 4/8.`;
      if (type === 'FILL_IN_BLANKS') {
        content = { answer: '1/2' };
      }
    }
  } else {
    // Default numbers question
    questionText = `Solve the following: 2 + 2`;
    content = generateContent(type);
  }

  return { questionText, content };
}

// Patterns strand
function generatePatternsQuestion(grade: number, topic: string, type: string): { questionText: string, content: Record<string, string> } {
  let questionText = 'What is the next number in the pattern: 2, 4, 6, 8, ...';
  let content: Record<string, string> = {};

  if (type === 'FILL_IN_BLANKS') {
    content = { answer: '10' };
  } else if (type === 'MULTIPLE_CHOICE') {
    content = {
      options: JSON.stringify(['9', '10', '11', '12']),
      answer: 'B'
    };
  }

  return { questionText, content };
}

// Space & Shape
function generateSpaceQuestion(grade: number, topic: string, type: string): { questionText: string, content: Record<string, string> } {
  let questionText = 'How many sides does a triangle have?';
  let content: Record<string, string> = {};

  if (type === 'FILL_IN_BLANKS') {
    content = { answer: '3' };
  } else if (type === 'MULTIPLE_CHOICE') {
    content = {
      options: JSON.stringify(['2', '3', '4', '5']),
      answer: 'B'
    };
  }

  return { questionText, content };
}

// Measurement
function generateMeasurementQuestion(grade: number, topic: string, type: string): { questionText: string, content: Record<string, string> } {
  let questionText = 'What unit is used to measure length?';
  let content: Record<string, string> = {};

  if (type === 'MULTIPLE_CHOICE') {
    content = {
      options: JSON.stringify(['Kilograms', 'Liters', 'Meters', 'Seconds']),
      answer: 'C'
    };
  }

  return { questionText, content };
}

// Data Handling
function generateDataQuestion(grade: number, topic: string, type: string): { questionText: string, content: Record<string, string> } {
  let questionText = 'What does a bar graph show?';
  let content: Record<string, string> = {};

  if (type === 'MULTIPLE_CHOICE') {
    content = {
      options: JSON.stringify(['Numbers', 'Comparisons', 'Time', 'Shapes']),
      answer: 'B'
    };
  }

  return { questionText, content };
}

// Generate content based on type (fallback)
function generateContent(type: string): Record<string, string> {
  switch (type) {
    case 'MULTIPLE_CHOICE':
      return {
        options: JSON.stringify(['Option A', 'Option B', 'Option C', 'Option D']),
        answer: 'A'
      };
    case 'TRUE_FALSE':
      return {
        answer: 'true'
      };
    case 'MATCH_COLUMNS':
      return {
        columnA: JSON.stringify(['Item 1', 'Item 2', 'Item 3', 'Item 4']),
        columnB: JSON.stringify(['Match 1', 'Match 2', 'Match 3', 'Match 4']),
        mapping: JSON.stringify({ 'Item 1': 'Match 1', 'Item 2': 'Match 2', 'Item 3': 'Match 3', 'Item 4': 'Match 4' })
      };
    case 'FILL_IN_BLANKS':
      return {
        answer: 'sample answer'
      };
    default:
      return {};
  }
}

// Main function to generate all packs
async function generateMathPacks() {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';

  for (const [gradeStr, strands] of Object.entries(mathTopics)) {
    const grade = parseInt(gradeStr);

    for (const [strand, topics] of Object.entries(strands)) {
      for (const topic of topics) {
        console.log(`Generating pack for Grade ${grade}, ${strand}, ${topic}`);

        // Generate 10 questions per topic
        const questionIds: string[] = [];
        let totalMarks = 0;

        for (let i = 0; i < 10; i++) {
          const type = questionTypes[Math.floor(Math.random() * questionTypes.length)];
          const cognitiveLevel = cognitiveLevels[Math.floor(Math.random() * cognitiveLevels.length)];
          const marks = Math.floor(Math.random() * 4) + 2; // 2-5 marks
          const difficulty = difficulties[Math.floor(Math.random() * difficulties.length)];

          const questionData = generateQuestion(grade, strand, topic, type, cognitiveLevel, marks, difficulty);

          try {
            const res = await fetch(`${baseUrl}/api/questions`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(questionData)
            });

            if (!res.ok) {
              throw new Error(`Failed to create question: ${res.statusText}`);
            }

            const createdQuestion = await res.json();
            questionIds.push(createdQuestion.id);
            totalMarks += marks;
          } catch (error) {
            console.error(`Error creating question ${i} for ${topic}:`, error);
          }
        }

        // Create pack
        const packData: Partial<QuestionPack> = {
          title: `Grade ${grade} ${strand} - ${topic}`,
          description: `CAPS-aligned Mathematics questions for ${topic} in Grade ${grade}`,
          subject: 'Mathematics',
          grade,
          term: 1, // Default to term 1
          totalMarks,
          questionCount: questionIds.length,
          questionIds,
          priceCents: 2999, // R29.99 default
          capsStrand: strand,
          isPublished: false
        };

        try {
          const res = await fetch(`${baseUrl}/api/packs`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(packData)
          });

          if (!res.ok) {
            throw new Error(`Failed to create pack: ${res.statusText}`);
          }

          const createdPack = await res.json();
          console.log(`Created pack: ${createdPack.id}`);
        } catch (error) {
          console.error(`Error creating pack for ${topic}:`, error);
        }
      }
    }
  }

  console.log('All packs generated successfully!');
}

// If run directly
if (require.main === module) {
  generateMathPacks().catch(console.error);
}

export { mathTopics, generateMathPacks };
