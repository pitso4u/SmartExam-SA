# Paystack Backend Integration Guide

This document outlines the backend implementation required for the Paystack + Firebase subscription architecture.

## Firebase Functions Required

### 1. `createPaystackCheckout`

Creates a Paystack checkout URL for subscription purchase.

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

admin.initializeApp();

const PAYSTACK_SECRET_KEY = functions.config().paystack.secret_key;
const PAYSTACK_BASE_URL = 'https://api.paystack.co';

exports.createPaystackCheckout = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { userId, email, amount, currency, plan } = data;
  
  try {
    // Create Paystack customer if doesn't exist
    const customerResponse = await axios.post(
      `${PAYSTACK_BASE_URL}/customer`,
      {
        email: email,
        metadata: {
          firebase_uid: userId
        }
      },
      {
        headers: {
          Authorization: `Bearer ${PAYSTACK_SECRET_KEY}`,
          'Content-Type': 'application/json'
        }
      }
    );

    const customerCode = customerResponse.data.data.customer_code;

    // Initialize subscription transaction
    const transactionResponse = await axios.post(
      `${PAYSTACK_BASE_URL}/transaction/initialize`,
      {
        email: email,
        amount: amount, // in cents
        currency: currency,
        plan: plan, // Plan code from Paystack dashboard
        metadata: {
          firebase_uid: userId,
          custom_fields: [
            {
              display_name: "User ID",
              variable_name: "firebase_uid",
              value: userId
            }
          ]
        }
      },
      {
        headers: {
          Authorization: `Bearer ${PAYSTACK_SECRET_KEY}`,
          'Content-Type': 'application/json'
        }
      }
    );

    return {
      authorizationUrl: transactionResponse.data.data.authorization_url,
      reference: transactionResponse.data.data.reference,
      customerCode: customerCode
    };

  } catch (error) {
    console.error('Paystack checkout creation failed:', error);
    throw new functions.https.HttpsError('internal', 'Failed to create checkout');
  }
});
```

### 2. `verifyPaystackPayment`

Verifies a Paystack payment transaction.

```javascript
exports.verifyPaystackPayment = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { reference } = data;
  
  try {
    const response = await axios.get(
      `${PAYSTACK_BASE_URL}/transaction/verify/${reference}`,
      {
        headers: {
          Authorization: `Bearer ${PAYSTACK_SECRET_KEY}`
        }
      }
    );

    const transaction = response.data.data;
    
    return {
      verified: transaction.status === 'success',
      data: transaction
    };

  } catch (error) {
    console.error('Payment verification failed:', error);
    throw new functions.https.HttpsError('internal', 'Failed to verify payment');
  }
});
```

## Webhook Endpoint

Create an HTTP endpoint to handle Paystack webhooks:

```javascript
exports.paystackWebhook = functions.https.onRequest(async (req, res) => {
  const sig = req.headers['x-paystack-signature'];
  const secret = functions.config().paystack.webhook_secret;
  
  // Verify webhook signature
  const crypto = require('crypto');
  const hash = crypto.createHmac('sha512', secret).update(JSON.stringify(req.body)).digest('hex');
  
  if (hash !== sig) {
    console.error('Invalid webhook signature');
    return res.status(401).send('Invalid signature');
  }

  const event = req.body;
  
  try {
    await processWebhookEvent(event);
    res.status(200).send('Webhook processed');
  } catch (error) {
    console.error('Webhook processing failed:', error);
    res.status(500).send('Processing failed');
  }
});

async function processWebhookEvent(event) {
  const { event: eventType, data } = event;
  
  // Extract user ID from metadata
  const userId = data.metadata?.firebase_uid;
  if (!userId) {
    console.error('No user ID in webhook data');
    return;
  }

  const userRef = admin.firestore().collection('users').doc(userId);
  const eventsRef = admin.firestore().collection('subscription_events');

  // Log the event for audit trail
  await eventsRef.add({
    userId: userId,
    eventType: eventType,
    paystackEventId: event.id,
    data: data,
    processedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  switch (eventType) {
    case 'subscription.create':
      await handleSubscriptionCreate(userRef, data);
      break;
      
    case 'subscription.disable':
      await handleSubscriptionDisable(userRef, data);
      break;
      
    case 'invoice.payment_failed':
      await handlePaymentFailed(userRef, data);
      break;
      
    case 'charge.success':
      await handleChargeSuccess(userRef, data);
      break;
  }
}

async function handleSubscriptionCreate(userRef, data) {
  await userRef.update({
    'subscription.status': 'active',
    'subscription.paystackCustomerCode': data.customer_code,
    'subscription.paystackSubscriptionCode': data.subscription_code,
    'subscription.currentPeriodEnd': data.next_payment_date
  });
}

async function handleSubscriptionDisable(userRef, data) {
  await userRef.update({
    'subscription.status': 'cancelled'
  });
}

async function handlePaymentFailed(userRef, data) {
  await userRef.update({
    'subscription.status': 'expired'
  });
}

async function handleChargeSuccess(userRef, data) {
  // Handle one-time payments if needed
  console.log('Charge successful for user:', data.metadata?.firebase_uid);
}
```

## Environment Configuration

Set up Firebase Functions configuration:

```bash
firebase functions:config:set paystack.secret_key="your_paystack_secret_key"
firebase functions:config:set paystack.webhook_secret="your_webhook_secret"
```

## Paystack Dashboard Setup

1. **Create Subscription Plans:**
   - Monthly Teacher Plan: R50/month
   - Plan code: `monthly_teacher`

2. **Configure Webhooks:**
   - URL: `https://your-region-your-project.cloudfunctions.net/paystackWebhook`
   - Events: `subscription.create`, `subscription.disable`, `invoice.payment_failed`, `charge.success`

3. **Test Settings:**
   - Enable test mode for development
   - Use test cards for initial testing

## Security Considerations

1. **Firebase Security Rules:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read their own subscription data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Only backend can write subscription fields
      allow write: if request.auth != null && 
                   request.auth.uid == userId &&
                   !request.resource.data.keys().hasAll(['subscription.status']);
    }
    
    // Only backend can write subscription events
    match /subscription_events/{eventId} {
      allow read: if request.auth != null;
      allow write: if false; // Only Cloud Functions can write
    }
  }
}
```

2. **Rate Limiting:**
   - Implement rate limiting on checkout creation
   - Monitor webhook processing for abuse

3. **Data Validation:**
   - Validate all incoming webhook data
   - Check user permissions before subscription updates

## Testing

1. **Test Webhooks:**
   ```bash
   curl -X POST https://your-function-url/paystackWebhook \
     -H "Content-Type: application/json" \
     -H "X-Paystack-Signature: test_signature" \
     -d '{"event":"subscription.create","data":{...}}'
   ```

2. **Test Checkout Flow:**
   - Use Paystack test cards
   - Verify webhook processing
   - Check Firestore updates

## Deployment

1. Deploy functions:
   ```bash
   firebase deploy --only functions
   ```

2. Configure webhook URL in Paystack dashboard

3. Test end-to-end flow with real payments (small amounts)

## Monitoring

1. **Firebase Functions Logs:**
   - Monitor webhook processing
   - Track failed transactions
   - Watch for unusual activity

2. **Paystack Dashboard:**
   - Monitor subscription metrics
   - Track payment success rates
   - Review webhook delivery status

## Error Handling

1. **Retry Logic:**
   - Implement exponential backoff for failed webhook processing
   - Store failed events for manual review

2. **Customer Support:**
   - Log all subscription changes
   - Provide manual override capabilities
   - Set up alerts for failed payments
