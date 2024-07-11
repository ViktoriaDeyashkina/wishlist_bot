


import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME;
    public static final String TELEGRAM_BOT_TOKEN;
    private DialogMode currentMode;
    private Event currentEvent;
    private Date date;
    private int count;

    private ArrayList<String> list = new ArrayList<>();
    int wishlistId;

    private User user;
    DbManager dbManager = new DbManager();

    static {
        Properties properties = new Properties();
        try {
            var is = ClassLoader.getSystemResourceAsStream("requests/properties_test.properties");
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TELEGRAM_BOT_NAME = properties.getProperty("TELEGRAM_BOT_NAME");
        TELEGRAM_BOT_TOKEN = properties.getProperty("TELEGRAM_BOT_TOKEN");
    }

    public Main() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) throws SQLException, ParseException {

        String messageText = getMessageText();
        String query = getCallbackQueryButtonKey();

        showMainMenu("главное меню бота", "/start",
                "редактировать данные о себе", "/profile",
                "создание вишлиста", "/create",
                "внесение изменений в уже созданные вишлисты", "/edit",
                "удаление вишлиста", "/delete",
                "просмотр своих вишлистов", "/viewmy",
                "просмотр доступных вишлистов друзей", "/viewfriend",
                "получить id чата", "/id");

        if (isMessageCommand()) {
            String text = messageText.substring(1);

            currentMode = DialogMode.valueOf(text.toUpperCase(Locale.ROOT));
            sendPhotoMessage(text);
            sendTextMessage(loadMessage(text));
            switch (currentMode) {
                case START:
                    String account = update.getMessage().getChat().getUserName();
                    Long chatId = getCurrentChatId();
                    if (account == null) {
                        account = update.getMessage().getChat().getFirstName() + " " + update.getMessage().getChat().getLastName();
                        account = account.replace("null", "").trim();
                    } else {
                        account = "@" + account;
                    }
                    if (!dbManager.isExist(String.format("SELECT EXISTS(SELECT Account_name FROM Users " +
                            "WHERE Account_name = '%s' OR chat_id = %d)", account, chatId))) {
                        dbManager.insertOrDelete(String.format("INSERT INTO Users (Account_name, chat_id) " +
                                "VALUES ('%s', %d)", account, chatId));
                    } else {
                        dbManager.insertOrDelete(String.format(
                                "UPDATE Users SET Account_name  = '%s', chat_id = %d WHERE Account_name  = '%s' OR chat_id = %d", account, chatId, account, chatId));
                    }
                    user = getUser(chatId);
                    if (user.name == null || user.birthday == null || user.sex == null) {
                        sendTextMessage("Давай начнем со знакомства. Переходи в раздел /profile и ответь на несколько вопросов о себе.");
                    }
                    return;
                case PROFILE:
                    sendTextButtonsMessage("Хочешь просмотреть или отредактировать информацию о себе?",
                            "Посмотреть", "do_look",
                            "Отредактировать", "do_correct");
                    return;
                case CREATE:
                    sendTextButtonsMessage("Выбери событие, для которого планируешь создать вишлист: ",
                            Event.BIRTHDAY.label, "event_BIRTHDAY",
                            Event.BIRTHDAYKID.label, "event_BIRTHDAYKID",
                            Event.NEWYEAR.label, "event_NEWYEAR",
                            Event.WEDDING.label, "event_WEDDING",
                            Event.MARCH8.label, "event_MARCH8",
                            Event.FEBRUARY23.label, "event_FEBRUARY23",
                            Event.ANNIVERSARY.label, "event_ANNIVERSARY",
                            Event.OTHER.label, "event_OTHER");
                    return;
                case EDIT:
                    ArrayList<String> list = getWishlists(getCurrentChatId());
                    if (!list.isEmpty()) {
                        sendTextButtonsMessage("Выбери вишлист для редактирования", list);
                    } else {
                        sendTextMessage("У вас нет ни одного вишлиста для редактирования. \nДля создания вишлиста нажмите /create");
                    }
                    return;
                case DELETE:
                    list = getWishlists(getCurrentChatId());
                    if (!list.isEmpty()) {
                        sendTextButtonsMessage("Выбери вишлист для удаления", list);
                    } else {
                        sendTextMessage("У вас нет ни одного вишлиста. \nДля создания вишлиста нажмите /create");
                    }
                    return;
                case VIEWMY:
                    list = getWishlists(getCurrentChatId());
                    if (!list.isEmpty()) {
                        sendTextButtonsMessage("Выбери вишлист для просмотра", list);
                    } else {
                        sendTextMessage("У вас нет ни одного вишлиста. \nДля создания вишлиста нажмите /create");
                    }
                    return;
                case VIEWFRIEND:
                    list = dbManager.getLists(String.format(
                                    "SELECT DISTINCT Users.Account_name, Users.ID FROM Users JOIN (SELECT Wishlists.ID, Account_id FROM Wishlists JOIN Access ON Wishlists.ID = Access.Wishlist_id JOIN Users ON Access.User_id = Users.ID WHERE Users.chat_id = %d) AS T ON Users.ID = T.Account_id",
                                    getCurrentChatId()),
                            "user_");
                    if (!list.isEmpty()) {
                        sendTextButtonsMessage("Выбери чей вишлист ты хочешь посмотреть", list);
                    } else {
                        sendTextMessage("Пока никто не поделился с Вами своими вишлистами");
                    }
                    return;
                case ID:
                    sendTextMessage("Твой id: \n\n" + String.valueOf(getCurrentChatId()));
                    return;
            }
        }

        if (!isMessageCommand()) {
            switch (currentMode) {
                case START:
                    return;
                case PROFILE:
                    if (query.startsWith("do_")) {
                            user = getUser(getCurrentChatId());
                        switch (query) {
                            case "do_look":
                                sendTextMessage(user.toString());
                                return;
                            case "do_correct":
                                sendTextMessage(user.toString());
                                sendTextButtonsMessage("Чтобы будешь редактировать?",
                                        "Имя", "correct_name",
                                        "Дата рождения", "correct_birthday",
                                        "Пол", "correct_sex");
                                return;
                        }
                    }

                    if (query.startsWith("correct_")) {
                        switch (query) {
                            case "correct_name":
                                sendTextButtonsMessage("Введите имя и затем нажмите кнопку \"Исправить\"",
                                        "Исправить", "name");
                                return;
                            case "correct_birthday":
                                sendTextButtonsMessage("Введите дату рождения в формате дд.мм.гггг и затем нажмите кнопку \"Исправить\"",
                                        "Исправить", "birthday");
                                return;
                            case "correct_sex":
                                sendTextButtonsMessage("Выбери пол:",
                                        "Мужской", "sex_1",
                                        "Женский", "sex_0");
                                return;
                        }
                    }
                    if (query.startsWith("name")) {
                        user.name = list.get(0);
                        sendTextButtonsMessage("Продолжить редактирование?",
                                "Имя", "correct_name",
                                "Дата рождения", "correct_birthday",
                                "Пол", "correct_sex",
                                "Сохранить данные", "save");
                        return;
                    }
                    if (query.startsWith("birthday")) {
                        user.birthday = new SimpleDateFormat("yyyy-MM-dd").format(getDate(list.get(0)));
                        sendTextButtonsMessage("Продолжить редактирование?",
                                "Имя", "correct_name",
                                "Дата рождения", "correct_birthday",
                                "Пол", "correct_sex",
                                "Сохранить данные", "save");
                        return;
                    }
                    if (query.startsWith("sex_")) {
                        int sex = Integer.parseInt(query.substring(4));
                        switch (sex) {
                            case 1:
                                user.sex = Sex.MALE;
                                break;
                            case 0:
                                user.sex = Sex.FEMALE;
                                break;
                        }
                        sendTextButtonsMessage("Продолжить редактирование?",
                                "Имя", "correct_name",
                                "Дата рождения", "correct_birthday",
                                "Пол", "correct_sex",
                                "Сохранить данные", "save");
                        return;
                    }
                    list.clear();
                    list.add(messageText);
                    if (query.startsWith("save")) {
                        dbManager.insertOrDelete(String.format("UPDATE Users SET Name = '%s', Birthday = '%s', Sex = %d where chat_id=%d",
                                user.name, user.birthday, user.sex.label, getCurrentChatId()));
                        sendTextMessage("Сохранены следующие данные: \n\n" + user.toString());
                    }
                    return;
                case CREATE:
                    Long chatId = getCurrentChatId();
                    if (query.startsWith("event_")) {

                        String text = query.substring(6).toLowerCase();
                        sendPhotoMessage(text);
                        sendTextMessage("Отличный выбор ❤\uFE0F");
                        currentEvent = Event.valueOf(text.toUpperCase(Locale.ROOT));

                        switch (currentEvent) {
                            case BIRTHDAY:
                                date = dbManager.getBirthday(String.format("SELECT Birthday FROM Users where chat_id = %d", chatId));
                                if (date != null) {
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(date);
                                    date = setDate(cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                                    sayYesNoToDate(date);
                                } else {
                                    sendTextMessage("Введите дату рождения в формате ДД.ММ.ГГГГ");
                                }
                                return;
                            case BIRTHDAYKID:
                                sendTextMessage("Введите дату рождения ребенка в формате ДД.ММ.ГГГГ");
                                return;
                            case NEWYEAR:
                                date = setDate(11, 31);
                                sayYesNoToDate(date);
                                return;
                            case WEDDING:
                                sendTextMessage("Введите дату свадьбы в формате ДД.ММ.ГГГГ");
                                return;
                            case MARCH8:
                                date = setDate(2, 8);
                                sayYesNoToDate(date);
                                return;
                            case FEBRUARY23:
                                date = setDate(1, 23);
                                sayYesNoToDate(date);
                                return;
                            case ANNIVERSARY:
                                sendTextMessage("Введите дату годовщины в формате ДД.ММ.ГГГГ");
                                return;
                            case OTHER:
                                sendTextMessage("Введите дату события в формате ДД.ММ.ГГГГ");
                                return;
                        }
                    }
                    if (query.startsWith("answ_")) {
                        String answ = query.substring(5);
                        switch (answ) {
                            case "yes":
                                if (dbManager.insertOrDelete(String.format
                                        ("INSERT INTO Wishlists (Account_id, event, Date) VALUES ((SELECT ID FROM Users WHERE chat_id=%d),'%s', '%s')",
                                                chatId, currentEvent.label, new SimpleDateFormat("yyyy-MM-dd").format(date)))) {
                                    sendTextMessage(String.format("Вишлист создан к событию - <b>%s</b>, которое состоится <b>%s</b>. \nВы можете добавлять желаемые подарки, а так же настраивать видимость вишлиста друзьям в разделе редактирования. \nЕсли желаете перейти к редактированию вишлиста нажмите /edit",
                                            currentEvent.label, new SimpleDateFormat("dd MMMM yyyy", new Locale("ru")).format(date)));
                                }
                                return;
                            case "no":
                                sendTextMessage("Введите дату в формате ДД.ММ.ГГГГ");
                                return;
                        }
                    }
                    date = getDate(messageText);
                case EDIT:
                    if (query.startsWith("wishlist_")) {
                        wishlistId = Integer.parseInt(query.substring(9));
                        sendTextButtonsMessage("Выбери режим редактирования",
                                "Добавить товары в вишлист", "answ_add",
                                "Удалить товары из вишлиста", "answ_del",
                                "Дать друзьям доступ к вишлисту", "answ_access",
                                "Изменить видимость вишлиста для друзей", "answ_ban");
                        return;
                    }
                    if (query.startsWith("answ_")) {
                        String answ = query.substring(5);
                        sendTextMessage(loadMessage(answ));
                        list.clear();
                        switch (answ) {
                            case "add":
                                sendTextButtonsMessage("Нажми как будешь готов",
                                        "Добавить", "do_add");
                                return;
                            case "access":
                                sendTextButtonsMessage("Нажми как будешь готов",
                                        "Открыть доступ", "do_access");
                                return;
                            case "del":
                                list = getGoods(wishlistId);
                                if (!list.isEmpty()) {
                                    sendTextButtonsMessage("Выбери товары для удаления из вишлиста", list);
                                } else {
                                    sendTextMessage("Нет подарков для удаления");
                                }
                                return;
                            case "ban":
                                list = dbManager.getLists(String.format(
                                                "SELECT Users.Account_name, Access.User_id FROM Access JOIN Users ON Users.ID = Access.User_id WHERE Wishlist_id = %d;", wishlistId),
                                        "user_");
                                if (!list.isEmpty()) {
                                    sendTextButtonsMessage("Выбери аккаунты для удаления из доступа к этому вишлисту", list);
                                } else {
                                    sendTextMessage("Нет аккаунтов, имеющих доступ к этому вишлисту");
                                }
                                return;
                        }
                    }
                    if (query.startsWith("do_")) {
                        if (!list.isEmpty()) {
                            switch (query) {
                                case "do_add":
                                    for (String str : list) {
                                        int index = str.indexOf("http");
                                        String name = str;
                                        String link = "";
                                        if (index != -1) {
                                            name = str.substring(0, index).trim();
                                            link = str.substring(index);
                                        }
                                        if (name.startsWith("Купить")) {
                                            name = name.replaceFirst("Купить", "");
                                        }
                                        addGoods(name, link);
                                        if (name.isEmpty()) {
                                            if (dbManager.getStatus(String.format(
                                                    "SELECT case WHEN Name IS NULL OR TRIM(Name) = '' THEN 'empty' ELSE Name END as Status FROM Goods WHERE link = '%s'", link))) {
                                                ArrayList<String> list1 = dbManager.getLists(String.format("SELECT Link, ID FROM Goods WHERE Link = '%s'", link), "name_");
                                                System.out.println(list1);
                                                sendTextButtonsMessage(String.format("Введите <b>наименование</b> для <a href=\"%s\"><i>ссылки на товар</i></a> и нажмите кнопку \"Добавить\"", link),
                                                        "Добавить на именование", list1.get(1));
                                            }
                                        }
                                    }
                                    list.clear();
                                    return;
                                case "do_access":
                                    for (String str : list) {
                                        doAccess(str);
                                    }
                                    list.clear();
                                    return;
                            }
                        } else {
                            sendTextMessage("Вы ничего не прислали для добавления");
                            return;
                        }
                    }
                    if (query.startsWith("name_")) {
                        String name = list.get(0);
                        System.out.println(name);
                        int index = Integer.parseInt(query.substring(5));
                        System.out.println(index);
                        if (dbManager.insertOrDelete(String.format("UPDATE Goods SET Name = '%s' WHERE ID = %d", name, index))) {
                            sendTextMessage("Наименование добавлено успешно");
                        } else {
                            sendTextMessage("Не удалось обновить информацию");
                        }
                        list.clear();
                        return;
                    }
                    if (query.startsWith("good_")) {
                        int id = Integer.parseInt(query.substring(5));
                        if (dbManager.insertOrDelete(String.format("delete from List where Good_id = %d and Wishlist_id = %d", id, wishlistId))) {
                            int i = list.indexOf(query);
                            sendTextMessage(list.get(i - 1) + " удален из списка");
                            list.remove(i);
                            list.remove(i - 1);
                            if (!list.isEmpty()) {
                                sendTextButtonsMessage("Выбери подарки для удаления из вишлиста", list);
                            } else {
                                sendTextMessage("Нет подарков для удаления");
                            }
                        }
                        return;
                    }
                    if (query.startsWith("user_")) {
                        long id = Integer.parseInt(query.substring(5));
                        String url = dbManager.getUrls(String.format("SELECT Account_name FROM Users WHERE ID = %d", id));
                        sendTextButtonsMessage(String.format("Действительно хотите удалить пользователя <a href=\"tg://user?id=%d\"><b>%s</b></a> из просматривающих вишлист?", id, url),
                                "Удалить", "del_" + id);

                    return;
            }
                    if (query.startsWith("del_")){
                        long id = Integer.parseInt(query.substring(4));
                        if (dbManager.insertOrDelete(String.format("DELETE FROM Access WHERE User_id = %d AND Wishlist_id = %d;", id, wishlistId))) {
                            int i = list.indexOf("user_" + id);
                            sendTextMessage(list.get(i - 1) + " удален из списка");
                            list.remove(i);
                            list.remove(i - 1);
                            if (!list.isEmpty()) {
                                sendTextButtonsMessage("Выбери аккаунты для удаления из доступа к этому вишлисту", list);
                            } else {
                                sendTextMessage("Нет аккаунтов, имеющих доступ к этому вишлисту");
                            }
                        }
                        return;
                    }
                    list.add(messageText);
                    return;
                case DELETE:
                    if (query.startsWith("wishlist_")){
                        wishlistId = Integer.parseInt(query.substring(9));
                        String text = String.format("Вы действительно хотите удалить вишлист к событию - %s:", dbManager.getEvent(wishlistId));
                        sendTextButtonsMessage(text, "Удалить вишлист", "del");
                    }
                    if (query.startsWith("del")) {
                        ArrayList<String> list1 = new ArrayList<>();
                        list1.add(String.format("DELETE FROM Access WHERE Wishlist_id = %d;", wishlistId));
                        list1.add(String.format("DELETE FROM List WHERE Wishlist_id = %d;", wishlistId));
                        list1.add(String.format("DELETE FROM Wishlists WHERE ID = %d;", wishlistId));
                        if (dbManager.insertTransaction(list1)) {
                            sendTextMessage("Вишлист удален");
                        } else {
                            sendTextMessage("Не удалось удалить, попробуйте позже");
                        }
                    }
                    return;
                case VIEWMY:
                    if (query.startsWith("wishlist_")) {
                        wishlistId = Integer.parseInt(query.substring(9));
                        ArrayList<String> list1 = getGoodsUrl(wishlistId);
                        if (!list1.isEmpty()) {
                            String text = String.format("Список подарков к событию - %s:", dbManager.getEvent(wishlistId));
                            sendUrlButtonsMessage(text, list1);
                        } else {
                            sendTextMessage("Вишлист пустой. Для добавления подарков в вишлист перейдите в раздел редактирования /edit");
                        }
                    }
                    return;
                case VIEWFRIEND:
                    if (query.startsWith("user_")) {
                        Long id = Long.parseLong(query.substring(5));
                        ArrayList<String> list1 = dbManager.getLists(String.format(
                                        "SELECT Wishlists.Event, Wishlists.iD FROM Wishlists JOIN Access ON Wishlists.ID = Access.Wishlist_id JOIN Users ON Access.User_id = Users.ID WHERE Users.chat_id = %d AND Wishlists.Account_id = %d",
                                        getCurrentChatId(), id),
                                "wishlist_");
                        if (!list1.isEmpty()) {
                            sendTextButtonsMessage("Выбери вишлист для просмотра", list1);
                        } else {
                            sendTextMessage("С Вами еще не поделились вишлистами");
                        }
                        return;
                    }
                    if (query.startsWith("wishlist_")) {
                        wishlistId = Integer.parseInt(query.substring(9));
                        ArrayList<String> list2 = getGoodsUrl(wishlistId);
                        if (!list2.isEmpty()) {
                            String text = String.format("Список подарков к событию - %s:", dbManager.getEvent(wishlistId));
                            sendUrlButtonsMessage(text, list2);
                        } else {
                            sendTextMessage("В этом вишлисте нет подарков");
                        }
                        return;
                    }
                    return;
            }
        }
    }

    public User getUser(Long chatId) throws SQLException {
        return dbManager.getUserInfo(String.format(
                "SELECT Name, Birthday, Sex FROM Users WHERE chat_id = %d", chatId));
    }

    public Date getDate(String text) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date date1 = null;
        try {
            date1 = dateFormat.parse(text);
            if (currentMode == DialogMode.CREATE) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date1);
                date1 = setDate(cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
                sayYesNoToDate(date1);
            }
        } catch (ParseException e) {
            sendTextMessage("Вы ввели неверную дату. Дата должна быть введена в формате дд.мм.гггг");
        }
        return date1;
    }

    public ArrayList<String> getGoods(int wishlistId) {
        try {
            return dbManager.getLists(String.format(
                            "SELECT Goods.Name, Goods.ID FROM Goods JOIN List ON Goods.ID = List.Good_id WHERE Wishlist_id = %d", wishlistId),
                    "good_");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<String> getGoodsUrl(int wishlistId) {
        try {
            return dbManager.getLists(String.format(
                            "SELECT Name, Link FROM Goods JOIN List ON Goods.ID = List.Good_id WHERE Wishlist_id = %d", wishlistId),
                    "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<String> getWishlists(Long chatId) {
        try {
            return dbManager.getLists(String.format(
                            "SELECT Event, Wishlists.ID FROM Wishlists JOIN Users ON Account_id = Users.id WHERE Users.chat_id = %d", chatId),
                    "wishlist_");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void sayYesNoToDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
        sendTextMessage("К этой дате будем готовиться: " + format.format(date) + "?");
        sendTextButtonsMessage("Выберете вариант ответа:",
                "ДА", "answ_yes",
                "НЕТ", "answ_no");
    }

    public Date setDate(int month, int day) {
        Date cur = Calendar.getInstance().getTime();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DATE, day);
        if (cal.getTime().before(cur) || cal.getTime().equals(cur)) {
            cal.add(Calendar.YEAR, 1);
        }
        return cal.getTime();

    }

    public void addGoods(String name, String link) throws SQLException {
        ArrayList <String> trans = new ArrayList<>();
        if (link.equals("") || !dbManager.isExist(String.format("SELECT EXISTS(SELECT ID FROM Goods WHERE Link = '%s')", link))) {
            trans.add(String.format("INSERT INTO Goods (Link, Name) VALUE ('%s', '%s')", link, name));
            trans.add("SET @a = LAST_INSERT_ID();");
            trans.add(String.format("INSERT INTO List (Wishlist_id, Good_id) VALUES (%d, @a);", wishlistId));
        } else {
            trans.add(String.format("INSERT INTO List (Wishlist_id, Good_id) VALUES (%d, (SELECT ID FROM Goods WHERE Link = '%s'))", wishlistId, link));
        }

        try {
            dbManager.insertTransaction(trans);
            sendTextMessage(String.format("<b>%s</b> добавлено успешно", name));
        } catch (Exception e) {
            sendTextMessage("Не удалось добавить. Попробуйте позже.");
        }
    }

    public void doAccess (String name) throws SQLException {
        ArrayList <String> trans = new ArrayList<>();
        Long chat = null;
        if (name.startsWith("@")) {
        if (!dbManager.isExist(String.format(
                "select EXISTS(SELECT Account_name FROM Users WHERE Account_name = '%s')", name))) {
            trans.add(String.format("INSERT INTO Users (Account_name) VALUE ('%s')", name));
            trans.add("SET @a = LAST_INSERT_ID();");
            trans.add(String.format("INSERT INTO Access (Wishlist_id, User_id) VALUES (%d, @a);", wishlistId));
        } else {
            trans.add(String.format("INSERT INTO Access (Wishlist_id, User_id) VALUES (%d, (SELECT ID from Users WHERE Account_name='%s'))", wishlistId, name));
        }
        try {
            chat = Long.valueOf(dbManager.getUrls(String.format("SELECT chat_id FROM Users WHERE Account_name = '%s'", name)));
        } catch (Exception e){

        }
    } else {
        try {
            chat = Long.parseLong(name);
            System.out.println(chat);
            trans.add(String.format("INSERT INTO Access (Wishlist_id, User_id) VALUES (%d, (SELECT ID FROM Users WHERE chat_id = %d ))", wishlistId, chat));
        } catch (Exception e) {
            sendTextMessage(name + " - не имя пользователя и не уникальный номер чата пользователя с ботом. \nИмя пользователя должно начинаться со знака @, номер должен состоять только из цифр.\n <b>Доступ не предоставлен</b>");
        }
        name = dbManager.getUrls(String.format("SELECT Account_name FROM Users WHERE chat_id = %d", chat));
            System.out.println(name);
        }

        if (name != null) {
            try {
                dbManager.insertTransaction(trans);
                if (chat != null) {
                    //sendTextMessage(String.format("Пользователь: [*%s*](tg://user?id=%d) получил доступ к вашему вишлисту", name, chat));
                    sendTextMessage(String.format("Пользователь: <a href=\"tg://user?id=%d\"><b>%s</b></a> получил доступ к вашему вишлисту", chat, name));
                } else sendTextMessage(String.format("Пользователь: <b>%s</b> получил доступ к вашему вишлисту", name));
            } catch (SQLException e) {
                sendTextMessage("Не удалось добавить. Попробуйте позже.");
                }
        } else {
            sendTextMessage(chat + " - не найден пользователь с таким id. Проверьте корректность введенных данных");
            }
    }

    public static void main(String[] args) throws TelegramApiException, IOException {

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new Main());

        }
    }
