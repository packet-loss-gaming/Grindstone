package com.skelril.aurora.jail;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class MySQLInmateDatbase implements InmateDatabase {

    private final String ip;
    private final String user;
    private final String pass;
    private final String table;
    private Connection conn;

    private final Logger log = CommandBook.inst().getLogger();

    /**
     * Used to lookup inmates by name
     */
    protected Map<String, Inmate> nameInmate = new HashMap<>();

    /**
     * A set of all inmates
     */
    protected final List<Inmate> inmates = new ArrayList<>();

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public MySQLInmateDatbase(String ip, String user, String pass, String table) {

        this.ip = ip;
        this.user = user;
        this.pass = pass;
        this.table = table;
    }

    private Connection getConnection() throws SQLException {

        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(ip, user, pass);
        }
        return conn;
    }

    @Override
    public boolean load() {

        boolean successful = true;

        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            conn = getConnection();
            String sql = "SELECT * FROM " + table;
            statement = conn.prepareStatement(sql);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String inmateName = resultSet.getString(2);
                String prisonName = resultSet.getString(3);
                String reason = resultSet.getString(4);
                int startDate = resultSet.getInt(5);
                int endDate = resultSet.getInt(6);

                Inmate inmate = new Inmate(inmateName, prisonName, reason, startDate, endDate);
                if (inmateName != null)
                    nameInmate.put(inmateName, inmate);
                inmates.add(inmate);
            }

            log.info(inmates.size() + " jailed name(s) loaded.");
        } catch (SQLException sql) {
            nameInmate = new HashMap<>();
            log.warning("Failed to load " + table.toLowerCase() + ".");
            successful = false;
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (conn != null) {
                    conn.close();
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return successful;
    }

    @Override
    public boolean save() {

        Connection conn = null;
        ResultSet resultSet = null;

        try {
            try {
                conn = getConnection();
                String sql = "CREATE TABLE `" + table + "` ("
                        + "`id` int(64) NOT NULL AUTO_INCREMENT,"
                        + "`inmate_name` varchar(255) NOT NULL,"
                        + "`prison_name` varchar(255) NOT NULL,"
                        + "`reason` varchar(255) NOT NULL,"
                        + "`startdate` int(64) NOT NULL,"
                        + "`enddate` int(64) NOT NULL,"
                        + "UNIQUE (`id`),"
                        + "FULLTEXT(`inmate_name`, `prison_name`, `reason`))";
                conn.prepareStatement(sql).execute();
            } catch (SQLException ignored) {
            }

            resultSet = conn.prepareStatement("SELECT `inmate_name` FROM " + table).executeQuery();

            List<String> names = new ArrayList<>();

            while (resultSet.next()) {
                names.add(resultSet.getString(0));
            }
            for (Inmate inmate : inmates) {
                if (!names.contains(inmate.getName())) {
                    conn.prepareStatement("INSERT INTO `" + table
                            + "` (`inmate_name`, `prison_name`, `reason`, `startdate`, `enddate`) "
                            + "VALUES ('" + inmate.getName() + "', '"
                            + inmate.getPrisonName() + "', `"
                            + inmate.getReason() + "`, `"
                            + inmate.getStart() + "`, `"
                            + inmate.getEnd() + "`)").executeQuery();
                }
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    @Override
    public boolean unload() {

        return true;
    }

    @Override
    public boolean isJailedName(String name) {

        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getJailedNameMessage(String name) {

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void jail(Player player, String prison, CommandSender source, String reason, long end) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void jail(String name, String prison, CommandSender source, String reason, long end) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean unjail(Player player, CommandSender source, String reason) {

        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean unjail(String name, CommandSender source, String reason) {

        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Inmate getJailedName(String name) {

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Inmate> getInmatesList() {

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Iterator<Inmate> iterator() {

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
