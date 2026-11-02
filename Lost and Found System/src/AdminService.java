import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminService
 * ------------
 * Everything about admin security:
 *   - login with hashed passwords
 *   - lock the account after 3 wrong passwords (for 5 minutes)
 *   - role-based authorization (SUPER_ADMIN / STAFF)
 *   - change password, create admins, unlock admins
 *   - audit log of important actions
 *
 * Old plain-text passwords (from version 1) are upgraded to a hash
 * automatically the first time that admin logs in successfully.
 */
public class AdminService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final int LOCK_MINUTES = 5;

    private static final String GENERIC_FAILURE = "Invalid username or password.";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Thrown when login fails or an action is not permitted. */
    public static class AuthException extends Exception {
        private static final long serialVersionUID = 1L;
        private final boolean locked;

        public AuthException(String message) {
            this(message, false);
        }

        public AuthException(String message, boolean locked) {
            super(message);
            this.locked = locked;
        }

        public boolean isLocked() {
            return locked;
        }
    }

    // ---------------------------------------------------------------
    //  LOGIN
    // ---------------------------------------------------------------

    public Admin login(String username, String password) throws SQLException, AuthException {
        String sql = "SELECT admin_id, username, password, role, failed_attempts, locked_until "
                   + "FROM admins WHERE username = ?";

        try (Connection con = DatabaseConnection.getConnection()) {
            Admin admin = null;
            String stored = null;
            LocalDateTime lockedUntil = null;
            int failed = 0;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        admin = new Admin(rs.getInt("admin_id"), rs.getString("username"), null);
                        admin.setRole(rs.getString("role"));
                        stored = rs.getString("password");
                        failed = rs.getInt("failed_attempts");
                        Timestamp t = rs.getTimestamp("locked_until");
                        if (t != null) {
                            lockedUntil = t.toLocalDateTime();
                        }
                    }
                }
            }

            // Unknown user: same message as a wrong password (do not reveal which usernames exist)
            if (admin == null) {
                writeLog(con, null, username, "LOGIN_FAILED", "Unknown username");
                throw new AuthException(GENERIC_FAILURE);
            }

            LocalDateTime now = LocalDateTime.now();
            if (lockedUntil != null) {
                if (now.isBefore(lockedUntil)) {
                    long minutes = Duration.between(now, lockedUntil).toMinutes() + 1;
                    writeLog(con, admin.getAdminId(), username, "LOGIN_BLOCKED", "Account is locked");
                    throw new AuthException("This account is locked. Try again in about "
                            + minutes + " minute(s).", true);
                }
                failed = 0;   // lock has expired, start counting again
            }

            if (!passwordMatches(stored, password)) {
                failed++;
                if (failed >= MAX_FAILED_ATTEMPTS) {
                    updateLock(con, admin.getAdminId(), 0, LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                    writeLog(con, admin.getAdminId(), username, "ACCOUNT_LOCKED",
                            "Too many wrong passwords; locked for " + LOCK_MINUTES + " minutes");
                    throw new AuthException("Too many failed attempts. The account is locked for "
                            + LOCK_MINUTES + " minutes.", true);
                }
                updateLock(con, admin.getAdminId(), failed, null);
                writeLog(con, admin.getAdminId(), username, "LOGIN_FAILED", "Wrong password");
                throw new AuthException(GENERIC_FAILURE);
            }

            // ---- success ----
            updateLock(con, admin.getAdminId(), 0, null);
            if (!PasswordUtil.isHashed(stored)) {
                storePassword(con, admin.getAdminId(), password);   // upgrade old plain-text password
            }
            writeLog(con, admin.getAdminId(), username, "LOGIN_SUCCESS", "Role: " + admin.getRole());
            return admin;
        }
    }

    // ---------------------------------------------------------------
    //  PASSWORD CHANGE
    // ---------------------------------------------------------------

    public void changePassword(Admin admin, String oldPassword, String newPassword)
            throws SQLException, AuthException {
        try (Connection con = DatabaseConnection.getConnection()) {
            String stored = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT password FROM admins WHERE admin_id = ?")) {
                ps.setInt(1, admin.getAdminId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stored = rs.getString("password");
                    }
                }
            }
            if (stored == null || !passwordMatches(stored, oldPassword)) {
                writeLog(con, admin.getAdminId(), admin.getUsername(), "PASSWORD_FAILED", "Wrong current password");
                throw new AuthException("The current password is incorrect.");
            }
            String problem = PasswordUtil.checkStrength(newPassword);
            if (problem != null) {
                throw new AuthException(problem);
            }
            storePassword(con, admin.getAdminId(), newPassword);
            writeLog(con, admin.getAdminId(), admin.getUsername(), "PASSWORD_CHANGED", null);
        }
    }

    // ---------------------------------------------------------------
    //  SUPER_ADMIN-only actions
    // ---------------------------------------------------------------

    public void createAdmin(Admin actor, String username, String password, String role)
            throws SQLException, AuthException {
        requireSuperAdmin(actor, "create an admin account");

        if (username == null || !username.matches("[A-Za-z0-9_]{3,50}")) {
            throw new AuthException("Username must be 3-50 characters: letters, digits or underscore.");
        }
        if (!Admin.SUPER_ADMIN.equals(role) && !Admin.STAFF.equals(role)) {
            throw new AuthException("Role must be SUPER_ADMIN or STAFF.");
        }
        String problem = PasswordUtil.checkStrength(password);
        if (problem != null) {
            throw new AuthException(problem);
        }

        String sql = "INSERT INTO admins (username, password, role) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, role);
            ps.executeUpdate();
            writeLog(con, actor.getAdminId(), actor.getUsername(), "ADMIN_CREATED",
                    "New account '" + username + "' with role " + role);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new AuthException("That username already exists.");
            }
            throw e;
        }
    }

    public List<Admin> listAdmins(Admin actor) throws SQLException, AuthException {
        requireSuperAdmin(actor, "view admin accounts");
        List<Admin> list = new ArrayList<>();
        String sql = "SELECT admin_id, username, role, failed_attempts, locked_until FROM admins ORDER BY admin_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Admin a = new Admin(rs.getInt("admin_id"), rs.getString("username"), null);
                a.setRole(rs.getString("role"));
                a.setFailedAttempts(rs.getInt("failed_attempts"));
                Timestamp t = rs.getTimestamp("locked_until");
                if (t != null) {
                    a.setLockedUntil(t.toLocalDateTime());
                }
                list.add(a);
            }
        }
        return list;
    }

    public void unlockAdmin(Admin actor, String username) throws SQLException, AuthException {
        requireSuperAdmin(actor, "unlock an account");
        String sql = "UPDATE admins SET failed_attempts = 0, locked_until = NULL WHERE username = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            if (ps.executeUpdate() == 0) {
                throw new AuthException("No admin account named '" + username + "'.");
            }
            writeLog(con, actor.getAdminId(), actor.getUsername(), "ADMIN_UNLOCKED", "Account '" + username + "'");
        }
    }

    /** Returns the newest log entries as {time, username, action, details}. */
    public List<String[]> getAuditLog(Admin actor, int limit) throws SQLException, AuthException {
        requireSuperAdmin(actor, "view the audit log");
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT log_time, username, action_type, details FROM admin_audit_log "
                   + "ORDER BY log_id DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String details = rs.getString("details");
                    rows.add(new String[] {
                            rs.getTimestamp("log_time").toLocalDateTime().format(STAMP),
                            rs.getString("username"),
                            rs.getString("action_type"),
                            details == null ? "" : details});
                }
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------
    //  Audit logging - call this from your admin menu, e.g.
    //      adminService.logAction(currentAdmin, "DELETE_ITEM", "found_id=3");
    //  Never throws: a logging problem must not break the real action.
    // ---------------------------------------------------------------

    public void logAction(Admin admin, String actionType, String details) {
        if (admin == null) {
            return;
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            writeLog(con, admin.getAdminId(), admin.getUsername(), actionType, details);
        } catch (SQLException ignored) {
            // intentionally silent
        }
    }

    // ---------------------------------------------------------------
    //  Private helpers
    // ---------------------------------------------------------------

    private void requireSuperAdmin(Admin actor, String what) throws AuthException {
        if (actor == null || !actor.canManageAdmins()) {
            logAction(actor, "ACCESS_DENIED", "Tried to " + what);
            throw new AuthException("ACCESS DENIED: only a SUPER_ADMIN can " + what + ".");
        }
    }

    /** New-style hashed password, or an old plain-text one (compared in constant time). */
    private boolean passwordMatches(String stored, String given) {
        if (stored == null || given == null) {
            return false;
        }
        if (PasswordUtil.isHashed(stored)) {
            return PasswordUtil.verify(given, stored);
        }
        return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
                                     given.getBytes(StandardCharsets.UTF_8));
    }

    private void storePassword(Connection con, int adminId, String plain) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE admins SET password = ? WHERE admin_id = ?")) {
            ps.setString(1, PasswordUtil.hash(plain));
            ps.setInt(2, adminId);
            ps.executeUpdate();
        }
    }

    private void updateLock(Connection con, int adminId, int failed, LocalDateTime until) throws SQLException {
        String sql = "UPDATE admins SET failed_attempts = ?, locked_until = ? WHERE admin_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, failed);
            if (until == null) {
                ps.setNull(2, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(2, Timestamp.valueOf(until));
            }
            ps.setInt(3, adminId);
            ps.executeUpdate();
        }
    }

    private void writeLog(Connection con, Integer adminId, String username, String actionType, String details) {
        String sql = "INSERT INTO admin_audit_log (admin_id, username, action_type, details) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (adminId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, adminId);
            }
            String name = (username == null || username.isEmpty()) ? "(blank)" : username;
            ps.setString(2, name.length() > 50 ? name.substring(0, 50) : name);
            ps.setString(3, actionType);
            if (details == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, details.length() > 255 ? details.substring(0, 255) : details);
            }
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // intentionally silent
        }
    }
}
