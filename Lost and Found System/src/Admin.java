import java.time.LocalDateTime;

/**
 * Admin - an administrator account (Encapsulation).
 *
 * ROLES (authorization):
 *   SUPER_ADMIN : everything, including deleting records and managing admin accounts
 *   STAFF       : view, search, update items and handle claims - but NOT delete
 *                 records and NOT manage other admins
 *
 * The permission checks live HERE (canDelete, canManageAdmins), so the rules
 * are written once and every menu just asks the object.
 */
public class Admin {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String STAFF = "STAFF";

    private int adminId;
    private String username;
    private String password;                 // cleared after login; never printed
    private String role = STAFF;             // least privilege by default
    private int failedAttempts;
    private LocalDateTime lockedUntil;

    public Admin() {
    }

    public Admin(int adminId, String username, String password) {
        this.adminId = adminId;
        this.username = username;
        this.password = password;
    }

    // ---------- permissions ----------
    public boolean isSuperAdmin() { return SUPER_ADMIN.equals(role); }
    public boolean canDelete() { return isSuperAdmin(); }
    public boolean canManageAdmins() { return isSuperAdmin(); }

    // ---------- getters and setters ----------
    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) {
        if (!SUPER_ADMIN.equals(role) && !STAFF.equals(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        this.role = role;
    }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    @Override
    public String toString() {
        return "Admin[" + username + ", " + role + "]";
    }
}
