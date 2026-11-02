import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConnection
 * ------------------
 * The ONLY class that knows the database address and login.
 * Every other class calls DatabaseConnection.getConnection().
 *
 * If your MySQL password is different, change DB_PASSWORD below.
 */
public class DatabaseConnection {

    // ---------- Settings (change only here) ----------
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/lost_and_found_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";   // <-- put YOUR MySQL password here

    // Prevent creating objects: this class only has static helpers
    private DatabaseConnection() {
    }

    /**
     * Opens and returns a NEW connection each time.
     * The caller is responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Checks whether the database can be reached.
     * Prints a friendly explanation if it cannot.
     */
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.out.println("[ERROR] Cannot connect to the database.");
            System.out.println("        Reason : " + explain(e));
            return false;
        }
    }

    /**
     * Turns a technical SQLException into a beginner-friendly message.
     */
    public static String explain(SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        int code = e.getErrorCode();
        if (code == 1045) {
            return "Wrong MySQL username or password (check DB_PASSWORD in DatabaseConnection.java).";
        } else if (code == 1049) {
            return "Database 'lost_and_found_db' does not exist. Run the SQL script first.";
        } else if (code == 1062) {
            return "Duplicate entry: this record already exists.";
        } else if (code == 1451) {
            return "Cannot delete: other records depend on this one.";
        } else if (code == 3819) {
            return "A value violates a database rule (for example, an invalid phone number).";
        } else if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            return "MySQL server is not running or is not on port 3306. Start the MySQL service.";
        } else if (msg.contains("No suitable driver")) {
            return "MySQL Connector/J JAR is missing from the classpath.";
        }
        return msg + " (error code " + code + ")";
    }

    // ---------- Closing helpers (overloaded: same name, different parameter) ----------

    public static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ignored) {
                // nothing useful to do if closing fails
            }
        }
    }

    public static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
