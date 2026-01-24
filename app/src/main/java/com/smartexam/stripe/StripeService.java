package com.smartexam.stripe;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;

/**
 * Enhanced StripeService with PaymentSheet integration for Android.
 */
public class StripeService {

    private final String publishableKey = "pk_test_..."; // Placeholder

    public StripeService(android.content.Context context) {
        PaymentConfiguration.init(context, publishableKey);
    }

    /**
     * Prepares and launches the Stripe Payment Sheet.
     * 
     * @param paymentIntentClientSecret The secret received from your backend after
     *                                  creating a PaymentIntent.
     * @param paymentSheet              The PaymentSheet instance from the Activity.
     */
    public void presentPaymentSheet(String paymentIntentClientSecret, PaymentSheet paymentSheet) {
        final PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("SmartExam SA")
                .allowsDelayedPaymentMethods(true)
                .build();

        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
    }

    /**
     * Backend simulation: Create PaymentIntent on server.
     */
    public String simulateBackendCreateIntent(String packId, int amountCents) {
        // This logic belongs on a Java/Node.js server using Stripe SDK
        // return stripe.paymentIntents.create({ amount: amountCents, currency: 'zar'
        // }).client_secret;
        return "pi_dummy_secret_12345";
    }
}
