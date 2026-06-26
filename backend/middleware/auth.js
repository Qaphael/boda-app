const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// You will need to download your Service Account JSON file from Firebase Console -> Project Settings -> Service accounts
// and point process.env.FIREBASE_SERVICE_ACCOUNT_PATH to it.
const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;

let firebaseInitialized = false;

if (serviceAccountPath) {
  try {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    console.log('✓ Firebase Admin SDK initialized successfully.');
    firebaseInitialized = true;
  } catch (error) {
    console.error('❌ Failed to initialize Firebase Admin SDK with key file:', error.message);
  }
} else {
  console.log('⚠️ FIREBASE_SERVICE_ACCOUNT_PATH not provided. Server is running in LOCAL DEVELOPMENT mode. Firebase JWT verification is mocked.');
}

/**
 * Authentication Middleware
 * Verifies the Firebase ID Token passed in the 'Authorization: Bearer <TOKEN>' header.
 */
async function verifyFirebaseToken(req, res, next) {
  const authHeader = req.headers.authorization;
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing or malformed Bearer Token' });
  }

  const token = authHeader.split('Bearer ')[1];

  if (!firebaseInitialized) {
    // Local Dev fallback: Accept any token and treat the token itself as the simulated uid
    console.log(`[DEV MODE] Mock-verifying token: ${token.substring(0, 10)}...`);
    req.user = {
      uid: token === 'test_rider_uid' ? 'test_rider_uid' : (token === 'test_driver_uid' ? 'test_driver_uid' : token),
      email: 'dev-user@gulu.boda',
      phone_number: '+256770000000',
      name: 'Gulu Dev Rider'
    };
    return next();
  }

  try {
    const decodedToken = await admin.auth().verifyIdToken(token);
    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email,
      phone_number: decodedToken.phone_number,
      name: decodedToken.name || decodedToken.email?.split('@')[0] || 'Gulu Passenger'
    };
    next();
  } catch (error) {
    console.error('Firebase token verification failed:', error.message);
    return res.status(403).json({ error: 'Forbidden: Invalid or expired authentication token' });
  }
}

module.exports = {
  verifyFirebaseToken,
  getFirebaseAdmin: () => admin
};
