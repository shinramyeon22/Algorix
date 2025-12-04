package Controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.stage.Stage;

public class FileChooser {
    
    public String openFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Java Files", "*.java"),
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath())));
                System.out.println("Loaded file: " + selectedFile.getAbsolutePath());
                return content;
            } catch (Exception e) {
                System.err.println("Error reading file: " + e.getMessage());
                return null;
            }
        }
        return null;
    }
}
 /*
          ███████╗███████╗ █████╗ ███╗   ██╗
          ██╔════╝██╔════╝██╔══██╗████╗  ██║
          ███████╗█████╗  ███████║██╔██╗ ██║
          ╚════██║██╔══╝  ██╔══██║██║╚██╗██║
          ███████║███████╗██║  ██║██║ ╚████║
          ╚══════╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═══╝        
        */

