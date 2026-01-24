import * as admin from 'firebase-admin';

const isConfigured =
  process.env.FIREBASE_PROJECT_ID &&
  process.env.FIREBASE_CLIENT_EMAIL &&
  process.env.FIREBASE_PRIVATE_KEY;

if (!admin.apps.length && isConfigured) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    }),
    storageBucket: process.env.FIREBASE_STORAGE_BUCKET,
  });
} else if (!isConfigured) {
  console.warn("Firebase Admin SDK not initialized: Missing environment variables.");
}

const db = isConfigured ? admin.firestore() : null;
const storage = isConfigured ? admin.storage() : null;
const auth = isConfigured ? admin.auth() : null;

export { db, storage, auth, isConfigured };
