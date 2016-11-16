package be.spyproof.nickmanager.da.player;

import be.spyproof.nickmanager.model.ImmutablePlayerData;
import be.spyproof.nickmanager.model.PlayerData;
import be.spyproof.nickmanager.util.Reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Spyproof on 26/10/2016.
 */

public class MySqlPlayerStorage implements IPlayerStorage
{
    private Connection connection = null;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<ImmutablePlayerData> pendingSaving = new ArrayList<>();
    private static int remainingTasks = 0;

    public MySqlPlayerStorage(String host, int port, String database, String user, String password)
            throws SQLException, IOException
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Where is your MySQL JDBC Driver?");
            e.printStackTrace();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
        }));
        this.connection = DriverManager
                .getConnection("jdbc:mysql://" + host + ':' + port + "/" + database, user, password);
        setupDatabase(database);
    }

    private void setupDatabase(String database) throws SQLException, IOException
    {
        executeSqlFile("mysql/createTables.sql");

        // Create views
        /*if (!connection.prepareStatement("SHOW FULL TABLES IN " + database + " WHERE TABLE_TYPE LIKE 'VIEW';")
                       .executeQuery().next())
            executeSqlFile("mysql/createViews.sql");*/

        // Create functions
        if (!connection.prepareStatement("SHOW FUNCTION STATUS " + "WHERE Db='" + database + "'").executeQuery()
                       .next())
            executeSqlFile("mysql/createFunctions.sql");

        // Create procedures
        if (!connection.prepareStatement("SHOW PROCEDURE STATUS " + "WHERE Db='" + database + "'").executeQuery()
                       .next())
            executeSqlFile("mysql/createProcedures.sql");
    }

    @Override
    public void savePlayer(PlayerData player)
    {
        ImmutablePlayerData immutablePlayer = ImmutablePlayerData.of(player);
        this.pendingSaving.add(immutablePlayer);
        remainingTasks++;
        this.executorService.execute(() -> {
            executeQuery("CALL savePlayer(?, ?)", immutablePlayer.getName(), immutablePlayer.getUuid().toString());
            executeQuery("CALL saveNicknamePlayerData(?, ?, ?, ?)", immutablePlayer.getUuid().toString(),
                         immutablePlayer.getLastChanged(), immutablePlayer.getTokensRemaining(),
                         immutablePlayer.hasAcceptedRules());
            try
            {
                PreparedStatement statement = this.connection.prepareStatement("CALL setActiveNickname(?, ?)");
                statement.setString(1, immutablePlayer.getUuid().toString());
                if (immutablePlayer.getNickname().isPresent())
                    statement.setString(2, immutablePlayer.getNickname().get());
                else
                    statement.setNull(2, Types.VARCHAR);
                statement.execute();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            finally
            {
                pendingSaving.remove(immutablePlayer);
                remainingTasks--;
            }
        });
    }

    @Override
    public void removePlayer(PlayerData player)
    {
        remainingTasks++;
        this.executorService.execute(() -> {
            executeQuery("CALL removePlayer(?)", player.getUuid().toString());
            remainingTasks--;
        });
    }

    @Override
    public Optional<PlayerData> getPlayer(String name)
    {
        Optional<PlayerData> cache = getSavePendingPlayer(name);
        if (cache.isPresent())
            return cache;

        try
        {
            return getPlayerSql(name);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlayerData> getPlayer(UUID uuid)
    {
        Optional<PlayerData> cache = getSavePendingPlayer(uuid);
        if (cache.isPresent())
            return cache;

        try
        {
            return getPlayerSql(uuid);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<PlayerData> getPlayerByNickname(String unformattedNickname, int limit)
    {
        Optional<List<PlayerData>> cache = getSavePendingPlayerByNickname(unformattedNickname, limit);
        if (cache.isPresent())
            return cache.get();

        try
        {
            return getPlayerByNicknameSql(unformattedNickname, limit);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public synchronized void close()
    {
        this.executorService.shutdown();
        if (remainingTasks > 0)
        {
            while (!this.executorService.isTerminated())
            {
                try
                {
                    System.out
                            .println(
                                    Reference.MetaData.PLUGIN_ID + ":MySQLPlayerStorage - Remaining players to save: " + remainingTasks);
                    this.wait(500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        if (this.connection != null)
        {
            try
            {
                if (!this.connection.isClosed())
                    this.connection.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }


    }

    private ResultSet executeQuery(String query, Object... args)
    {
        try
        {
            PreparedStatement prepStatement = connection.prepareStatement(query);
            for (int i = 0; i < args.length; i++)
            {
                if (args[i] instanceof String)
                {
                    prepStatement.setString(i + 1, (String) args[i]);
                } else if (args[i] instanceof Boolean)
                {
                    prepStatement.setBoolean(i + 1, (Boolean) args[i]);
                } else if (args[i] instanceof Integer)
                {
                    prepStatement.setInt(i + 1, (Integer) args[i]);
                } else if (args[i] instanceof Long)
                {
                    prepStatement.setLong(i + 1, (Long) args[i]);
                } else if (args[i] instanceof Double)
                {
                    prepStatement.setDouble(i + 1, (Double) args[i]);
                } else if (args[i] instanceof Float)
                {
                    prepStatement.setFloat(i + 1, (Float) args[i]);
                } else if (args[i] == null)
                {
                    prepStatement.setNull(i + 1, Types.VARCHAR);
                }
            }

            ResultSet data;

            if (query.toLowerCase().startsWith("select"))
            {
                data = prepStatement.executeQuery();
            } else
            {
                int rows = prepStatement.executeUpdate();
                data = null;
            }

            return data;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private Optional<PlayerData> getSavePendingPlayer(UUID uuid)
    {
        ImmutablePlayerData immutablePlayerData = null;
        Iterator<ImmutablePlayerData> iterator = this.pendingSaving.iterator();
        while (iterator.hasNext())
        {
            ImmutablePlayerData playerData = iterator.next();
            if (playerData.getUuid().equals(uuid))
                immutablePlayerData = playerData;
        }

        if (immutablePlayerData == null)
            return Optional.empty();
        else
            return Optional.of(PlayerData.of(immutablePlayerData));
    }

    private Optional<PlayerData> getSavePendingPlayer(String name)
    {
        ImmutablePlayerData immutablePlayerData = null;
        Iterator<ImmutablePlayerData> iterator = this.pendingSaving.iterator();
        while (iterator.hasNext())
        {
            ImmutablePlayerData playerData = iterator.next();
            if (playerData.getName().equalsIgnoreCase(name))
                immutablePlayerData = playerData;
        }

        if (immutablePlayerData == null)
            return Optional.empty();
        else
            return Optional.of(PlayerData.of(immutablePlayerData));
    }

    private Optional<List<PlayerData>> getSavePendingPlayerByNickname(String unformattedNickname, int limit)
    {
        List<ImmutablePlayerData> immutablePlayers = new LinkedList<>();

        Iterator<ImmutablePlayerData> iterator = this.pendingSaving.iterator();
        while (iterator.hasNext() && immutablePlayers.size() < limit)
        {
            ImmutablePlayerData playerData = iterator.next();
            Optional<String> nickname = playerData.getNickname();
            if (nickname.isPresent() && nickname.get().replaceAll(Reference.COLOR_AND_STYLE_PATTERN, "").equalsIgnoreCase(unformattedNickname))
                immutablePlayers.add(playerData);
        }

        if (immutablePlayers.size() == 0)
            return Optional.empty();
        else
        {
            return Optional.of(immutablePlayers.stream().map(PlayerData::of).collect(Collectors.toList()));
        }
    }

    private Optional<PlayerData> getPlayerSql(String name) throws SQLException
    {
        CallableStatement call = this.connection.prepareCall("CALL getNicknameDataByName('" + name + "')");
        call.execute();
        ResultSet set = call.getResultSet();
        if (set == null)
            return Optional.empty();

        if (set.next())
        {
            PlayerData playerData = new PlayerData(name, UUID.fromString(set.getString("UUID")));
            playerData.setNickname(set.getString("Nickname"));
            playerData.setLastChanged(set.getLong("Last changed"));
            playerData.setTokensRemaining(set.getInt("Tokens"));
            playerData.setAcceptedRules(set.getBoolean("Accepted rules"));
            String archive = set.getString("Archived nicknames");
            if (archive != null)
                for (String nick : archive.split(";"))
                    playerData.addPastNickname(nick);
            return Optional.of(playerData);
        }

        return Optional.empty();
    }

    private Optional<PlayerData> getPlayerSql(UUID uuid) throws SQLException
    {
        CallableStatement call = this.connection.prepareCall("CALL getNicknameDataByUuid('" + uuid.toString() + "')");
        call.execute();
        ResultSet set = call.getResultSet();
        if (set == null)
            return Optional.empty();

        if (set.next())
        {
            PlayerData playerData = new PlayerData(set.getString("Last known name"), uuid);
            playerData.setNickname(set.getString("Nickname"));
            playerData.setLastChanged(set.getLong("Last changed"));
            playerData.setTokensRemaining(set.getInt("Tokens"));
            playerData.setAcceptedRules(set.getBoolean("Accepted rules"));
            String archive = set.getString("Archived nicknames");
            if (archive != null)
                for (String nick : archive.split(";"))
                    playerData.addPastNickname(nick);
            return Optional.of(playerData);
        }

        return Optional.empty();
    }

    private List<PlayerData> getPlayerByNicknameSql(String unformattedNickname, int limit) throws SQLException
    {
        List<PlayerData> players = new ArrayList<>();

        CallableStatement call = this.connection
                .prepareCall("CALL getNicknameDataByNickname('" + unformattedNickname + "'," + limit + ")");
        call.execute();
        ResultSet set = call.getResultSet();
        while (set != null && set.next())
        {
            PlayerData playerData = new PlayerData(set.getString("Last known name"),
                                                   UUID.fromString(set.getString("UUID")));
            playerData.setNickname(set.getString("Nickname"));
            String archive = set.getString("Archived nicknames");
            if (archive != null)
                for (String nick : archive.split(";"))
                    playerData.addPastNickname(nick);
            players.add(playerData);
        }


        return players;
    }

    private void executeSqlFile(String path) throws IOException, SQLException
    {
        executeMultipleStatements(readInnerFile(path));
    }

    private void executeMultipleStatements(Collection<String> bigQuery) throws SQLException
    {
        String delimiter = ";";
        Statement s = connection.createStatement();

        StringBuilder statement = new StringBuilder();
        for (String sqlQuery : bigQuery)
        {
            sqlQuery = sqlQuery.replace('\n', ' ').trim();
            if (!sqlQuery.startsWith("--") && !sqlQuery.startsWith("//") && sqlQuery.length() > 0)
            {
                int delimitedIndex = sqlQuery.toLowerCase().indexOf("delimiter ");
                if (delimitedIndex == -1)
                {
                    statement.append(sqlQuery.replace(delimiter, ";")).append('\n');

                    if (sqlQuery.length() > 0 && sqlQuery.endsWith(delimiter))
                    {
                        s.addBatch(statement.toString());
                        statement = new StringBuilder();
                    }
                } else
                {
                    delimiter = sqlQuery.substring(10);
                }
            }
        }
        s.executeBatch();
    }

    private List<String> readInnerFile(String path) throws IOException
    {
        List<String> fileContent = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(this.getClass().getResourceAsStream('/' + path), "UTF-8"));

        String line;
        while ((line = reader.readLine()) != null)
            fileContent.add(line);

        return fileContent;
    }
}
