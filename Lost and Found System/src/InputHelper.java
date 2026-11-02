import java.io.Console;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * InputHelper
 * -----------
 * ALL keyboard input goes through this class. Every method keeps asking
 * until the user types something valid, so the program never crashes
 * because of bad input.
 */
public class InputHelper {

    private static final Scanner SCANNER = new Scanner(System.in);

    // STRICT mode rejects impossible dates such as 2026-02-30
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private InputHelper() {
    }

    // ---------------------------------------------------------------
    //  Basic reading
    // ---------------------------------------------------------------

    /** Reads one raw line. Exits politely if the input stream is closed (Ctrl+Z). */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return SCANNER.nextLine().trim();
        } catch (NoSuchElementException e) {
            System.out.println("\n[INFO] Input closed. Exiting program.");
            System.exit(0);
            return "";   // never reached, needed by the compiler
        }
    }

    /**
     * Reads a password. In a real Command Prompt the typed characters are
     * hidden. (If the program is started from an IDE there is no console,
     * so it falls back to normal visible input.)
     */
    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            if (chars == null) {
                System.out.println("\n[INFO] Input closed. Exiting program.");
                System.exit(0);
            }
            String value = new String(chars).trim();
            java.util.Arrays.fill(chars, ' ');
            return value;
        }
        return readLine(prompt);
    }

    /** Required text: cannot be empty, cannot be longer than maxLength. */
    public static String readRequired(String prompt, int maxLength) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                System.out.println("[ERROR] This field cannot be empty. Please try again.");
            } else if (value.length() > maxLength) {
                System.out.println("[ERROR] Too long. Maximum " + maxLength + " characters allowed.");
            } else {
                return value;
            }
        }
    }

    /** Optional text: pressing Enter returns an empty string (used for "keep old value"). */
    public static String readOptional(String prompt, int maxLength) {
        while (true) {
            String value = readLine(prompt);
            if (value.length() > maxLength) {
                System.out.println("[ERROR] Too long. Maximum " + maxLength + " characters allowed.");
            } else {
                return value;
            }
        }
    }

    // ---------------------------------------------------------------
    //  Numbers and menus
    // ---------------------------------------------------------------

    /** Menu choice between min and max (inclusive). */
    public static int readChoice(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int number = Integer.parseInt(text);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException e) {
                // fall through to the error message
            }
            System.out.println("[ERROR] Invalid choice. Please enter a number from " + min + " to " + max + ".");
        }
    }

    /** Reads a positive whole number such as an item ID. */
    public static int readPositiveInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                int number = Integer.parseInt(text);
                if (number > 0) {
                    return number;
                }
            } catch (NumberFormatException e) {
                // fall through
            }
            System.out.println("[ERROR] Please enter a valid positive number.");
        }
    }

    // ---------------------------------------------------------------
    //  Contact number and dates
    // ---------------------------------------------------------------

    public static boolean isValidContact(String text) {
        return text != null && text.matches("[0-9]{10}");
    }

    /** Contact number: exactly 10 digits. */
    public static String readContact(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (isValidContact(value)) {
                return value;
            }
            System.out.println("[ERROR] Contact number must be exactly 10 digits (numbers only).");
        }
    }

    /** Date in yyyy-MM-dd format. Must be a real calendar date and not in the future. */
    public static LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            LocalDate date = parseDate(value);
            if (date == null) {
                System.out.println("[ERROR] Invalid date. Use the format yyyy-MM-dd (example: 2026-10-25).");
            } else if (date.isAfter(LocalDate.now())) {
                System.out.println("[ERROR] The date cannot be in the future.");
            } else {
                return date;
            }
        }
    }

    /** Optional date: Enter returns null (keep the old date). */
    public static LocalDate readOptionalDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return null;
            }
            LocalDate date = parseDate(value);
            if (date == null) {
                System.out.println("[ERROR] Invalid date. Use the format yyyy-MM-dd (example: 2026-10-25).");
            } else if (date.isAfter(LocalDate.now())) {
                System.out.println("[ERROR] The date cannot be in the future.");
            } else {
                return date;
            }
        }
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------
    //  Confirmation and screen helpers
    // ---------------------------------------------------------------

    /** Asks a yes/no question. Accepts y, yes, n, no (any case). */
    public static boolean confirm(String question) {
        while (true) {
            String answer = readLine(question + " (Y/N): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("[ERROR] Please type Y or N.");
        }
    }

    public static void pause() {
        readLine("\nPress Enter to continue...");
    }

    /** Prints a centred title between two lines of '=' characters. */
    public static void printHeader(String title) {
        int width = 40;
        String line = "=".repeat(width);
        int padding = Math.max(0, (width - title.length()) / 2);
        System.out.println();
        System.out.println(line);
        System.out.println(" ".repeat(padding) + title);
        System.out.println(line);
    }
}
