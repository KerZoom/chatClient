package com.chatClient.ui;

import com.chatClient.auth.FirebaseAuthClient;
import com.chatClient.database.FirestoreUtil;
import com.formdev.flatlaf.FlatDarkLaf;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;


public class LoginWindow extends JFrame {
    private JTextField emailFieldLogin;
    private JPasswordField passwordFieldLogin;
    private JButton loginButton;
    private JLabel switchToRegisterLabel; // Changed to JLabel

    private JTextField emailFieldRegister;
    private JTextField usernameField;
    private JPasswordField passwordFieldRegister;
    private JButton registerButton;
    private JLabel switchToLoginLabel; // Changed to JLabel
    private JPanel loginPanel;
    private JPanel registerPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private String documentId;

    public LoginWindow() {
        // Set Dark Mode Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // Load Custom Icon
        try {
            InputStream iconStream = getClass().getClassLoader().getResourceAsStream("chat.png");
            if (iconStream != null) {
                ImageIcon icon = new ImageIcon(new ImageIcon(iconStream.readAllBytes()).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                setIconImage(icon.getImage());
            } else {
                System.err.println("Icon not found");
            }
        } catch (IOException e) {
            System.err.println("Failed to load icon");
            e.printStackTrace();
        }

        setTitle("Chat App - Login/Register");
        setSize(400, 300); // Fixed size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set background color for the JFrame
        getContentPane().setBackground(new Color(40, 40, 40)); // Dark gray

        // CardLayout for switching between panels
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(40, 40, 40));

        // Create Login Panel
        loginPanel = createLoginPanel();
        mainPanel.add(loginPanel, "login");

        // Create Register Panel
        registerPanel = createRegisterPanel();
        mainPanel.add(registerPanel, "register");

        add(mainPanel);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Email
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(emailLabel, gbc);

        emailFieldLogin = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(emailFieldLogin, gbc);

        // Password
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passwordLabel, gbc);

        passwordFieldLogin = new JPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(passwordFieldLogin, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(40, 40, 40));
        loginButton = new JButton("Login");
        buttonPanel.add(loginButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        // Switch to Register Label
        switchToRegisterLabel = new JLabel("<html><u>Don't have an account? Register</u></html>");
        switchToRegisterLabel.setForeground(Color.CYAN);
        switchToRegisterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(switchToRegisterLabel, gbc);

        // Button Actions
        loginButton.addActionListener(e -> login());
        switchToRegisterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cardLayout.show(mainPanel, "register");
            }
        });

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Username
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(usernameLabel, gbc);

        usernameField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(usernameField, gbc);

        // Email
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(emailLabel, gbc);

        emailFieldRegister = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(emailFieldRegister, gbc);

        // Password
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        passwordFieldRegister = new JPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(passwordFieldRegister, gbc);

        // Register Button
        registerButton = new JButton("Register");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(registerButton, gbc);

        // Switch to Login Label
        switchToLoginLabel = new JLabel("<html><u>Already have an account? Login</u></html>");
        switchToLoginLabel.setForeground(Color.CYAN);
        switchToLoginLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(switchToLoginLabel, gbc);

        // Button Actions
        registerButton.addActionListener(e -> register());
        switchToLoginLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cardLayout.show(mainPanel, "login");
            }
        });

        return panel;
    }

    private void login() {
        String email = emailFieldLogin.getText().trim();
        String password = new String(passwordFieldLogin.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter email and password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, String> loginResult = FirebaseAuthClient.loginUser(email, password);
        if (loginResult != null) {
            String idToken = loginResult.get("idToken");
            this.documentId = loginResult.get("documentId");
            JOptionPane.showMessageDialog(this, "Login successful!");
            this.dispose();
            new ChatWindow(email, documentId);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void register() {
        String username = usernameField.getText().trim();
        String email = emailFieldRegister.getText().trim();
        String password = new String(passwordFieldRegister.getPassword()).trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username, email and password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (FirebaseAuthClient.usernameExists(username)) {
            JOptionPane.showMessageDialog(this, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (FirebaseAuthClient.emailExists(email)) {
            JOptionPane.showMessageDialog(this, "Email already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            try {
                this.documentId = FirebaseAuth.getInstance().createUser(new com.google.firebase.auth.UserRecord.CreateRequest().setEmail(email).setPassword(password)).getUid();
            } catch (FirebaseAuthException e) {
                JOptionPane.showMessageDialog(this, "Failed to create user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String idToken = FirebaseAuthClient.registerUser(email, password, username, null);
        if (idToken != null) {
            JOptionPane.showMessageDialog(this, "Registration successful!");
            cardLayout.show(mainPanel, "login");
        } else {
            JOptionPane.showMessageDialog(this, "Registration failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            FirestoreUtil.initialize();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize Firestore");
        }
        SwingUtilities.invokeLater(LoginWindow::new);
    }
}