import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Client {
    private Socket clientSocket;
    private OutputStream outputStream;
    private ObjectOutputStream objectOutputStream;
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;
    private String userId;

    public void startConnection(String ip, int port) throws IOException, ClassNotFoundException {
        clientSocket = new Socket(ip, port);

        outputStream = clientSocket.getOutputStream();
        objectOutputStream = new ObjectOutputStream(outputStream);

        inputStream = clientSocket.getInputStream();
        objectInputStream = new ObjectInputStream(inputStream);


    }

    public void sendMessage(String senderId, String message) throws IOException, ClassNotFoundException {
        Post post = new Post(senderId, new Date(), message);
        objectOutputStream.writeObject(post);

        try{
            post = (Post)objectInputStream.readObject();

            System.out.println("____________________________________________");
            System.out.println("Sender: "+senderId);
            System.out.println("Date: "+post.getTime());
            System.out.println("Message: "+post.getMessage());
        }
        catch(EOFException e){
            return;
        }


    }

    public void stopConnection() throws IOException {
        inputStream.close();
        outputStream.close();
        clientSocket.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        Client client = new Client();


        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String userId = args[2];
        client.userId = userId;
        client.startConnection(host, port);

        client.printPreviousPosts();

        Scanner sc= new Scanner(System.in);

        while(true){
            System.out.println("Do wish to post a message? (Y/N)");
            String a = sc.nextLine();
            String recipient = "all";
            String message = "";
            if(a.equals("Y")){
                System.out.println("Enter user ID of the recipient");
                recipient = sc.nextLine();
            }
            else if(a.equals("N")){
                return;
            }
            else{
                System.out.println("Enter a valid answer (Y/N)");
            }

            if(recipient.equals("all")){
                System.out.println("Enter your message");
                message = sc.nextLine();
                if(!message.isEmpty()){
                    client.sendMessage(userId, message);
                }

            }
            else{
                String publicKey = recipient;

                System.out.println("Enter your message");

                message = sc.nextLine();

                message = encrypt(message, publicKey);

                if(!message.isEmpty()){
                    client.sendMessage(userId, message);
                }
            }
        }

    }

    protected static String DEFAULT_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static String encrypt(String data, String keyName)
            throws BadPaddingException, IllegalBlockSizeException,
            InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance(DEFAULT_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, readPublicKey(keyName));
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    private void printPreviousPosts() throws IOException, ClassNotFoundException {

        //print previous posts
        Post post = (Post)objectInputStream.readObject();
        if(post.getPosts() != null){
            for(Post p: post.getPosts()){
                System.out.println("____________________________________________");
                System.out.println("Sender: "+p.getSenderId());
                System.out.println("Date: "+p.getTime());
                String message = p.getMessage();
                try{
                    message = decryptMessage(p.getMessage());
                }
                catch(IllegalArgumentException | IOException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchPaddingException | InvalidKeySpecException | NoSuchAlgorithmException e){
                    System.out.println("Exception thrown "+" "+ e.getClass().getSimpleName() +e.getMessage());
                    message = p.getMessage();
                }

                System.out.println("Message: "+p.getMessage());
            }
        }

    }

    private String decryptMessage(String message) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Path path = Paths.get("C:\\Users\\gmbug\\OneDrive\\Documents\\GitHub\\Server-Client-Demo\\src\\"+userId+"-private.der");

        PrivateKey privateKey;
        byte[] bytes = Files.readAllBytes(path);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(keySpec);

        Cipher decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decryptedBytes = decryptCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    //Reads public key into memory, returns it if already read once.
    private static PublicKey readPublicKey(String keyName)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey publicKey;

        Path path = Paths.get("C:\\Users\\gmbug\\OneDrive\\Documents\\GitHub\\Server-Client-Demo\\src\\"+keyName+"-public.key");
        byte[] publicKeyBytes = Files.readAllBytes(path);

        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        publicKey = keyFactory.generatePublic(publicSpec);
        return publicKey;
    }


    public static class Post implements Serializable {

        private String senderId;
        private Date time;
        private String message;
        private List<Post> posts;

        public Post() {
        }

        public Post(List<Post> posts) {
            this.posts = posts;
        }
        public Post(String senderId, Date time, String message) {
            this.senderId = senderId;
            this.time = time;
            this.message = message;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }

        public String getTime() {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            return dateFormat.format(time);
        }

        public void setTime(Date time) {
            this.time = time;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<Post> getPosts() {
            return posts;
        }

        public void setPosts(List<Post> posts) {
            this.posts = posts;
        }
    }

}