package main.com.chatClient.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.cloud.Timestamp;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import main.com.chatClient.config.FirebaseConfig;
import main.com.chatClient.database.FirestoreUtil;
import main.com.chatClient.services.ChatService;
import main.com.chatClient.services.ChatWindowListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ChatWindow extends JFrame implements ChatWindowListener {

    private final JPanel messagesPanel;
    private final JScrollPane scrollPane;
    private final JTextField messageField;
    private final JButton sendButton;
    private final JButton uploadButton;

    private final String username;
    private final String email;
    private final ChatService chatService;
    private final List<String> imageExtensions = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");

    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;
    private ImageIcon placeholderIcon;
    private boolean allMessagesLoaded = false;

    public ChatWindow(String email, String documentId, String username) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        this.email = email;
        this.chatService = new ChatService();
        this.username = username;

        loadPlaceholderIcon();

        setTitle("Chat - " + username);
        setSize(500, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(40, 40, 40));

        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(new Color(40, 40, 40));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(40, 40, 40));
        messageField = new JTextField();
        sendButton = new JButton("Send");
        uploadButton = new JButton("Upload");
        inputPanel.add(uploadButton, BorderLayout.WEST);
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
        //yes
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        uploadButton.addActionListener(e -> uploadFile());

        chatService.addMessageListener(this);
        loadAllMessages();

        setLocationRelativeTo(null);
        setVisible(true);
    }
    //Test

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatService.sendMessage(email, username, message);
            messageField.setText("");
        }
    }

    private void loadPlaceholderIcon() {
        try {
            InputStream placeholderStream = getClass().getClassLoader().getResourceAsStream("placeholder.png");
            if (placeholderStream != null) {
                Image image = new ImageIcon(placeholderStream.readAllBytes()).getImage();
                placeholderIcon = new ImageIcon(image.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH));
            } else {
                System.err.println("Placeholder icon not found");
                placeholderIcon = null;
            }
        } catch (IOException e) {
            System.err.println("Failed to load placeholder icon");
            e.printStackTrace();
            placeholderIcon = null;
        }
    }

    private JPanel createMessagePanel(String senderUsername, LocalDateTime messageTimestamp, String message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(new Color(60, 60, 60));
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel iconLabel = new JLabel(placeholderIcon);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        leftPanel.add(iconLabel);
        messagePanel.add(leftPanel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel usernameLabel = new JLabel(senderUsername);
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        topRow.add(usernameLabel, BorderLayout.WEST);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        JLabel timeLabel = new JLabel(messageTimestamp.format(timeFormatter));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        topRow.add(timeLabel, BorderLayout.EAST);
        textPanel.add(topRow);

        if (message.startsWith("file:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String originalName = parts[1];
                String filePath = parts[2];
                String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
                String bucketName = FirebaseConfig.STORAGE_BUCKET;
                String encodedFilePath = null;
                encodedFilePath = URLEncoder.encode(filePath, StandardCharsets.UTF_8).replace("+", "%20");
                String downloadUrl = "https://storage.googleapis.com/" + bucketName + "/" + encodedFilePath;

                if (imageExtensions.contains(extension)) {
                    JLabel imageLabel = new JLabel("Loading image...");
                    imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    textPanel.add(imageLabel);

                    new SwingWorker<ImageIcon, Void>() {
                        @Override
                        protected ImageIcon doInBackground() throws Exception {
                            URL url = new URL(downloadUrl);
                            Image image = ImageIO.read(url);
                            int width = image.getWidth(null);
                            int maxWidth = 200;
                            if (width > maxWidth) {
                                double scale = (double) maxWidth / width;
                                int newHeight = (int) (image.getHeight(null) * scale);
                                Image scaledImage = image.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                                return new ImageIcon(scaledImage);
                            }
                            return new ImageIcon(image);
                        }

                        @Override
                        protected void done() {
                            try {
                                imageLabel.setText(null);
                                imageLabel.setIcon(get());
                            } catch (Exception e) {
                                imageLabel.setText("Failed to load image");
                            }
                        }
                    }.execute();

                    imageLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            try {
                                Desktop.getDesktop().browse(new URI(downloadUrl));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                } else {
                    JLabel linkLabel = new JLabel("<html><a href='#'>" + originalName + "</a></html>");
                    linkLabel.setForeground(Color.BLUE);
                    linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    linkLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            try {
                                Desktop.getDesktop().browse(new URI(downloadUrl));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    textPanel.add(linkLabel);
                }
            } else {
                JTextArea messageArea = createMessageArea(message);
                textPanel.add(messageArea);
            }
        } else {
            JTextArea messageArea = createMessageArea(message);
            textPanel.add(messageArea);
        }

        messagePanel.add(textPanel, BorderLayout.CENTER);
        return messagePanel;
    }

    private void addMessagePanel(String senderUsername, LocalDateTime messageTimestamp, String message, boolean isNewMessage) {
        JPanel messagePanel = createMessagePanel(senderUsername, messageTimestamp, message);
        if (isNewMessage) {
            messagesPanel.add(messagePanel);
            messagesPanel.add(Box.createVerticalStrut(10));
        } else {
            messagesPanel.add(messagePanel, 0);
            messagesPanel.add(Box.createVerticalStrut(10), 0);
        }
        messagesPanel.revalidate();
        if (isNewMessage) {
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
    }

    private JTextArea createMessageArea(String message) {
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(new Color(60, 60, 60));
        messageArea.setForeground(Color.WHITE);
        messageArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        messageArea.setBorder(null);
        messageArea.setMargin(new Insets(5, 0, 0, 0));
        return messageArea;
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String contentType = Files.probeContentType(selectedFile.toPath());
                String fileName = "user_uploaded_files/" + UUID.randomUUID().toString() + "_" + selectedFile.getName();
                String originalName = selectedFile.getName();

                String disposition = "attachment; filename=\"" + originalName + "\"";

                Storage storage = FirebaseConfig.getStorage();
                BlobInfo blobInfo = BlobInfo.newBuilder(FirebaseConfig.STORAGE_BUCKET, fileName)
                        .setContentType(contentType)
                        .setContentDisposition(disposition)
                        .setAcl(Arrays.asList(com.google.cloud.storage.Acl.of(com.google.cloud.storage.Acl.User.ofAllUsers(), com.google.cloud.storage.Acl.Role.READER)))
                        .build();

                storage.create(blobInfo, Files.readAllBytes(selectedFile.toPath()));
                chatService.sendMessage(email, username, "file:" + originalName + ":" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to upload file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onNewMessage(String senderUsername, String message) {
        SwingUtilities.invokeLater(() -> {
            addMessagePanel(senderUsername, LocalDateTime.now(), message, true);
        });
    }

    private void loadInitialMessages() {
        loadAllMessages();
    }

    private void loadAllMessages() {
        List<Map<String, Object>> messages = FirestoreUtil.getAllMessages();
        if (!messages.isEmpty()) {
            Collections.reverse(messages);
            for (Map<String, Object> message : messages) {
                String senderId = (String) message.get("senderId");
                String senderUsername = (String) message.get("username");
                String messageContent = (String) message.get("message");
                LocalDateTime messageTimestamp = ((Timestamp) message.get("timestamp")).toSqlTimestamp().toLocalDateTime();
                addMessagePanel(senderUsername, messageTimestamp, messageContent, false);
            }
        } else {
            allMessagesLoaded = true;
        }
    }
}