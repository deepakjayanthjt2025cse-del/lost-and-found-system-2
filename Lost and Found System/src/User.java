/**
 * User - a person who reports items. Pure data class (Encapsulation).
 */
public class User {

    private int userId;
    private String userName;
    private String contactNumber;

    public User() {
    }

    public User(int userId, String userName, String contactNumber) {
        this.userId = userId;
        this.userName = userName;
        this.contactNumber = contactNumber;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }
        this.userName = userName.trim();
    }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) {
        if (contactNumber == null || !contactNumber.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("Contact number must be exactly 10 digits.");
        }
        this.contactNumber = contactNumber;
    }

    @Override
    public String toString() {
        return userName + " (" + contactNumber + ")";
    }
}
