

import java.util.Date;

public class User {
    public String name;
    public String birthday;
    public Sex sex;

    @Override
    public String toString() {
        if (sex != null) {
            return String.format("Имя: \n%s \n\nДата рождения: \n%s \n\nПол: \n%s",
                    name, birthday, sex.name);
        } else return String.format("Имя: \n%s \n\nДата рождения: \n%s \n\nПол: \n%s",
                name, birthday, sex);
    }
}
