package network_chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class Client {
    private static final int HISTORY_LIMIT = 10;
    private static final String END_LINE = "   --- End of session ---";

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
    private File fileHistory;
    private BufferedWriter bwToFileHistory;

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
                    if (strFromServer.startsWith(ChatConstants.AUTH_SUCCESS)) {
                        prepareHistoryInput(strFromServer);
                        break;
                    }
                    txtAreaChat.appendText(strFromServer + "\n");
                }

                txtAreaChat.clear();
                loadHistory();

                // reading from server
                while (true) {
                    String strFromServer = in.readUTF();
                    if (strFromServer.equals(ChatConstants.STOP_WORD)) {
                        break;
                    }
                    txtAreaChat.appendText(strFromServer + "\n");
                    bwToFileHistory.write(strFromServer + "\n");
                    bwToFileHistory.flush();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    bwToFileHistory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Platform.exit();
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

    /**
     * Подготовка файла с историей и открытие потока для ввода в файл
     */
    private void prepareHistoryInput(String strFromServer) throws IOException {
        String[] parts = strFromServer.split("\\s+");
        fileHistory = new File("history_" + parts[1] + ".txt");

        if (!fileHistory.exists()) fileHistory.createNewFile(); // проверка на существование файла с историей

        bwToFileHistory = new BufferedWriter(new FileWriter(fileHistory, true));
    }

    /**
     * Сохранение истории. Добавляются в файл новые строки чата, которых не было до этого в файле
     */
    private void saveHistory() {

        try {
            if (!fileHistory.exists()) fileHistory.createNewFile(); // проверка на существование файла с историей
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileHistory, true))) {

            /**
             * Собирается лист из текущих строк чата
             */
            List<String> list = Arrays.stream(txtAreaChat.getText().split("\\n"))
                    .collect(Collectors.toCollection(ArrayList::new));

            /**
             * Проходим итератором по листу и фиксируем номер строки End of session.
             * На выходе из цикла получаем номер самой нижней этой строки
             */
            Iterator<String> iter = list.iterator();
            int endLinePos = 0;
            for (int i = 1; i <= list.size(); i++) {
                if (iter.next().equals(END_LINE)) {
                    endLinePos = i;
                }
            }

            /**
             * Собираем только то, что находится после последней строки End of session.
             * Т.е. только историю в текущей сессии
             */
            bw.write(list.stream()
                    .skip(endLinePos)
                    .collect(Collectors.joining("\n"))
            );
            bw.write("\n" + END_LINE + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загрузка истории чата после успешной авторизации
     */
    private void loadHistory() {
        if (!fileHistory.exists()) return;

        try {
            /**
             * Количество строк в файле с историей
             */
            long countLines = Files.lines(fileHistory.toPath()).count();

            /**
             * Если в файле с историей, много строк (больше HISTORY_LIMIT),
             * то добавляем в начало чата фразу: ... n more lines
             * где n — количество строк, которые есть в файле истории, но в чат уже не влезают
             */
            if (countLines > HISTORY_LIMIT) {
                txtAreaChat.appendText("... " + (countLines - HISTORY_LIMIT) + " more lines\n");
            } else {
                countLines = HISTORY_LIMIT;
            }

            /**
             * Собираем из файла с историей n = HISTORY_LIMIT последних строк
             */
            Files.lines(fileHistory.toPath())
                    .skip(countLines - HISTORY_LIMIT)
                    .forEach(str -> txtAreaChat.appendText(str + "\n"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * Ниже показываю, что умею считывать стандартным способом
         * (правда здесь очень простая реализация)
         */
//        String s;
//        try (BufferedReader bw = new BufferedReader(new FileReader(file))) {
//            while ((s = bw.readLine()) != null) {
//                txtAreaChat.appendText(s + "\n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

}
