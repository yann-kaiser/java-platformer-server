package com.poulpicious.server;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * I had to create this file to ensure JavaFX would run, since my code was hard to reorganize in PoulpiciousServer.class
 * 
 * @author yann
 *
 */
public class ServerApplication extends Application {
	private TextArea console;

	@Override
	public void start(Stage primaryStage) throws Exception {
		StackPane root = new StackPane();
		console = new TextArea();
		console.setEditable(false);
		root.getChildren().add(console);

		Scene scene = new Scene(root, 870, 530);

		primaryStage.setTitle("Poulpicious Server");
		primaryStage.setScene(scene);
		
		PoulpiciousServer.get().start(console);
		
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
		
		PoulpiciousServer.get().quit();
	}

}
