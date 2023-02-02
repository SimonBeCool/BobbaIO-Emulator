package io.bobba.poc.core.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.users.LoginOkComposer;
import io.bobba.poc.communication.outgoing.users.UpdateCreditsBalanceComposer;
import io.bobba.poc.communication.outgoing.users.UpdateHabboClubComposer;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.misc.logging.LogLevel;
import io.bobba.poc.misc.logging.Logging;


public class UserManager {
	private Map<Integer, User> users;
	private int nextId;
	private static final int NUM_FRIENDS = 10;
	
	public UserManager() {
		this.users = new ConcurrentHashMap<>();
		this.nextId = 1;
	}
	
	public User getUser(int id) {
		return users.getOrDefault(id, null);
	}
	
	private void addDummyFriends(User user) {
		int friendsAdded = 0;
		Iterator<User> it = users.values().iterator();
		while (it.hasNext() && friendsAdded < NUM_FRIENDS) {
			User otherUser = it.next();
			if (user != otherUser) {
				user.getMessenger().addHardFriendship(otherUser);
				friendsAdded++;	
			}
		}
		user.getMessenger().serializeFriends();
	}
	
	public void tryLogin(GameClient client, String username, String look) {
	    if (client.getUser() == null) {
	        User user = getUserFromDB(username, client);
	        if (user == null) {
	            user = addUserToDB(username, look, client);
	        }
	        client.setUser(user);            

	        client.sendMessage(new LoginOkComposer(user.getId(), user.getUsername(), user.getLook(), user.getMotto()));
	        client.sendMessage(new UpdateCreditsBalanceComposer(user.getCredits()));
	        client.sendMessage(new UpdateHabboClubComposer(user.getHabboClub()));
	        
	        if (Logging.getInstance().getLogLevel() == LogLevel.Verbose) {
	            Logging.getInstance().writeLine(client.getUser().getUsername() + " (" + client.getUser().getId() + ") has logged in!", LogLevel.Verbose, this.getClass()); 
	        }
	            
	        addDummyFriends(user);
	    } else {
	        Logging.getInstance().writeLine("Client already logged!", LogLevel.Warning, this.getClass());
	        client.stop();
	    }
	}
	
	public User getUserFromDB(String username, GameClient client) {
	    Connection connection = null;
	    PreparedStatement statement = null;
	    ResultSet resultSet = null;

	    try {
	        connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	        statement = connection.prepareStatement("SELECT * FROM users WHERE username = ?");
	        statement.setString(1, username);
	        resultSet = statement.executeQuery();

	        if (resultSet.next()) {
	            int id = resultSet.getInt("id");
	            int rank = resultSet.getInt("rank");
	            int credits = resultSet.getInt("credits");
	            int hc_days = resultSet.getInt("hc_days");
	            int home_room_id = resultSet.getInt("home_room_id");
	            String look = resultSet.getString("look");
	            String motto = resultSet.getString("motto");

	            User user = new User(id, username, motto, look, rank, credits, hc_days, home_room_id, client);
	            return user;
	        }
	    } catch (SQLException e) {
	        Logging.getInstance().writeLine("Failed to get user from database: " + e.getMessage(), LogLevel.Warning, this.getClass());
	    } finally {
	        try {
	            if (resultSet != null) {
	                resultSet.close();
	            }
	            if (statement != null) {
	                statement.close();
	            }
	            if (connection != null) {
	                connection.close();
	            }
	        } catch (SQLException e) {
	            Logging.getInstance().writeLine("Failed to close resources: " + e.getMessage(), LogLevel.Warning, this.getClass());
	        }
	    }

	    return null;
	}
	private User addUserToDB(String username, String look, GameClient client) {
	    Connection connection = null;
	    PreparedStatement statement = null;
	    ResultSet resultSet = null;

	    try {
	        connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();

	        statement = connection.prepareStatement("INSERT INTO users (username, look, motto, `rank`, credits, hc_days, home_room_id) VALUES (?, ?, ?, 1, 1337, 14, 1)", Statement.RETURN_GENERATED_KEYS);
	        statement.setString(1, username);
	        statement.setString(2, look);
	        statement.setString(3, "I love BobbaIO <3");
	        statement.executeUpdate();

	        resultSet = statement.getGeneratedKeys();
	        if (resultSet.next()) {
	            int userId = resultSet.getInt(1);

	            User user = new User(userId, username, "I love BobbaIO <3", look, 1, 1337, 14, 1, client);
	            return user;
	        }
	    } catch (SQLException e) {
	        Logging.getInstance().writeLine("Error adding user to the database: " + e.getMessage(), LogLevel.Warning, this.getClass());
	    } finally {
	        try {
	            if (resultSet != null) { resultSet.close(); }
	            if (statement != null) { statement.close(); }
	            if (connection != null){ connection.close();}
	        } catch (SQLException e) {
	            Logging.getInstance().writeLine("Error closing database connection: " + e.getMessage(), LogLevel.Warning, this.getClass());
	        }
	    }
	    return null;
	}
}
