package com.poulpicious.server;

import java.util.ArrayList;

import com.poulpicious.network.packets.Packet05MatchmakingResponse;
import com.poulpicious.network.packets.Packet07PlayerJoin;

public class MatchmakingEngine implements Runnable {

	private static class MatchmakingEngineHolder {
		private static final MatchmakingEngine _instance = new MatchmakingEngine();
	}

	public static MatchmakingEngine get() {
		return MatchmakingEngineHolder._instance;
	}

	private boolean running;

	private ArrayList<ServerPlayer> playersSearching = new ArrayList<ServerPlayer>();

	public MatchmakingEngine() {
		this.running = true;
	}

	@Override
	public void run() {
		while (running) {
			synchronized (playersSearching) {
				if (playersSearching.size() > 0) {
					ServerPlayer current = playersSearching.get(0);

					Room room = PoulpiciousServer.get().getRoom(0);

					Packet05MatchmakingResponse pmr = new Packet05MatchmakingResponse();
					pmr.nbPlayers = room.getPlayerCount();
					current.getConnection().sendTCP(pmr);
					room.addPlayer(current);
					
					Packet07PlayerJoin ppj = new Packet07PlayerJoin();
					ppj.id = current.getConnection().getID();
					ppj.username = current.getUsername();

					for (ServerPlayer p : room.getPlayers()) {
						p.getConnection().sendTCP(ppj);

						Packet07PlayerJoin ppjToJoiner = new Packet07PlayerJoin();
						ppjToJoiner.id = p.getConnection().getID();
						ppjToJoiner.username = p.getUsername();

						current.getConnection().sendTCP(ppjToJoiner);
					}

					playersSearching.remove(0);
				}
			}
		}
	}

	public void registerPlayer(ServerPlayer player) {
		synchronized (playersSearching) {
			this.playersSearching.add(player);
		}
	}

	public void unregisterPlayer(ServerPlayer serverPlayer) {
		synchronized (playersSearching) {
			if (this.playersSearching.contains(serverPlayer))
				this.playersSearching.remove(serverPlayer);
		}
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

}
