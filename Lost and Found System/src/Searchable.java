/**
 * Searchable (INTERFACE - Abstraction)
 * ------------------------------------
 * Any class that can be searched by a keyword promises to provide matches().
 * The interface says WHAT must be done, not HOW.
 */
public interface Searchable {

    /** Returns true if this object matches the given search keyword. */
    boolean matches(String keyword);
}
