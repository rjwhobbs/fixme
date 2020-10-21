package fortytwo.data;

import java.sql.*;
import java.util.ArrayList;

public class DataBase {
    private static final String DATA_BASE_URL = "jdbc:sqlite:trans.db";
    private static Connection connection;

    public static void connect() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DATA_BASE_URL);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        connection = conn;
    }

    public static void close() {
        try {
            if (connection != null)
                connection.close();
            connection = null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Connection getConnection() {
        if (connection == null)
            connect();
        return connection;
    }

    public static void insert(String OrderID, String Message, String Reason) {
        String sqlQuery = "INSERT INTO Trans(OrderID, Message, Reason) VALUES(?, ?, ?)";
        int id = 0;
        try (PreparedStatement pstmt = getConnection().prepareStatement(sqlQuery)) {
            pstmt.setString(1, OrderID);
            pstmt.setString(2, Message);
            pstmt.setString(3, Reason);
            pstmt.executeUpdate();
        } catch (NullPointerException | SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}