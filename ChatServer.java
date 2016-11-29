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
    private static MessageBuffer messageBuffer;
    private static PacketSender sender;

    public static void main(String args[]) {

        //Default Port
        int portNumber = 7777;
        messageBuffer = new MessageBuffer();
        sender = new PacketSender(threads,messageBuffer);

        try
        {
            serverSocket = new ServerSocket(portNumber);

        }

        catch (IOException e)
        {
            System.out.println(e);
        }

        //initialise the thread for sending messages
        Thread senderThread = new Thread(sender);
        senderThread.start();

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
                        threads.set(i,new ClientThread(clientSocket, threads,messageBuffer));
                        Thread t = new Thread(threads.get(i));
                        t.start();
                        clientCount++;
                        System.out.println("Clients: " + clientCount);
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

class Packet
{
    String name;
    String message;

    public Packet(String name, String message)
    {
        this.name = name;
        this.message = message;
    }

    public String toString()
    {
        return name + " says: " + message;
    }
}


class MessageBuffer
{
    ArrayList<Packet> messages;
    int currentIndex;

    public
    MessageBuffer()
    {
        this.messages = new ArrayList<>(Collections.nCopies(10, null));
        currentIndex = 0;
    }

    synchronized void add(Packet p)
    {
        messages.add(p);
        currentIndex++;
    }

    synchronized Packet remove()
    {
        if(currentIndex > 0) {
            currentIndex--;
            return (messages.remove(currentIndex));
        }
        else
        {
            return null;
        }
    }
}

class PacketSender implements Runnable //consider this the consumer
{
    private final ArrayList<ClientThread> clients;
    private MessageBuffer messageBuffer;

    public PacketSender(ArrayList<ClientThread> clients, MessageBuffer messageBuffer)
    {
        this.clients = clients;
        this.messageBuffer = messageBuffer;
    }

    @Override
    public synchronized void run()
    {
        try
        {
            while (true)
            {
                Packet p = messageBuffer.remove();


                if(p != null) {
                    String message = p.toString();
                    for (int i = 0; i < clients.size(); i++) {
                        if (clients.get(i) != null) {
                            clients.get(i).outputStream.println(message);
                        }
                    }
                }

            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}

class ClientThread implements Runnable //consider this the producer
{
    BufferedReader inputStream = null;
    PrintStream outputStream = null;
    private Socket clientSocket = null;
    private final  ArrayList<ClientThread> threads;
    private int maxClientsCount;
    private String name;
    private String quitMsg;
    private MessageBuffer messageBuffer;

    public ClientThread(Socket clientSocket, ArrayList<ClientThread> threads, MessageBuffer messageBuffer)
    {
        this.messageBuffer = messageBuffer;
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

    public synchronized void run()
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
                    messageBuffer.add(new Packet(name,line));
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