const admin = require('firebase-admin');

// Initialize Firebase Admin SDK using environment variables
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: 'smartexam-sa',
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
  }),
  projectId: 'smartexam-sa'
});

const auth = admin.auth();

async function setAdminClaims() {
  try {
    // Replace with your actual admin email
    const adminEmail = 'pitso4u@gmail.com';
    
    // Get user by email
    const userRecord = await auth.getUserByEmail(adminEmail);
    
    // Set custom claims for admin role
    await auth.setCustomUserClaims(userRecord.uid, {
      admin: true,
      role: 'admin',
      accessLevel: 'full'
    });
    
    console.log(`✅ Admin claims set for ${adminEmail}`);
    console.log('User UID:', userRecord.uid);
    
    // Verify the claims were set
    const updatedUser = await auth.getUser(userRecord.uid);
    console.log('Custom claims:', updatedUser.customClaims);
    
  } catch (error) {
    console.error('❌ Error setting admin claims:', error);
  } finally {
    process.exit();
  }
}

// Run the setup
setAdminClaims();
