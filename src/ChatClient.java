import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;

public class ChatClient extends JFrame {

    private static final long serialVersionUID = -3686334002367908392L;
    private static final String userName =
            System.getProperty("user.name", "User With No Name");
    protected boolean loggedIn;
    protected JFrame cp;
    protected static final int PORTNUM = ChatProtocol.PORTNUM;
    protected int port;
    protected Socket sock;
    protected BufferedReader is;
    protected PrintWriter pw;
    protected TextField tf;
    protected TextArea ta;
    protected Button loginButton;
    protected Button logoutButton;
    final static String TITLE = "Bartosz Szablewsi Komunikator";

    public ChatClient() {
        cp = this;
        cp.setTitle(TITLE);
        cp.setLayout(new BorderLayout());
        port = PORTNUM;

        ta = new TextArea(14, 80);
        ta.setEditable(false);		// readonly
        ta.setFont(new Font("Monospaced", Font.PLAIN, 11));
        cp.add(BorderLayout.NORTH, ta);

        Panel p = new Panel();

        p.add(loginButton = new Button("Login"));
        loginButton.setEnabled(true);
        loginButton.requestFocus();
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                login();
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
                tf.requestFocus();	// set keyboard focus in right place!
            }
        });

        p.add(logoutButton = new Button("Logout"));
        logoutButton.setEnabled(false);
        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logout();
                loginButton.setEnabled(true);
                logoutButton.setEnabled(false);
                loginButton.requestFocus();
            }
        });

        p.add(new Label("Message here:"));
        tf = new TextField(40);
        tf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (loggedIn) {
                    pw.println(ChatProtocol.CMD_BCAST+tf.getText());
                    tf.setText("");
                }
            }
        });
        p.add(tf);

        cp.add(BorderLayout.SOUTH, p);

        cp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cp.pack();
    }

    protected String serverHost = "localhost";

    public void login() {
        showStatus("In login!");
        if (loggedIn)
            return;
        try {
            sock = new Socket(serverHost, port);
            is = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            pw = new PrintWriter(sock.getOutputStream(), true);
            showStatus("Got socket");

            pw.println(ChatProtocol.CMD_LOGIN + userName);

            loggedIn = true;

        } catch(IOException e) {
            showStatus("Can't get socket to " +
                    serverHost + "/" + port + ": " + e);
            cp.add(new Label("Can't get socket: " + e));
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                String line;
                try {
                    while (loggedIn && ((line = is.readLine()) != null))
                        ta.append(line + "\n");
                } catch(IOException e) {
                    showStatus("Lost another client!\n" + e);
                    return;
                }
            }
        }).start();
    }

    public void logout() {
        if (!loggedIn)
            return;
        loggedIn = false;
        try {
            if (sock != null)
                sock.close();
        } catch (IOException ign) {
            // so what?
        }
    }

    public void showStatus(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        ChatClient room101 = new ChatClient();
        room101.pack();
        room101.setVisible(true);
    }
}
