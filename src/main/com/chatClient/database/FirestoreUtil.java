package main.com.chatClient.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirestoreUtil {
    private static Firestore db;
    private static Storage storage;
    private static FirebaseApp firebaseApp;

    public static void initialize() throws IOException {
        FileInputStream serviceAccount = new FileInputStream("firebase-adminsdk.json");

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

        if (firebaseApp == null) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setStorageBucket("fine-rite-443512-n6.firebasestorage.app")
                    .build();
            firebaseApp = FirebaseApp.initializeApp(options);
        }

        if (db == null) {
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(credentials)
                    .build();
            db = firestoreOptions.getService();
        }
        if (storage == null) {
            StorageOptions storageOptions = StorageOptions.newBuilder().setCredentials(credentials).build();
            storage = storageOptions.getService();
        }
    }

    public static Firestore getFirestore() {
        if (db == null) {
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize Firestore", e);
            }
        }
        return db;
    }

    public static Storage getStorage() {
        if (storage == null) {
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize Storage", e);
            }
        }
        return storage;
    }

    public static void testFirestoreConnection() {
        try {
            Firestore firestore = getFirestore();
            System.out.println("Firestore Connected");
            for (QueryDocumentSnapshot doc : firestore.collection("users").get().get().getDocuments()) {
                System.out.println("Found user: " + doc.getString("email"));
            }
        } catch (Exception e) {
            System.err.println("Firestore connection failed");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            initialize();
            testFirestoreConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Additional Methods ---

    /**
     * Retrieves a document from a specified collection and document ID.
     *
     * @param collectionName The name of the collection.
     * @param documentId     The ID of the document.
     * @return The DocumentSnapshot if found, null otherwise.
     */
    public static DocumentSnapshot getDocument(String collectionName, String documentId) {
        try {
            DocumentReference docRef = db.collection(collectionName).document(documentId);
            return docRef.get().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adds a new document to a specified collection.
     *
     * @param collectionName The name of the collection.
     * @param data           A Map containing the data to be stored.
     * @return The DocumentReference of the newly created document.
     */
    public static DocumentReference addDocument(String collectionName, Map<String, Object> data) {
        try {
            return db.collection(collectionName).add(data).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Performs a query on a collection with a where clause.
     *
     * @param collectionName The name of the collection.
     * @param field          The field to query on.
     * @param operator       The comparison operator (e.g., "==", ">", "<").
     * @param value          The value to compare against.
     * @return A QuerySnapshot containing the results of the query.
     */
    public static QuerySnapshot queryDocuments(String collectionName, String field, Query.Direction operator, Object value) {
        try {
            Query query = db.collection(collectionName).orderBy(field, operator).whereEqualTo(field, value);
            return query.get().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Performs a query on a collection with a where clause.
     *
     * @param collectionName The name of the collection.
     * @param field          The field to query on.
     * @param operator       The comparison operator (e.g., "==", ">", "<").
     * @param value          The value to compare against.
     * @return A QuerySnapshot containing the results of the query.
     */
    public static QuerySnapshot queryDocuments(String collectionName, String field, Query.Direction operator, String value) {
        try {
            Query query = db.collection(collectionName).orderBy(field, operator).whereEqualTo(field, value);
            return query.get().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a user by their email.
     *
     * @param email The email of the user.
     * @return The DocumentSnapshot of the user if found, null otherwise.
     */
    public static DocumentSnapshot getUserByEmail(String email) {
        try {
            QuerySnapshot querySnapshot = db.collection("users").whereEqualTo("email", email).get().get();
            if (!querySnapshot.isEmpty()) {
                return querySnapshot.getDocuments().get(0);
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user.
     * @return The DocumentSnapshot of the user if found, null otherwise.
     */
    public static DocumentSnapshot getUserByUsername(String username) {
        try {
            QuerySnapshot querySnapshot = db.collection("users").whereEqualTo("username", username).get().get();
            if (!querySnapshot.isEmpty()) {
                return querySnapshot.getDocuments().get(0);
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adds a new message to the "messages" collection.
     *
     * @param message  The message content.
     * @param senderId The ID of the sender.
     * @param username The username of the sender.
     * @return The DocumentReference of the newly created message.
     */
    public static DocumentReference addMessage(String message, String senderId, String username) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("senderId", senderId);
        messageData.put("timestamp", FieldValue.serverTimestamp());
        messageData.put("username", username);
        return addDocument("messages", messageData);
    }

    /**
     * Retrieves the latest messages from the "messages" collection.
     *
     * @param limit The maximum number of messages to retrieve.
     * @return A list of Message objects.
     */
    public static List<Map<String, Object>> getLatestMessages(int limit) {
        try {
            Query query = db.collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit);
            QuerySnapshot querySnapshot = query.get().get();
            List<Map<String, Object>> messages = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                messages.add(doc.getData());
            }
            return messages;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves older messages from the "messages" collection.
     *
     * @param limit          The maximum number of messages to retrieve.
     * @param lastDocumentId The ID of the last document retrieved.
     * @return A list of Message objects.
     */
    public static List<Map<String, Object>> getOlderMessages(int limit, String lastDocumentId) {
        try {
            DocumentSnapshot lastDocument = getDocument("messages", lastDocumentId);
            if (lastDocument == null) {
                return new ArrayList<>();
            }
            Query query = db.collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastDocument)
                    .limit(limit);
            QuerySnapshot querySnapshot = query.get().get();
            List<Map<String, Object>> messages = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                messages.add(doc.getData());
            }
            return messages;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Checks if a username already exists in the "users" collection.
     *
     * @param username The username to check.
     * @return True if the username exists, false otherwise.
     */
    public static boolean usernameExists(String username) {
        return getUserByUsername(username) != null;
    }

    /**
     * Checks if an email already exists in the "users" collection.
     *
     * @param email The email to check.
     * @return True if the email exists, false otherwise.
     */
    public static boolean emailExists(String email) {
        return getUserByEmail(email) != null;
    }

}