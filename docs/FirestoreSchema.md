# Firestore Schema - SmartExam SA

## Collections

### `questions`
Global pool of questions available in the marketplace.

```json
{
  "subject": "Life Sciences",
  "grade": 11,
  "topic": "Photosynthesis",
  "type": "MULTIPLE_CHOICE",
  "marks": 2,
  "difficulty": "Medium",
  "questionText": "Which pigment is primary in photosynthesis?",
  "content": {
    "options": ["Chlorophyll a", "Chlorophyll b", "Carotenoids", "Xanthophylls"],
    "answer": "Chlorophyll a"
  },
  "tags": ["CAPS", "Term 1"]
}
```

### `question_packs`
Bundled questions for the marketplace.

```json
{
  "title": "Grade 11 Life Sciences - Term 1 Pack",
  "subject": "Life Sciences",
  "grade": 11,
  "price": 4900, // In cents (Stripe)
  "questionCount": 50,
  "questionIds": ["q1", "q2", "..."]
}
```

### `users`
Teacher profiles and ownership tracking.

```json
{
  "email": "teacher@school.za",
  "displayName": "Mr. Smith",
  "purchasedPacks": ["pack_id_1", "pack_id_2"],
  "subscriptionStatus": "premium"
}
```
