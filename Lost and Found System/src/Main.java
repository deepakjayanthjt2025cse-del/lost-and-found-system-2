import java.sql.SQLException;
import java.util.List;

/**
 * Main entry point for the Lost and Found System.
 * Run with: java -cp "out;lib\mysql-connector-j-26.7.0.jar" Main
 */
public class Main {
    private static final ItemService ITEMS = new ItemService();
    private static final AdminService ADMINS = new AdminService();

    private Main() { }

    public static void main(String[] args) {
        InputHelper.printHeader("LOST AND FOUND SYSTEM");
        if (!DatabaseConnection.testConnection()) {
            System.out.println("Start MySQL and check the settings in DatabaseConnection.java.");
            return;
        }

        while (true) {
            System.out.println("\n1. Report a lost item");
            System.out.println("2. Report a found item");
            System.out.println("3. Search items");
            System.out.println("4. View my reports");
            System.out.println("5. Admin sign in");
            System.out.println("6. Exit");
            int choice = InputHelper.readChoice("\nChoose an option: ", 1, 6);
            try {
                switch (choice) {
                    case 1: reportLost(); break;
                    case 2: reportFound(); break;
                    case 3: searchItems(false); break;
                    case 4: viewMyReports(); break;
                    case 5: adminSignIn(); break;
                    default:
                        System.out.println("Goodbye.");
                        return;
                }
            } catch (SQLException e) {
                System.out.println("[ERROR] " + DatabaseConnection.explain(e));
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private static void reportLost() throws SQLException {
        InputHelper.printHeader("REPORT A LOST ITEM");
        LostItem item = new LostItem();
        fillItem(item);
        item.setLocation(InputHelper.readRequired("Where was it lost? ", 100));
        item.setDate(InputHelper.readDate("Date lost (yyyy-MM-dd): "));
        int id = ITEMS.addLostItem(item);
        System.out.println("[SUCCESS] Lost item report saved. ID: " + id);
    }

    private static void reportFound() throws SQLException {
        InputHelper.printHeader("REPORT A FOUND ITEM");
        FoundItem item = new FoundItem();
        fillItem(item);
        item.setLocation(InputHelper.readRequired("Where was it found? ", 100));
        item.setDate(InputHelper.readDate("Date found (yyyy-MM-dd): "));
        int id = ITEMS.addFoundItem(item);
        System.out.println("[SUCCESS] Found item report saved. ID: " + id);
    }

    private static void fillItem(Item item) {
        item.setItemName(InputHelper.readRequired("Item name: ", 100));
        item.setCategory(InputHelper.readRequired("Category: ", 50));
        item.setDescription(InputHelper.readRequired("Description: ", 255));
        String colour = InputHelper.readOptional("Colour (Enter for Not specified): ", 30);
        item.setColour(colour.isEmpty() ? Item.DEFAULT_COLOUR : colour);
        item.setSpecifications(InputHelper.readOptional("Private identifying details (optional): ", 255));
        item.setReporterName(InputHelper.readRequired("Your name: ", 100));
        item.setReporterContact(InputHelper.readContact("Your 10-digit contact number: "));
    }

    private static void searchItems(boolean adminView) throws SQLException {
        InputHelper.printHeader("SEARCH ITEMS");
        System.out.println("1. Lost items\n2. Found items\n3. Both");
        int type = InputHelper.readChoice("Search which? ", 1, 3);
        String keyword = InputHelper.readRequired("Search keyword: ", 100);
        if (type == 1 || type == 3) {
            System.out.println("\nLOST ITEMS");
            printItems(ITEMS.searchLostItems(keyword), adminView);
        }
        if (type == 2 || type == 3) {
            System.out.println("\nFOUND ITEMS");
            printItems(ITEMS.searchFoundItems(keyword), adminView);
        }
    }

    private static void viewMyReports() throws SQLException {
        InputHelper.printHeader("VIEW MY REPORTS");
        String contact = InputHelper.readContact("Your 10-digit contact number: ");
        System.out.println("\nLOST ITEMS");
        printItems(ITEMS.getLostItemsByContact(contact), false);
        System.out.println("\nFOUND ITEMS");
        printItems(ITEMS.getFoundItemsByContact(contact), false);
    }

    private static void adminSignIn() {
        Admin admin = AdminAccountMenu.login(ADMINS);
        if (admin != null) {
            adminMenu(admin);
        }
    }

    private static void adminMenu(Admin admin) {
        while (true) {
            InputHelper.printHeader("ADMIN DASHBOARD");
            System.out.println("Signed in: " + admin.getUsername() + " [" + admin.getRole() + "]\n");
            System.out.println("1. Search items");
            System.out.println("2. Update item colour/specifications");
            System.out.println("3. Account and security");
            System.out.println("4. Sign out");
            int choice = InputHelper.readChoice("\nChoose an option: ", 1, 4);
            try {
                switch (choice) {
                    case 1:
                        searchItems(true);
                        break;
                    case 2:
                        updateItem(admin);
                        break;
                    case 3:
                        AdminAccountMenu.run(ADMINS, admin);
                        break;
                    default:
                        ADMINS.logAction(admin, "LOGOUT", "Admin signed out");
                        System.out.println("Signed out.");
                        return;
                }
            } catch (SQLException e) {
                System.out.println("[ERROR] " + DatabaseConnection.explain(e));
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
            InputHelper.pause();
        }
    }

    private static void updateItem(Admin admin) throws SQLException {
        InputHelper.printHeader("UPDATE ITEM DETAILS");
        System.out.println("1. Lost item\n2. Found item");
        int kind = InputHelper.readChoice("Choose item type: ", 1, 2);
        String type = kind == 1 ? "LOST" : "FOUND";
        int id = InputHelper.readPositiveInt("Item ID: ");
        String colour = InputHelper.readRequired("New colour (max 30 characters): ", 30);
        String specifications = InputHelper.readOptional("New private identifying details (blank clears): ", 255);
        boolean updated = ITEMS.updateColourAndSpecifications(type, id, colour, specifications);
        if (updated) {
            ADMINS.logAction(admin, "ITEM_UPDATED", type + " item id=" + id);
            System.out.println("[SUCCESS] Item details updated.");
        } else {
            System.out.println("No item with that ID was found.");
        }
    }

    private static void printItems(List<? extends Item> items, boolean adminView) {
        if (items.isEmpty()) {
            System.out.println("No matching items.");
            return;
        }
        System.out.println(Item.tableHeader());
        for (Item item : items) {
            System.out.println(item.displayRow(adminView));
        }
    }
}