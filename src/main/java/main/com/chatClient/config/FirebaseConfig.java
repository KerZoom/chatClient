package main.com.chatClient.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class FirebaseConfig {
    public static final String API_KEY = "AIzaSyCGqisilUJHpLCESYQgML09tHTuZQUJw04";
    private static final String PROJECT_ID = "fine-rite-443512-n6";
    public static final String STORAGE_BUCKET = "fine-rite-443512-n6.firebasestorage.app/user_uploaded_files";
    private static Firestore firestore;
    private static Storage storage;

    public static synchronized void initialize(String idToken) {
        if (firestore == null || storage == null) {
            try {
                FirestoreOptions.Builder firestoreBuilder = FirestoreOptions.newBuilder()
                        .setProjectId(PROJECT_ID);
                StorageOptions.Builder storageBuilder = StorageOptions.newBuilder()
                        .setProjectId(PROJECT_ID);

                if (idToken != null) {
                    GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(idToken, null));
                    firestoreBuilder.setCredentials(credentials);
                    storageBuilder.setCredentials(credentials);
                }

                firestore = firestoreBuilder.build().getService();
                storage = storageBuilder.build().getService();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
            }
        }
    }

    public static Firestore getFirestore() {
        if (firestore == null) {
            throw new IllegalStateException("Firebase not initialized. Call initialize() first.");
        }
        return firestore;
    }

    public static Storage getStorage() {
        if (storage == null) {
            throw new IllegalStateException("Firebase not initialized. Call initialize() first.");
        }
        return storage;
    }
}