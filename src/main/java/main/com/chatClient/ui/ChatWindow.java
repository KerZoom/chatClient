package main.com.chatClient.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.cloud.Timestamp;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import main.com.chatClient.config.FirebaseConfig;
import main.com.chatClient.database.FirestoreUtil;
import main.com.chatClient.services.ChatService;
import main.com.chatClient.services.ChatWindowListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Main chat window for the application.
 */
public class ChatWindow extends JFrame implements ChatWindowListener {

      private final JPanel messagesPanel;
    private final JScrollPane scrollPane;
    private final JTextField messageField;
    private final JButton sendButton;
    private final JButton uploadButton;
    private final JLabel statusLabel;
    private JLabel loadingMoreLabel;

      private final String uid;
    private final String username;

      private final ChatService chatService;

      private final List<String> imageExtensions = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
    private static final int ICON_WIDTH = 32;
    private static final int ICON_HEIGHT = 32;
    private final Map<String, ImageIcon> userIconCache = new HashMap<>();

      private static final int MESSAGES_PER_PAGE = 20;
    private Timestamp oldestMessageTimestamp = null;
    private boolean allMessagesLoaded = false;
    private boolean isLoadingMore = false;

    /**
     * Creates a new chat window.
     *
     * @param uid      User's UID
     * @param username User's display name
     */
    public ChatWindow(String uid, String username) {
              try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex.getMessage());
        }

              this.uid = uid;
        this.username = username;

              this.chatService = new ChatService();

              setTitle("Chat - " + username);
        setSize(800, 600);
        setMinimumSize(new Dimension(500, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(40, 40, 40));

              addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupResources();
            }
        });

              messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(new Color(40, 40, 40));

              loadingMoreLabel = new JLabel("Loading older messages...", SwingConstants.CENTER);
        loadingMoreLabel.setForeground(Color.LIGHT_GRAY);
        loadingMoreLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        loadingMoreLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        loadingMoreLabel.setVisible(false);

              JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(loadingMoreLabel, BorderLayout.CENTER);
        messagesPanel.add(topPanel);

              scrollPane = new JScrollPane(messagesPanel);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

              scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!e.getValueIsAdjusting() && !isLoadingMore && !allMessagesLoaded) {
                JScrollBar scrollBar = (JScrollBar) e.getAdjustable();
                if (scrollBar.getValue() <= scrollBar.getVisibleAmount() / 4) {
                    loadMoreMessages();
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);

              JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(50, 50, 50));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusLabel = new JLabel("Connected as " + username);
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.NORTH);

              JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(new Color(50, 50, 50));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        sendButton = new JButton("Send");
        sendButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        uploadButton = new JButton("Upload");
        uploadButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        inputPanel.add(uploadButton, BorderLayout.WEST);
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

              sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        uploadButton.addActionListener(e -> uploadFile());

              chatService.addMessageListener(this);
        loadInitialMessages();

              setLocationRelativeTo(null);
        setVisible(true);

              messageField.requestFocusInWindow();
    }

    /**
     * Cleans up resources when the window is closed.
     */
    private void cleanupResources() {
        chatService.removeMessageListener(this);
        System.out.println("Chat window closed, resources cleaned up");
    }

    /**
     * Generates a user icon based on the username.
     *
     * @param username The username to base the icon on
     * @return ImageIcon with the user's initial
     */
    private ImageIcon generateUserIcon(String username) {
              if (userIconCache.containsKey(username)) {
            return userIconCache.get(username);
        }

        BufferedImage icon = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();

              int hash = username.hashCode();
        Color userColor = new Color(
                Math.abs(hash) % 200 + 55,
                Math.abs(hash >> 8) % 200 + 55,
                Math.abs(hash >> 16) % 200 + 55
        );

              g2d.setColor(userColor);
        g2d.fillOval(0, 0, ICON_WIDTH, ICON_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(initial);
        int textHeight = fm.getHeight();

        g2d.drawString(initial,
                (ICON_WIDTH - textWidth) / 2,
                (ICON_HEIGHT - textHeight) / 2 + fm.getAscent());

        g2d.dispose();
        ImageIcon userIcon = new ImageIcon(icon);
        userIconCache.put(username, userIcon);
        return userIcon;
    }

    /**
     * Sends a message.
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            setStatus("Sending message...");
            boolean success = chatService.sendMessage(uid, username, message);

            if (success) {
                messageField.setText("");
                messageField.requestFocusInWindow();
            } else {
                setStatus("Failed to send message. Please try again.");
            }
        }
    }

    /**
     * Updates the status label.
     *
     * @param status Status message
     */
    private void setStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    /**
     * Creates a message panel.
     *
     * @param senderUsername   Sender's username
     * @param messageTimestamp Message timestamp
     * @param message          Message content
     * @return Message panel
     */
    private JPanel createMessagePanel(String senderUsername, LocalDateTime messageTimestamp, String message) {
              JPanel messagePanel = new JPanel(new BorderLayout(10, 0));
        messagePanel.setBackground(new Color(50, 50, 50));
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

              boolean isCurrentUser = senderUsername.equals(username);
        if (isCurrentUser) {
            messagePanel.setBackground(new Color(60, 70, 80));
        }

              JLabel iconLabel = new JLabel(generateUserIcon(senderUsername));
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
        usernameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        topRow.add(usernameLabel, BorderLayout.WEST);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        JLabel timeLabel = new JLabel(messageTimestamp.format(timeFormatter));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        topRow.add(timeLabel, BorderLayout.EAST);

        textPanel.add(topRow);

              if (message.startsWith("file:")) {
                      addFileContent(textPanel, message);
        } else {
                      JTextArea messageArea = createMessageArea(message);
            textPanel.add(messageArea);
        }

        messagePanel.add(textPanel, BorderLayout.CENTER);
        return messagePanel;
    }

    /**
     * Adds file content to a message panel.
     *
     * @param textPanel Panel to add content to
     * @param message   File message
     */
    private void addFileContent(JPanel textPanel, String message) {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) {
            JTextArea messageArea = createMessageArea("Invalid file attachment");
            textPanel.add(messageArea);
            return;
        }

        String originalName = parts[1];
        String filePath = parts[2];

        String extension = "";
        if (originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }

        String bucketName = FirebaseConfig.STORAGE_BUCKET;
        String encodedFilePath = URLEncoder.encode(filePath, StandardCharsets.UTF_8).replace("+", "%20");
        String downloadUrl = "https://storage.googleapis.com/" + bucketName + "/" + encodedFilePath;

        if (imageExtensions.contains(extension)) {
            addImageContent(textPanel, downloadUrl);
        } else {
            addFileLink(textPanel, originalName, downloadUrl);
        }
    }

    /**
     * Adds an image to a message panel.
     *
     * @param textPanel Panel to add image to
     * @param imageUrl  Image URL
     */
    private void addImageContent(JPanel textPanel, String imageUrl) {
        JLabel imageLabel = new JLabel("Loading image...");
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        textPanel.add(imageLabel);

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    URL url = new URL(imageUrl.replace(" ", "%20"));
                    Image image = ImageIO.read(url);

                    if (image == null) {
                        return null;
                    }

                    int width = image.getWidth(null);
                    int maxWidth = 300;
                    if (width > maxWidth) {
                        double scale = (double) maxWidth / width;
                        int newHeight = (int) (image.getHeight(null) * scale);
                        Image scaledImage = image.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaledImage);
                    }
                    return new ImageIcon(image);
                } catch (Exception e) {
                    System.err.println("Failed to load image: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        imageLabel.setText(null);
                        imageLabel.setIcon(icon);
                    } else {
                        imageLabel.setText("Failed to load image");
                    }
                } catch (Exception e) {
                    imageLabel.setText("Failed to load image");
                    System.err.println("Error displaying image: " + e.getMessage());
                }
            }
        }.execute();

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(imageUrl));
                } catch (Exception ex) {
                    System.err.println("Failed to open image in browser: " + ex.getMessage());
                }
            }
        });
    }

    /**
     * Adds a file link to a message panel.
     *
     * @param textPanel Panel to add link to
     * @param fileName  File name
     * @param fileUrl   File URL
     */
    private void addFileLink(JPanel textPanel, String fileName, String fileUrl) {
        JLabel linkLabel = new JLabel("<html><a href='#'>" + fileName + "</a></html>");
        linkLabel.setForeground(new Color(100, 150, 255));
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(fileUrl));
                } catch (Exception ex) {
                    System.err.println("Failed to open file in browser: " + ex.getMessage());
                    JOptionPane.showMessageDialog(
                            ChatWindow.this,
                            "Could not open file. The URL has been copied to clipboard.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);

                    StringSelection stringSelection = new StringSelection(fileUrl);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                }
            }
        });

        textPanel.add(linkLabel);
    }

    /**
     * Adds a message panel to the chat.
     *
     * @param senderUsername   Sender's username
     * @param messageTimestamp Message timestamp
     * @param message          Message content
     */
    private void addMessagePanel(String senderUsername, LocalDateTime messageTimestamp, String message) {
        JPanel messagePanel = createMessagePanel(senderUsername, messageTimestamp, message);
        messagesPanel.add(messagePanel);
        messagesPanel.add(Box.createVerticalStrut(5));

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });

        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    /**
     * Creates a text area for a message.
     *
     * @param message Message text
     * @return Text area
     */
    private JTextArea createMessageArea(String message) {
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(new Color(60, 60, 60));
        messageArea.setForeground(Color.WHITE);
        messageArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messageArea.setBorder(null);
        messageArea.setMargin(new Insets(5, 0, 0, 0));
        return messageArea;
    }

    /**
     * Uploads a file.
     */
    private void uploadFile() {
        if (!FirebaseConfig.isInitialized()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot upload file: Not connected to Firebase",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            setStatus("Upload failed: Firebase not initialized");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file to upload");
        fileChooser.setFileSystemView(FileSystemView.getFileSystemView());
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Images & Documents",
                "png", "jpg", "jpeg", "gif", "webp", "pdf", "doc", "docx", "txt"
        );
        fileChooser.setFileFilter(filter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        long fileSize = selectedFile.length();
        long maxFileSize = 5 * 1024 * 1024; // 5MB

        if (fileSize > maxFileSize) {
            JOptionPane.showMessageDialog(
                    this,
                    "File is too large. Maximum file size is 5MB.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        setStatus("Uploading file: " + selectedFile.getName() + "...");
        uploadButton.setEnabled(false);
        sendButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    String contentType = Files.probeContentType(selectedFile.toPath());
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }

                    String fileName = "user_uploaded_files/" + UUID.randomUUID() + "_" + selectedFile.getName();
                    String originalName = selectedFile.getName();
                    String disposition = "attachment; filename=\"" + originalName + "\"";

                    Storage storage = FirebaseConfig.getStorage();
                    System.out.println("Uploading to bucket: " + FirebaseConfig.STORAGE_BUCKET + ", Path: " + fileName);
                    BlobInfo blobInfo = BlobInfo.newBuilder(FirebaseConfig.STORAGE_BUCKET, fileName)
                            .setContentType(contentType)
                            .setContentDisposition(disposition)
                            .setMetadata(Collections.singletonMap("uploadedBy", "chat-app-service"))
                            .setAcl(Collections.singletonList(
                                    com.google.cloud.storage.Acl.of(
                                            com.google.cloud.storage.Acl.User.ofAllUsers(),
                                            com.google.cloud.storage.Acl.Role.READER)))
                            .build();

                    Objects.requireNonNull(storage).create(blobInfo, Files.readAllBytes(selectedFile.toPath()));
                    System.out.println("File uploaded successfully: " + fileName);
                    return chatService.sendMessage(uid, username, "file:" + originalName + ":" + fileName);
                } catch (StorageException e) {
                    System.err.println("Storage error uploading file: HTTP " + e.getCode() + ", Reason: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                } catch (Exception e) {
                    System.err.println("General error uploading file: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        setStatus("File uploaded successfully");
                    } else {
                        setStatus("Failed to upload file");
                        JOptionPane.showMessageDialog(
                                ChatWindow.this,
                                "Failed to upload file. Please check your connection or Firebase configuration.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    setStatus("Error uploading file");
                    JOptionPane.showMessageDialog(
                            ChatWindow.this,
                            "Error uploading file: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } finally {
                    uploadButton.setEnabled(true);
                    sendButton.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Handles new messages.
     *
     * @param senderUsername Sender's username
     * @param message        Message content
     */
    @Override
    public void onNewMessage(String senderUsername, String message) {
        SwingUtilities.invokeLater(() -> {
            addMessagePanel(senderUsername, LocalDateTime.now(), message);
            setStatus("Connected as " + username);
        });
    }

    /**
     * Loads initial messages.
     */
    private void loadInitialMessages() {
        setStatus("Loading messages...");

        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return FirestoreUtil.getRecentMessages(MESSAGES_PER_PAGE);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> messages = get();
                    displayMessages(messages);

                    if (messages.size() < MESSAGES_PER_PAGE) {
                        allMessagesLoaded = true;
                    }

                    if (messages.isEmpty()) {
                        setStatus("No messages yet. Send one to start chatting!");
                    } else {
                        setStatus("Connected as " + username);
                    }

                    SwingUtilities.invokeLater(() -> {
                        JScrollBar vertical = scrollPane.getVerticalScrollBar();
                        vertical.setValue(vertical.getMaximum());
                    });
                } catch (Exception e) {
                    System.err.println("Error loading messages: " + e.getMessage());
                    setStatus("Error loading messages. Please restart the application.");
                }
            }
        }.execute();
    }

    /**
     * Loads more (older) messages.
     */
    private void loadMoreMessages() {
        if (isLoadingMore || allMessagesLoaded || oldestMessageTimestamp == null) {
            return;
        }

        isLoadingMore = true;
        loadingMoreLabel.setVisible(true);
        loadingMoreLabel.setText("Loading older messages...");

        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                Thread.sleep(300);
                return FirestoreUtil.getOlderMessages(oldestMessageTimestamp, MESSAGES_PER_PAGE);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> messages = get();

                    if (messages.isEmpty()) {
                        allMessagesLoaded = true;
                        loadingMoreLabel.setText("No more messages");
                        Timer timer = new Timer(2000, e -> loadingMoreLabel.setVisible(false));
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        int scrollValue = scrollPane.getVerticalScrollBar().getValue();
                        int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();

                        System.out.println("Loaded " + messages.size() + " older messages");
                        displayOlderMessages(messages);

                        if (messages.size() < MESSAGES_PER_PAGE) {
                            allMessagesLoaded = true;
                        }

                        SwingUtilities.invokeLater(() -> {
                            JScrollBar vertical = scrollPane.getVerticalScrollBar();
                            int newMax = vertical.getMaximum();
                            vertical.setValue(scrollValue + (newMax - scrollMax));
                        });
                    }
                } catch (Exception b) {
                    System.err.println("Error loading more messages: " + b.getMessage());
                    loadingMoreLabel.setText("Error loading more messages");
                    Timer timer = new Timer(2000, e -> {
                        loadingMoreLabel.setVisible(false);
                    });
                    timer.setRepeats(false);
                    timer.start();
                } finally {
                    isLoadingMore = false;
                }
            }
        }.execute();
    }

    /**
     * Displays messages in the chat window.
     *
     * @param messages List of messages to display
     */
    private void displayMessages(List<Map<String, Object>> messages) {
        System.out.println("displayMessages: messages.size() = " + messages.size());
        for (Map<String, Object> message : messages) {
            System.out.println("displayMessages: message = " + message);
        }

        while (messagesPanel.getComponentCount() > 1) {
            messagesPanel.remove(1);
        }

        int insertIndex = 1;
        for (Map<String, Object> message : messages) {
            String senderUsername = (String) message.get("username");
            String messageContent = (String) message.get("message");
            Timestamp timestamp = (Timestamp) message.get("timestamp");

            LocalDateTime messageTime = (timestamp != null)
                    ? timestamp.toSqlTimestamp().toLocalDateTime()
                    : LocalDateTime.now();

            JPanel messagePanel = createMessagePanel(senderUsername, messageTime, messageContent);
            messagesPanel.add(messagePanel, insertIndex++);
            messagesPanel.add(Box.createVerticalStrut(5), insertIndex++);
        }

        if (!messages.isEmpty()) {
            oldestMessageTimestamp = (Timestamp) messages.get(0).get("timestamp");
        }

        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    /**
     * Displays older messages above existing messages.
     *
     * @param messages List of older messages to display
     */
    private void displayOlderMessages(List<Map<String, Object>> messages) {
        System.out.println("displayOlderMessages: messages.size() = " + messages.size());
        for (Map<String, Object> message : messages) {
            System.out.println("displayOlderMessages: message = " + message);
        }

        int insertIndex = 1;
        for (Map<String, Object> message : messages) {
            String senderUsername = (String) message.get("username");
            String messageContent = (String) message.get("message");
            Timestamp timestamp = (Timestamp) message.get("timestamp");

            LocalDateTime messageTime = (timestamp != null)
                    ? timestamp.toSqlTimestamp().toLocalDateTime()
                    : LocalDateTime.now();

            JPanel messagePanel = createMessagePanel(senderUsername, messageTime, messageContent);
            messagesPanel.add(messagePanel, insertIndex++);
            messagesPanel.add(Box.createVerticalStrut(5), insertIndex++);
        }

        if (!messages.isEmpty()) {
            oldestMessageTimestamp = (Timestamp) messages.get(0).get("timestamp");
            System.out.println("New oldest timestamp: " + oldestMessageTimestamp);
        }

        loadingMoreLabel.setVisible(false);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
}
