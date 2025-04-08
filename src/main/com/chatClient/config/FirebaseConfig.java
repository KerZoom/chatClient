package main.com.chatClient.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class FirebaseConfig {
    public static final String API_KEY = "AIzaSyCGqisilUJHpLCESYQgML09tHTuZQUJw04"; // Replace with your actual Web API key
    private static final String PROJECT_ID = "fine-rite-443512-n6"; // Replace with your actual project ID
    public static final String STORAGE_BUCKET = "fine-rite-443512-n6.appspot.com"; // Replace with your actual storage bucket
    private static Firestore firestore;
    private static Storage storage;

    public static synchronized void initialize() {
        if (firestore == null || storage == null) {
            try {
                FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                        .setProjectId(PROJECT_ID)
                        .build();
                firestore = firestoreOptions.getService();

                StorageOptions storageOptions = StorageOptions.newBuilder()
                        .setProjectId(PROJECT_ID)
                        .build();
                storage = storageOptions.getService();
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