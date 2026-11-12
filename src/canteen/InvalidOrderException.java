package canteen;

/** Custom checked exception for business-rule violations (empty cart, bad cancel, etc.). */
public class InvalidOrderException extends Exception {
    public InvalidOrderException(String message) { super(message); }
}
