package com.chatClient.ui;

import com.chatClient.database.FirestoreUtil;
import com.chatClient.models.Message;
import com.chatClient.services.ChatService;
import com.chatClient.services.ChatWindowListener;
import com.formdev.flatlaf.FlatDarkLaf;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Query;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.StorageClient;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ChatWindow extends JFrame implements ChatWindowListener {
    private final JTextPane chatArea;
    private final JTextField messageField;
    private final JButton sendButton;
    private final JButton uploadButton;
    private String username;
    private final String email;
    private final String documentId;
    private final ChatService chatService;
    private final List<String> imageExtensions = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 150;
    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;
    private ImageIcon placeholderIcon;
    private LocalDate lastMessageDate = null;
    private final Map<String, ImageIcon> iconCache = new HashMap<>();
    private String lastMessageId = null;

    public ChatWindow(String email, String documentId) {
        // Set Dark Mode Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        this.email = email;
        this.documentId = documentId;
        this.chatService = new ChatService();

        loadPlaceholderIcon();
        // Fetch username from Firestore before opening the chat
        this.username = fetchUsername(documentId);

        setTitle("Chat - " + username);
        setSize(500, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Set background color for the JFrame
        getContentPane().setBackground(new Color(40, 40, 40)); // Dark gray

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setContentType("text/html");
        chatArea.setBackground(new Color(60, 60, 60)); // Slightly lighter dark gray for contrast
        chatArea.setForeground(Color.WHITE);
        Font chatFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        chatArea.setFont(chatFont);
        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int pos = chatArea.viewToModel2D(e.getPoint());
                if (pos >= 0) {
                    StyledDocument doc = chatArea.getStyledDocument();
                    Element element = doc.getCharacterElement(pos);
                    AttributeSet as = element.getAttributes();
                    String fileName = (String) as.getAttribute("hyperlink");
                    if (fileName != null) {
                        try {
                            String url = "https://firebasestorage.googleapis.com/v0/b/fine-rite-443512-n6.appspot.com/o/" + fileName.replace("/", "%2F") + "?alt=media";
                            Desktop.getDesktop().browse(new URL(url).toURI());
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(40, 40, 40));

        messageField = new JTextField();
        sendButton = new JButton("Send");
        uploadButton = new JButton("Upload");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(uploadButton, BorderLayout.WEST);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        uploadButton.addActionListener(e -> uploadFile());

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        chatService.addMessageListener(this);
        chatArea.setText("");

        // Load initial messages
        loadInitialMessages();

        setLocationRelativeTo(null);
        setVisible(true);
    }

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
                placeholderIcon = new ImageIcon(new ImageIcon(placeholderStream.readAllBytes()).getImage().getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH));
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

    private void appendMessageToChatArea(String senderUsername, String senderDocumentId, String message) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate messageDate = now.toLocalDate();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = now.format(timeFormatter);

        try {
            StyledDocument doc = chatArea.getStyledDocument();
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, Color.WHITE);

            // Date/Time Separator
            if (lastMessageDate == null || !lastMessageDate.isEqual(messageDate)) {
                lastMessageDate = messageDate;
                String formattedDate = messageDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                insertDateSeparator(doc, formattedDate);
            }

            // User Icon
            ImageIcon userIcon = getUserIcon(senderDocumentId);
            JLabel iconLabel = new JLabel(userIcon);

            MutableAttributeSet iconAttributes = new SimpleAttributeSet();
            StyleConstants.setComponent(iconAttributes, iconLabel);

            // Username (Bold) and Time (Smaller, Normal)
            SimpleAttributeSet usernameAttributes = new SimpleAttributeSet();
            StyleConstants.setBold(usernameAttributes, true);
            StyleConstants.setForeground(usernameAttributes, Color.WHITE);

            SimpleAttributeSet timeAttributes = new SimpleAttributeSet();
            StyleConstants.setForeground(timeAttributes, Color.GRAY);
            StyleConstants.setFontSize(timeAttributes, 12); // Smaller font size for time

            // Create a paragraph element for the message
            MutableAttributeSet paragraphAttributes = new SimpleAttributeSet();
            StyleConstants.setLeftIndent(paragraphAttributes, 40f); // Indent the message text
            StyleConstants.setSpaceBelow(paragraphAttributes, 5f); // Add space below the paragraph
            doc.setParagraphAttributes(doc.getLength(), 0, paragraphAttributes, false);

            // Insert the icon, username, and time
            doc.insertString(doc.getLength(), " ", iconAttributes);
            doc.insertString(doc.getLength(), "  " + senderUsername + "  ", usernameAttributes);
            doc.insertString(doc.getLength(), formattedTime + "\n", timeAttributes);

            // Check if the message contains a dot (.) - a simple way to identify potential filenames
            if (message.contains(".")) {
                String fileExtension = message.substring(message.lastIndexOf(".") + 1).toLowerCase();
                String originalFileName = getOriginalFileName(message);
                if (imageExtensions.contains(fileExtension)) {
                    addImagePreview(message, doc);
                } else {
                    MutableAttributeSet linkAttributes = new SimpleAttributeSet();
                    StyleConstants.setForeground(linkAttributes, Color.CYAN);
                    StyleConstants.setUnderline(linkAttributes, true);
                    linkAttributes.addAttribute("hyperlink", message);
                    doc.insertString(doc.getLength(), originalFileName + "\n", attributes);
                    doc.insertString(doc.getLength(), "[Download File]", linkAttributes);
                    doc.setCharacterAttributes(doc.getLength() - ("[Download File]").length(), ("[Download File]").length(), linkAttributes, false);
                    doc.insertString(doc.getLength(), "\n", attributes);
                }
            } else {
                doc.insertString(doc.getLength(), message + "\n", attributes);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void insertDateSeparator(StyledDocument doc, String formattedDate) throws BadLocationException {
        SimpleAttributeSet separatorAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(separatorAttributes, Color.GRAY);
        StyleConstants.setBold(separatorAttributes, true);
        // Use HTML to create a full-width line
        String separatorHtml = "<div style='width: 100%; text-align: center; margin: 10px 0; color: gray;'>------------------ " + formattedDate + " ------------------</div>";
        doc.insertString(doc.getLength(), separatorHtml, separatorAttributes);
    }

    private String getOriginalFileName(String fullFileName) {
        String[] parts = fullFileName.split("_", 2);
        if (parts.length > 1) {
            return parts[1];
        }
        return fullFileName;
    }

    private void addImagePreview(String fileName, StyledDocument doc) throws BadLocationException {
        try {
            String url = "https://firebasestorage.googleapis.com/v0/b/fine-rite-443512-n6.appspot.com/o/" + fileName.replace("/", "%2F") + "?alt=media";
            ImageIcon imageIcon = new ImageIcon(new URL(url));
            Image image = imageIcon.getImage();

            double widthRatio = (double) PREVIEW_WIDTH / image.getWidth(null);
            double heightRatio = (double) PREVIEW_HEIGHT / image.getHeight(null);
            double scaleRatio = Math.min(widthRatio, heightRatio);

            int newWidth = (int) (image.getWidth(null) * scaleRatio);
            int newHeight = (int) (image.getHeight(null) * scaleRatio);

            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            ImageIcon scaledImageIcon = new ImageIcon(scaledImage);
            JLabel imageLabel = new JLabel(scaledImageIcon);
            MutableAttributeSet imageAttributes = new SimpleAttributeSet();
            StyleConstants.setComponent(imageAttributes, imageLabel);
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            doc.insertString(doc.getLength(), " ", imageAttributes);
            imageAttributes.addAttribute("hyperlink", fileName);
            doc.setCharacterAttributes(doc.getLength() - 1, 1, imageAttributes, false);
            doc.insertString(doc.getLength(), "\n", attributes);
            doc.insertString(doc.getLength(), " \n", attributes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fetchUsername(String documentId) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(documentId);
            return userRecord.getDisplayName();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String fetchUserIconUrl(String documentId) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(documentId);
            return userRecord.getPhotoUrl();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ImageIcon getUserIcon(String userDocumentId) {
        // Check if the icon is in the cache
        if (iconCache.containsKey(userDocumentId)) {
            return iconCache.get(userDocumentId);
        }

        // If not in the cache, fetch it
        String iconUrl = fetchUserIconUrl(userDocumentId);
        ImageIcon userIcon;
        if (iconUrl != null) {
            try {
                userIcon = new ImageIcon(new URL(iconUrl));
                if (userIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                    userIcon = placeholderIcon;
                }
            } catch (IOException e) {
                userIcon = placeholderIcon;
            }
        } else {
            userIcon = placeholderIcon;
        }

        // Scale the icon and add it to the cache
        if (userIcon != null) {
            Image scaledImage = userIcon.getImage().getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH);
            userIcon = new ImageIcon(scaledImage);
            iconCache.put(userDocumentId, userIcon);
        }

        return userIcon;
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String contentType = Files.probeContentType(selectedFile.toPath());
                // Upload to Firebase Storage
                String fileName = "user_uploaded_files/" + UUID.randomUUID().toString() + "_" + selectedFile.getName();

                // Create BlobInfo with content type
                BlobInfo blobInfo = BlobInfo.newBuilder(StorageClient.getInstance().bucket().getName(), fileName)
                        .setContentType(contentType)
                        .build();

                // Upload the file
                StorageClient.getInstance().bucket().getStorage().create(blobInfo, Files.readAllBytes(selectedFile.toPath()));

                chatService.sendMessage(email, username, fileName);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to upload file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onNewMessage(String username, String message) {
        // Corrected line: Use getEmailByUsername() instead of getUsernameByEmail()
        String senderDocumentId = chatService.getDocumentIdByUsername(username);
        SwingUtilities.invokeLater(() -> {
            appendMessageToChatArea(username, senderDocumentId, message);
        });
    }

    private void loadInitialMessages() {
        List<Map<String, Object>> messages = FirestoreUtil.getLatestMessages(50);
        if (!messages.isEmpty()) {
            lastMessageId = messages.get(0).get("timestamp").toString();
        }
        for (Map<String, Object> message : messages) {
            String senderId = (String) message.get("senderId");
            String senderUsername = (String) message.get("username");
            String messageContent = (String) message.get("message");
            appendMessageToChatArea(senderUsername, senderId, messageContent);
        }
    }
}