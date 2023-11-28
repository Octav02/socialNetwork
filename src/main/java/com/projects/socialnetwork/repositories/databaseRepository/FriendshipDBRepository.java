package com.projects.socialnetwork.repositories.databaseRepository;

import com.projects.socialnetwork.enums.FriendshipRequestStatus;
import com.projects.socialnetwork.models.Friendship;
import com.projects.socialnetwork.models.User;
import com.projects.socialnetwork.repositories.Repository;
import com.projects.socialnetwork.validators.FriendshipValidator;
import com.projects.socialnetwork.validators.Validator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class FriendshipDBRepository implements Repository<UUID, Friendship> {
    String url;
    String username;
    String password;
    private Validator<Friendship> validator;
    UserDBRepository userRepository;

    public FriendshipDBRepository(String url, String username, String password, UserDBRepository userRepository) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.userRepository = userRepository;
        validator = new FriendshipValidator();
    }

    @Override
    public Optional<Friendship> getById(UUID uuid) {
        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            PreparedStatement statement = connection.prepareStatement("select * from friendships where id = ?");
            statement.setObject(1, uuid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                UUID user1Id = (UUID) resultSet.getObject("user1_id");
                UUID user2Id = (UUID) resultSet.getObject("user2_id");
                FriendshipRequestStatus status = FriendshipRequestStatus.valueOf(resultSet.getString("friendship_status").toUpperCase());
                LocalDateTime friendsSince = resultSet.getTimestamp("friends_since").toLocalDateTime();


                User user1 = userRepository.getById(user1Id).get();
                User user2 = userRepository.getById(user2Id).get();
                Friendship friendship = new Friendship(user1, user2, friendsSince, status);
                friendship.setId(uuid);
                return Optional.of(friendship);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<Friendship> getAll() {
        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            Set<Friendship> friendships = new HashSet<>();
            PreparedStatement statement = connection.prepareStatement("select * from friendships");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                UUID id = (UUID) resultSet.getObject("id");
                UUID user1Id = (UUID) resultSet.getObject("user1_id");
                UUID user2Id = (UUID) resultSet.getObject("user2_id");
                FriendshipRequestStatus status = FriendshipRequestStatus.valueOf(resultSet.getString("friendship_status").toUpperCase());
                LocalDateTime friendsSince = resultSet.getTimestamp("friends_since").toLocalDateTime();

                User user1 = userRepository.getById(user1Id).get();
                User user2 = userRepository.getById(user2Id).get();
                Friendship friendship = new Friendship(user1, user2, friendsSince, status);
                friendship.setId(id);
                friendships.add(friendship);
            }
            return friendships;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Friendship> save(Friendship entity) {
        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            validator.validate(entity);
            PreparedStatement statement = connection.prepareStatement("insert into friendships(id,user1_id,user2_id,friends_since,friendship_status) values (?,?,?,?,?)");
            statement.setObject(1, entity.getId());
            statement.setObject(2, entity.getUser1().getId());
            statement.setObject(3, entity.getUser2().getId());
            statement.setObject(4, entity.getFriendsSince());
            statement.setString(5, entity.getFriendshipStatus().toString());
            statement.executeUpdate();
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Friendship> delete(UUID uuid) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("delete from friendships where id = ?")) {
            statement.setObject(1, uuid, Types.OTHER);
            Optional<Friendship> foundFriendship = getById(uuid);
            int response = 0;
            if (foundFriendship.isPresent()) {
                response = statement.executeUpdate();
            }
            return response == 0 ? Optional.empty() : foundFriendship;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Friendship> update(Friendship entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("update friendships set user1_id = ?, user2_id = ?, friends_since = ?, friendship_status = ? where id = ?")) {
            validator.validate(entity);
            statement.setObject(1, entity.getUser1().getId(), Types.OTHER);
            statement.setObject(2, entity.getUser2().getId(), Types.OTHER);
            statement.setObject(3, entity.getFriendsSince(), Types.OTHER);
            statement.setString(4, entity.getFriendshipStatus().toString());
            statement.setObject(5, entity.getId(), Types.OTHER);
            statement.executeUpdate();
            int response = statement.executeUpdate();
            return response == 0 ? Optional.empty() : Optional.of(entity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> getFriendsOfUser(UUID uuid) {
        try (Connection connection = DriverManager.getConnection(url,username,password)) {
            List<User> friends = new ArrayList<>();
            PreparedStatement statement = connection.prepareStatement("select * from friendships where user1_id = ? or user2_id = ?");
            statement.setObject(1, uuid);
            statement.setObject(2, uuid);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                UUID user1Id = (UUID) resultSet.getObject("user1_id");
                UUID user2Id = (UUID) resultSet.getObject("user2_id");
                if (user1Id.equals(uuid)) {
                    friends.add(userRepository.getById(user2Id).get());
                } else {
                    friends.add(userRepository.getById(user1Id).get());
                }
            }
            return friends;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
