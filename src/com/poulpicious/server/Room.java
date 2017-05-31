package com.poulpicious.server;

import java.util.Collection;
import java.util.HashMap;

import com.poulpicious.network.packets.Packet09StartGame;
import com.poulpicious.network.packets.Packet10PlayerDisconnect;

/**
 * Basically contains the list of players in the current room.
 * @author yann
 *
 */
public class Room {

	private HashMap<Integer, ServerPlayer> players;

	public Room() {
		this.players = new HashMap<Integer, ServerPlayer>();
	}

	public void addPlayer(ServerPlayer player) {
		this.players.put(player.getConnection().getID(), player);
		player.setRoom(this);
	}

	public Collection<ServerPlayer> getPlayers() {
		return players.values();
	}

	public int getPlayerCount() {
		return players.size();
	}

	public void verifyAllReady() {
		boolean canStart = true;
		for (ServerPlayer pl : players.values()) {
			canStart &= pl.isReady();
		}

		if (canStart) {
			Packet09StartGame psg = new Packet09StartGame();
			for (ServerPlayer pl : players.values()) {
				pl.getConnection().sendTCP(psg);
			}
		}
	}

	public void removePlayer(ServerPlayer player) {
		this.players.remove(player.getConnection().getID());
		
		Packet10PlayerDisconnect ppd = new Packet10PlayerDisconnect();
		ppd.id =  player.getConnection().getID();
		for (ServerPlayer pl : players.values()) {
			pl.getConnection().sendTCP(ppd);
		}
	}

}
