import React from 'react';

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="bg-white shadow-sm rounded-lg">
          <div className="px-6 py-8 sm:px-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-8">Legal Terms & Policies</h1>
            
            {/* Privacy Policy */}
            <section className="mb-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6">5.1 PRIVACY POLICY (POPIA-COMPLIANT)</h2>
              <h3 className="text-xl font-medium text-gray-800 mb-4">Privacy Policy – SmartExam SA</h3>
              
              <div className="text-sm text-gray-600 mb-6">
                <p>Effective Date: [Insert date]</p>
                <p>Last Updated: [Insert date]</p>
              </div>

              <p className="text-gray-700 mb-6">
                SmartExam SA (Pty) Ltd ("SmartExam SA", "we", "our", "us") respects your privacy and is committed to protecting personal information in accordance with the Protection of Personal Information Act, 4 of 2013 (POPIA).
              </p>

              <div className="space-y-6">
                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">1. Information We Collect</h4>
                  <h5 className="font-medium text-gray-800 mb-2">1.1 Personal Information</h5>
                  <p className="text-gray-700 mb-3">We may collect:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Full name</li>
                    <li>Email address</li>
                    <li>School name</li>
                    <li>Grade and subject preferences</li>
                    <li>Subscription and transaction references</li>
                  </ul>

                  <h5 className="font-medium text-gray-800 mb-2 mt-4">1.2 Learner Information</h5>
                  <p className="text-gray-700 mb-3">SmartExam SA does not require learner names or identities.</p>
                  <p className="text-gray-700 mb-3">Any learner-related data is:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Aggregated</li>
                    <li>Anonymised</li>
                    <li>Used strictly for assessment generation and reporting</li>
                  </ul>

                  <h5 className="font-medium text-gray-800 mb-2 mt-4">1.3 Technical Information</h5>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Device information</li>
                    <li>App usage analytics</li>
                    <li>IP address (web admin portal only)</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">2. How We Use Information</h4>
                  <p className="text-gray-700 mb-3">We use personal information to:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Provide access to the platform</li>
                    <li>Process subscriptions and purchases</li>
                    <li>Sync purchased content</li>
                    <li>Generate assessments and PDFs</li>
                    <li>Provide customer support</li>
                    <li>Improve platform performance</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">3. Lawful Basis for Processing</h4>
                  <p className="text-gray-700 mb-3">We process information based on:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>User consent</li>
                    <li>Contractual necessity (subscriptions)</li>
                    <li>Legal obligations</li>
                    <li>Legitimate educational interests</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">4. Data Storage & Security</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Data is stored securely using Firebase (Google Cloud) infrastructure.</li>
                    <li>Local data on devices is stored using encrypted Android storage where possible.</li>
                    <li>Access is restricted to authorised systems and personnel only.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">5. Third Parties</h4>
                  <p className="text-gray-700 mb-3">We share limited data with:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Paystack – payment processing</li>
                    <li>Google Firebase – authentication and cloud data storage</li>
                  </ul>
                  <p className="text-gray-700 font-medium">We do not sell personal data to third parties.</p>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">6. Data Retention</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Account data is retained while the account is active.</li>
                    <li>Transaction records are retained as required by law.</li>
                    <li>Users may request deletion of personal data where legally permissible.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">7. User Rights</h4>
                  <p className="text-gray-700 mb-3">Under POPIA, users may:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Request access to their personal data</li>
                    <li>Request correction or deletion</li>
                    <li>Withdraw consent (subject to contractual obligations)</li>
                  </ul>
                  <p className="text-gray-700">Requests can be sent to: privacy@smartexam.co.za</p>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">8. Children's Information</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>SmartExam SA is designed for educators.</li>
                    <li>We do not knowingly collect personal information from children directly.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">9. Changes to This Policy</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>We may update this policy periodically.</li>
                    <li>Continued use of the platform constitutes acceptance.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">10. Contact</h4>
                  <div className="text-gray-700">
                    <p>SmartExam SA (Pty) Ltd</p>
                    <p>Email: privacy@smartexam.co.za</p>
                  </div>
                </div>
              </div>
            </section>

            {/* Terms of Service */}
            <section className="mb-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6">5.2 TERMS OF SERVICE</h2>
              <h3 className="text-xl font-medium text-gray-800 mb-4">Terms of Service – SmartExam SA</h3>
              
              <div className="text-sm text-gray-600 mb-6">
                <p>Effective Date: [Insert date]</p>
              </div>

              <p className="text-gray-700 mb-6">
                By accessing or using SmartExam SA, you agree to these Terms.
              </p>

              <div className="space-y-6">
                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">1. Eligibility</h4>
                  <p className="text-gray-700 mb-3">You must be:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>A teacher, educator, or authorised school representative</li>
                    <li>Legally able to enter into a binding agreement</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">2. Account Responsibility</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>You are responsible for maintaining account security.</li>
                    <li>You must provide accurate and current information.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">3. Intellectual Property</h4>
                  <h5 className="font-medium text-gray-800 mb-2">3.1 Platform IP</h5>
                  <p className="text-gray-700">All software, designs, and systems belong to SmartExam SA (Pty) Ltd.</p>

                  <h5 className="font-medium text-gray-800 mb-2 mt-4">3.2 User-Created Content</h5>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>You retain ownership of questions you create.</li>
                    <li>You grant SmartExam SA a non-exclusive licence to store and process this content.</li>
                  </ul>

                  <h5 className="font-medium text-gray-800 mb-2 mt-4">3.3 Marketplace Content</h5>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Purchased question packs are licensed, not sold.</li>
                    <li>Redistribution, resale, or sharing is strictly prohibited.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">4. Subscriptions</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Subscriptions are billed monthly in advance.</li>
                    <li>Access continues while payment remains active.</li>
                    <li>Cancellation stops future billing but does not refund prior periods.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">5. Acceptable Use</h4>
                  <p className="text-gray-700 mb-3">You may not:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Attempt to bypass licensing</li>
                    <li>Reverse-engineer the platform</li>
                    <li>Use content outside permitted educational purposes</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">6. Termination</h4>
                  <p className="text-gray-700 mb-3">We reserve the right to suspend or terminate accounts for:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Policy violations</li>
                    <li>Fraudulent activity</li>
                    <li>Abuse of the platform</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">7. Limitation of Liability</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>SmartExam SA is provided "as is".</li>
                    <li>We are not liable for indirect or consequential damages arising from use.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">8. Governing Law</h4>
                  <p className="text-gray-700">These Terms are governed by the laws of the Republic of South Africa.</p>
                </div>
              </div>
            </section>

            {/* Refund Policy */}
            <section className="mb-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6">5.3 REFUND POLICY (PAYSTACK-SAFE)</h2>
              <h3 className="text-xl font-medium text-gray-800 mb-4">Refund Policy – SmartExam SA</h3>

              <div className="space-y-6">
                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">Subscriptions</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Subscription fees are non-refundable once billed.</li>
                    <li>Users may cancel at any time to prevent future charges.</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">One-Time Purchases (Question Packs)</h4>
                  <p className="text-gray-700 mb-3">Refunds may be granted within 7 days only if:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Content is materially defective</li>
                    <li>Content is inaccessible due to platform fault</li>
                  </ul>
                  <p className="text-gray-700 font-medium mb-3">No refunds for:</p>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Change of mind</li>
                    <li>Content already downloaded and accessed</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">Processing Refunds</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Approved refunds are processed via Paystack</li>
                    <li>Refunds may take 5–10 business days</li>
                  </ul>
                </div>
              </div>
            </section>

            {/* Cookie & Tracking Disclosure */}
            <section className="mb-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6">5.4 COOKIE & TRACKING DISCLOSURE</h2>
              <h3 className="text-xl font-medium text-gray-800 mb-4">Cookie & Tracking Policy – SmartExam SA</h3>

              <p className="text-gray-700 mb-6">
                SmartExam SA uses limited cookies and tracking technologies on its web-based admin portal.
              </p>

              <div className="space-y-6">
                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">What We Use</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Authentication cookies</li>
                    <li>Session management</li>
                    <li>Basic analytics (performance & error tracking)</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">What We Do NOT Do</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>No advertising trackers</li>
                    <li>No third-party marketing cookies</li>
                  </ul>
                </div>

                <div>
                  <h4 className="text-lg font-medium text-gray-900 mb-3">User Control</h4>
                  <ul className="list-disc list-inside text-gray-700 space-y-1 ml-4">
                    <li>Users may disable cookies via browser settings, but some features may not function correctly.</li>
                  </ul>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}
