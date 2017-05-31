package com.poulpicious.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.poulpicious.network.packets.Packet00Login;
import com.poulpicious.network.packets.Packet01LoginAcknowledge;
import com.poulpicious.network.packets.Packet02CharacterInfos;
import com.poulpicious.network.packets.Packet03CharacterInfosResponse;
import com.poulpicious.network.packets.Packet04RequestMatchmaking;
import com.poulpicious.network.packets.Packet05MatchmakingResponse;
import com.poulpicious.network.packets.Packet06StopMatchmaking;
import com.poulpicious.network.packets.Packet07PlayerJoin;
import com.poulpicious.network.packets.Packet08PlayerReady;
import com.poulpicious.network.packets.Packet09StartGame;
import com.poulpicious.network.packets.Packet10PlayerDisconnect;
import com.poulpicious.network.packets.Packet11PlayerUpdate;
import com.poulpicious.network.packets.Packet12PlayerShoot;
import com.poulpicious.network.packets.Packet13PlayerDeath;

import javafx.scene.control.TextArea;

public class PoulpiciousServer {

	private static class PoulpiciousServerHolder {
		private static final PoulpiciousServer _instance = new PoulpiciousServer();
	}

	public static PoulpiciousServer get() {
		return PoulpiciousServerHolder._instance;
	}

	private Server server;

	private TextArea console;

	private Map<Integer, ServerPlayer> serverPlayers = new HashMap<Integer, ServerPlayer>();
	private List<Room> rooms;

	private Thread matchmakingEngineThread;

	// The classes are registered.
	private PoulpiciousServer() {
		this.server = new Server();
		this.rooms = Collections.synchronizedList(new ArrayList<Room>());

		DatabaseManager.get().connect();

		Kryo k = this.server.getKryo();
		k.register(Packet00Login.class);
		k.register(Packet01LoginAcknowledge.class);
		k.register(Packet02CharacterInfos.class);
		k.register(Packet03CharacterInfosResponse.class);
		k.register(Packet04RequestMatchmaking.class);
		k.register(Packet05MatchmakingResponse.class);
		k.register(Packet06StopMatchmaking.class);
		k.register(Packet07PlayerJoin.class);
		k.register(Packet08PlayerReady.class);
		k.register(Packet09StartGame.class);
		k.register(Packet10PlayerDisconnect.class);
		k.register(Packet11PlayerUpdate.class);
		k.register(Packet12PlayerShoot.class);
		k.register(Packet13PlayerDeath.class);
	}

	// The server is started.
	public void start(TextArea console) {
		this.console = console;
		try {
			this.rooms.add(new Room());
			server.addListener(new PoulpiciousServerListener(this));

			// We start the matchmaking engine in background, so it nevers stops looking for players to put in rooms
			// Should definitely be put in 'wait mode' when no players are searching a game.
			this.matchmakingEngineThread = new Thread(MatchmakingEngine.get(), "matchmaking");
			this.matchmakingEngineThread.start();

			server.start();
			server.bind(25565, 25565);

			writeToConsole("Master server started.");
		} catch (IOException e) {
			e.printStackTrace();
			writeToConsole("Master server couldn't start.");
		}
	}

	public void writeToConsole(String text) {
		if (console != null)
			console.appendText(text + "\n");
	}

	public void registerPlayer(Connection c, String username) {
		this.serverPlayers.put(c.getID(), new ServerPlayer(c, username));
	}

	public void unregisterPlayer(ServerPlayer player) {
		this.serverPlayers.remove(player.getConnection().getID());
	}

	public ServerPlayer getServerPlayer(int connID) {
		return this.serverPlayers.get(connID);
	}

	public Room getRoom(int index) {
		synchronized (rooms) {
			return this.rooms.get(0);
		}
	}

	public List<Room> getRooms() {
		return rooms;
	}

	public void quit() {
		MatchmakingEngine.get().setRunning(false);
		this.server.stop();
		DatabaseManager.get().disconnect();
	}

}
