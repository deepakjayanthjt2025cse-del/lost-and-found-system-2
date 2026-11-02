import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemService
 * -----------
 * All database work for lost and found items (user side).
 * Every method uses PreparedStatement (the "?" placeholders), so user
 * input can never change the meaning of the SQL (no SQL injection).
 *
 * Methods throw SQLException; the menu code catches it and prints a
 * friendly message with DatabaseConnection.explain(e).
 */
public class ItemService {

    // ---------------------------------------------------------------
    //  CREATE
    // ---------------------------------------------------------------

    /**
     * Saves a lost item. The reporter's name and contact must already be
     * set on the item (setReporterName / setReporterContact).
     * Returns the new lost_id.
     */
    public int addLostItem(LostItem item) throws SQLException {
        String sql = "INSERT INTO lost_items "
                   + "(user_id, item_name, category, description, lost_location, lost_date, colour, specifications, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'LOST')";
        return insertItem(item, sql);
    }

    /** Saves a found item with status AVAILABLE. Returns the new found_id. */
    public int addFoundItem(FoundItem item) throws SQLException {
        String sql = "INSERT INTO found_items "
                   + "(user_id, item_name, category, description, found_location, found_date, colour, specifications, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE')";
        return insertItem(item, sql);
    }

    /**
     * Shared by both add methods (polymorphism: the parameter is an Item,
     * so a LostItem or a FoundItem both work).
     * A transaction makes sure the user row and the item row are saved
     * together, or not at all.
     */
    private int insertItem(Item item, String sql) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId = findOrCreateUser(con, item.getReporterName(), item.getReporterContact());

                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setString(2, item.getItemName());
                    ps.setString(3, item.getCategory());
                    ps.setString(4, item.getDescription());
                    ps.setString(5, item.getLocation());
                    ps.setDate(6, Date.valueOf(item.getDate()));
                    ps.setString(7, item.getColour());
                    if (item.getSpecifications() == null) {
                        ps.setNull(8, Types.VARCHAR);
                    } else {
                        ps.setString(8, item.getSpecifications());
                    }
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        int newId = keys.next() ? keys.getInt(1) : -1;
                        con.commit();
                        item.setId(newId);
                        item.setUserId(userId);
                        return newId;
                    }
                }
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Finds the user with this contact number, or creates a new one.
     * If the number already exists, the stored name is kept.
     */
    private int findOrCreateUser(Connection con, String name, String contact) throws SQLException {
        if (!InputHelper.isValidContact(contact)) {
            throw new IllegalArgumentException("Contact number must be exactly 10 digits.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }

        String find = "SELECT user_id FROM users WHERE contact_number = ?";
        try (PreparedStatement ps = con.prepareStatement(find)) {
            ps.setString(1, contact);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }

        String insert = "INSERT INTO users (user_name, contact_number) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.setString(2, contact);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not create the user record.");
    }

    // ---------------------------------------------------------------
    //  READ : search (OVERLOADED - three versions of the same name)
    // ---------------------------------------------------------------

    private static final String LOST_SELECT =
            "SELECT l.lost_id, l.user_id, l.item_name, l.category, l.description, "
          + "l.lost_location, l.lost_date, l.colour, l.specifications, l.status, u.user_name, u.contact_number "
          + "FROM lost_items l JOIN users u ON l.user_id = u.user_id ";

    private static final String FOUND_SELECT =
            "SELECT f.found_id, f.user_id, f.item_name, f.category, f.description, "
          + "f.found_location, f.found_date, f.colour, f.specifications, f.status, u.user_name, u.contact_number "
          + "FROM found_items f JOIN users u ON f.user_id = u.user_id ";

    /** Version 1: search ALL three fields (name, category, location) at once. */
    public List<LostItem> searchLostItems(String keyword) throws SQLException {
        String sql = LOST_SELECT
                   + "WHERE l.item_name LIKE ? OR l.category LIKE ? OR l.lost_location LIKE ? OR l.colour LIKE ? "
                   + "ORDER BY l.lost_id";
        String pattern = likePattern(keyword);
        return queryLost(sql, pattern, pattern, pattern, pattern);
    }

    /** Version 2: search ONE field. criterion = "NAME", "CATEGORY", "LOCATION" or "COLOUR". */
    public List<LostItem> searchLostItems(String criterion, String keyword) throws SQLException {
        // The column name is chosen from a fixed list - never copied from user input.
        String column;
        switch (criterion.toUpperCase()) {
            case "NAME":     column = "l.item_name";     break;
            case "CATEGORY": column = "l.category";      break;
            case "LOCATION": column = "l.lost_location"; break;
            case "COLOUR":   column = "l.colour";        break;
            default: throw new IllegalArgumentException("Unknown search field: " + criterion);
        }
        String sql = LOST_SELECT + "WHERE " + column + " LIKE ? ORDER BY l.lost_id";
        return queryLost(sql, likePattern(keyword));
    }

    public List<FoundItem> searchFoundItems(String keyword) throws SQLException {
        String sql = FOUND_SELECT
                   + "WHERE f.item_name LIKE ? OR f.category LIKE ? OR f.found_location LIKE ? OR f.colour LIKE ? "
                   + "ORDER BY f.found_id";
        String pattern = likePattern(keyword);
        return queryFound(sql, pattern, pattern, pattern, pattern);
    }

    public List<FoundItem> searchFoundItems(String criterion, String keyword) throws SQLException {
        String column;
        switch (criterion.toUpperCase()) {
            case "NAME":     column = "f.item_name";      break;
            case "CATEGORY": column = "f.category";       break;
            case "LOCATION": column = "f.found_location"; break;
            case "COLOUR":   column = "f.colour";         break;
            default: throw new IllegalArgumentException("Unknown search field: " + criterion);
        }
        String sql = FOUND_SELECT + "WHERE " + column + " LIKE ? ORDER BY f.found_id";
        return queryFound(sql, likePattern(keyword));
    }

    // ---------------------------------------------------------------
    //  READ : "View My Reports" (by contact number)
    // ---------------------------------------------------------------

    public List<LostItem> getLostItemsByContact(String contact) throws SQLException {
        String sql = LOST_SELECT + "WHERE u.contact_number = ? ORDER BY l.lost_id";
        return queryLost(sql, contact);
    }

    public List<FoundItem> getFoundItemsByContact(String contact) throws SQLException {
        String sql = FOUND_SELECT + "WHERE u.contact_number = ? ORDER BY f.found_id";
        return queryFound(sql, contact);
    }

    // ---------------------------------------------------------------
    //  UPDATE : colour and specifications (call from your admin "Update Item" screen)
    // ---------------------------------------------------------------

    /**
     * type = "LOST" or "FOUND". Returns true if a row was changed.
     * The setters of a temporary object do the validation (Encapsulation at work).
     */
    public boolean updateColourAndSpecifications(String type, int id, String colour, String specs)
            throws SQLException {
        LostItem checker = new LostItem();
        checker.setColour(colour);            // throws IllegalArgumentException if invalid
        checker.setSpecifications(specs);

        String sql;
        switch (type.toUpperCase()) {
            case "LOST":
                sql = "UPDATE lost_items SET colour = ?, specifications = ? WHERE lost_id = ?";
                break;
            case "FOUND":
                sql = "UPDATE found_items SET colour = ?, specifications = ? WHERE found_id = ?";
                break;
            default:
                throw new IllegalArgumentException("Type must be LOST or FOUND.");
        }
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, checker.getColour());
            if (checker.getSpecifications() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, checker.getSpecifications());
            }
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------------------------------------------------------
    //  Private helpers
    // ---------------------------------------------------------------

    /** Wraps the keyword in % signs and escapes the special characters % and _ */
    private String likePattern(String keyword) {
        String safe = keyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + safe + "%";
    }

    /** Runs a SELECT on lost_items and converts every row into a LostItem object. */
    private List<LostItem> queryLost(String sql, String... params) throws SQLException {
        List<LostItem> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LostItem item = new LostItem(
                            rs.getInt("lost_id"), rs.getInt("user_id"),
                            rs.getString("item_name"), rs.getString("category"),
                            rs.getString("description"), rs.getString("lost_location"),
                            rs.getDate("lost_date").toLocalDate(), rs.getString("status"),
                            rs.getString("colour"), rs.getString("specifications"));
                    item.setReporterName(rs.getString("user_name"));
                    item.setReporterContact(rs.getString("contact_number"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    private List<FoundItem> queryFound(String sql, String... params) throws SQLException {
        List<FoundItem> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FoundItem item = new FoundItem(
                            rs.getInt("found_id"), rs.getInt("user_id"),
                            rs.getString("item_name"), rs.getString("category"),
                            rs.getString("description"), rs.getString("found_location"),
                            rs.getDate("found_date").toLocalDate(), rs.getString("status"),
                            rs.getString("colour"), rs.getString("specifications"));
                    item.setReporterName(rs.getString("user_name"));
                    item.setReporterContact(rs.getString("contact_number"));
                    list.add(item);
                }
            }
        }
        return list;
    }
}
