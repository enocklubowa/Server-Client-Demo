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
        post.setSignature(new String(sign(post, userId), StandardCharsets.ISO_8859_1));
        objectOutputStream.writeObject(post);
        //objectOutputStream.writeObject();

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

    //Combines plaintext with senders private key to create a signature which can be verified using the senders public key.
    public static byte[] sign(Post post, String keyName) {
        byte[] sign = null;
        byte[] postBytes = (post.toString()).getBytes(StandardCharsets.ISO_8859_1);
        try {

            PrivateKey privKey = readPrivateKey(keyName);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privKey);
            signer.update(postBytes);

            sign = signer.sign();

        } catch (Exception e) {
            System.out.println("Error in signing");
        }



        return sign;
    }

    private static PrivateKey readPrivateKey(String keyName) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        Path path = Paths.get(keyName+".key");

        PrivateKey privateKey;
        byte[] bytes = Base64.getDecoder().decode(Files.readAllBytes(path));

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(keySpec);

        return privateKey;
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
        return new String(cipher.doFinal(data.getBytes()), StandardCharsets.ISO_8859_1);
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
                    message = decryptMessage(p.getMessage(), userId);
                }
                catch(IllegalArgumentException | IOException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchPaddingException | InvalidKeySpecException | NoSuchAlgorithmException e){
                    if(message.isEmpty()){
                        message = p.getMessage();
                    }

                }

                System.out.println("Message: "+message);
            }
        }

    }

    private String decryptMessage(String message, String keyName) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptCipher = Cipher.getInstance(DEFAULT_TRANSFORMATION);
        decryptCipher.init(Cipher.DECRYPT_MODE, readPrivateKey(keyName));
        decryptCipher.update(message.getBytes(StandardCharsets.ISO_8859_1));

        //System.out.println("Decrypted message is "+new String(decryptCipher.doFinal()));

        return new String(decryptCipher.doFinal());
    }

    //Reads public key into memory, returns it if already read once.
    public static PublicKey readPublicKey(String keyName)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey publicKey;

        Path path = Paths.get(keyName+".pub");
        byte[] publicKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(path));

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
        private String signature;

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

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getSignature() {
            return signature;
        }

        @Override
        public String toString() {
            return "Post{" +
                    "senderId='" + senderId + '\'' +
                    ", time=" + time +
                    ", message='" + message + '\'' +
                    ", posts=" + posts +
                    '}';
        }
    }

}

