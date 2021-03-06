package v2x;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static v2x.PseudonymAuthority.CERTIFICATE_AMOUNT;

public class Compromised {
    static final int MULTICAST_PORT = 2020;
    static final int UNICAST_PORT = 2021;
    static final String OWN_CERTIFICATE_LOCATION = "Authentication/OBU-X-certificate0.crt";
    static final String CA_CERTIFICATE_LOCATION = "Authentication/CA-certificate.crt";
    static final String OWN_PRIVATE_KEY_LOCATION = "Authentication/OBU-X-private-key0.der";
    static final String CRL_LOCATION = "Authentication/CRL-X.crl";
    static final String DNS_PRIVATE_KEY = "Authentication/OBU-X-private-key0.der";
    static final String MALICIOUS_DNS_RESPONSE = "artoria.saber.fgo=2001:0db8:85a3:0000:0000:8a2e:0370:7334";

    /**
     * Handles the initialization of the program to see which experiment it is
     * running
     *
     * @param args input from the command line when running the program
     */
    public static void main(String args[]) throws IOException, ClassNotFoundException, CertificateException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
            InvalidKeyException, InvalidKeySpecException {
        int mode = Integer.parseInt(args[0]);
        switch (mode) {
            case 1:
                System.out.println("running test 1");
                runFirstTest();
                break;
            case 2:
                System.out.println("running test 2");
                runSecondTest();
                break;
            case 3:
                System.out.println("running test 3");
                runThirdTest(Integer.parseInt(args[1]));
                break;
            case 4:
                System.out.println("running test 4");
                runFourthTest(Integer.parseInt(args[1]));
                break;
        }
    }

    /**
     * Waits for an input and checks if it is a query for the first test.
     *
     * @return <code>String</code> a string that is the IP address of the sender
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static String receiveQueryTest1() throws IOException, ClassNotFoundException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        byte[] buffer = new byte[256];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            Message message = CommunicationFunctions.byteArrayToMessage(buffer);
            String request = message.getValue("Query");
            if (request.equals("Query")) {
                //System.out.println("query received");
                String inetAddress = packet.getAddress().getHostAddress();
                return inetAddress;
            }
        }
    }

    /**
     * Sends a message with the incorrect answer.
     *
     * @param returnIPAddress a string that is the IP address of who to send to
     * @throws IOException
     */
    private static void sendAnswerTest1(String returnIPAddress) throws IOException {
        InetAddress address = InetAddress.getByName(returnIPAddress);
        DatagramSocket clientSocket = new DatagramSocket();
        Message answer = new Message();
        answer.putValue("Answer", "1");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(answer);
        objectOutputStream.flush();
        byte[] data = byteArrayOutputStream.toByteArray();
        DatagramPacket answerPacket = new DatagramPacket(data, data.length, address, UNICAST_PORT);
        clientSocket.send(answerPacket);
        //System.out.println("answer sent");
        clientSocket.close();
    }

    /**
     * Handles the first test.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void runFirstTest() throws IOException, ClassNotFoundException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        WaitQueryOne waitQueryOne = new WaitQueryOne(serverSocket, UNICAST_PORT, "1");
        waitQueryOne.start();
    }

    /**
     * Waits for an input, checks if it is a query, and checks if it is correctly
     * authenticated
     *
     * @return <code>String</code> a string that is the IP address of the sender
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    private static String receiveQueryTest2()
            throws IOException, ClassNotFoundException, CertificateException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        byte[] buffer = new byte[65508];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            Message message = CommunicationFunctions.byteArrayToMessage(buffer);
            String request = message.getValue("Query");
            if (request.equals("Query")) {
                //System.out.println("query received");
                String certificate = message.getValue("Certificate");
                String encryptedHash = message.getValue("Hash");
                if (AuthenticationFunctions.authenticateMessage(request, encryptedHash, certificate,
                        CA_CERTIFICATE_LOCATION)) {
                    String inetAddress = packet.getAddress().getHostAddress();
                    serverSocket.close();
                    return inetAddress;
                }
            }
        }
    }

    /**
     * Sends a message with the incorrect answer for the second test along with a
     * certificate and an encrypted hash of the message.
     *
     * @param returnIPAddress a string that is the IP address of who to send to
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     */
    private static void sendAnswerTest2(String returnIPAddress)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        String userCertificate = AuthenticationFunctions.getCertificate(OWN_CERTIFICATE_LOCATION);
        PrivateKey userPrivateKey = AuthenticationFunctions.getPrivateKey(OWN_PRIVATE_KEY_LOCATION);
        String message = "1";
        String hash = AuthenticationFunctions.hashMessage(message);
        String authentication = AuthenticationFunctions.encryptMessage(hash, userPrivateKey);
        InetAddress address = InetAddress.getByName(returnIPAddress);
        DatagramSocket clientSocket = new DatagramSocket();
        Message answer = new Message();
        answer.putValue("Answer", message);
        answer.putValue("Certificate", userCertificate);
        answer.putValue("Hash", authentication);
        byte[] data = CommunicationFunctions.messageToByteArray(answer);
        DatagramPacket answerPacket = new DatagramPacket(data, data.length, address, UNICAST_PORT);
        clientSocket.send(answerPacket);
        //System.out.println("answer sent");
        clientSocket.close();
    }

    /**
     * Handles the second test.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    private static void runSecondTest() throws IOException, ClassNotFoundException, CertificateException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
            InvalidKeyException, InvalidKeySpecException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        WaitQueryTwo waitQueryTwo = new WaitQueryTwo(serverSocket, UNICAST_PORT, "1",
                CA_CERTIFICATE_LOCATION, OWN_CERTIFICATE_LOCATION, OWN_PRIVATE_KEY_LOCATION);
        waitQueryTwo.start();
    }

    /**
     * Waits for an input, checks if it is a query, and checks if it is correctly
     * authenticated. Same as the second test.
     *
     * @return <code>String</code> a string that is the IP address of the sender
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    private static String receiveQueryTest3()
            throws IOException, ClassNotFoundException, CertificateException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        byte[] buffer = new byte[65508];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            Message message = CommunicationFunctions.byteArrayToMessage(buffer);
            String request = message.getValue("Query");
            if (request.equals("Query")) {
                //System.out.println("query received");
                String certificate = message.getValue("Certificate");
                String encryptedHash = message.getValue("Hash");
                if (AuthenticationFunctions.authenticateMessage(request, encryptedHash, certificate,
                        CA_CERTIFICATE_LOCATION)) {
                    String inetAddress = packet.getAddress().getHostAddress();
                    serverSocket.close();
                    return inetAddress;
                }
            }
        }
    }

    /**
     * Sends a DNS message which is an incorrectly signed message. The DNS message
     * is wrapped inside a message that is signed by the sender.
     *
     * @param returnIPAddress a string that is the IP address of who to send to
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     */
    private static void sendAnswerTest3(String returnIPAddress, int number)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchPaddingException {

        String userCertificate = AuthenticationFunctions
                .getCertificate("Authentication/OBU-X-certificate" + number + ".crt");
        PrivateKey userPrivateKey = AuthenticationFunctions
                .getPrivateKey("Authentication/OBU-X-private-key" + number + ".der");
        PrivateKey dnsPrivateKey = AuthenticationFunctions.getPrivateKey(DNS_PRIVATE_KEY);

        String innerAnswer = "0";
        String innerHash = AuthenticationFunctions.hashMessage(innerAnswer);
        String innerEncryptedHash = AuthenticationFunctions.encryptMessage(innerHash, dnsPrivateKey);

        Message innerMessage = new Message();
        innerMessage.putValue("Answer", innerAnswer);
        innerMessage.putValue("Hash", innerEncryptedHash);

        byte[] innerMessageByte = CommunicationFunctions.messageToByteArray(innerMessage);
        byte[] innerMessageByteBase64 = Base64.getEncoder().encode(innerMessageByte);
        String innerMessageString = new String(innerMessageByteBase64);

        String outerHash = AuthenticationFunctions.hashMessage(innerMessageString);
        String outerEncryptedHash = AuthenticationFunctions.encryptMessage(outerHash, userPrivateKey);

        Message outerMessage = new Message();
        outerMessage.putValue("Answer", innerMessageString);
        outerMessage.putValue("Hash", outerEncryptedHash);
        outerMessage.putValue("Certificate", userCertificate);

        byte[] outerMessageByte = CommunicationFunctions.messageToByteArray(outerMessage);
        InetAddress address = InetAddress.getByName(returnIPAddress);
        DatagramPacket answerPacket = new DatagramPacket(outerMessageByte, outerMessageByte.length, address,
                UNICAST_PORT);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.send(answerPacket);
        //System.out.println("answer sent");
        clientSocket.close();
    }

    /**
     * Handles the third test.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    private static synchronized void runThirdTest(int rate) throws IOException, ClassNotFoundException, CertificateException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
            InvalidKeyException, InvalidKeySpecException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        WaitQueryThree waitQueryThree = new WaitQueryThree(serverSocket, UNICAST_PORT, "1",
                CA_CERTIFICATE_LOCATION, "Authentication/OBU-X-certificate.crt", "Authentication/OBU-X-private-key.der", rate, DNS_PRIVATE_KEY);
        waitQueryThree.start();

//        int counter = 0;
//        int number = 0;
//        while (true) {
//            String returnIPAddress = receiveQueryTest3();
//            sendAnswerTest3(returnIPAddress, number);
//            if (number > CERTIFICATE_AMOUNT - 2) {
//                System.out.println("certificate limit reached");
//            }
//            else if (counter != 0 && counter % rate == 0) {
//                System.out.println("changing certificate");
//                number++;
//            }
//            counter++;
//        }
    }

    private static String receiveQueryTest4()
            throws IOException, ClassNotFoundException, CertificateException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        byte[] buffer = new byte[65508];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            Message message = CommunicationFunctions.byteArrayToMessage(buffer);
            String request = message.getValue("Query");
            if (request.equals("Query")) {
                //System.out.println("query received");
                String certificate = message.getValue("Certificate");
                String encryptedHash = message.getValue("Hash");
                if (AuthenticationFunctions.authenticateMessage(request, encryptedHash, certificate,
                        CA_CERTIFICATE_LOCATION)) {
                    String inetAddress = packet.getAddress().getHostAddress();
                    serverSocket.close();
                    return inetAddress;
                }
            }
        }
    }

    private static void sendAnswerTest4(String returnIPAddress, int number)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        String userCertificate = AuthenticationFunctions
                .getCertificate("Authentication/OBU-X-certificate" + number + ".crt");
        PrivateKey userPrivateKey = AuthenticationFunctions
                .getPrivateKey("Authentication/OBU-X-private-key" + number + ".der");

        String innerAnswer = MALICIOUS_DNS_RESPONSE;

        Message innerMessage = new Message();
        innerMessage.putValue("Answer", innerAnswer);

        byte[] innerMessageByte = CommunicationFunctions.messageToByteArray(innerMessage);
        byte[] innerMessageByteBase64 = Base64.getEncoder().encode(innerMessageByte);
        String innerMessageString = new String(innerMessageByteBase64);

        String outerHash = AuthenticationFunctions.hashMessage(innerMessageString);
        String outerEncryptedHash = AuthenticationFunctions.encryptMessage(outerHash, userPrivateKey);

        Message outerMessage = new Message();
        outerMessage.putValue("Answer", innerMessageString);
        outerMessage.putValue("Hash", outerEncryptedHash);
        outerMessage.putValue("Certificate", userCertificate);

        byte[] outerMessageByte = CommunicationFunctions.messageToByteArray(outerMessage);
        InetAddress address = InetAddress.getByName(returnIPAddress);
        DatagramPacket answerPacket = new DatagramPacket(outerMessageByte, outerMessageByte.length, address,
                UNICAST_PORT);
        DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.send(answerPacket);
        //System.out.println("answer sent");
        clientSocket.close();
    }

    private static synchronized void runFourthTest(int rate) throws IOException, ClassNotFoundException, CertificateException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
            InvalidKeyException, InvalidKeySpecException {
        MulticastSocket serverSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress group = InetAddress.getByName("225.0.0.0");
        serverSocket.joinGroup(group);
        WaitQueryFour waitQueryFour = new WaitQueryFour(serverSocket, UNICAST_PORT,
                MALICIOUS_DNS_RESPONSE,
                CA_CERTIFICATE_LOCATION, "Authentication/OBU-X-certificate.crt", "Authentication/OBU-X-private-key.der", rate, DNS_PRIVATE_KEY);
        waitQueryFour.start();
//        int counter = 0;
//        int number = 0;
//        while (true) {
//            String returnIPAddress = receiveQueryTest4();
//            sendAnswerTest4(returnIPAddress, number);
//            if (number > CERTIFICATE_AMOUNT - 2) {
//                System.out.println("certificate limit reached");
//            }
//            else if (counter != 0 && counter % rate == 0) {
//                System.out.println("changing certificate");
//                number++;
//            }
//            counter++;
//        }
    }
}
