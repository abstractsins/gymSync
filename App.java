import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.net.InetSocketAddress;

import javax.swing.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.nio.file.Files;

import java.io.*;
import java.net.URI;

public class App extends JPanel implements ActionListener, PropertyChangeListener {
    
    private static JFrame frame;
    private JProgressBar progressBar;
    private JTextArea taskOutput;
    
    // GymSync instance
    public GymSync gymSync = new GymSync();
    
    public App() {
        super(new BorderLayout());
        buildGui();
        GymSync.setLogCallback(this::logMessage);  // Set the logging callback method'
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "start" -> transferStart();
            case "close" -> System.exit(0);
            case "graph" -> {
                try {
                   openGraph();
                } catch (Exception ex) {
                    ex.printStackTrace();  // Handle the exception (e.g., log it or show an error message)
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported command: " + e.getActionCommand());
        }
    }

    private void buildGui() {
        taskOutput = createTextArea();
        progressBar = createProgressBar();

        JButton startButton = createButton("Transfer", "start");
        JButton graphButton = createButton("Open Graph", "graph");
        JButton closeButton = createButton("Close", "close");

        JPanel panel1 = new JPanel();
        panel1.add(startButton);
        panel1.add(progressBar);

        JPanel panel2 = new JPanel();
        panel2.add(graphButton);
        panel2.add(closeButton);

        add(panel1, BorderLayout.PAGE_START);
        add(new JScrollPane(taskOutput), BorderLayout.CENTER);
        add(panel2, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    private JTextArea createTextArea() {
        JTextArea textArea = new JTextArea(5, 20);
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        textArea.append(gymSync.toString() + "\n");
        return textArea;
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

    private void transferStart() {
        taskOutput.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        String intro = "Looking for file: " + gymSync.fileSource + " on source device: " + gymSync.deviceId + "...\n";
        logMessage(intro);

        if (GymSync.deviceCheck()) {

            logMessage("Device has been found...");
            progressBar.setValue(25);

            if (GymSync.fileCheck()) {
                logMessage("File has been found...");
                progressBar.setValue(50);
                gymSync.fileTransfer();
                progressBar.setValue(75);
                gymSync.fileManagement();
                progressBar.setValue(100);
            } else {
                logError("Error: File was not found!");
            }
        } else {
            logError("Error: Device not found!");
        }
    }

    public void logMessage(String message, String type) {
        if (type.equals("error")) {
            System.err.println(message);
            taskOutput.append(message + "\n");
        } else {
            System.out.println(message);
        }
        taskOutput.append(message + "\n");
    }

    public void logError(String message) {
        progressBar.setIndeterminate(true);
        logMessage(message, "error");
    }

    private void openGraph() throws Exception {
        // Handle graph opening logic
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String root = "D:/projects/gymRecords";
                String path = exchange.getRequestURI().getPath();

                if (path.equals("/")) {
                    path = "/index.html";
                }

                File file = new File(root + path);
                if (file.exists() && !file.isDirectory()) {
                    exchange.sendResponseHeaders(200, file.length());
                    OutputStream os = exchange.getResponseBody();
                    Files.copy(file.toPath(), os);
                    os.close();
                } else {
                    String response = "404 (Not Found)\n";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        });

        server.start();
        logMessage("Server started on port 8080");

        if(Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080"));
        }
    }

    public static void createAndShowGUI() {
        frame = new JFrame("Gym Progress Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JComponent contentPane = new App();
        frame.setContentPane(contentPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setSize(600, 400);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Property changed: " + evt.getPropertyName());
    }
}
