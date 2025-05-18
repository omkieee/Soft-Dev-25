    package controller;

    import java.sql.Connection;
    import java.sql.DriverManager;
    import java.sql.SQLException;

    public class DatabaseConnection {

        private static final String DATABASE_PATH = "C:/Users/USER/Desktop/RestaurantManagementSystem/src/MrAreLevelUpCuisineDB.accdb";
        private static final String URL = "jdbc:ucanaccess://" + DATABASE_PATH;

        public static Connection getConnection() {
            try {
                return DriverManager.getConnection(URL);
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
