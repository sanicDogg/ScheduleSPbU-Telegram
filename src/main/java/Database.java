import org.postgresql.util.PSQLException;

import java.sql.*;
import java.util.ArrayList;

public class Database {
    private Connection connection;
    private String table = "users";

    public Database() throws SQLException {
        this.connection = getConnection();
    }

    private Connection getConnection() throws SQLException {
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        return DriverManager.getConnection(dbUrl);
    }

    // Выполнить SQL-запрос
    public int executeUpdate(String query) throws SQLException {
        Statement statement = this.connection.createStatement();
        // Для Insert, Update, Delete
        return statement.executeUpdate(query);
    }
    // Добавить пользователя в таблицу
    public int addUser(long chat_id, String username_tg, String group, String userClassJSON) throws SQLException {
        String query = "INSERT INTO " + this.table + " (username_tg, \"group\", chat_id, \"user.class\") " +
                "VALUES ('" + username_tg + "','" + group + "'," + chat_id + ",'" +  userClassJSON + "');";
        return  executeUpdate(query);
    }

    // Изменить поля пользователя в таблице
    public int editUser(long chat_id, String username_tg, String group, String userClassJSON) throws SQLException {
        String query = "UPDATE public.users\n" +
                "\tSET username_tg='" + username_tg + "', \"group\"='" + group + "', chat_id=" +
                chat_id + ", \"user.class\"='" + userClassJSON + "'\n" +
                "\tWHERE chat_id=" + chat_id + ";";
        return  executeUpdate(query);
    }

    // Поиск нужного пользователя (возвращает список из двух элементов -
    // chat_id и JSON)
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
}
