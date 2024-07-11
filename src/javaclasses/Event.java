

public enum Event {
    BIRTHDAY ("День Рождения"),
    BIRTHDAYKID ("День Рождения ребенка"),
    NEWYEAR ("Новый Год"),
    WEDDING ("Свадьба"),
    MARCH8 ("8 Марта"),
    FEBRUARY23 ("23 Февраля"),
    ANNIVERSARY ("Годовщина"),
    OTHER ("Другое");
    public final String label;

    private Event (String label) {
        this.label = label;
    }
}
