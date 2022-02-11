/* This program generates a pair of matching public/private RSA keys.
 * It takes a userid as an argument, and places the generated keys in
 * "<userid>.pub" and "<userid>.prv" in the current working directory.
 * It is up to you to put the generated keys at some appropriate
 * location for the client/server programs to use.
 *
 * DO NOT use or somehow invoke the code here from inside your client/
 * server to generate new keys.
 */

import java.io.*;
import java.security.*;

public class RSAKeyGen {

    public static void main(String [] args) throws Exception {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        System.out.println("Public key format "+publicKey.getFormat());
        System.out.println("Private key format "+privateKey.getFormat());

        try (FileOutputStream fos = new FileOutputStream(args[0]+"-public"+".key")) {
            fos.write(publicKey.getEncoded());
        }

        try (FileOutputStream fos = new FileOutputStream(args[0]+"-private"+".der")) {
            fos.write(privateKey.getEncoded());
        }

        /*
        if (args.length != 1) {
            System.err.println("Usage: java RSAKeyGen userid");
            System.exit(-1);
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();

        ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(args[0] + ".der"));
        objOut.writeObject(kp.getPublic());
        objOut.close();

        objOut = new ObjectOutputStream(new FileOutputStream(args[0] + ".prv"));
        objOut.writeObject(kp.getPrivate());


         */
    }

}
