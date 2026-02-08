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
Teacher profiles and subscription tracking. Document ID = firebaseAuthUid

```json
{
  "email": "teacher@school.co.za",
  "role": "teacher",
  "trialStartDate": "2026-02-01T00:00:00Z",
  "subscription": {
    "status": "trial",
    "paystackCustomerCode": null,
    "paystackSubscriptionCode": null,
    "currentPeriodEnd": null
  },
  "createdAt": "2026-02-01T00:00:00Z"
}
```

#### Subscription Status Values
- `trial` - User is in 14-day trial period
- `active` - Paid subscription is active
- `expired` - Subscription has expired
- `cancelled` - Subscription was cancelled

### `subscription_events`
Paystack webhook events for audit trail

```json
{
  "userId": "firebaseAuthUid",
  "eventType": "subscription.create",
  "paystackEventId": "evt_xxxxx",
  "data": {
    "subscription_code": "SUB_xxxxx",
    "customer_code": "CUS_xxxxx",
    "next_payment_date": "2026-03-01T00:00:00Z"
  },
  "processedAt": "2026-02-01T00:00:00Z"
}
```
