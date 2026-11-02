import java.time.LocalDate;

/**
 * LostItem (INHERITANCE - child of Item)   [VERSION 2]
 */
public class LostItem extends Item {

    private String lostLocation;

    public LostItem() {
        super(0, 0, null, null, null, null, "LOST");   // starts with the default status
    }

    /** Version 1 constructor (kept for older code). */
    public LostItem(int id, int userId, String itemName, String category,
                    String description, String lostLocation, LocalDate lostDate, String status) {
        super(id, userId, itemName, category, description, lostDate, status);
        this.lostLocation = lostLocation;
    }

    /** Version 2 constructor (constructor OVERLOADING) with colour and specifications. */
    public LostItem(int id, int userId, String itemName, String category,
                    String description, String lostLocation, LocalDate lostDate, String status,
                    String colour, String specifications) {
        super(id, userId, itemName, category, description, lostDate, status, colour, specifications);
        this.lostLocation = lostLocation;
    }

    @Override
    public String getLocation() { return lostLocation; }

    @Override
    public void setLocation(String location) {
        this.lostLocation = requireText(location, "Lost location");
    }

    @Override
    public String getType() { return "LOST"; }

    @Override
    public String[] getAllowedStatuses() { return new String[] {"LOST", "RETURNED"}; }

    @Override
    public String getDetails() {
        return "Lost Item ID   : " + getId() + "\n"
             + "Item Name      : " + getItemName() + "\n"
             + "Category       : " + getCategory() + "\n"
             + "Colour         : " + getColour() + "\n"
             + "Description    : " + getDescription() + "\n"
             + "Specifications : " + specificationsText() + "\n"
             + "Lost Location  : " + lostLocation + "\n"
             + "Lost Date      : " + getDate() + "\n"
             + "Status         : " + getStatus();
    }
}
