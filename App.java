import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.text.*;

public class App extends JPanel implements ActionListener, PropertyChangeListener {
    
    private static JFrame frame;
    private JProgressBar progressBar;
    private JTextPane textPane;
    private StyledDocument doc;
    private final GymSync gymSync = new GymSync();

    public App() {
        super(new BorderLayout());
        buildGui();
        GymSync.setLogCallback(this::logNormalMessage);
        GymSync.setErrorLogCallback(this::logErrorMessage);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "start" -> transferStart();
            case "close" -> System.exit(0);
            case "graph" -> openGraphSafely();
            default -> throw new UnsupportedOperationException("Unsupported command: " + e.getActionCommand());
        }
    }

    private void buildGui() {
        textPane = createTextPane();
        progressBar = createProgressBar();

        JPanel panel1 = createButtonPanel(
            createButton("Transfer", "start"),
            progressBar
        );

        JPanel panel2 = createButtonPanel(
            createButton("Open Graph", "graph"),
            createButton("Close", "close")
        );

        add(panel1, BorderLayout.PAGE_START);
        add(new JScrollPane(textPane), BorderLayout.CENTER);
        add(panel2, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        logNormalMessage(gymSync.toString());
    }

    private JTextPane createTextPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFont(new Font("Consolas", Font.BOLD, 16));
        doc = pane.getStyledDocument();
        return pane;
    }

    private JProgressBar createProgressBar() {
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        return progressBar;
    }

    private JButton createButton(String text, String actionCommand) {
        JButton button = new JButton(text);
        button.setActionCommand(actionCommand);
        button.addActionListener(this);
        return button;
    }

    private JPanel createButtonPanel(JComponent... components) {
        JPanel panel = new JPanel();
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    private void transferStart() {
        clearLog();
        resetProgress();

        logNormalMessage("Looking for file: " + GymSync.fileSource + " on source device: " + GymSync.deviceId + "...\n");

        if (GymSync.deviceCheck()) {
            logNormalMessage("Device has been found...");
            updateProgress(25);

            if (GymSync.fileCheck()) {
                logNormalMessage("File has been found...");
                updateProgress(50);

                GymSync.fileTransfer();
                updateProgress(75);

                GymSync.fileManagement();
                updateProgress(100);
            } else {
                logErrorMessage("Error: File was not found!");
            }
        } else {
            logErrorMessage("Error: Device not found!");
        }
    }

    private void logNormalMessage(String message) {
        logMessage(message, null);
    }

    private void logErrorMessage(String message) {
        progressBar.setIndeterminate(true);
        logMessage(message, getErrorStyle());
    }

    private void logMessage(String message, Style style) {
        try {
            doc.insertString(doc.getLength(), message + "\n", style);
            if (style == null) {
                System.out.println(message);
            } else {
                System.err.println(message);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private Style getErrorStyle() {
        Style errorStyle = textPane.addStyle("Error Style", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
        return errorStyle;
    }

    private void resetProgress() {
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);
    }

    private void updateProgress(int value) {
        progressBar.setValue(value);
    }

    private void clearLog() {
        textPane.setText("");
    }
        
    private void openGraphSafely() {
        try {
            openGraph();
        } catch (BindException e) {
            logErrorMessage("Port 8080 is already in use. Trying to open the browser anyway.");
            openBrowserSafely();
        } catch (Exception e) {
            logAndHandleException("An error occurred while opening the graph", e);
            openBrowserSafely();
        }
    }

    private void openBrowserSafely() {
        try {
            openBrowser();
        } catch (Exception e) {
            logAndHandleException("An error occurred while opening the browser", e);
        }
    }

    private void openBrowser() throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        }
    }

    private void logAndHandleException(String message, Exception e) {
        logErrorMessage(message + ": " + e.getMessage());
        e.printStackTrace();
    }

    private void openGraph() throws IOException, URISyntaxException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new FileHandler());
        server.start();
        logNormalMessage("\nServer started on port 8080");

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        }
    }

    public static void createAndShowGUI() {
        frame = new JFrame("Gym Progress Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new App());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Property changed: " + evt.getPropertyName());
    }

    static class FileHandler implements HttpHandler {
        private static final String ROOT_DIR = "D:/projects/gymRecords";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath().equals("/") ? "/index.html" : exchange.getRequestURI().getPath();
            File file = new File(ROOT_DIR + path);

            if (file.exists() && !file.isDirectory()) {
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(file.toPath(), os);
                }
            } else {
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
