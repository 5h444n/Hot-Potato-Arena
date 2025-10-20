package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.UserDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LeaderboardController {

    @FXML private TableView<Map.Entry<String, Integer>> leaderboardTable;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> rankColumn;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> usernameColumn;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> scoreColumn;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // Set cell value factories for columns
        rankColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("#" + (leaderboardTable.getItems().indexOf(cellData.getValue()) + 1))
        );
        usernameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKey())
        );
        scoreColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue().toString())
        );

        // Load data
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        List<Map.Entry<String, Integer>> scores = userDAO.getDeathmatchLeaderboard();
        ObservableList<Map.Entry<String, Integer>> observableScores = FXCollections.observableArrayList(scores);
        leaderboardTable.setItems(observableScores);
    }

    @FXML
    private void handleBack() {
        switchToView("/com/demo/game/fxml/gamemodeselection.fxml");
    }

    private void switchToView(String fxmlFile) {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
            FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
        } catch (IOException e) {
            System.err.println("Failed to switch view: " + e.getMessage());
        }
    }
}