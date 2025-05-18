package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import view.LoginForm;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddNewUserController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField contactField;

    @FXML
    private ComboBox<String> usernameField;

    @FXML
    private TextField passwordField;



    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll("Admin", "Staff");
        loadUsernames();

        usernameField.setOnAction(event -> loadUserDetails(usernameField.getValue()));
    }

    private void loadUsernames() {
        String query = "SELECT Username FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                usernameField.getItems().add(rs.getString("Username"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to load usernames: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUserDetails(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        String query = "SELECT * FROM users WHERE Username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                roleComboBox.setValue(rs.getString("Role"));
                firstNameField.setText(rs.getString("FirstName"));
                lastNameField.setText(rs.getString("LastName"));
                addressField.setText(rs.getString("Address"));
                emailField.setText(rs.getString("Email"));
                contactField.setText(rs.getString("Contact"));
                passwordField.setText(rs.getString("Password"));
            } else {
                JOptionPane.showMessageDialog(null, "User not found.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    @FXML
    private void deleteUser(ActionEvent event) {
        String username = usernameField.getValue();
        String currentUsername = LoginController.getCurrentUsername(); // Get the logged-in user

        if (username == null || username.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please select a username to delete.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prevent the logged-in user from deleting their own username
        if (username.equals(currentUsername)) {
            JOptionPane.showMessageDialog(null, "You cannot delete your own username.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }



        int confirm = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete user: " + username + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String deleteQuery = "DELETE FROM users WHERE Username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {

            pstmt.setString(1, username);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "User deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                usernameField.getItems().remove(username);
            } else {
                JOptionPane.showMessageDialog(null, "User not found or could not be deleted.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    @FXML
    private void onHomeBtnClick(ActionEvent event) {
        String userRole = LoginForm.getUserRole();
        userRole = "Admin";
        navigate(event, "homeFrame.fxml", "Mr. Are Level Up Cuisine | Home Page");
    }

    private void navigate(ActionEvent event, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/" + fxmlFile)); // Update the path!
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        contactField.clear();
        addressField.clear();
        usernameField.getSelectionModel().clearSelection();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void saveUser(ActionEvent event) {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String address = addressField.getText();
        String contact = contactField.getText();
        String username = usernameField.getValue();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || address.isEmpty() || contact.isEmpty() || username.isEmpty() || password.isEmpty() || role == null || role.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            String checkQuery = "SELECT COUNT(*) FROM users WHERE Username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                String query;
                if (count > 0) {
                    int confirm = JOptionPane.showConfirmDialog(null,
                            "User already exists. Do you want to update the existing User?",
                            "Confirm Update",
                            JOptionPane.YES_NO_OPTION);

                    if (confirm != JOptionPane.YES_OPTION) {
                        JOptionPane.showMessageDialog(null, "Update cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    query = "UPDATE users SET FirstName = ?, LastName = ?, Email = ?, Address = ?, Contact = ?, Password = ?, Role = ? WHERE Username = ?";
                } else {
                    query = "INSERT INTO users (FirstName, LastName, Email, Address, Contact, Password, Role, Username) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                }

                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, firstName);
                    pstmt.setString(2, lastName);
                    pstmt.setString(3, email);
                    pstmt.setString(4, address);
                    pstmt.setString(5, contact);
                    pstmt.setString(6, password);
                    pstmt.setString(7, role);

                    if (count > 0) {
                        pstmt.setString(8, username); // For update
                    } else {
                        pstmt.setString(8, username); // For insert
                    }

                    pstmt.executeUpdate();

                    String message = (count > 0) ? "User updated successfully!" : "User added successfully!";
                    JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
