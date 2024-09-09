import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GymSync {

    // Hard coded device ID we want to match
    static final String deviceId = "R3CR702TVAH"; 
    // File name to transfer
    static final String fileSource = "Gym.txt";
    // Android directory to check
    static final String androidSource = "/sdcard/Documents/";
    // Local directory to copy to
    static final String destinationPath = "D:/Projects/gymRecords/";
    // Local file name - file name to rename
    static final String localFile = "gymRecords.txt"; 

    private static boolean deviceCheck() {
        Pattern pattern = Pattern.compile(deviceId);

        try {
            Process process = new ProcessBuilder("adb", "devices").start();
            
            try (InputStreamReader inputStream = new InputStreamReader(process.getInputStream());
                 BufferedReader reader = new BufferedReader(inputStream)) {
                 
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return true;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Process terminated with non-zero exit code: " + exitCode);
            }

        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Process was interrupted: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        return false;
    }

    private static boolean fileCheck() {
        Pattern pattern = Pattern.compile(fileSource);
        String command = "adb -s " + deviceId + " shell ls " + androidSource;
        String[] commandArray = command.split(" ");

        try {
            Process process = new ProcessBuilder(commandArray).start();
            
            try (InputStreamReader inputStream = new InputStreamReader(process.getInputStream());
                 BufferedReader reader = new BufferedReader(inputStream)) {
                 
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return true;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Process terminated with non-zero exit code: " + exitCode);
            }

        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Process was interrupted: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        return false;
    }

    private static void fileTransfer() {
        System.out.println(">>> Initiating file transfer.");

        try {
            String command = "adb -s " + deviceId + " pull " + androidSource + fileSource + " " + destinationPath;
            Process process = new ProcessBuilder(command.split(" ")).start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("File transfer failed with exit code: " + exitCode);
            } else {
                System.out.println("File transfer complete.");
            }

        } catch (IOException e) {
            System.err.println("IOException occurred during file transfer: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("File transfer was interrupted: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private static void oldFileRename() {
        Path filePath = Paths.get(destinationPath + localFile);
        
        try {
            BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
            FileTime creationTime = attributes.creationTime();
            LocalDate creationDate = Instant.ofEpochMilli(creationTime.toMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
            String backupFileName = "gymRecords " + creationDate + ".txt";
            Path backupFilePath = Paths.get(destinationPath + backupFileName);

            Files.move(filePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("+++ Old file renamed successfully: " + backupFileName);

        } catch (IOException e) {
            System.err.println("IOException occurred while renaming the old file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void newFileRename() {
        Path filePath = Paths.get(destinationPath + fileSource);
        
        try {
            Path newFilePath = Paths.get(destinationPath + localFile);
            Files.move(filePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("+++ New file renamed successfully: " + localFile);

        } catch (IOException e) {
            System.err.println("IOException occurred while renaming the new file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void removeAndroidFile() { 
        String command = "adb -s " + deviceId + " shell rm " + androidSource + fileSource;
        try {
            Process process = new ProcessBuilder(command.split(" ")).start();
            process.waitFor(); // Ensure the process completes
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                System.out.println("+++ Android file removed successfully: " + fileSource);
            } else {
                System.err.println("Failed to remove Android file with exit code: " + exitCode);
            }
        } catch (IOException e) {
            System.err.println("IOException occurred while removing Android file: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("File removal was interrupted: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private static void fileManagement() {
        oldFileRename();
        newFileRename();
        removeAndroidFile();
    }
    
    public String toString() {
        String str = "--- Looking for file: " + fileSource;
        str += "\n--- on source device: " + deviceId;
        return str;
    }
        
    public static void main(String[] args) {
        System.out.println("##### PROGRAM START #####");
        GymSync gymSync = new GymSync();
        System.out.println(gymSync);
        
        boolean isDeviceConnected = deviceCheck();
        if (isDeviceConnected) {
            System.out.println(">>> Device has been found.");
            boolean doesFileExist = fileCheck();
            if (doesFileExist) {
                System.out.println(">>> File has been found.");
                fileTransfer();
                fileManagement();
            } else {
                System.out.println("!!! File was not found!");
            }
        } else {
            System.out.println("!!! Device not found.");
        }
        
        System.out.println("##### PROGRAM END #####");
    }
}
