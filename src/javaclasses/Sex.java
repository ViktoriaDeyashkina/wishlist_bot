import org.apache.commons.lang3.ObjectUtils;

public enum Sex {
    MALE (1, "Мужской"),
    FEMALE (0, "Женский");


    public final int label;
    public final String name;

    private Sex (int label, String name) {
        this.label = label;
        this.name = name;
    }

}
