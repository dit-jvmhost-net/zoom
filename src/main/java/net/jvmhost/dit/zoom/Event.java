package net.jvmhost.dit.zoom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.json.JSONException;
import org.json.JSONObject;

import utils.Log;

class Event {

	private final Supplier<PreparedStatement> statement;
	private final String object;
	private final String action;
	private final Timestamp time;
	private final JSONObject json;
	private final String account;
	private static final String SET_HEADERS = ",clientid = ?, authorization = ?, x_zm_signature = ?, x_zm_request_timestamp = ?, x_zm_trackingid = ?";
	private static final String SQL = "insert into event set object = ?, action = ?, timestamp = ?, json = ?, account = ?" + SET_HEADERS;
	private final Function<String, String> headers;
	
	Event(Connection connection, JSONObject input, UnaryOperator<String> headers) {
		this.headers = headers;
		this.statement = () -> {
			try {
				return connection.prepareStatement(SQL);
			} catch (SQLException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		};
		try {
			String string = input.getString("event");
			int dot = string.indexOf('.');
			object = string.substring(0, dot);
			action = string.substring(dot + 1);
			time = new Timestamp(input.getLong("event_ts"));
			JSONObject payload = input.getJSONObject("payload");
			json = payload.getJSONObject("object");
			account = payload.getString("account_id");
		} catch (JSONException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	void insert() throws SQLException {
		try (PreparedStatement sql = statement.get()) {
			sql.setString(1, object);
			sql.setString(2, action);
			sql.setTimestamp(3, time);
			sql.setString(4, json.toString());
			sql.setString(5, account);
			Log.info(() -> "myLog account: " + account);
			sql.setString(6, headers.apply("clientId"));
			sql.setString(7, headers.apply("authorization"));
			sql.setString(8, headers.apply("x-zm-signature"));
			String zmRequestTimestamp = headers.apply("x-zm-request-timestamp");
			Log.info(() -> "x-zm-request-timestamp: " + zmRequestTimestamp);
			sql.setTimestamp(9, new Timestamp(Long.parseLong(zmRequestTimestamp) * 1000));
			sql.setString(10, headers.apply("x-zm-trackingid"));
			sql.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
}
