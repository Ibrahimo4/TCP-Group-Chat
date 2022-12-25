package fop.w11pchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient implements Runnable{

    private final Socket clientSocket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private boolean closed;

    public ChatClient() throws IOException {
        this.clientSocket = new Socket("localhost", 5000);
        this.reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        this.writer = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.closed = false;
    }

    @Override
    public void run() {
        try{

            InputSupplier iP = new InputSupplier();
            Thread iPThread = new Thread(iP);
            iPThread.start();

            String inputtedMessage = "";

            while((inputtedMessage = reader.readLine()) != null){
                System.out.println(inputtedMessage);
            }
        }catch(IOException e){
            shutDownTheConnection();
        }
    }

    public void shutDownTheConnection() {
        this.closed = true;
        try{
            closeReaderWriter();
            if(!this.clientSocket.isClosed()){
                this.clientSocket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    private void closeReaderWriter() {
        try {
            this.reader.close();
            this.writer.close();
        }catch (Exception ignored){

        }
    }



    private class InputSupplier implements Runnable{


        @Override
        public void run() {
            try{

                BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

                while(!closed){
                    String msg = inputReader.readLine();
                    if(msg.equals("LOGOUT")){
                        writer.println("LOGOUT");
                        inputReader.close();
                        shutDownTheConnection();
                    }else{
                        writer.println(msg);
                    }

                }

            }catch (Exception e){
                shutDownTheConnection();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ChatClient ourClient = new ChatClient();
        ourClient.run();
    }
}