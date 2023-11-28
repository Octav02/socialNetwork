package com.projects.socialnetwork.services;

import com.projects.socialnetwork.dtos.UserFriendDTO;
import com.projects.socialnetwork.enums.FriendshipRequestStatus;
import com.projects.socialnetwork.exceptions.FriendshipConflictException;
import com.projects.socialnetwork.exceptions.UserConflictException;
import com.projects.socialnetwork.models.Friendship;
import com.projects.socialnetwork.models.User;
import com.projects.socialnetwork.dtos.UserFriendshipDTO;
import com.projects.socialnetwork.repositories.Repository;
import com.projects.socialnetwork.repositories.databaseRepository.FriendshipDBRepository;
import com.projects.socialnetwork.repositories.databaseRepository.UserDBRepository;
import javafx.scene.input.InputMethodTextRun;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NetworkService implements Service {

    UserDBRepository userRepository;
    FriendshipDBRepository friendshipRepository;

    public NetworkService(UserDBRepository userRepository, FriendshipDBRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Override
    public Optional<User> addUser(String firstName, String lastName, String username, String email, String password) {
        User user = new User(firstName, lastName, username, email, password);
        if (alreadyExists(user))
            throw new UserConflictException("There is already a user with this username/password");

        return userRepository.save(user);
    }

    /**
     * Checks if a user with the same username/email exists
     *
     * @return true if a user with these credentials exists, false otherwise
     */
    private boolean alreadyExists(User user) {
        return userRepository.getUserByEmail(user.getEmail()).isPresent() || userRepository.getUserByUsername(user.getUsername()).isPresent();
    }

    @Override
    public Optional<User> deleteUserByUsername(String username) {
        if (username == null)
            throw new IllegalArgumentException("The username cannot be null");

        User user = userRepository.getUserByUsername(username).orElseThrow(() -> new UserConflictException("The user does not exist"));

        return userRepository.delete(user.getId());
    }

    @Override
    public Optional<User> deleteUserById(UUID id) {
        if (id == null)
            throw new IllegalArgumentException("The id cannot be null");
        User user = userRepository.delete(id).orElseThrow(() -> new UserConflictException("The user does not exist"));
        return Optional.of(user);
    }

    @Override
    public void sendFriendRequest(UUID senderID, UUID receiverID) {
        User sender = userRepository.getById(senderID).orElseThrow(() -> new UserConflictException("The user does not exist"));
        User receiver = userRepository.getById(receiverID).orElseThrow(() -> new UserConflictException("The user does not exist"));

        if (existsFriendship(sender, receiver))
            throw new FriendshipConflictException("The friendship already exists");

        Friendship friendship = new Friendship(sender, receiver, LocalDateTime.now(), FriendshipRequestStatus.PENDING);
        friendshipRepository.save(friendship);
    }

    @Override
    public void acceptFriendRequest(UUID senderID, UUID receiverID) {
        User sender = userRepository.getById(senderID).orElseThrow(() -> new UserConflictException("The user does not exist"));
        User receiver = userRepository.getById(receiverID).orElseThrow(() -> new UserConflictException("The user does not exist"));

        if (!existsFriendship(sender, receiver))
            throw new FriendshipConflictException("The friendship does not exist");

        Friendship friendship = StreamSupport.stream(friendshipRepository.getAll().spliterator(), false).filter(friendship1 ->
                (friendship1.getUser1().equals(sender) && friendship1.getUser2().equals(receiver)) ||
                        (friendship1.getUser1().equals(receiver) && friendship1.getUser2().equals(sender))
        ).findFirst().orElseThrow();

        friendship.setFriendshipStatus(FriendshipRequestStatus.ACCEPTED);
        friendshipRepository.update(friendship);
    }

    @Override
    public void declineFriendRequest(UUID senderID, UUID receiverID) {
        User sender = userRepository.getById(senderID).orElseThrow(() -> new UserConflictException("The user does not exist"));
        User receiver = userRepository.getById(receiverID).orElseThrow(() -> new UserConflictException("The user does not exist"));

        if (!existsFriendship(sender, receiver))
            throw new FriendshipConflictException("The friendship does not exist");

        Friendship friendship = StreamSupport.stream(friendshipRepository.getAll().spliterator(), false).filter(friendship1 ->
                (friendship1.getUser1().equals(sender) && friendship1.getUser2().equals(receiver)) ||
                        (friendship1.getUser1().equals(receiver) && friendship1.getUser2().equals(sender))
        ).findFirst().orElseThrow();

        friendship.setFriendshipStatus(FriendshipRequestStatus.REJECTED);
//        //TODO: Sterge din baza de date in loc de Rejected
//        friendshipRepository.update(friendship);
        friendshipRepository.delete(friendship.getId());
    }


    /**
     * Checks if a friendship between two users exists
     *
     * @param user1 - first user
     * @param user2 - second user
     * @return true if the friendship exists, false otherwise
     */
    private boolean existsFriendship(User user1, User user2) {
        Iterable<Friendship> friendships = friendshipRepository.getAll();
        return StreamSupport.stream(friendships.spliterator(), false).anyMatch(x ->
                (x.getUser1().equals(user1) && x.getUser2().equals(user2)) ||
                        (x.getUser1().equals(user2) && x.getUser2().equals(user1))
        );


    }

    @Override
    public void deleteFriendship(UUID user1ID, UUID user2ID) {

        User user1 = userRepository.getById(user1ID).orElseThrow(() -> new UserConflictException("The user does not exist"));
        User user2 = userRepository.getById(user2ID).orElseThrow(() -> new UserConflictException("The user does not exist"));
        if (!existsFriendship(user1, user2))
            throw new FriendshipConflictException("The friendship does not exist");

        Friendship friendshipToDelete = StreamSupport.stream(friendshipRepository.getAll().spliterator(), false).filter(friendship ->
                (friendship.getUser1().equals(user1) && friendship.getUser2().equals(user2)) ||
                        (friendship.getUser1().equals(user2) && friendship.getUser2().equals(user1))
        ).findFirst().orElseThrow();

        user2.removeFriend(user1);
        user1.removeFriend(user2);
        friendshipRepository.delete(friendshipToDelete.getId());
    }


    @Override
    public Iterable<User> getAllUsers() {
        return userRepository.getAll();
    }

    @Override
    public Iterable<Friendship> getAllFriendships() {
        return friendshipRepository.getAll();
    }

    private void dfs(User user, Set<User> visited, Set<User> community) {
        visited.add(user);
        community.add(user);

        for (User friend : user.getFriends()) {
            if (!visited.contains(friend)) {
                dfs(friend, visited, community);
            }
        }
    }

    @Override
    public int getNumberOfCommunities() {
        AtomicInteger numberOfCommunities = new AtomicInteger();
        Set<User> visited = new HashSet<>();

        StreamSupport.stream(userRepository.getAll().spliterator(),false)
                .filter(user -> !visited.contains(user))
                .forEach(user -> {
                    numberOfCommunities.getAndIncrement();
                    Set<User> community = new HashSet<>();
                    dfs(user, visited, community);
                });

        return numberOfCommunities.get();
    }

    @Override
    public Iterable<Iterable<User>> getAllCommunities() {
        Set<Iterable<User>> allCommunities = new HashSet<>();
        Set<User> visited = new HashSet<>();

        StreamSupport.stream(userRepository.getAll().spliterator(), false)
                .filter(user -> !visited.contains(user))
                .forEach(user -> {
                    Set<User> community = new HashSet<>();
                    dfs(user, visited, community);
                    allCommunities.add(community);
                });


        return allCommunities;
    }

    @Override
    public Iterable<User> getMostSociableCommunity() {
        return StreamSupport.stream(getAllCommunities().spliterator(),false)
                .max(Comparator.comparingInt(community -> StreamSupport.stream(community.spliterator(), false)
                        .mapToInt(user -> user.getFriends().size())
                        .sum()))
                .orElse(null);
    }

//    @Override
//    public Iterable<UserFriendshipDTO> friendshipsOfUserOnMonth(String username, Month month) {
//        User user = getUserByUsername(username).get();
//
//        Set<UserFriendshipDTO> userFriendshipDTOS = new HashSet<>();
//        for (Friendship f : friendshipRepository.getAll()) {
//            if (f.getFriendsSince().getMonth().equals(month)) {
//                if (f.getUser1().equals(user)) {
//                    userFriendshipDTOS.add(new UserFriendshipDTO(f.getUser2().getFirstName(),f.getUser2().getLastName(), f.getFriendsSince()));
//                } else if (f.getUser2().equals(user)) {
//                    userFriendshipDTOS.add(new UserFriendshipDTO(f.getUser1().getFirstName(),f.getUser1().getLastName(), f.getFriendsSince()));
//                }
//            }
//        }
//        return userFriendshipDTOS;
//
//    }

    @Override
    public Iterable<UserFriendshipDTO> friendshipsOfUserOnMonth(String username, Month month) {
        User user = userRepository.getUserByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return StreamSupport.stream(friendshipRepository.getAll().spliterator(),false)
                .filter(f -> f.getFriendsSince().getMonth().equals(month))
                .filter(f -> f.getUser1().equals(user) || f.getUser2().equals(user))
                .map(f -> f.getUser1().equals(user) ?
                        new UserFriendshipDTO(f.getUser2().getFirstName(), f.getUser2().getLastName(), f.getFriendsSince()) :
                        new UserFriendshipDTO(f.getUser1().getFirstName(), f.getUser1().getLastName(), f.getFriendsSince()))
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<User> updateUser(UUID id, String firstName, String lastName, String username, String email, String password) {
        User user = new User(id, firstName, lastName, username, email, password);
        return userRepository.update(user);
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.getUserByUsername(username).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        if (!user.getPassword().equals(password))
            throw new IllegalArgumentException("Password is incorrect");
        return user;
    }

    public Iterable<UserFriendDTO> getFriendsOfUser(UUID id) {
        User user = userRepository.getById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist"));

        Iterable<Friendship> friendships = StreamSupport.stream(friendshipRepository.getAll().spliterator(), false)
                .filter(friendship -> friendship.getFriendshipStatus().equals(FriendshipRequestStatus.ACCEPTED))
                .collect(Collectors.toSet());


         Iterable<UserFriendDTO> test = StreamSupport.stream(friendships.spliterator(), false)
                .filter(friendship -> friendship.getUser1().equals(user) || friendship.getUser2().equals(user))
                .map(friendship -> friendship.getUser1().equals(user) ?
                        new UserFriendDTO(friendship.getUser2().getUsername(), friendship.getUser2().getEmail(), friendship.getFriendsSince()) :
                        new UserFriendDTO(friendship.getUser1().getUsername(), friendship.getUser1().getEmail(), friendship.getFriendsSince()))
                .collect(Collectors.toSet());

        System.out.println(test);

        return test;

    }

    public Iterable<Friendship> getPendingFriendRequests(UUID id) {
        User user = userRepository.getById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        return StreamSupport.stream(friendshipRepository.getAll().spliterator(), false)
                .filter(friendship -> friendship.getUser2().equals(user) && friendship.getFriendshipStatus().equals(FriendshipRequestStatus.PENDING))
                .collect(Collectors.toSet());
    }

    public Iterable<User> getAllUsersExcept(UUID id) {
        User user = userRepository.getById(id).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        return StreamSupport.stream(userRepository.getAll().spliterator(), false)
                .filter(user1 -> !user1.equals(user))
                .collect(Collectors.toSet());
    }



    public User getUserByUsername(String username) {
        return userRepository.getUserByUsername(username).orElseThrow(() -> new IllegalArgumentException("User does not exist"));
    }
}
