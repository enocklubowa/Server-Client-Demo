import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Server {
    private ServerSocket serverSocket;
    public static List<Client.Post> posts = new ArrayList<>();

    public void start(int port) throws IOException, ClassNotFoundException {
        //Server socket to serve clients
        serverSocket = new ServerSocket(port);

        while (true) {
            //accept client socket
            new ClientHandler(serverSocket.accept()).start();
        }


    }

    public void stop() throws IOException {
        serverSocket.close();
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int portNumber = Integer.parseInt(args[0]);
        Server server = new Server();
        server.start(portNumber);
    }

    private static class ClientHandler extends Thread{
        private Socket clientSocket;
        private OutputStream outputStream;
        private ObjectOutputStream objectOutputStream;
        private InputStream inputStream;
        private ObjectInputStream objectInputStream;

        public ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        public void run(){
            System.out.println("New Client connected");

            //When connection is established between client and server, start reading/writing
            try {
                outputStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //to write objects to the output stream
            try {
                objectOutputStream = new ObjectOutputStream(outputStream);
                //objectOutputStream.writeObject(new Post(posts));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                inputStream = clientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                objectInputStream = new ObjectInputStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //write previous posts
            try {
                objectOutputStream.writeObject(new Client.Post(posts));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Client.Post post;
            byte[] signature;
            try{
                while( (post = (Client.Post)objectInputStream.readObject()) != null){
                    if(verify(post.toString().getBytes(StandardCharsets.ISO_8859_1),
                            post.getSignature().getBytes(StandardCharsets.ISO_8859_1),
                            post.getSenderId())){
                        System.out.println("Sender Verified");

                        System.out.println(post.getMessage());
                        posts.add(post);
                        objectOutputStream.writeObject(post);

                    }
                    else{
                        System.out.println("Sender Not Verified");
                    }



                }

                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
            catch(IOException | ClassNotFoundException e){

            }

        }


    }

    private static Boolean verify(byte[] plainText, byte[] signature, String keyName) {

        System.out.println("Username used for verification: "+keyName);
        byte[] signatureBytes = null;
        Signature verifier = null;
        boolean verified = false;
        try {

            PublicKey pubKey = Client.readPublicKey(keyName);
            verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(pubKey);
            verifier.update(plainText);

            signatureBytes = signature;
            verified = verifier.verify(signatureBytes);
        } catch (Exception e) {
            System.out.println("Error in verify");
        }
        return verified;
    }
}