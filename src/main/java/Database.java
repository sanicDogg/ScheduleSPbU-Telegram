import org.postgresql.util.PSQLException;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Database {
    private final Connection connection;
    private final String table = "users";

    public Database() throws SQLException, URISyntaxException {
        this.connection = getConnection();
    }

    private Connection getConnection() throws SQLException, URISyntaxException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
//                + "?sslmode=require";

        return DriverManager.getConnection(dbUrl, username, password);
    }

    // Добавить пользователя в таблицу
    public void addUser(long chat_id, String username_tg, String group, String userClassJSON) throws SQLException {
        String query = "INSERT INTO " + this.table + " (username_tg, \"group\", chat_id, \"user.class\") " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username_tg);
            pstmt.setString(2, group);
            pstmt.setLong(3, chat_id);
            pstmt.setString(4, userClassJSON);

            pstmt.executeUpdate();
        }
    }

    // Изменить поля пользователя в таблице
    public void editUser(long chat_id, String username_tg, String group, String userClassJSON) throws SQLException {
        // SQL запрос с параметрами-заполнителями
        String query = "UPDATE public.users SET username_tg = ?, \"group\" = ?, \"user.class\" = ? WHERE chat_id = ?";

        // Создание PreparedStatement и установка значений параметров
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username_tg);
            pstmt.setString(2, group);
            pstmt.setString(3, userClassJSON);
            pstmt.setLong(4, chat_id);

            // Выполнение запроса на обновление
            pstmt.executeUpdate();
        }
    }

    // Поиск нужного пользователя (возвращает список из двух элементов - chat_id и JSON)
    public ArrayList<String> findUser(long chat_id) throws SQLException {
        String query = "SELECT * FROM " + this.table + " WHERE chat_id = " + chat_id;
        PreparedStatement statement = this.connection.prepareStatement(query);
        boolean hasResult = statement.execute();

        ArrayList<String> chatJSON = new ArrayList<>();
        // space is nothing... //
        String username = "space";
        String json = "";

        if (hasResult) {
            ResultSet resultSet = statement.getResultSet();
            resultSet.next();
            try {
                username = resultSet.getString("username_tg");
                json = resultSet.getString("user.class");
            } catch (PSQLException e) {
                username = "undefined";
            }
        }

        chatJSON.add(username);
        chatJSON.add(json);
        return chatJSON;
    }

    /**
     * Вернуть словарь всех пользователей из БД
     * @return HashMap ключи - chat_id, значения - json пользователя
     */

    public HashMap<Long, String> getAllUsers() throws SQLException {
        HashMap<Long, String> users = new HashMap<>();

        String query = "SELECT * FROM " + this.table;
        PreparedStatement statement = this.connection.prepareStatement(query);
        boolean hasResult = statement.execute();

        if (hasResult) {
            ResultSet resultSet = statement.getResultSet();
            while (true) {
                resultSet.next();
                try {
                    long chat_id = resultSet.getLong("chat_id");
                    String json = resultSet.getString("user.class");

                    users.put(chat_id, json);
                } catch (PSQLException e) {
                    break;
                }
            }
        }

        return users;
    }
}
