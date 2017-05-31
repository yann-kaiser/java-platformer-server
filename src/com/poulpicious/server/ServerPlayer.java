package com.poulpicious.server;

import com.esotericsoftware.kryonet.Connection;

public class ServerPlayer {
	
	private Connection connection;
	private String username;
	private Room room;
	private boolean ready;
	
	public ServerPlayer(Connection c, String username) {
		this.connection = c;
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public void setRoom(Room room) {
		this.room = room;
	}
	
	public Room getRoom() {
		return this.room;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

}
