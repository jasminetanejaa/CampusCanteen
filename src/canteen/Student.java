package canteen;

/** A student who pre-orders food. Inherits from Person. */
public class Student extends Person {
    public Student(String name) { super(name); }
    @Override public String getRole() { return "Student"; }
}
