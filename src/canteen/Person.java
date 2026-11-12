package canteen;

/** Abstract base class: every user of the system has a name and a role. */
public abstract class Person {
    protected final String name;

    protected Person(String name) { this.name = name; }

    public String getName() { return name; }
    public abstract String getRole();
}
