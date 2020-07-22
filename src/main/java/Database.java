import org.postgresql.util.PSQLException;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;

public class Database {
    private final String DATABASE_URL;
    private Connection connection;
    private String table = "users";

    public Database(String url) throws URISyntaxException, SQLException {
        this.DATABASE_URL = url;
        this.connection = getConnection();
    }

    public Database() throws URISyntaxException, SQLException {
        this.DATABASE_URL = "postgres://bhxeblxhldvnoy:78a34cb6e88887224cc87ee191023728f6e3ba70272de90e4550025a247c78ec@ec2-18-203-62-227.eu-west-1.compute.amazonaws.com:5432/d4qcqsv0ujkg35";
        this.connection = getConnection();
    }

    private Connection getConnection() throws URISyntaxException, SQLException {
//        URI dbUri = new URI(this.DATABASE_URL);
//
//        String username = "bhxeblxhldvnoy";
//        String password = "78a34cb6e88887224cc87ee191023728f6e3ba70272de90e4550025a247c78ec";
//
//        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
//
//        return DriverManager.getConnection(dbUrl, username, password);

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
