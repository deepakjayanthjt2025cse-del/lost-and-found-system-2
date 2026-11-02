import java.time.LocalDate;

/**
 * Item (ABSTRACT CLASS - Abstraction and Inheritance parent)   [VERSION 2]
 * ----------------------------------------------------------
 * Holds everything LostItem and FoundItem have in common.
 * NEW in v2: colour and specifications.
 *
 * ENCAPSULATION : every field is private; setters validate their input.
 * ABSTRACTION   : abstract methods MUST be written by each child.
 * POLYMORPHISM  : displayRow() is OVERLOADED; getLocation()/getDetails() are OVERRIDDEN.
 */
public abstract class Item implements Searchable {

    public static final String DEFAULT_COLOUR = "Not specified";

    // ---------- private fields (Encapsulation) ----------
    private int id;
    private int userId;
    private String itemName;
    private String category;
    private String description;
    private LocalDate date;
    private String colour = DEFAULT_COLOUR;
    private String specifications;        // optional: brand, model, serial number, marks...
    private String status;
    private String reporterName;
    private String reporterContact;

    // ---------- constructors ----------
    protected Item() {
    }

    /** Version 1 constructor (kept so older code still compiles). */
    protected Item(int id, int userId, String itemName, String category,
                   String description, LocalDate date, String status) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.date = date;
        this.status = status;
    }

    /** Version 2 constructor with colour and specifications. */
    protected Item(int id, int userId, String itemName, String category,
                   String description, LocalDate date, String status,
                   String colour, String specifications) {
        this(id, userId, itemName, category, description, date, status);
        this.colour = (colour == null || colour.trim().isEmpty()) ? DEFAULT_COLOUR : colour;
        this.specifications = (specifications == null || specifications.trim().isEmpty())
                ? null : specifications;
    }

    // ---------- abstract methods (children must implement) ----------
    public abstract String getLocation();
    public abstract void setLocation(String location);
    public abstract String getType();               // "LOST" or "FOUND"
    public abstract String[] getAllowedStatuses();
    public abstract String getDetails();            // full multi-line description

    // ---------- Searchable ----------
    @Override
    public boolean matches(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        String k = keyword.trim().toLowerCase();
        return contains(itemName, k) || contains(category, k)
            || contains(getLocation(), k) || contains(colour, k);
    }

    private boolean contains(String text, String keywordLower) {
        return text != null && text.toLowerCase().contains(keywordLower);
    }

    // ---------- table formatting ----------
    private static final String ROW_FORMAT = "%-5s%-16s%-12s%-10s%-20s%-16s%-11s%-10s";

    public static String tableHeader() {
        String head = String.format(ROW_FORMAT,
                "ID", "ITEM NAME", "CATEGORY", "COLOUR", "DESCRIPTION", "LOCATION", "DATE", "STATUS");
        return head + "\n" + "-".repeat(head.length());
    }

    public static String fit(String text, int width) {
        if (text == null) {
            return "";
        }
        if (text.length() > width - 1) {
            return text.substring(0, width - 4) + "...";
        }
        return text;
    }

    /** OVERLOADING #1 - one table row (what ordinary users see). */
    public String displayRow() {
        return String.format(ROW_FORMAT,
                id, fit(itemName, 16), fit(category, 12), fit(colour, 10), fit(description, 20),
                fit(getLocation(), 16), date, status);
    }

    /**
     * OVERLOADING #2 - admin view: adds the specifications and the reporter.
     * Specifications are deliberately NOT shown to ordinary users, so a false
     * claimant cannot simply copy them; the admin compares them with the claim.
     */
    public String displayRow(boolean adminView) {
        String row = displayRow();
        if (adminView) {
            if (specifications != null) {
                row += "\n     Specifications: " + specifications;
            }
            if (reporterName != null) {
                row += "\n     Reported by   : " + reporterName + " (" + reporterContact + ")";
            }
        }
        return row;
    }

    @Override
    public String toString() {
        return displayRow();
    }

    // ---------- helpers for children / setters ----------
    protected static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    /** Text used by getDetails() in both children. */
    protected String specificationsText() {
        return specifications == null ? "-" : specifications;
    }

    // ---------- getters and setters ----------
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) {
        this.itemName = requireText(itemName, "Item name");
    }

    public String getCategory() { return category; }
    public void setCategory(String category) {
        this.category = requireText(category, "Category");
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = requireText(description, "Description");
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be empty.");
        }
        this.date = date;
    }

    public String getColour() { return colour; }
    public void setColour(String colour) {
        String c = requireText(colour, "Colour");
        if (c.length() > 30) {
            throw new IllegalArgumentException("Colour must be 30 characters or fewer.");
        }
        this.colour = c;
    }

    public String getSpecifications() { return specifications; }
    /** Optional: a blank value clears the specifications. */
    public void setSpecifications(String specifications) {
        if (specifications == null || specifications.trim().isEmpty()) {
            this.specifications = null;
            return;
        }
        String s = specifications.trim();
        if (s.length() > 255) {
            throw new IllegalArgumentException("Specifications must be 255 characters or fewer.");
        }
        this.specifications = s;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        String s = requireText(status, "Status").toUpperCase();
        for (String allowed : getAllowedStatuses()) {
            if (allowed.equals(s)) {
                this.status = s;
                return;
            }
        }
        throw new IllegalArgumentException("Invalid status '" + status + "' for a " + getType() + " item.");
    }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterContact() { return reporterContact; }
    public void setReporterContact(String reporterContact) { this.reporterContact = reporterContact; }
}
