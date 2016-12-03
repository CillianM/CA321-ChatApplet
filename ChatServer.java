import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class ChatServer
{
    // The default server socket.
    private static ServerSocket serverSocket = null;
    // The default client socket.
    private static Socket clientSocket = null;
    private static ArrayList<ClientThread> threads = new ArrayList<>();
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

        //initialise the thread for sending messages to and from clients
        Thread senderThread = new Thread(sender);
        senderThread.start();

        //Create a client socket for each connection and  create a new thread for it
        while (true)
        {
            try
            {
                //This inputStream a new connection no matter what
                clientSocket = serverSocket.accept();
                ClientThread thread = new ClientThread(clientSocket, threads,messageBuffer);
                threads.add(thread);
                Thread t = new Thread(thread);
                t.start();
                printActiveClients();
            }
            catch (IOException e)
            {
                System.out.println(e);
            }
        }
    }

    static void printActiveClients()
    {
        int count = 0;
        for (ClientThread thread : threads) {
            if (thread != null) {
                count++;
            }
        }
        System.out.println("Clients: " + count);
    }
}

class MessageBuffer
{
    ArrayList<String> messages;
    int occupied = 0;
    int currentIndex;

    public MessageBuffer()
    {
        this.messages = new ArrayList<>();
        currentIndex = 0;
    }

    synchronized void add(String s)
    {
        messages.add(s);
        occupied++;
        currentIndex++;
        notifyAll();
    }

    synchronized String remove()
    {
        try
        {
            while (occupied < 1) wait();
            currentIndex--;
            occupied--;
            return (messages.remove(currentIndex));

        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
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
                String message = messageBuffer.remove();

                if(message != null) {
                    clients.stream().filter(client -> client != null).forEach(client -> {
                        client.outputStream.println(message);
                    });
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
    private String name;
    private MessageBuffer messageBuffer;

    public ClientThread(Socket clientSocket, ArrayList<ClientThread> threads, MessageBuffer messageBuffer)
    {
        this.messageBuffer = messageBuffer;
        this.clientSocket = clientSocket;
        this.threads = threads;
        try
        {
            inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputStream = new PrintStream(clientSocket.getOutputStream());
            this.name = inputStream.readLine();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void run()
    {
        try
        {
            messageBuffer.add(name + " just joined the chatroom...");

            while (true)
            {

                String line = inputStream.readLine();
                messageBuffer.add(name + " says: " + line);
            }


        }
        catch (IOException e)
        {
            //Remove the users thread
            int size = threads.size();
            for (int i = 0; i < size; i++)
            {
                if (threads.get(i) == this)
                {
                    threads.remove(i);
                    break;
                }
            }

            messageBuffer.add(name + " has just left the chatroom...");
            printActiveClients();

            try
            {
                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
            catch (IOException ie)
            {
                e.printStackTrace();
            }
        }
    }

    void printActiveClients()
    {
        int count = 0;
        for (ClientThread thread : threads) {
            if (thread != null) {
                count++;
            }
        }
        System.out.println("Clients: " + count);
    }
}