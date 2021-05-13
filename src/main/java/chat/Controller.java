package chat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Controller {
    @FXML
    Button btnSend;
    @FXML
    TextArea txtAreaMsg;
    @FXML
    TextArea txtAreaChat;

    String msg;

    @FXML
    public void initialize() {
        txtAreaMsg.textProperty().addListener((observable, oldValue, newValue) ->
                btnSend.setDisable(observable.getValue().isEmpty()));
    }

    @FXML
    public void btnClicked(ActionEvent actionEvent) {
        sendMessage();
    }

    @FXML
    public void txtAreaSendMsg(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            keyEvent.consume();
            sendMessage();
        }
    }

    private void sendMessage() {
        if (!(msg = txtAreaMsg.getText().trim()).isEmpty()) {
            txtAreaMsg.clear();
            txtAreaChat.appendText("[You]: " + msg + "\n");
            txtAreaChat.setScrollTop(Double.MAX_VALUE);
        }
    }
}
