package com.projects.socialnetwork.validators;

import com.projects.socialnetwork.exceptions.ValidationException;
import com.projects.socialnetwork.models.User;

/**
 * Validator for the User entity
 */
public class UserValidator implements Validator<User> {
    @Override
    public void validate(User entity) throws ValidationException {
        String errors = "";
        errors += validateFirstName(entity.getFirstName());
        errors += validateLastName(entity.getLastName());
        errors += validateUsername(entity.getUsername());
        errors += validateEmail(entity.getEmail());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }




    private String validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "Email cannot be empty!\n";
        }
        if (!email.contains("@")) {
            return "Invalid email !\n";
        }
        return "";
    }

    private String validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "Username cannot be empty!\n";
        }
        return "";
    }

    private String validateLastName(String lastName) {
        if (lastName == null || lastName.isEmpty()) {
            return "Last name cannot be empty!\n";
        }
        return "";
    }

    private String validateFirstName(String firstName) {
        if (firstName == null || firstName.isEmpty()) {
            return "First name cannot be empty!\n";
        }
        return "";
    }
}
