import java.sql.*;

// every window calls DBConnection.getConnection() to talk to MySQL
public class DBConnection
{
    public static Connection getConnection() throws Exception
    {
       
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Establish connection - XAMPP MySQL port=3307
        return DriverManager.getConnection("jdbc:mysql://localhost:3307/airline_db", "root", "");
    }
}
