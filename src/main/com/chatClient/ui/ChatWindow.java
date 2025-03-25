package main.com.chatClient.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.cloud.Timestamp;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.StorageClient;
import main.com.chatClient.database.FirestoreUtil;
import main.com.chatClient.services.ChatService;
import main.com.chatClient.services.ChatWindowListener;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ChatWindow extends JFrame implements ChatWindowListener {
    private final JTextPane chatArea;
    private final JTextField messageField;
    private final JButton sendButton;
    private final JButton uploadButton;
    private String username;
    private final String email;
    private final ChatService chatService;
    private final List<String> imageExtensions = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 150;
    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;
    private ImageIcon placeholderIcon;
    private LocalDate lastMessageDate = null;

    public ChatWindow(String email, String documentId) {
        // Set Dark Mode Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        this.email = email;
        this.chatService = new ChatService();

        loadPlaceholderIcon();
        this.username = fetchUsername(documentId);

        setTitle("Chat - " + username);
        setSize(500, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        getContentPane().setBackground(new Color(40, 40, 40));

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setContentType("text/html");
        chatArea.setBackground(new Color(60, 60, 60));
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

            if (lastMessageDate == null || !lastMessageDate.isEqual(messageDate)) {
                lastMessageDate = messageDate;
                String formattedDate = messageDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                insertDateSeparator(doc, formattedDate);
            }

            ImageIcon userIcon = placeholderIcon;
            JLabel iconLabel = new JLabel(userIcon);

            MutableAttributeSet iconAttributes = new SimpleAttributeSet();
            StyleConstants.setComponent(iconAttributes, iconLabel);

            SimpleAttributeSet usernameAttributes = new SimpleAttributeSet();
            StyleConstants.setBold(usernameAttributes, true);
            StyleConstants.setForeground(usernameAttributes, Color.WHITE);

            SimpleAttributeSet timeAttributes = new SimpleAttributeSet();
            StyleConstants.setForeground(timeAttributes, Color.GRAY);
            StyleConstants.setFontSize(timeAttributes, 12);

            MutableAttributeSet paragraphAttributes = new SimpleAttributeSet();
            StyleConstants.setLeftIndent(paragraphAttributes, 40f);
            StyleConstants.setSpaceBelow(paragraphAttributes, 5f);
            doc.setParagraphAttributes(doc.getLength(), 0, paragraphAttributes, false);

            SimpleAttributeSet messageBoxAttributes = new SimpleAttributeSet();
            StyleConstants.setBackground(messageBoxAttributes, new Color(68, 68, 68));
            StyleConstants.setLeftIndent(messageBoxAttributes, 5f);
            StyleConstants.setRightIndent(messageBoxAttributes, 5f);
            StyleConstants.setSpaceAbove(messageBoxAttributes, 5f);
            StyleConstants.setSpaceBelow(messageBoxAttributes, 5f);

            doc.insertString(doc.getLength(), " ", iconAttributes);
            doc.insertString(doc.getLength(), "  " + senderUsername + "  ", usernameAttributes);
            doc.insertString(doc.getLength(), formattedTime + "\n", timeAttributes);

            int start = doc.getLength();
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
            int end = doc.getLength();

            doc.setCharacterAttributes(start, end - start, messageBoxAttributes, false);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void insertDateSeparator(StyledDocument doc, String formattedDate) throws BadLocationException {
        SimpleAttributeSet separatorAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(separatorAttributes, Color.GRAY);
        StyleConstants.setBold(separatorAttributes, true);
        StyleConstants.setAlignment(separatorAttributes, StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(separatorAttributes, 10f);
        StyleConstants.setSpaceBelow(separatorAttributes, 10f);

        String separatorLine = "------------------ " + formattedDate + " ------------------";
        doc.insertString(doc.getLength(), separatorLine + "\n", separatorAttributes);
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

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String contentType = Files.probeContentType(selectedFile.toPath());
                String fileName = "user_uploaded_files/" + UUID.randomUUID().toString() + "_" + selectedFile.getName();

                BlobInfo blobInfo = BlobInfo.newBuilder(StorageClient.getInstance().bucket().getName(), fileName)
                        .setContentType(contentType)
                        .build();

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
        String senderDocumentId = chatService.getDocumentIdByUsername(username);
        SwingUtilities.invokeLater(() -> {
            appendMessageToChatArea(username, senderDocumentId, message);
        });
    }

    private void loadInitialMessages() {
        List<Map<String, Object>> messages = FirestoreUtil.getLatestMessages(50);
        if (!messages.isEmpty()) {
            Map<String, Object> firstMessage = messages.get(0);
            LocalDateTime firstMessageDateTime = ((Timestamp) firstMessage.get("timestamp")).toSqlTimestamp().toLocalDateTime();
            lastMessageDate = firstMessageDateTime.toLocalDate();
            for (int i = messages.size() - 1; i >= 0; i--) {
                Map<String, Object> message = messages.get(i);
                String senderId = (String) message.get("senderId");
                String senderUsername = (String) message.get("username");
                String messageContent = (String) message.get("message");
                appendMessageToChatArea(senderUsername, senderId, messageContent);
            }
        } else {
            lastMessageDate = LocalDate.now();
        }
    }
}