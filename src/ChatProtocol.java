public class ChatProtocol {

    public static final int PORTNUM = 9999;
    public static final int MAX_LOGIN_LENGTH = 20;
    public static final char SEPARATOR = '\\';
    public static final char COMMAND = '\\';
    public static final char CMD_LOGIN = 'L';
    public static final char CMD_QUIT  = 'Q';
    public static final char CMD_MESG  = 'M';
    public static final char CMD_BCAST = 'B';
    public static final char RESP_PUBLIC = 'P';
    public static final char RESP_PRIVATE = 'M';
    public static final char RESP_SYSTEM = 'S';

//         TODO in main loop:
//         if (text.charAt(0) == '/')
//        		send(text);
//         else send("B"+text);

    public static boolean isValidLoginName(String login) {
        if (login.length() > MAX_LOGIN_LENGTH)
            return false;
        return true;
    }
}
