/* Our chat client */
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

class ChatClient extends Panel implements Runnable
{
    /* Display */
    private TextField textfield = new TextField();
    private TextArea textarea = new TextArea();
    private Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    /* Communication */
    private static Socket s;
    private static PrintWriter pw;
    private static BufferedReader br;

    private static String quitMsg;

    public ChatClient(String host, int port, String nickname) {

	/* Set up display */
	setLayout(new BorderLayout());
        textarea.setFont(font);
        textfield.setFont(font);
	add(BorderLayout.SOUTH, textfield);
	add(BorderLayout.CENTER, textarea);

	/* Associate sendChat with textfield callback */

	textfield.addActionListener(new ActionListener()
    {
		public void actionPerformed(ActionEvent e)
        {
            textarea.append(nickname +" says: " + e.getActionCommand() + "\n");
            sendChat(e.getActionCommand());
		}
    });

	try
    {
	    s = new Socket(host, port);
	    pw = new PrintWriter(s.getOutputStream(), true);
	    br = new BufferedReader(new InputStreamReader(s.getInputStream()));

	    /* Send nickname to chat server */
	    pw.println(nickname);
        quitMsg = br.readLine();

	    /* Become a thread */
	    new Thread(this).start();
	} catch (IOException e) {System.out.println(e);}
    }

    /* Called whenever user hits return in the textfield */
    private void sendChat(String message) {
	    pw.println(message);
	    textfield.setText(" ");
    }

    static void close()
    {
        try {
            pw.println(quitMsg);
            br.close();
            pw.close();
            s.close();
        }
        catch (IOException e)
        {

        }
    }

    /* Add strings from chat server to the textarea */
    public void run()
    {
	    String message;

        try
        {
            while ((message = br.readLine()) != null)
            {
                textarea.append(message + "\n");
            }
        }

        catch (IOException e)
        {
            System.err.println("IOException:  " + e);
        }
    }
}

public class ChatApplet extends Applet
{
    public void init() {

	/* Retrieve parameters */
	int port = Integer.parseInt(getParameter("port"));
	String host = getParameter("host");
	String nickname = getParameter("nickname");

	/* Set up display */
	setLayout(new BorderLayout());
	add(BorderLayout.CENTER, new ChatClient(host, port, nickname));
    }

    public void stop() {
        ChatClient.close();
    }

}
