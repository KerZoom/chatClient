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
      private JTextField emailFieldLogin;
    private JPasswordField passwordFieldLogin;
    private JButton loginButton;
    private JLabel switchToRegisterLabel;
    
      private JTextField emailFieldRegister;
    private JPasswordField passwordFieldRegister;
    private JButton registerButton;
    private JLabel switchToLoginLabel;
    
      private JPanel loginPanel;
    private JPanel registerPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private String username;

      private JLabel statusLabel;

    /**
     * Creates the login/register window.
     */
    public LoginWindow() {
              try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }

              loadAppIcon();

              setTitle("Chat App - Login/Register");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(40, 40, 40));

              cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(40, 40, 40));

              loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();

              mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");

              add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
        
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

              JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(emailLabel, gbc);

        emailFieldLogin = new JTextField();
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(emailFieldLogin, gbc);

              JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        passwordFieldLogin = new JPasswordField();
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(passwordFieldLogin, gbc);

              loginButton = new JButton("Login");
        gbc.insets = new Insets(20, 20, 2, 20);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(loginButton, gbc);

              statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.ORANGE);
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(statusLabel, gbc);

              switchToRegisterLabel = new JLabel("<html><div style='text-align: center;'>Don't have an account? <a href='#'>Register</a></div></html>");
        switchToRegisterLabel.setForeground(Color.CYAN);
        switchToRegisterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(switchToRegisterLabel, gbc);

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

              JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(emailLabel, gbc);

        emailFieldRegister = new JTextField();
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(emailFieldRegister, gbc);

              JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(passwordLabel, gbc);

        passwordFieldRegister = new JPasswordField();
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(passwordFieldRegister, gbc);

              registerButton = new JButton("Register");
        gbc.insets = new Insets(20, 20, 2, 20);
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(registerButton, gbc);
        
              JLabel registerStatusLabel = new JLabel(" ");
        registerStatusLabel.setForeground(Color.ORANGE);
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(registerStatusLabel, gbc);

              switchToLoginLabel = new JLabel("<html><div style='text-align: center;'>Already have an account? <a href='#'>Login</a></div></html>");
        switchToLoginLabel.setForeground(Color.CYAN);
        switchToLoginLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(switchToLoginLabel, gbc);

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
                      String email = emailFieldLogin.getText().trim();
            String password = new String(passwordFieldLogin.getPassword()).trim();

                      if (email.isEmpty() || password.isEmpty()) {
                setStatus("Please enter email and password", true);
                return;
            }

                      loginButton.setEnabled(false);
            setStatus("Logging in...", false);

                      Map<String, String> loginResult = FirebaseAuthClient.loginUser(email, password);
            if (loginResult != null) {
                String idToken = loginResult.get("idToken");
                String uid = loginResult.get("uid");
                
                              FirebaseConfig.initialize(idToken);
                
                              String username = email.substring(0, email.indexOf('@'));
                
                              if (username == null) {
                    username = email.substring(0, email.indexOf('@'));
                }
                
                              setStatus("Login successful!", false);
                JOptionPane.showMessageDialog(this, "Welcome back, " + username + "!");
                this.dispose();
                new ChatWindow(uid, username);
            } else {
                              setStatus("Invalid email or password", true);
                loginButton.setEnabled(true);
            }
        } catch (Exception e) {
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
                      String email = emailFieldRegister.getText().trim();
            String password = new String(passwordFieldRegister.getPassword()).trim();

                      if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
                      if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

                      registerButton.setEnabled(false);

                      String username =   email.substring(0, email.indexOf('@'));
            Map<String, String> registerResult = FirebaseAuthClient.registerUser(email, password, username);
            if (registerResult != null) {
                String idToken = registerResult.get("idToken");
                String uid = registerResult.get("uid");
                
                              FirebaseConfig.initialize(idToken);
                
                              boolean userCreated = FirestoreUtil.addUser(uid, username, email);
                
                if (userCreated) {
                                      JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                                      cardLayout.show(mainPanel, "login");
                    
                                      emailFieldLogin.setText(email);
                    passwordFieldLogin.setText("");
                } else {
                                      JOptionPane.showMessageDialog(this, "Account created but failed to store user profile", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                              JOptionPane.showMessageDialog(this, "Registration failed. Email may already be in use.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
                      registerButton.setEnabled(true);
        } catch (Exception e) {
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