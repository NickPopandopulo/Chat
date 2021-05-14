package network_chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class ClientApp extends Application {

    private static Scene scene;
    private static FXMLLoader loader;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("client"), 600, 475);
        stage.setScene(scene);
        stage.setTitle("Chat");
        stage.setResizable(false);
        stage.requestFocus();
        stage.show();

        stage.setOnCloseRequest(event -> System.exit(0));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        loader = new FXMLLoader(ClientApp.class.getResource(fxml + ".fxml"));
        return loader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}