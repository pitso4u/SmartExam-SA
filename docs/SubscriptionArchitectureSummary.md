# SmartExam SA - Firebase + Paystack Subscription Architecture

## Architecture Overview

The subscription system follows the core principle: **WinDev never decides subscription status. Firebase does.**

### Key Components

#### 1. Android App (WinDev)
- **Reads subscription state** from Firebase
- **Enforces UI + watermark** based on Firebase status
- **Never trusts itself** for subscription decisions
- **Handles Paystack checkout** flow

#### 2. Firebase
- **Decides access** - single source of truth
- **Stores user subscription data**
- **Handles real-time updates**
- **Provides security rules**

#### 3. Paystack
- **Collects money** via checkout
- **Sends webhook events** to backend
- **Manages subscription lifecycle**

## Data Model

### Users Collection
```json
{
  "email": "teacher@school.co.za",
  "role": "teacher", 
  "trialStartDate": "2026-02-01T00:00:00Z",
  "subscription": {
    "status": "trial", // trial, active, expired, cancelled
    "paystackCustomerCode": null,
    "paystackSubscriptionCode": null,
    "currentPeriodEnd": null
  },
  "createdAt": "2026-02-01T00:00:00Z"
}
```

### Subscription Events Collection
```json
{
  "userId": "firebaseAuthUid",
  "eventType": "subscription.create",
  "paystackEventId": "evt_xxxxx",
  "data": { ... },
  "processedAt": "2026-02-01T00:00:00Z"
}
```

## Implementation Status

### ✅ Completed Components

1. **UserSubscription Model** (`UserSubscription.java`)
   - Firestore data model
   - Subscription status validation
   - Watermark logic helpers

2. **SubscriptionManager** (`SubscriptionManager.java`)
   - Firebase subscription state reading
   - Real-time listener support
   - Caching mechanism (1 hour)
   - Trial user creation

3. **PaystackService** (`PaystackService.java`)
   - Checkout URL generation
   - Payment verification
   - Firebase Functions integration

4. **SubscriptionPoller** (`SubscriptionPoller.java`)
   - Post-payment status polling
   - 3-second intervals for 30 seconds max
   - Automatic subscription activation detection

5. **PDFGenerator Integration**
   - Watermark application based on subscription
   - "TRIAL VERSION" watermark for non-premium users
   - Clean PDFs for active subscriptions

6. **SubscriptionActivity** (`SubscriptionActivity.java`)
   - Complete subscription UI flow
   - Status display and management
   - Paystack checkout integration
   - Real-time verification

7. **Backend Documentation** (`PaystackBackendIntegration.md`)
   - Firebase Functions implementation
   - Webhook handling
   - Security rules
   - Deployment guide

8. **Updated Firestore Schema** (`FirestoreSchema.md`)
   - Subscription fields added
   - Event tracking structure
   - Status values documentation

## Flow Implementation

### 1. First User Login (Trial Creation)
```
WinDev → SubscriptionManager.getSubscription()
       → Firebase: User document doesn't exist
       → Create trial user (14 days)
       → Return trial status
```

### 2. Subscription Purchase Flow
```
WinDev → PaystackService.createCheckoutUrl()
       → Firebase Function → Paystack API
       → Return checkout URL
       → Open browser for payment
       → Start SubscriptionPoller
       → Paystack webhook → Firebase update
       → Poller detects activation
       → UI updates automatically
```

### 3. PDF Generation with Watermark
```
WinDev → PDFGenerator.generateTest()
       → SubscriptionManager.canPrintClean()
       → Apply watermark if needed
       → Generate PDF
```

## Security Architecture

### Firebase Security Rules
- Users can read their own subscription data
- Users cannot write subscription fields (backend only)
- Audit trail in subscription_events collection

### Paystack Integration
- Webhook signature verification
- Server-side payment processing
- No client-side subscription decisions

## Testing Strategy

### Manual Testing
1. **Trial Flow**: New user gets 14-day trial
2. **Payment Flow**: Complete Paystack checkout
3. **Watermark Logic**: Verify PDF watermarks
4. **Real-time Updates**: Test subscription activation

### Automated Testing
- Unit tests for SubscriptionManager
- Integration tests for PaystackService
- UI tests for SubscriptionActivity

## Deployment Requirements

### Firebase Functions
```bash
firebase functions:config:set paystack.secret_key="sk_test_..."
firebase functions:config:set paystack.webhook_secret="whsec_..."
firebase deploy --only functions
```

### Paystack Configuration
1. Create monthly plan (R50)
2. Set webhook URL
3. Enable test mode
4. Configure redirect URLs

## Next Steps

1. **Backend Deployment**: Set up Firebase Functions
2. **Paystack Setup**: Configure payment plans
3. **Testing**: End-to-end subscription flow
4. **Monitoring**: Set up error tracking
5. **Production**: Switch to live Paystack keys

## Benefits of This Architecture

1. **Security**: Firebase controls access, not the app
2. **Real-time**: Instant subscription updates
3. **Audit Trail**: Complete payment event history
4. **Scalable**: Supports school licenses and multi-device
5. **Offline Grace**: Caching for poor connectivity
6. **Compliance**: Payment data never touches client

## Files Created/Modified

### New Files
- `app/src/main/java/com/smartexam/models/UserSubscription.java`
- `app/src/main/java/com/smartexam/subscription/SubscriptionManager.java`
- `app/src/main/java/com/smartexam/subscription/PaystackService.java`
- `app/src/main/java/com/smartexam/subscription/SubscriptionPoller.java`
- `app/src/main/java/com/smartexam/activities/SubscriptionActivity.java`
- `app/src/main/res/layout/activity_subscription.xml`
- `docs/PaystackBackendIntegration.md`
- `docs/SubscriptionArchitectureSummary.md`

### Modified Files
- `app/src/main/java/com/smartexam/utils/PDFGenerator.java`
- `docs/FirestoreSchema.md`

The architecture is now fully implemented and ready for backend deployment and testing.
