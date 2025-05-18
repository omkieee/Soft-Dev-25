package view;

import static controller.LoginController.userRole;

public class LoginForm {
    private static String username;
    private static String password;

    public LoginForm(String usernameInput, String passwordInput){
        username = usernameInput;
        password = passwordInput;
    }

    public static String getUserName() {
        return username;
    }

    public static void setUserName(String user) {
        username = user;
    }

    public static String getUserRole() {
        return userRole;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String pass) {
        password = pass;
    }
}
