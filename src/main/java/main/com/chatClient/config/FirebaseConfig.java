package main.com.chatClient.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Configuration class for Firebase/Google Cloud services.
 */
public class FirebaseConfig {
    // Firebase/Google Cloud configuration
    public static final String API_KEY = "AIzaSyCGqisilUJHpLCESYQgML09tHTuZQUJw04";
    private static final String PROJECT_ID = "fine-rite-443512-n6";
    public static final String STORAGE_BUCKET = "fine-rite-443512-n6.firebasestorage.app/user_uploaded_files";
    // Service instances
    private static Firestore firestore;
    private static Storage storage;
    private static boolean initialized = false;

    /**
     * Initializes Firebase services with the provided authentication token.
     *
     * @param idToken Firebase ID token
     */
    public static synchronized void initialize(String idToken) {
        try {
            if (idToken == null || idToken.isEmpty()) {
                System.err.println("Cannot initialize Firebase: ID token is empty");
                return;
            }

            System.out.println("Initializing Firebase services with token");

            // Create Google credentials from Firebase ID token
            GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(idToken, null));

            // Build Firestore instance
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setProjectId(PROJECT_ID)
                    .setCredentials(credentials)
                    .build();
            firestore = firestoreOptions.getService();

            // Build Storage instance
            StorageOptions storageOptions = StorageOptions.newBuilder()
                    .setProjectId(PROJECT_ID)
                    .setCredentials(credentials)
                    .build();
            storage = storageOptions.getService();

            // Mark as initialized
            initialized = true;
            System.out.println("Firebase services initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
            initialized = false;
        }
    }

    /**
     * Gets the Firestore database instance.
     *
     * @return Firestore instance or null if not initialized
     */
    public static Firestore getFirestore() {
        if (!initialized || firestore == null) {
            System.err.println("Firebase not initialized. Call initialize() first.");
            return null;
        }
        return firestore;
    }

    /**
     * Gets the Cloud Storage instance.
     *
     * @return Storage instance or null if not initialized
     */
    public static Storage getStorage() {
        if (!initialized || storage == null) {
            System.err.println("Firebase not initialized. Call initialize() first.");
            return null;
        }
        return storage;
    }

    /**
     * Checks if Firebase has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
}