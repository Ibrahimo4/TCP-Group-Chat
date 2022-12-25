package fop.w11pchat;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer extends Thread {

    private final ServerSocket sSocket;
    private final LinkedList<ClientSupplier> connectedClients;
    private boolean closed;
    private ExecutorService ourPoolOfConnections;
    private final String[] penguinFacts;

    public ChatServer() throws IOException {
        System.out.println("Server is waiting for users on port 5000");
        this.sSocket = new ServerSocket(5000);
        this.connectedClients = new LinkedList<>();
        this.closed = false;
        this.penguinFacts = new String[]{
                "The largest Penguin, can grow to be over four feet!",
                "There was once a \"mega\" penguin that stood 6.5 feet tall and weighed more than 250 pounds.",
                "Penguins tend to be monogamous.",
                "Male emperor penguins incubate eggs while the female goes hunting.",
                "The female penguins grow to be over four feet.",
                "Penguins are carnivores.",
                "Some penguins can reach speeds of 22 miles an hour.",
                "The world's oldest penguin is estimated to be an impressive 40 years old.",
                "Emperor penguins huddle to keep warm.",
                "Penguins can't fly, but they CAN become airborne. Some can leap as high as nine feet!\n",
        };
    }


    public void run(){
        try {
            try {
                while (!closed) {
                    Socket ourClient = sSocket.accept();
                    ourPoolOfConnections = Executors.newCachedThreadPool();
                    ClientSupplier supp = new ClientSupplier(ourClient);
                    this.connectedClients.add(supp);
                    this.ourPoolOfConnections.execute(supp);
                }
            } catch (IOException e) {
                try {
                    shutDownTheServer();
                } catch (IOException exception) {
                    throw new RuntimeException(e);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void broadcast(String msg){
        for(ClientSupplier c:this.connectedClients){
            if(c != null){
                c.sendMessage(msg);
            }
        }
    }


    public void shutDownTheServer() throws IOException {
        this.closed = true;
        if(!this.sSocket.isClosed()){
            this.sSocket.close();
        }
        this.connectedClients.forEach(eachClient -> {
            try {
                eachClient.closeConnectionWithClient();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }



    private class ClientSupplier implements Runnable{

        private final Socket clientSocket;
        private String nickName;
        private BufferedReader reader;
        private PrintWriter writer;

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        public ClientSupplier(Socket clientSocket){

            this.clientSocket = clientSocket;
        }



        @Override
        public void run() {

            try{

                this.reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                this.writer = new PrintWriter(
                        clientSocket.getOutputStream(), true);

                this.writer.println("Please enter your nickname: ");

                this.nickName = reader.readLine();

                writer.println("Connection accepted, welcome!");
                writer.println("""
                    Instructions:
                    1. Simply type the message to send broadcast to all active clients
                    2. Type @username <space> 'yourmessage' without quotes to send message to desired client
                    3. Type 'WHOIS' without quotes to see list of active clients
                    4. Type 'LOGOUT' without quotes to logoff from the server
                    5. Type 'PINGU' without quotes to request a random penguin fact
                    """);

                System.out.println("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' joined the chatroom!");
                broadcast("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' joined the chatroom!");
                String msg;



                while((msg = reader.readLine()) != null){
                    if(msg.startsWith("PINGU")){
                        Random randomNumber = new Random();
                        String funFact = penguinFacts[randomNumber.nextInt(0, penguinFacts.length)];
                        broadcast("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' asked for a fun fact about penguins, so here's one: " + funFact);
                        System.out.println("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' asked for a fun fact about penguins, so here's one: " + funFact);

                    }else if(msg.startsWith("LOGOUT")){
                        broadcast("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' left the chatroom!");
                        System.out.println("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' left the chatroom!");
                        this.closeConnectionWithClient();

                    }else if(msg.startsWith("@")){
                        String[] splittedString = msg.split(" ");
                        String userToSendTheMessageTo = (splittedString[0].split("@"))[1];
                        StringBuilder theMessageToSend = new StringBuilder();
                        for(int i = 1; i < splittedString.length; i++){
                            theMessageToSend.append(splittedString[i]).append(" ");
                        }
                        if(Objects.equals(this.getNickName(), userToSendTheMessageTo)){
                            writer.println("Are you talking to yourself or my eyes are deceiving me?!");
                        }else {
                            if (connectedClients.stream().noneMatch(each -> each.getNickName().equals(userToSendTheMessageTo))) {
                                this.writer.println("This user doesn't exist, try writing 'WHOIS' to see the list of active users!");

                            } else {
                                connectedClients.forEach(eachConnectedClient -> {
                                    if (Objects.equals(eachConnectedClient.getNickName(), userToSendTheMessageTo)) {
                                        eachConnectedClient.writer.println("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' texted you: " + theMessageToSend);
                                    }
                                });
                            }
                        }
                    }
                    else if (msg.startsWith("WHOIS")) {
                        StringBuilder listOfActiveUsers = new StringBuilder();

                        for(int i = 0; i < connectedClients.size(); i++){
                            listOfActiveUsers.append(i).append(". ").append(
                                    Objects.equals(connectedClients.get(i).nickName, this.getNickName()) ? connectedClients.get(i).nickName + " - You": connectedClients.get(i).nickName
                            ).append("\n");
                        }
                        writer.println("Here are the currently active/connected users you can chat with");
                        writer.println(listOfActiveUsers);
                    }else{
                        broadcast("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' wrote: " + msg);
                        System.out.println("At " + LocalTime.now().format(formatter) + " '" + this.getNickName() + "' wrote: " + msg);
                    }

                }

            }catch (Exception E){
                try {
                    closeConnectionWithClient();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
        public String getNickName() {
            return nickName;
        }

        public void sendMessage(String msg) {
            this.writer.println(msg);
        }

        public void closeConnectionWithClient() throws IOException {
            closeReaderWriter();
            clientSocket.close();
            connectedClients.remove(this);
            if(!clientSocket.isClosed()) {
                clientSocket.close();
            }
        }
        private void closeReaderWriter() throws IOException {
            reader.close();
            writer.close();
        }

    }


    public static void main(String[] args) throws IOException {
        ChatServer groupChatServer = new ChatServer();
        groupChatServer.start();
    }

}



