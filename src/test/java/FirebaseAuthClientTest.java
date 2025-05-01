import com.fasterxml.jackson.databind.node.ObjectNode;
import main.com.chatClient.auth.FirebaseAuthClient;
import main.com.chatClient.config.FirebaseConfig;
import main.com.chatClient.database.FirestoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
public class FirebaseAuthClientTest {

    private MockedStatic<FirebaseAuthClient> firebaseAuthClientStaticMock;
    private MockedStatic<FirestoreUtil> firestoreUtilMock;
    private MockedStatic<FirebaseConfig> firebaseConfigMock; // <-- Added

    @BeforeEach
    void setUp() {
        // Mock FirebaseConfig first if FirestoreUtil depends on it during static initialization
        firebaseConfigMock = Mockito.mockStatic(FirebaseConfig.class);
        firebaseConfigMock.when(FirebaseConfig::getFirestore).thenReturn(null); // Prevent actual DB connection

        // Mock FirestoreUtil
        firestoreUtilMock = Mockito.mockStatic(FirestoreUtil.class);
        // Prevent FirestoreUtil.addUser from doing anything real in tests
        firestoreUtilMock.when(() -> FirestoreUtil.addUser(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> null);

        // Mock FirebaseAuthClient static methods we want to control/verify
        firebaseAuthClientStaticMock = Mockito.mockStatic(FirebaseAuthClient.class);
        // Make sure the actual logic of login/register is called unless specifically mocked otherwise
        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.loginUser(anyString(), anyString()))
                .thenCallRealMethod();
        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.registerUser(anyString(), anyString(), anyString()))
                .thenCallRealMethod();
        // Mock the underlying network call
        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.sendPostRequest(anyString(), any(ObjectNode.class)))
                .thenReturn(null); // Default mock behavior (can be overridden per test)
    }

    @AfterEach
    void tearDown() {
        firebaseAuthClientStaticMock.close();
        firestoreUtilMock.close();
        firebaseConfigMock.close(); // <-- Added
    }

    @Test
    void testLoginUser_Success() {
        String email = "test@example.com";
        String password = "password123";
        String expectedUid = "user123";
        String expectedToken = "tokenXYZ";
        String successResponseJson = String.format(
                "{\"kind\":\"identitytoolkit#VerifyPasswordResponse\",\"localId\":\"%s\",\"email\":\"%s\",\"displayName\":\"\",\"idToken\":\"%s\",\"registered\":true,\"refreshToken\":\"refreshXYZ\",\"expiresIn\":\"3600\"}",
                expectedUid, email, expectedToken
        );

        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.sendPostRequest(
                        startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"),
                        any(ObjectNode.class)
                ))
                .thenReturn(successResponseJson);

        Map<String, String> result = FirebaseAuthClient.loginUser(email, password);
        assertNotNull(result, "Login result should not be null on success");
        assertEquals(expectedToken, result.get("idToken"), "idToken should match");
        assertEquals(expectedUid, result.get("uid"), "uid should match");

        firebaseAuthClientStaticMock.verify(() -> FirebaseAuthClient.sendPostRequest(
                startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"),
                any(ObjectNode.class)
        ));
        firestoreUtilMock.verifyNoInteractions();
    }

    @Test
    void testLoginUser_Failure_InvalidPassword() {
        String email = "test@example.com";
        String password = "wrongpassword";
        String failureResponseJson =
                "{\"error\":{\"code\":400,\"message\":\"INVALID_PASSWORD\",\"errors\":[{\"message\":\"INVALID_PASSWORD\",\"domain\":\"global\",\"reason\":\"invalid\"}]}}";

        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.sendPostRequest(
                        startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"),
                        any(ObjectNode.class)
                ))
                .thenReturn(failureResponseJson);

        Map<String, String> result = FirebaseAuthClient.loginUser(email, password);
        assertNull(result, "Login result should be null on API error");

        firebaseAuthClientStaticMock.verify(() -> FirebaseAuthClient.sendPostRequest(
                startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"),
                any(ObjectNode.class)
        ));
        firestoreUtilMock.verifyNoInteractions();
    }

    @Test
    void testLoginUser_Failure_NetworkError() {
        String email = "test@example.com";
        String password = "password123";

        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.sendPostRequest(
                        startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"),
                        any(ObjectNode.class)
                ))
                .thenReturn(null);

        Map<String, String> result = FirebaseAuthClient.loginUser(email, password);

        assertNull(result, "Login result should be null on network error");

        firebaseAuthClientStaticMock.verify(() -> FirebaseAuthClient.sendPostRequest(
                startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"),
                any(ObjectNode.class)
        ));
        firestoreUtilMock.verifyNoInteractions();
    }


    @Test
    void testRegisterUser_Success() {
        String email = "new@example.com";
        String password = "newpassword";
        String username = "NewUser";
        String expectedUid = "newUser456";
        String expectedToken = "newTokenABC";
        String successResponseJson = String.format(
                "{\"kind\":\"identitytoolkit#SignupNewUserResponse\",\"idToken\":\"%s\",\"email\":\"%s\",\"refreshToken\":\"refreshABC\",\"expiresIn\":\"3600\",\"localId\":\"%s\"}",
                expectedToken, email, expectedUid
        );

        // Specific mock for this test case
        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.sendPostRequest(
                        startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signUp"),
                        any(ObjectNode.class)
                ))
                .thenReturn(successResponseJson);

        // Call the method under test
        Map<String, String> result = FirebaseAuthClient.registerUser(email, password, username); // Get the Map

        // Assertions
        assertNotNull(result, "Registration result should not be null on success"); // Check map isn't null
        assertEquals(expectedToken, result.get("idToken"), "idToken should match"); // Check idToken in map
        assertEquals(expectedUid, result.get("uid"), "uid should match"); // Check uid in map

        // Verifications
        firebaseAuthClientStaticMock.verify(() -> FirebaseAuthClient.sendPostRequest(
                startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signUp"),
                any(ObjectNode.class)
        ));
        // Verify FirestoreUtil.addUser was called with the UID, username, and email
        firestoreUtilMock.verify(() -> FirestoreUtil.addUser(eq(expectedUid), eq(username), eq(email)));
    }

    @Test
    void testRegisterUser_Failure_EmailExists() {
        String email = "existing@example.com";
        String password = "password123";
        String username = "ExistingUser";
        String failureResponseJson =
                "{\"error\":{\"code\":400,\"message\":\"EMAIL_EXISTS\",\"errors\":[{\"message\":\"EMAIL_EXISTS\",\"domain\":\"global\",\"reason\":\"invalid\"}]}}";

        // Specific mock for this test case
        firebaseAuthClientStaticMock.when(() -> FirebaseAuthClient.sendPostRequest(
                        startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signUp"),
                        any(ObjectNode.class)
                ))
                .thenReturn(failureResponseJson);

        // Call the method under test
        Map<String, String> result = FirebaseAuthClient.registerUser(email, password, username); // Get the result (should be null)

        // Assertion
        assertNull(result, "Registration result should be null when email exists"); // Check the map itself is null

        // Verifications
        firebaseAuthClientStaticMock.verify(() -> FirebaseAuthClient.sendPostRequest(
                startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signUp"),
                any(ObjectNode.class)
        ));
        // Verify FirestoreUtil.addUser was NOT called
        firestoreUtilMock.verifyNoInteractions();
    }

    @Test
    void testRegisterUser_Failure_NetworkError() {
        String email = "new@example.com";
        String password = "newpassword";
        String username = "NewUser";

        // sendPostRequest already mocked to return null by default in setUp

        // Call the method under test
        Map<String, String> result = FirebaseAuthClient.registerUser(email, password, username); // Get the result (should be null)

        // Assertion
        assertNull(result, "Registration result should be null on network error"); // Check the map itself is null

        // Verifications
        firebaseAuthClientStaticMock.verify(() -> FirebaseAuthClient.sendPostRequest(
                startsWith("https://identitytoolkit.googleapis.com/v1/accounts:signUp"),
                any(ObjectNode.class)
        ));
        // Verify FirestoreUtil.addUser was NOT called
        firestoreUtilMock.verifyNoInteractions();
    }
}