import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

class ChatServer
{
    // The default server socket.
    private static ServerSocket serverSocket = null;
    // The default client socket.
    private static Socket clientSocket = null;
    private static int clientCount = 0;
    private static ArrayList<ClientThread> threads = new ArrayList<>(Collections.nCopies(10, null));

    public static void main(String args[]) {

        //Default Port
        int portNumber = 7777;

        try
        {
            serverSocket = new ServerSocket(portNumber);
        }

        catch (IOException e)
        {
            System.out.println(e);
        }


        //Create a client socket for each connection and  create a new thread for it
        while (true)
        {
            try
            {
                //This inputStream a new connection no matter what
                clientSocket = serverSocket.accept();
                int i = 0;
                //Look through the collection of threads and find an empty one
                int size = threads.size();
                for (i = 0; i < size; i++)
                {
                    if (threads.get(i) == null)
                    {
                        threads.set(i,new ClientThread(clientSocket, threads));
                        Thread t = new Thread(threads.get(i));
                        t.start();
                        clientCount++;
                        System.out.println("Clients " + clientCount);
                        break;
                    }
                }
            }
            catch (IOException e)
            {
                System.out.println(e);
            }
        }
    }
}

class ClientThread implements Runnable
{
    private BufferedReader inputStream = null;
    private PrintStream outputStream = null;
    private Socket clientSocket = null;
    private final  ArrayList<ClientThread> threads;
    private int maxClientsCount;
    private String name;
    private String quitMsg;

    public ClientThread(Socket clientSocket, ArrayList<ClientThread> threads)
    {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.size();
        try
        {
            inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputStream = new PrintStream(clientSocket.getOutputStream());
            this.name = inputStream.readLine();
            this.quitMsg = "###" +clientSocket.getRemoteSocketAddress().toString() + "***";
            this.outputStream.println(quitMsg);
        }
        catch (IOException e)
        {

        }
    }

    public void run()
    {
        int maxClientsCount = this.maxClientsCount;
        ArrayList<ClientThread> threads = this.threads;

        try
        {

            for (int i = 0; i < maxClientsCount; i++)
            {
                if (threads.get(i) != null)
                {
                    threads.get(i).outputStream.println(name + " just joined the chatroom...");
                }
            }
            while (true)
            {
                String line = inputStream.readLine();
                if (line.equals(quitMsg))
                {
                    break;
                }
                else
                {
                    for (int i = 0; i < maxClientsCount; i++)
                    {
                        if (threads.get(i) != null && threads.get(i) != this)
                        {
                            threads.get(i).outputStream.println(name + " says: " + line);
                        }
                    }
                }
            }

            for (int i = 0; i < maxClientsCount; i++)
            {
                if (threads.get(i) != null && threads.get(i) != this)
                {
                    threads.get(i).outputStream.println(name + " has just left the chatroom...");
                }
            }

            //Remove the users thread
            for (int i = 0; i < maxClientsCount; i++)
            {
                if (threads.get(i) == this)
                {
                    threads.set(i,null);
                }
            }

            //Close all streams when done
            inputStream.close();
            outputStream.close();
            clientSocket.close();
        }
        catch (IOException e)
        {

        }
    }
}