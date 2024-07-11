

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class DbManager {
    public static final String DB_URL;
    private static final String user;
    private static final String password;
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;

    static {
        Properties properties = new Properties();
        try {
            var is = ClassLoader.getSystemResourceAsStream("requests/properties_test.properties");
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DB_URL = properties.getProperty("DB_URL");
        user = properties.getProperty("user");
        password = properties.getProperty("password");
    }

    public boolean isExist (String request) throws SQLException {
        synchronized (this) {
            con = DriverManager.getConnection(DB_URL, user, password);
            stmt = con.createStatement();
            rs = stmt.executeQuery(request);
            String result = null;
            while (rs.next()) {
                result = rs.getString(1);
            }
            con.close();
            return Integer.parseInt(result) == 1;
        }
    }

    public boolean insertOrDelete(String request) throws SQLException {
        System.out.println(request);
        synchronized (this) {
            try {
                con = DriverManager.getConnection(DB_URL, user, password);
                stmt = con.createStatement();
                stmt.executeUpdate(request);
            } catch (SQLException e) {
                return false;
            }
            con.close();
            return true;
        }
    }

    public boolean insertTransaction (ArrayList <String> list) throws SQLException {
        synchronized (this) {
            try {
                con = DriverManager.getConnection(DB_URL, user, password);
                stmt = con.createStatement();
                con.setAutoCommit(false);
                for (String request : list) {
                    stmt.executeUpdate(request);
                }
                con.commit();
                return true;
            } catch (Exception e) {
                con.rollback();
                return false;
            } finally {
                con.close();
            }
        }
    }

    public Date getBirthday (String request) throws SQLException, ParseException {
        synchronized (this) {
            con = DriverManager.getConnection(DB_URL, user, password);
            stmt = con.createStatement();
            rs = stmt.executeQuery(request);
            String result = null;
            while (rs.next()) {
                result = rs.getString(1);
            }
            if (result != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date date = format.parse(result);
                return date;
            }
            con.close();
            return null;
        }
    }

    public String getEvent (int wishlistId) throws SQLException, ParseException {
        synchronized (this) {
            con = DriverManager.getConnection(DB_URL, user, password);
            stmt = con.createStatement();
            rs = stmt.executeQuery(String.format("select Event, Date from Wishlists where ID = %d", wishlistId));
            String result = null;
            while (rs.next()) {
                String event = rs.getString(1);
                String date = rs.getString(2);
                result = String.format("<b>%s</b>, которое состоится <b>%s</b>",
                        event, new SimpleDateFormat("dd MMMM yyyy", new Locale("ru")).format(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(date)));
            }
            con.close();
            return result;
        }
    }

    public ArrayList <String> getLists(String request, String str) throws SQLException {
        synchronized (this) {
            con = DriverManager.getConnection(DB_URL, user, password);
            stmt = con.createStatement();
            ArrayList<String> list = new ArrayList<>();
            rs = stmt.executeQuery(request);
            while (rs.next()) {
                String result = rs.getString(1);
                String id = rs.getString(2);
                if (result.isEmpty()){
                    result = id;
                }
                if (id == null || id.isEmpty()) {
                    id = "https://безссылки.ru";
                }
                list.add(result);
                list.add(str + id);
            }
            System.out.println(list);
            con.close();
            return list;
        }
    }

    public String getUrls (String request) {
        synchronized (this){
            String result = null;
            try {
                con = DriverManager.getConnection(DB_URL, user, password);
                stmt = con.createStatement();
                rs = stmt.executeQuery(request);
                while (rs.next()) {
                    result = rs.getString(1);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return result;
        }

    }

    public boolean getStatus (String request) throws SQLException {
        synchronized (this) {
            con = DriverManager.getConnection(DB_URL, user, password);
            stmt = con.createStatement();
            rs = stmt.executeQuery(request);
            String result = null;
            while (rs.next()) {
                result = rs.getString(1);
            }
            con.close();
            return result.equals("empty");
        }
    }

    public User getUserInfo (String request) throws SQLException {
        synchronized (this) {
            con = DriverManager.getConnection(DB_URL, user, password);
            stmt = con.createStatement();
            User user = new User();
            rs = stmt.executeQuery(request);
            while (rs.next()) {
                user.name = rs.getString(1);
                user.birthday = rs.getString(2);
                String sex1 = rs.getString(3);
                if (sex1 != null) {
                    int sex2 = Integer.parseInt(sex1);
                    switch (sex2) {
                        case 0:
                            user.sex = Sex.FEMALE;
                            break;
                        case 1:
                            user.sex = Sex.MALE;
                            break;
                    }
                }
            }
            con.close();
            return user;
        }
    }
}
