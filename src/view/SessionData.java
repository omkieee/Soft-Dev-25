package view;

public class SessionData {
    private static int currentLogId;

    public static int getCurrentLogId() {
        return currentLogId;
    }

    public static void setCurrentLogId(int id) {
        currentLogId = id;
    }
}
