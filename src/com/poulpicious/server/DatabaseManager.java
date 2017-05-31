package com.poulpicious.server;

import java.util.ArrayList;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.poulpicious.network.packets.Packet02CharacterInfos;

public class DatabaseManager {

	private static class DatabaseManagerHolder {
		private static final DatabaseManager _instance = new DatabaseManager();
	}

	public static DatabaseManager get() {
		return DatabaseManagerHolder._instance;
	}

	private MongoClient databaseClient;
	private MongoCollection<Document> usersCollection;
	private MongoCollection<Document> charactersCollection;

	public void connect() {
		databaseClient = new MongoClient();
		usersCollection = databaseClient.getDatabase("poulpicious").getCollection("users");
		charactersCollection = databaseClient.getDatabase("poulpicious").getCollection("characters");
	}

	public void disconnect() {
		databaseClient.close();
	}

	public void createUser(String username, String password) {
		usersCollection.insertOne(new Document("username", username).append("password", password));
	}

	public Document getUser(String username) {
		try {
			FindIterable<Document> user = usersCollection.find(new Document("username", username));

			return user.first();
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<Document> getCharacters(String owner) {
		try {
			ArrayList<Document> charsList = new ArrayList<Document>();
			FindIterable<Document> characs = charactersCollection.find(new Document("owner", owner));

			characs.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					charsList.add(document);
				}
			});

			if (charsList.isEmpty()) {
				System.out.println("Creating a new character for '" + owner + "'.");
				charactersCollection.insertOne(new Document("owner", owner).append("level", 1));
				charsList.add(charactersCollection.find(new Document("owner", owner)).first());
			}

			return charsList;
		} catch (Exception e) {
			return null;
		}
	}

	public void saveCharacter(String owner, Packet02CharacterInfos infos) {
		PoulpiciousServer.get().writeToConsole("Saving a character " + infos.healthPoints);

		Document charac = new Document("owner", owner).append("level", infos.level).append("experience",
				infos.experience);
		charac.append("points.health", infos.healthPoints);
		charac.append("points.damage", infos.damagePoints);
		charac.append("points.resistance", infos.resistancePoints);
		charactersCollection.updateOne(new Document("owner", owner), new Document("$set", charac));
	}

}
