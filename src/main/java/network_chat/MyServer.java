package network_chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyServer {

    private AuthService authService;
    private List<ClientHandler> clients;

    public static final Logger LOGGER = LogManager.getLogger(MyServer.class);

    public MyServer() {
        try (ServerSocket server = new ServerSocket(ChatConstants.PORT)) {
            authService = new BaseAuthService();
            authService.start();

            ExecutorService executorService = Executors.newCachedThreadPool();

            LOGGER.info("Server is started!");
            clients = new ArrayList<>();
            while (true) {
                LOGGER.info("Wait for clients...");
                Socket socket = server.accept();
                LOGGER.info(socket.getInetAddress() + " client is connected!");
                new ClientHandler(this, socket, executorService);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isNickBusy(String nick) {
        return clients.stream().anyMatch(client -> client.getNick().equals(nick));
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    /**
     * (Дополнительно для упрощения) если лист пустой, то отправим всем. Непустой - отфильтруем
     */
    public synchronized void broadcastMessage(String message, List<String> nicknames) {
        clients.stream()
                .filter(client -> !message.isEmpty() && (nicknames.isEmpty() || nicknames.contains(client.getNick())))
                .forEach(client -> client.sendMsg(message));
    }

    /**
     * Общие информационные сообщения, либо отправка всем
     */
    public synchronized void broadcastMessage(String message) {
        broadcastMessage(message, List.of());
    }

    /**
     * Личные сообщения, либо всем
     */
    public synchronized void broadcastMessage(String message, String fromNick) {
        String[] partsMsg = message.split("\\s+");
        List<String> nicknames = new ArrayList<>();
        String prefix = "[" + fromNick + "]: ";

        // если первое слово /direct
        if (partsMsg[0].equalsIgnoreCase(ChatConstants.DIRECT)) {

            // получатель и отправитель
            nicknames.addAll(List.of(partsMsg[1], fromNick));

            message = "direct: " + prefix + Arrays.stream(partsMsg)
                    .skip(2)
                    .collect(Collectors.joining(" "));

            // если первое слово /list
        } else if (partsMsg[0].equalsIgnoreCase(ChatConstants.SEND_TO_LIST)) {

            // список получателей
            nicknames = Arrays.stream(partsMsg)
                    .skip(1)
                    .takeWhile(e -> !e.equalsIgnoreCase(ChatConstants.SEND_TO_LIST)) // взять все, что между /list.../list
                    .distinct()
                    .collect(Collectors.toList());
            // ... и отправитель
            nicknames.add(fromNick);

            message = "direct: " + prefix + Arrays.stream(partsMsg)
                    .skip(1)
                    .dropWhile(e -> !e.equalsIgnoreCase(ChatConstants.SEND_TO_LIST))
                    .skip(1)
                    .collect(Collectors.joining(" "));
        } else {
            // сообщение всем
            message = prefix + message;
        }

        broadcastMessage(message, nicknames);
    }

    /**
     * Список пользователей в сети. Всем
     */
    public synchronized void broadcastClients() {
        broadcastClients(List.of());
    }

    /**
     * Список пользователей в сети. Тому, кто попросил вывести список
     */
    public synchronized void broadcastClients(List<String> list) {
        String clientsMessage = ChatConstants.CLIENTS_LIST +
                " " +
                clients.stream()
                        .map(ClientHandler::getNick)
                        .collect(Collectors.joining(" "));

        broadcastMessage(clientsMessage + "\n", list);
    }

}
