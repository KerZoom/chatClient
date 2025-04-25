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

public class LoginWindow extends JFrame {
    private JTextField emailFieldLogin;
    private JPasswordField passwordFieldLogin;
    private JButton loginButton;
    private JLabel switchToRegisterLabel;
    private JTextField emailFieldRegister;
    private JTextField usernameField;
    private JPasswordField passwordFieldRegister;
    private JButton registerButton;
    private JLabel switchToLoginLabel;
    private JPanel loginPanel;
    private JPanel registerPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private String uid;
    private String username;

    public LoginWindow() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

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
        setSize(400, 300);
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
    }

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

        switchToRegisterLabel = new JLabel("<html><div style='text-align: center;'>Don't have an account? <a href='#'>Register</a></div></html>");
        switchToRegisterLabel.setForeground(Color.CYAN);
        switchToRegisterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(switchToRegisterLabel, gbc);

        loginButton.addActionListener(e -> login());
        switchToRegisterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cardLayout.show(mainPanel, "register");
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


    // This a comment
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 20, 2, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(usernameLabel, gbc);

        usernameField = new JTextField();
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(usernameField, gbc);

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
        gbc.gridy = 7;
        panel.add(registerButton, gbc);

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
            this.uid = loginResult.get("uid");
            System.out.println("LoginWindow " + loginResult);
            FirebaseConfig.initialize(idToken);
            JOptionPane.showMessageDialog(this, "Login successful!");
            this.dispose();
            new ChatWindow(email, uid, username);
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

        String idToken = FirebaseAuthClient.registerUser(email, password, username);
        System.out.println("RegisterWindow " + idToken);
        if (idToken != null) {
            FirebaseConfig.initialize(idToken);
            FirestoreUtil.addUser(idToken, username, email);
        }

        if (idToken != null) {
            JOptionPane.showMessageDialog(this, "Registration successful!");
            cardLayout.show(mainPanel, "login");
        } else {
            JOptionPane.showMessageDialog(this, "Registration failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginWindow::new);
    }
}