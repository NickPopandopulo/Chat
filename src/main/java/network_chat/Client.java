package network_chat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
    @FXML
    Button btnSend;
    @FXML
    TextArea txtAreaMsg;
    @FXML
    TextArea txtAreaChat;

    private String msg;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    @FXML
    public void initialize() throws IOException {
        txtAreaMsg.textProperty().addListener((observable, oldValue, newValue) ->
                btnSend.setDisable(observable.getValue().isEmpty()));

        socket = new Socket(ChatConstants.HOST, ChatConstants.PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        Thread thread = new Thread(() -> {
            try {
                // authentication
                while (true) {
                    String strFromServer = in.readUTF();
                    if (strFromServer.equals(ChatConstants.AUTH_SUCCESS)) {
                        break;
                    }
                    txtAreaChat.appendText(strFromServer + "\n");
                }
                // reading from server
                while (true) {
                    String strFromServer = in.readUTF();
                    if (strFromServer.equals(ChatConstants.STOP_WORD)) {
                        break;
                    }
                    txtAreaChat.appendText(strFromServer + "\n");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
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
        try {
            if (!(msg = txtAreaMsg.getText().trim()).isEmpty()) {
                txtAreaMsg.clear();
                out.writeUTF(msg);
                txtAreaChat.setScrollTop(Double.MAX_VALUE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
