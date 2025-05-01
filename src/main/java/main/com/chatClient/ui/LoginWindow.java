package main.com.chatClient.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import main.com.chatClient.auth.FirebaseAuthClient;
import main.com.chatClient.config.FirebaseConfig;
import main.com.chatClient.database.FirestoreUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Login and registration window for the chat application.
 */
public class LoginWindow extends JFrame {
    // Login panel components
    private JTextField emailFieldLogin;
    private JPasswordField passwordFieldLogin;
    private JButton loginButton;
    private JLabel switchToRegisterLabel;
    
    // Register panel components
    private JTextField emailFieldRegister;
    private JPasswordField passwordFieldRegister;
    private JButton registerButton;
    private JLabel switchToLoginLabel;
    
    // Panel management
    private JPanel loginPanel;
    private JPanel registerPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private String username;

    // Status indicator
    private JLabel statusLabel;

    /**
     * Creates the login/register window.
     */
    public LoginWindow() {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }

        // Load application icon
        loadAppIcon();

        // Set up window properties
        setTitle("Chat App - Login/Register");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(40, 40, 40));

        // Set up card layout for login/register panels
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(40, 40, 40));

        // Create panels
        loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();

        // Add panels to main panel
        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");

        // Show main panel
        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
        
        // Show login panel by default
        cardLayout.show(mainPanel, "login");
    }

    /**
     * Loads application icon from resources.
     */
    private void loadAppIcon() {
        try {
            InputStream iconStream = getClass().getClassLoader().getResourceAsStream("chat.png");
            if (iconStream != null) {
                ImageIcon icon = new ImageIcon(new ImageIcon(iconStream.readAllBytes()).getImage()
                        .getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                setIconImage(icon.getImage());
                iconStream.close();
            } else {
                System.err.println("Icon not found in resources");
            }
        } catch (IOException e) {
            System.err.println("Failed to load icon: " + e.getMessage());
        }
    }

    /**
     * Creates the login panel.
     * 
     * @return Login panel
     */
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 20, 2, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Email field
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(emailLabel, gbc);

        emailFieldLogin = new JTextField();
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(emailFieldLogin, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        passwordFieldLogin = new JPasswordField();
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(passwordFieldLogin, gbc);

        // Login button
        loginButton = new JButton("Login");
        gbc.insets = new Insets(20, 20, 2, 20);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(loginButton, gbc);

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.ORANGE);
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(statusLabel, gbc);

        // Register link
        switchToRegisterLabel = new JLabel("<html><div style='text-align: center;'>Don't have an account? <a href='#'>Register</a></div></html>");
        switchToRegisterLabel.setForeground(Color.CYAN);
        switchToRegisterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(switchToRegisterLabel, gbc);

        // Add event listeners
        loginButton.addActionListener(e -> login());
        
        switchToRegisterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cardLayout.show(mainPanel, "register");
                clearStatus();
            }
        });
        
        passwordFieldLogin.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }
            }
        });

        return panel;
    }

    /**
     * Creates the registration panel.
     * 
     * @return Registration panel
     */
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 20, 2, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Email field
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(emailLabel, gbc);

        emailFieldRegister = new JTextField();
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(emailFieldRegister, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(passwordLabel, gbc);

        passwordFieldRegister = new JPasswordField();
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(passwordFieldRegister, gbc);

        // Register button
        registerButton = new JButton("Register");
        gbc.insets = new Insets(20, 20, 2, 20);
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(registerButton, gbc);
        
        // Status label
        JLabel registerStatusLabel = new JLabel(" ");
        registerStatusLabel.setForeground(Color.ORANGE);
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(registerStatusLabel, gbc);

        // Login link
        switchToLoginLabel = new JLabel("<html><div style='text-align: center;'>Already have an account? <a href='#'>Login</a></div></html>");
        switchToLoginLabel.setForeground(Color.CYAN);
        switchToLoginLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(switchToLoginLabel, gbc);

        // Add event listeners
        registerButton.addActionListener(e -> register());
        
        switchToLoginLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cardLayout.show(mainPanel, "login");
                clearStatus();
            }
        });
        
        passwordFieldRegister.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    register();
                }
            }
        });

        return panel;
    }

    /**
     * Clears status messages.
     */
    private void clearStatus() {
        if (statusLabel != null) {
            statusLabel.setText(" ");
        }
    }

    /**
     * Sets a status message.
     * 
     * @param message Status message
     * @param isError Whether the message is an error
     */
    private void setStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setForeground(isError ? Color.RED : Color.GREEN);
        }
    }

    /**
     * Handles user login.
     */
    private void login() {
        try {
            // Get login credentials
            String email = emailFieldLogin.getText().trim();
            String password = new String(passwordFieldLogin.getPassword()).trim();

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                setStatus("Please enter email and password", true);
                return;
            }

            // Disable login button and show status
            loginButton.setEnabled(false);
            setStatus("Logging in...", false);

            // Attempt login
            Map<String, String> loginResult = FirebaseAuthClient.loginUser(email, password);
            if (loginResult != null) {
                String idToken = loginResult.get("idToken");
                String uid = loginResult.get("uid");
                
                // Initialize Firebase with the token
                FirebaseConfig.initialize(idToken);
                
                // Get username from Firestore
                String username = email.substring(0, email.indexOf('@'));
                
                // If username is null (not found in Firestore), use email prefix
                if (username == null) {
                    username = email.substring(0, email.indexOf('@'));
                }
                
                // Success - open chat window
                setStatus("Login successful!", false);
                JOptionPane.showMessageDialog(this, "Welcome back, " + username + "!");
                this.dispose();
                new ChatWindow(uid, username);
            } else {
                // Login failed
                setStatus("Invalid email or password", true);
                loginButton.setEnabled(true);
            }
        } catch (Exception e) {
            // Handle errors
            setStatus("Login error: " + e.getMessage(), true);
            loginButton.setEnabled(true);
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles user registration.
     */
    private void register() {
        try {
            // Get registration data
            String email = emailFieldRegister.getText().trim();
            String password = new String(passwordFieldRegister.getPassword()).trim();

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validate password length
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Disable register button
            registerButton.setEnabled(false);

            // Attempt registration
            String username =   email.substring(0, email.indexOf('@'));
            Map<String, String> registerResult = FirebaseAuthClient.registerUser(email, password, username);
            if (registerResult != null) {
                String idToken = registerResult.get("idToken");
                String uid = registerResult.get("uid");
                
                // Initialize Firebase with the token
                FirebaseConfig.initialize(idToken);
                
                // Create user profile in Firestore
                boolean userCreated = FirestoreUtil.addUser(uid, username, email);
                
                if (userCreated) {
                    // Registration successful
                    JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Switch to login panel
                    cardLayout.show(mainPanel, "login");
                    
                    // Pre-fill login fields
                    emailFieldLogin.setText(email);
                    passwordFieldLogin.setText("");
                } else {
                    // User creation failed
                    JOptionPane.showMessageDialog(this, "Account created but failed to store user profile", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                // Registration failed
                JOptionPane.showMessageDialog(this, "Registration failed. Email may already be in use.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
            // Re-enable register button
            registerButton.setEnabled(true);
        } catch (Exception e) {
            // Handle errors
            JOptionPane.showMessageDialog(this, "Registration error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            registerButton.setEnabled(true);
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Application entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginWindow::new);
    }
}