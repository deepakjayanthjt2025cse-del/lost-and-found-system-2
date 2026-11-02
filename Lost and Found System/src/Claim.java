import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Claim - a request to take ownership of a found item (Encapsulation).
 * Status values: PENDING, APPROVED, REJECTED.
 */
public class Claim {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ROW_FORMAT = "%-5s%-9s%-16s%-16s%-12s%-17s%-10s";

    private int claimId;
    private int foundId;
    private String claimantName;
    private String claimantContact;
    private String claimDescription;
    private LocalDateTime claimDate;
    private String claimStatus = "PENDING";
    private String foundItemName;     // filled when we JOIN with found_items

    public Claim() {
    }

    public Claim(int claimId, int foundId, String claimantName, String claimantContact,
                 String claimDescription, LocalDateTime claimDate, String claimStatus) {
        this.claimId = claimId;
        this.foundId = foundId;
        this.claimantName = claimantName;
        this.claimantContact = claimantContact;
        this.claimDescription = claimDescription;
        this.claimDate = claimDate;
        this.claimStatus = claimStatus;
    }

    public static String tableHeader() {
        String head = String.format(ROW_FORMAT,
                "ID", "FOUND ID", "ITEM", "CLAIMANT", "CONTACT", "DATE", "STATUS");
        return head + "\n" + "-".repeat(head.length());
    }

    /** One table row for this claim. */
    public String displayRow() {
        return String.format(ROW_FORMAT,
                claimId, foundId, Item.fit(foundItemName, 16), Item.fit(claimantName, 16),
                claimantContact, claimDate == null ? "" : claimDate.format(STAMP), claimStatus);
    }

    /** Full multi-line view, used when the admin reviews a claim. */
    public String getDetails() {
        return "Claim ID      : " + claimId + "\n"
             + "Found Item ID : " + foundId + (foundItemName == null ? "" : " (" + foundItemName + ")") + "\n"
             + "Claimant      : " + claimantName + "\n"
             + "Contact       : " + claimantContact + "\n"
             + "Proof given   : " + claimDescription + "\n"
             + "Claim Date    : " + (claimDate == null ? "" : claimDate.format(STAMP)) + "\n"
             + "Status        : " + claimStatus;
    }

    @Override
    public String toString() {
        return displayRow();
    }

    // ---------- getters and setters ----------
    public int getClaimId() { return claimId; }
    public void setClaimId(int claimId) { this.claimId = claimId; }

    public int getFoundId() { return foundId; }
    public void setFoundId(int foundId) { this.foundId = foundId; }

    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) {
        if (claimantName == null || claimantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Claimant name cannot be empty.");
        }
        this.claimantName = claimantName.trim();
    }

    public String getClaimantContact() { return claimantContact; }
    public void setClaimantContact(String claimantContact) {
        if (claimantContact == null || !claimantContact.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("Contact number must be exactly 10 digits.");
        }
        this.claimantContact = claimantContact;
    }

    public String getClaimDescription() { return claimDescription; }
    public void setClaimDescription(String claimDescription) {
        if (claimDescription == null || claimDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Claim description cannot be empty.");
        }
        this.claimDescription = claimDescription.trim();
    }

    public LocalDateTime getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDateTime claimDate) { this.claimDate = claimDate; }

    public String getClaimStatus() { return claimStatus; }
    public void setClaimStatus(String claimStatus) {
        String s = claimStatus == null ? "" : claimStatus.trim().toUpperCase();
        if (!s.equals("PENDING") && !s.equals("APPROVED") && !s.equals("REJECTED")) {
            throw new IllegalArgumentException("Invalid claim status: " + claimStatus);
        }
        this.claimStatus = s;
    }

    public String getFoundItemName() { return foundItemName; }
    public void setFoundItemName(String foundItemName) { this.foundItemName = foundItemName; }
}
