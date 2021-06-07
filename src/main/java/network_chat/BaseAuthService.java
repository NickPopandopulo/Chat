package network_chat;

import java.sql.*;
import java.util.Optional;

/**
 * Implementation of the authentication service that runs on the inner list
 */
public class BaseAuthService implements AuthService {

    private final static String DATABASE_URL = "jdbc:sqlite:javadb.db";
    private static Connection connection;
    private static Statement stmt;

    private final static int GET_ID = 0;
    private final static int GET_NICK = 1;

    @Override
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DATABASE_URL);
            stmt = connection.createStatement();

            createTable();
            fillTable();

            System.out.println("BaseAuthService started.");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String dropTableIfExists = "DROP TABLE IF EXISTS Users;";

        stmt.execute(dropTableIfExists);

        String createTable = "CREATE TABLE IF NOT EXISTS Users (" +
                "UserID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "NickName VARCHAR(30) NOT NULL, " +
                "Login VARCHAR(30) NOT NULL, " +
                "Password VARCHAR(30) NOT NULL " +
                ")";

        stmt.execute(createTable);
    }

    private void fillTable() throws SQLException {
        PreparedStatement prepInsert = connection.prepareStatement("INSERT INTO Users (NickName, Login, Password) " +
                "VALUES (?, ?, ?)");

        for (int i = 1; i < 4; i++) {
            prepInsert.setString(1, "nick" + i);
            prepInsert.setString(2, "l" + i);
            prepInsert.setString(3, "p" + i);
            prepInsert.addBatch();
        }

        prepInsert.executeBatch();
    }

    @Override
    public void stop() {
        try {
            stmt.execute("DROP TABLE Users;");
            stmt.close();
            connection.close();
            System.out.println("BaseAuthService stopped.");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public Optional<String> getNickByLoginAndPass(String login, String pass) {
        return getDataByLoginAndPass(login, pass, GET_NICK);
    }

    @Override
    public Optional<String> getIDByLoginAndPass(String login, String pass) {
        return getDataByLoginAndPass(login, pass, GET_ID);
    }

    /**
     * Вспомогательный метод, чтобы получить по логину и паролю либо nick, либо ID
     */
    private Optional<String> getDataByLoginAndPass(String login, String pass, int status) {
        try {
            String selectQuery = "SELECT * FROM USERS WHERE Login = '" + login + "' AND Pass = '" + pass + "';";
            ResultSet rs = stmt.executeQuery(selectQuery);
            if (status == GET_NICK) {
                return Optional.ofNullable(rs.getString("NickName"));
            } else if (status == GET_ID) {
                return Optional.ofNullable(rs.getString("UserID"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public synchronized void changeNick(int id, String newNickName) {
        try {
            String updateQuery = "UPDATE Users SET NickName = '" + newNickName + "' WHERE UserID = " + id + ";";
            stmt.executeUpdate(updateQuery);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
