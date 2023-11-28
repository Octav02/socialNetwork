package com.projects.socialnetwork;

import com.projects.socialnetwork.controllers.LoginController;
import com.projects.socialnetwork.repositories.databaseRepository.FriendshipDBRepository;
import com.projects.socialnetwork.repositories.databaseRepository.UserDBRepository;
import com.projects.socialnetwork.services.NetworkService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    private NetworkService service;

    @Override
    public void start(Stage stage) throws IOException {



        String url = "jdbc:postgresql://localhost:5432/social_network";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        UserDBRepository userDBRepository = new UserDBRepository(url, username, password);
        FriendshipDBRepository friendshipDBRepository = new FriendshipDBRepository(url, username, password, userDBRepository);
        service = new NetworkService(userDBRepository, friendshipDBRepository);
        initView(stage);
        stage.show();
    }

    private void initView(Stage stage) throws IOException {
        FXMLLoader loginLoader = new FXMLLoader();
        loginLoader.setLocation(getClass().getResource("views/login-view.fxml"));
        AnchorPane loginLayout = loginLoader.load();
        stage.setScene(new Scene(loginLayout));

        LoginController loginController = loginLoader.getController();
        loginController.setService(service);
    }

    public static void main(String[] args) {

        launch();
    }
}