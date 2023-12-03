package com.projects.socialnetwork.controllers;

import com.projects.socialnetwork.dtos.UserFriendDTO;
import com.projects.socialnetwork.models.Friendship;
import com.projects.socialnetwork.models.Message;
import com.projects.socialnetwork.models.User;
import com.projects.socialnetwork.services.NetworkService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HomeController {
    public Label welcomeUserLabel;
    public TableView<UserFriendDTO> tableViewFriends;
    public TableView<User> tableViewAllUsers;
    public TableColumn<User, String> tableColumnUsername;
    public TableColumn<User, String> tableColumnEmail;
    public TableColumn<User, String> tableColumnFirstName;
    public TableColumn<User, String> tableColumnLastName;
    public TableColumn<UserFriendDTO, String> friendTableColumnUsername;
    public TableColumn<UserFriendDTO, String> friendTableColumnEmail;
    public TableColumn<UserFriendDTO, String> tableColumnFriendsSince;
    public ListView<Friendship> pendingRequestListView;
    public ListView<UserFriendDTO> messageFriendsListView;
    public ListView<Message> messagesListView;
    public TextField messageTextField;

    private NetworkService service;

    private User loggedInUser;

    private User selectedUser;

    private ObservableList<User> allUsersModel = FXCollections.observableArrayList();

    private ObservableList<UserFriendDTO> friendsModel = FXCollections.observableArrayList();

    private ObservableList<Friendship> recievedFriendRequestModel = FXCollections.observableArrayList();


    public void setService(NetworkService service) {
        this.service = service;
        initModel();
    }

    private void initModel() {
        Iterable<User> users = service.getAllUsers();
        List<User> userList = StreamSupport.stream(users.spliterator(), false)
                .toList();
        allUsersModel.setAll(userList);

        Iterable<UserFriendDTO> friends = service.getFriendsOfUser(loggedInUser.getId());
        List<UserFriendDTO> friendsList = StreamSupport.stream(friends.spliterator(), false)
                .toList();
        friendsModel.setAll(friendsList);

        Iterable<Friendship> friendRequests = service.getPendingFriendRequests(loggedInUser.getId());
        List<Friendship> friendRequestList = StreamSupport.stream(friendRequests.spliterator(), false)
                .toList();
        recievedFriendRequestModel.setAll(friendRequestList);


    }

    @FXML
    private void initialize() {
        tableColumnUsername.setCellValueFactory(new PropertyValueFactory<User, String>("username"));
        tableColumnEmail.setCellValueFactory(new PropertyValueFactory<User, String>("email"));
        tableColumnFirstName.setCellValueFactory(new PropertyValueFactory<User, String>("firstName"));
        tableColumnLastName.setCellValueFactory(new PropertyValueFactory<User, String>("lastName"));
        tableViewAllUsers.setItems(allUsersModel);

        friendTableColumnUsername.setCellValueFactory(new PropertyValueFactory<UserFriendDTO, String>("username"));
        friendTableColumnEmail.setCellValueFactory(new PropertyValueFactory<UserFriendDTO, String>("email"));
        tableColumnFriendsSince.setCellValueFactory(new PropertyValueFactory<UserFriendDTO, String>("friendsSince"));
        tableViewFriends.setItems(friendsModel);

        messageFriendsListView.setItems(friendsModel);


        pendingRequestListView.setItems(recievedFriendRequestModel);

        messageFriendsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateMessagesListView(newValue);
            }
        });

        messagesListView.setCellFactory(param -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getMessage());
                    if (item.getFrom().equals(loggedInUser)) {
                        setAlignment(Pos.CENTER_RIGHT);
                        setStyle("-fx-background-color: lightblue;");
                    } else {
                        setAlignment(Pos.CENTER_LEFT);
                        setStyle("-fx-background-color: lightgreen;");
                    }
                }
            }
        });

    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
        System.out.println(loggedInUser);
        welcomeUserLabel.setText("Welcome " + loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
    }

    public void handleSendRequest(ActionEvent event) {
        try {
            service.sendFriendRequest(loggedInUser.getId(), selectedUser.getId());

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void handleUserSelection(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
            selectedUser = tableViewAllUsers.getSelectionModel().getSelectedItem();
        }
    }

    public void handleLogOut(ActionEvent event) throws IOException {
        FXMLLoader loginLoader = new FXMLLoader();
        loginLoader.setLocation(getClass().getResource("/com/projects/socialnetwork/views/login-view.fxml"));
        Stage homeStage = (Stage) ((Node) event.getSource()).getScene().getWindow();


        AnchorPane loginLayout = loginLoader.load();
        Scene scene = new Scene(loginLayout);
        homeStage.setScene(scene);


        LoginController loginController = loginLoader.getController();
        loginController.setService(service);

        homeStage.show();

    }


    @FXML
    public void handleAcceptRequest(ActionEvent event) {
        User sender = pendingRequestListView.getSelectionModel().getSelectedItem().getUser1();
        User receiver = pendingRequestListView.getSelectionModel().getSelectedItem().getUser2();

        try {
            service.acceptFriendRequest(sender.getId(), receiver.getId());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setContentText("Friend request accepted!");
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        initModel();
    }

    @FXML
    public void handleRejectRequest(ActionEvent event) {
        User sender = pendingRequestListView.getSelectionModel().getSelectedItem().getUser1();
        User receiver = pendingRequestListView.getSelectionModel().getSelectedItem().getUser2();

        try {
            service.declineFriendRequest(sender.getId(), receiver.getId());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setContentText("Friend request declined!");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        initModel();
    }

    private void populateMessagesListView(UserFriendDTO selectedFriend) {
        User friendUser = service.getUserByUsername(selectedFriend.getUsername());
        Iterable<Message> messages = service.getMessagesBetweenUsers(loggedInUser, friendUser);
        List<Message> messagesList = StreamSupport.stream(messages.spliterator(), false)
                .sorted(Comparator.comparing(Message::getSentAt))
                .collect(Collectors.toList());
        ObservableList<Message> messagesModel = FXCollections.observableArrayList(messagesList);
        messagesListView.setItems(messagesModel);
    }

    public void handleDeleteFriend(ActionEvent event) {
        User sender = service.getUserByUsername(tableViewFriends.getSelectionModel().getSelectedItem().getUsername());
        User receiver = loggedInUser;

        try {
            service.declineFriendRequest(sender.getId(), receiver.getId());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setContentText("Friend deleted!");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
        initModel();
    }



    public void handleSendSingleMessage(KeyEvent keyEvent) {
        UserFriendDTO selectedFriend = messageFriendsListView.getSelectionModel().getSelectedItem();
        if (keyEvent.getCode().toString().equals("ENTER")) {
            String message = messageTextField.getText();
            User sender = loggedInUser;
            User receiver = service.getUserByUsername(selectedFriend.getUsername());
            service.sendOneToOneMessage(sender, receiver, message);

            populateMessagesListView(selectedFriend);
            messageTextField.clear();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setContentText("Message sent!");
            alert.showAndWait();
        }
    }

    public void handleGoToMessageAll(ActionEvent event) {
        try {
            FXMLLoader messageAllLoader = new FXMLLoader();
            messageAllLoader.setLocation(getClass().getResource("/com/projects/socialnetwork/views/message-all-view.fxml"));

            AnchorPane messageAllLayout = messageAllLoader.load();
            Scene scene = new Scene(messageAllLayout);

            MessageAllController messageAllController = messageAllLoader.getController();
            messageAllController.setControllerSettings(service, loggedInUser);

            Stage messageAllStage = new Stage();
            messageAllStage.setScene(scene);
            messageAllStage.show();

        }
        catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
