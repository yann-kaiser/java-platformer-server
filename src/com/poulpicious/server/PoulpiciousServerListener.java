package com.poulpicious.server;

import java.util.ArrayList;

import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.poulpicious.network.packets.Packet00Login;
import com.poulpicious.network.packets.Packet01LoginAcknowledge;
import com.poulpicious.network.packets.Packet02CharacterInfos;
import com.poulpicious.network.packets.Packet04RequestMatchmaking;
import com.poulpicious.network.packets.Packet06StopMatchmaking;
import com.poulpicious.network.packets.Packet08PlayerReady;
import com.poulpicious.network.packets.Packet11PlayerUpdate;
import com.poulpicious.network.packets.Packet12PlayerShoot;
import com.poulpicious.network.packets.Packet13PlayerDeath;

public class PoulpiciousServerListener extends Listener {

	private PoulpiciousServer server;

	public PoulpiciousServerListener(PoulpiciousServer server) {
		this.server = server;
	}

	@Override
	public void connected(Connection c) {
	}

	/**
	 * It is the same code as in the client. The only thing, is that the server sends information from one player to the rest of the players
	 * that are in the same room.
	 */
	@Override
	public void received(Connection c, Object o) {
		if (o instanceof Packet00Login) {
			Packet00Login pl = (Packet00Login) o;

			Packet01LoginAcknowledge pla = new Packet01LoginAcknowledge();

			Document user = DatabaseManager.get().getUser(pl.username);

			if (user == null) {
				pla.accepted = false;
			} else {
				pla.accepted = BCrypt.checkpw(pl.password, (String) user.get("password"));

				if (pla.accepted) {
					PoulpiciousServer.get().writeToConsole("Player '" + pl.username + "' logged in, sending infos");
					this.server.registerPlayer(c, pl.username);
				} else {
					PoulpiciousServer.get().writeToConsole("Wrong password for " + pl.username);
				}
			}
			pla.username = pl.username;
			c.sendTCP(pla);

			if (pla.accepted) {
				ArrayList<Document> characters = DatabaseManager.get().getCharacters(pla.username);

				for (Document charac : characters) {
					server.writeToConsole(charac.toJson());

					Document points = (Document) charac.get("points");

					Packet02CharacterInfos charInfos = new Packet02CharacterInfos();
					charInfos.level = charac.getInteger("level", 1);
					charInfos.experience = charac.getInteger("experience", 0);

					if (points != null) {
						charInfos.healthPoints = points.getInteger("health", 0);
						charInfos.damagePoints = points.getInteger("damage", 0);
						charInfos.resistancePoints = points.getInteger("resistance", 0);
					} else {
						charInfos.healthPoints = 0;
						charInfos.damagePoints = 0;
						charInfos.resistancePoints = 0;
					}

					charInfos.playerID = c.getID();

					c.sendTCP(charInfos);
				}
			}
		} else if (o instanceof Packet02CharacterInfos) {
			Packet02CharacterInfos pci = (Packet02CharacterInfos) o;

			if (pci.save) {
				DatabaseManager.get().saveCharacter(PoulpiciousServer.get().getServerPlayer(c.getID()).getUsername(),
						pci);
			} else {
				ServerPlayer player = server.getServerPlayer(c.getID());
				if (player == null || player.getRoom() == null)
					return;
				
				server.writeToConsole("Sending pci");
				
				for (ServerPlayer otherPlayer : player.getRoom().getPlayers()) {
					if (otherPlayer == player)
						continue;

					otherPlayer.getConnection().sendTCP(pci);
				}
			}
		} else if (o instanceof Packet04RequestMatchmaking) {
			// Start matchmaking for the player
			PoulpiciousServer.get()
					.writeToConsole(server.getServerPlayer(c.getID()).getUsername() + " started searching a game.");
			MatchmakingEngine.get().registerPlayer(server.getServerPlayer(c.getID()));
			// Send response and send packet for "player joined"
		} else if (o instanceof Packet06StopMatchmaking) {
			PoulpiciousServer.get()
					.writeToConsole(server.getServerPlayer(c.getID()).getUsername() + " stopped searching a game.");
			MatchmakingEngine.get().unregisterPlayer(server.getServerPlayer(c.getID()));
			c.sendTCP(o);
		} else if (o instanceof Packet08PlayerReady) {
			Packet08PlayerReady ppr = (Packet08PlayerReady) o;
			ServerPlayer player = server.getServerPlayer(c.getID());
			player.setReady(ppr.ready);

			for (ServerPlayer otherPlayer : player.getRoom().getPlayers()) {
				if (otherPlayer == player)
					continue;

				otherPlayer.getConnection().sendTCP(ppr);
			}

			player.getRoom().verifyAllReady();
		} else if (o instanceof Packet11PlayerUpdate) {
			Packet11PlayerUpdate ppp = (Packet11PlayerUpdate) o;
			ppp.id = c.getID();
			ServerPlayer player = server.getServerPlayer(c.getID());

			for (ServerPlayer otherPlayer : player.getRoom().getPlayers()) {
				if (otherPlayer == player)
					continue;

				otherPlayer.getConnection().sendUDP(ppp);
			}
		} else if (o instanceof Packet12PlayerShoot) {
			Packet12PlayerShoot pps = (Packet12PlayerShoot) o;
			ServerPlayer player = server.getServerPlayer(c.getID());

			for (ServerPlayer otherPlayer : player.getRoom().getPlayers()) {
				if (otherPlayer == player)
					continue;

				otherPlayer.getConnection().sendTCP(pps);
			}
		} else if (o instanceof Packet13PlayerDeath) {
			Packet13PlayerDeath ppd = (Packet13PlayerDeath) o;
			ServerPlayer player = server.getServerPlayer(c.getID());

			for (ServerPlayer otherPlayer : player.getRoom().getPlayers()) {
				if (otherPlayer == player)
					continue;

				otherPlayer.getConnection().sendTCP(ppd);
			}
		}
	}

	@Override
	public void disconnected(Connection c) {
		ServerPlayer player = server.getServerPlayer(c.getID());
		if (player != null) {
			PoulpiciousServer.get().writeToConsole(server.getServerPlayer(c.getID()).getUsername() + " logged out.");

			if (player.getRoom() != null) {
				player.getRoom().removePlayer(player);
			}

			MatchmakingEngine.get().unregisterPlayer(player);
			PoulpiciousServer.get().unregisterPlayer(player);
		} else
			PoulpiciousServer.get().writeToConsole("A non-authenticated player logged out.");
	}

	@Override
	public void idle(Connection c) {
	}
}
