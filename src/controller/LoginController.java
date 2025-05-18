package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import view.LoginForm;
import view.SessionData;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

public class LoginController {

    @FXML
    private TextField usernameInput;

    @FXML
    private PasswordField passwordInput;

    @FXML
    private Button loginBtn;

    public static String userRole;

    @FXML
    public void initialize() {
        usernameInput.setOnAction(event -> handleLogin());
        passwordInput.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameInput.getText();
        String password = passwordInput.getText();

        String role = authenticateUser(username, password);

        if (role != null) {
            userRole = role;
            insertLoginLog(username);
            LoginController.currentUsername = username;

            if(userRole.equals("Admin")) {
                openHomeFrame();
            } else if (userRole.equals("Staff")) {
                openHomeFrameStaff();
            }

        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    private void insertLoginLog(String username) {
        String query = "INSERT INTO UserLogs (Username, LoggedIn) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                System.out.println("Login log insert failed.");
                return;
            }

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int logId = keys.getInt(1);
                SessionData.setCurrentLogId(logId);
                System.out.println("Login log created with LogID: " + logId);
            } else {
                System.out.println("No generated keys returned.");
            }

        } catch (SQLException e) {
            System.err.println("SQL Error State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }

    }

    public static void logLogoutTime() {
        int logId = SessionData.getCurrentLogId();
        if (logId == 0) {
            System.out.println("No login session found.");
            return;
        }

        String query = "UPDATE UserLogs SET LoggedOut = ? WHERE LogID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, logId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Logged out time recorded for LogID: " + logId);
            } else {
                System.out.println("LogID not found: " + logId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String authenticateUser(String username, String password) {
        String query = "SELECT Username, Role FROM Users WHERE Username=? AND Password=?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String dbUsername = resultSet.getString("Username");
                if (dbUsername.equals(username)) { // Case-sensitive check
                    LoginForm.setUserName(dbUsername);
                    return resultSet.getString("Role");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String currentUsername;

    public static String getCurrentUsername() {
        return currentUsername;
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openHomeFrame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/homeFrame.fxml"));
            AnchorPane root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Mr. Are Level Up Cuisine | Home Page");
            stage.setMaximized(true);
            stage.setScene(new Scene(root));
            stage.show();

            Stage loginStage = (Stage) loginBtn.getScene().getWindow();
            loginStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load Home Page.");
        }
    }

    private void openHomeFrameStaff() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/homeFrameStaff.fxml"));
            AnchorPane root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Mr. Are Level Up Cuisine | Home Page");
            stage.setMaximized(true);
            stage.setScene(new Scene(root));
            stage.show();

            Stage loginStage = (Stage) loginBtn.getScene().getWindow();
            loginStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load Home Page.");
        }
    }
}
