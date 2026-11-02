import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AdminAccountMenu
 * ----------------
 * The login screen and the "Account and Security" menu.
 *
 * How to plug it into your existing program:
 *
 *   AdminService adminService = new AdminService();
 *   Admin admin = AdminAccountMenu.login(adminService);   // null = login failed
 *   if (admin != null) {
 *       // ... your admin dashboard; add one extra option that calls:
 *       AdminAccountMenu.run(adminService, admin);
 *   }
 */
public class AdminAccountMenu {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private AdminAccountMenu() {
    }

    // ---------------------------------------------------------------
    //  LOGIN  (returns the logged-in Admin, or null)
    // ---------------------------------------------------------------

    public static Admin login(AdminService service) {
        InputHelper.printHeader("ADMIN LOGIN");

        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = InputHelper.readRequired("Username: ", 50);
            String password = InputHelper.readPassword("Password: ");
            try {
                Admin admin = service.login(username, password);
                System.out.println("\n[SUCCESS] Welcome, " + admin.getUsername()
                        + " (" + admin.getRole() + ").");
                return admin;
            } catch (AdminService.AuthException e) {
                System.out.println("[DENIED] " + e.getMessage());
                if (e.isLocked()) {
                    return null;
                }
            } catch (SQLException e) {
                System.out.println("[ERROR] " + DatabaseConnection.explain(e));
                return null;
            }
        }
        System.out.println("[DENIED] Too many failed attempts. Returning to the main menu.");
        return null;
    }

    // ---------------------------------------------------------------
    //  ACCOUNT AND SECURITY MENU
    // ---------------------------------------------------------------

    public static void run(AdminService service, Admin admin) {
        while (true) {
            InputHelper.printHeader("ACCOUNT AND SECURITY");
            System.out.println("Logged in as: " + admin.getUsername() + "  [" + admin.getRole() + "]\n");
            System.out.println("1. Change My Password");
            System.out.println("2. Add New Admin Account      (Super Admin only)");
            System.out.println("3. View Admin Accounts        (Super Admin only)");
            System.out.println("4. Unlock an Admin Account    (Super Admin only)");
            System.out.println("5. View Audit Log             (Super Admin only)");
            System.out.println("6. Back");

            int choice = InputHelper.readChoice("\nEnter your choice: ", 1, 6);
            try {
                switch (choice) {
                    case 1: changePassword(service, admin); break;
                    case 2: addAdmin(service, admin);       break;
                    case 3: viewAdmins(service, admin);     break;
                    case 4: unlockAdmin(service, admin);    break;
                    case 5: viewAuditLog(service, admin);   break;
                    default: return;
                }
            } catch (AdminService.AuthException e) {
                System.out.println("[DENIED] " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("[ERROR] " + DatabaseConnection.explain(e));
            }
            InputHelper.pause();
        }
    }

    private static void changePassword(AdminService service, Admin admin)
            throws SQLException, AdminService.AuthException {
        InputHelper.printHeader("CHANGE PASSWORD");
        String oldPassword = InputHelper.readPassword("Current password : ");
        String newPassword = InputHelper.readPassword("New password     : ");
        String again = InputHelper.readPassword("Confirm password : ");
        if (!newPassword.equals(again)) {
            System.out.println("[ERROR] The two new passwords do not match.");
            return;
        }
        service.changePassword(admin, oldPassword, newPassword);
        System.out.println("[SUCCESS] Password changed.");
    }

    private static void addAdmin(AdminService service, Admin admin)
            throws SQLException, AdminService.AuthException {
        if (!admin.canManageAdmins()) {                       // fail fast, before asking questions
            service.logAction(admin, "ACCESS_DENIED", "Tried to open 'Add New Admin'");
            throw new AdminService.AuthException("ACCESS DENIED: only a SUPER_ADMIN can add admin accounts.");
        }
        InputHelper.printHeader("ADD NEW ADMIN");
        System.out.println("Password rules: at least 8 characters, with a letter and a digit.\n");
        String username = InputHelper.readRequired("New username     : ", 50);
        String password = InputHelper.readPassword("Password         : ");
        String again = InputHelper.readPassword("Confirm password : ");
        if (!password.equals(again)) {
            System.out.println("[ERROR] The two passwords do not match.");
            return;
        }
        System.out.println("Role:  1. STAFF   2. SUPER_ADMIN");
        int r = InputHelper.readChoice("Choose role (1-2): ", 1, 2);
        String role = (r == 1) ? Admin.STAFF : Admin.SUPER_ADMIN;

        service.createAdmin(admin, username, password, role);
        System.out.println("[SUCCESS] Admin '" + username + "' created with role " + role + ".");
    }

    private static void viewAdmins(AdminService service, Admin admin)
            throws SQLException, AdminService.AuthException {
        List<Admin> admins = service.listAdmins(admin);
        InputHelper.printHeader("ADMIN ACCOUNTS");
        String format = "%-5s%-18s%-13s%-8s%-18s";
        String head = String.format(format, "ID", "USERNAME", "ROLE", "FAILED", "LOCKED UNTIL");
        System.out.println(head);
        System.out.println("-".repeat(head.length()));
        for (Admin a : admins) {
            boolean locked = a.getLockedUntil() != null && a.getLockedUntil().isAfter(java.time.LocalDateTime.now());
            System.out.println(String.format(format, a.getAdminId(), a.getUsername(), a.getRole(),
                    a.getFailedAttempts(), locked ? a.getLockedUntil().format(STAMP) : "-"));
        }
    }

    private static void unlockAdmin(AdminService service, Admin admin)
            throws SQLException, AdminService.AuthException {
        if (!admin.canManageAdmins()) {
            service.logAction(admin, "ACCESS_DENIED", "Tried to open 'Unlock Admin'");
            throw new AdminService.AuthException("ACCESS DENIED: only a SUPER_ADMIN can unlock accounts.");
        }
        String username = InputHelper.readRequired("Username to unlock: ", 50);
        service.unlockAdmin(admin, username);
        System.out.println("[SUCCESS] Account '" + username + "' is unlocked.");
    }

    private static void viewAuditLog(AdminService service, Admin admin)
            throws SQLException, AdminService.AuthException {
        List<String[]> rows = service.getAuditLog(admin, 20);
        InputHelper.printHeader("AUDIT LOG (LATEST 20)");
        String format = "%-21s%-12s%-17s%s";
        String head = String.format(format, "TIME", "USER", "ACTION", "DETAILS");
        System.out.println(head);
        System.out.println("-".repeat(78));
        if (rows.isEmpty()) {
            System.out.println("No entries yet.");
            return;
        }
        for (String[] r : rows) {
            System.out.println(String.format(format, r[0], Item.fit(r[1], 12), Item.fit(r[2], 17), r[3]));
        }
    }
}
