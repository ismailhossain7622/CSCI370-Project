package edu.cs;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnectionUtil {

    private static final String URL =
        "jdbc:mysql://66.108.66.246:4000/cs370S26?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";

    private static final String USER = "remoteuser";
    private static final String PASS = "remote123";

    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASS);
    }
}