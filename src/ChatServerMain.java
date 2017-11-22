import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServerMain {

    protected final static String CHATMASTER_ID = "ChatMaster";

    protected final static String SEP = ": ";

    protected ServerSocket servSock;

    protected List<ChatHandler> clients;

    private static boolean DEBUG = false;


    public static void main(String[] argv) throws IOException {
        System.out.println("SzablewskiSys ChatServer 0.1 starting...");
        if (argv.length == 1 && argv[0].equals("-debug"))
            DEBUG = true;
        ChatServerMain w = new ChatServerMain();
        w.runServer();			// should never return.
        System.out.println("**ERROR* ChatServer 0.1 quitting");
    }


    ChatServerMain() throws IOException {
        clients = new ArrayList<>();

        servSock = new ServerSocket(ChatProtocol.PORTNUM);
        System.out.println("SzablewskiSys Chat Server Listening on port " +
                ChatProtocol.PORTNUM);
    }

    public void runServer() {
        try {
            while (true) {
                Socket userSocket = servSock.accept();
                String hostName = userSocket.getInetAddress().getHostName();
                System.out.println("Accepted from " + hostName);
                ChatHandler cl = new ChatHandler(userSocket, hostName);
                String welcomeMessage;
                synchronized (clients) {
                    clients.add(cl);
                    if (clients.size() == 1) {
                        welcomeMessage = "Welcome! you're the first one here";
                    } else {
                        welcomeMessage = "Welcome! you're the latest of " +
                                clients.size() + " users.";
                    }
                }
                cl.start();
                cl.send(CHATMASTER_ID, welcomeMessage);
            }
        } catch(IOException e) {
            log("IO Exception in runServer: " + e);
        }
    }

    protected void log(String s) {
        System.out.println(s);
    }

    protected class ChatHandler extends Thread {
        protected Socket clientSock;
        protected BufferedReader is;
        protected PrintWriter pw;
        protected String clientIP;
        protected String login;

        public ChatHandler(Socket sock, String clnt) throws IOException {
            clientSock = sock;
            clientIP = clnt;
            is = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
            pw = new PrintWriter(sock.getOutputStream(), true);
        }

        public void run() {
            String line;
            try {
                while ((line = is.readLine()) != null) {
                    char c = line.charAt(0);
                    line = line.substring(1);
                    switch (c) {
                        case ChatProtocol.CMD_LOGIN:
                            if (!ChatProtocol.isValidLoginName(line)) {
                                send(CHATMASTER_ID, "LOGIN " + line + " invalid");
                                log("LOGIN INVALID from " + clientIP);
                                continue;
                            }
                            login = line;
                            broadcast(CHATMASTER_ID, login +
                                    " joins us, for a total of " +
                                    clients.size() + " users");
                            break;
                        case ChatProtocol.CMD_MESG:
                            if (login == null) {
                                send(CHATMASTER_ID, "please login first");
                                continue;
                            }
                            int where = line.indexOf(ChatProtocol.SEPARATOR);
                            String recip = line.substring(0, where);
                            String mesg = line.substring(where+1);
                            log("MESG: " + login + "-->" + recip + ": "+ mesg);
                            ChatHandler cl = lookup(recip);
                            if (cl == null)
                                psend(CHATMASTER_ID, recip + " not logged in.");
                            else
                                cl.psend(login, mesg);
                            break;
                        case ChatProtocol.CMD_QUIT:
                            broadcast(CHATMASTER_ID,
                                    "Goodbye to " + login + "@" + clientIP);
                            close();
                            return;		// The end of this ChatHandler

                        case ChatProtocol.CMD_BCAST:
                            if (login != null)
                                broadcast(login, line);
                            else
                                log("B<L FROM " + clientIP);
                            break;
                        default:
                            log("Unknown cmd " + c + " from " + login + "@" + clientIP);
                    }
                }
            } catch (IOException e) {
                log("IO Exception: " + e);
            } finally {
                // the sock ended (darn it), so we're done, bye now
                System.out.println(login + SEP + "All Done");
                String message = "This should never appear.";
                synchronized(clients) {
                    clients.remove(this);
                    if (clients.size() == 0) {
                        System.out.println(CHATMASTER_ID + SEP +
                                "I'm so lonely I could cry...");
                    } else if (clients.size() == 1) {
                        message =
                                "Hey, you're talking to yourself again";
                    } else {
                        message =
                                "There are now " + clients.size() + " users";
                    }
                }
                broadcast(CHATMASTER_ID, message);
            }
        }

        protected void close() {
            if (clientSock == null) {
                log("close when not open");
                return;
            }
            try {
                clientSock.close();
                clientSock = null;
            } catch (IOException e) {
                log("Failure during close to " + clientIP);
            }
        }

        public void send(String sender, String mesg) {
            pw.println(sender + SEP + mesg);
        }

        protected void psend(String sender, String msg) {
            send("<*" + sender + "*>", msg);
        }

        public void broadcast(String sender, String mesg) {
            System.out.println("Broadcasting " + sender + SEP + mesg);
            clients.forEach(sib -> {
                if (DEBUG)
                    System.out.println("Sending to " + sib);
                sib.send(sender, mesg);
            });
            if (DEBUG) System.out.println("Done broadcast");
        }

        protected ChatHandler lookup(String nick) {
            synchronized (clients) {
                for (ChatHandler cl : clients) {
                    if (cl.login.equals(nick))
                        return cl;
                }
            }
            return null;
        }

        public String toString() {
            return "ChatHandler[" + login + "]";
        }
    }
}