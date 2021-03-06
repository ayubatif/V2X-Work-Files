package v2x;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Querier extends Thread {
    static final int MULTICAST_PORT = 2020;
    static final int UNICAST_PORT = 2021;
    static final String OWN_CERTIFICATE_LOCATION = "Authentication/OBU-A-certificate0.crt";
    static final String CA_CERTIFICATE_LOCATION = "Authentication/CA-certificate.crt";
    static final String OWN_PRIVATE_KEY_LOCATION = "Authentication/OBU-A-private-key0.der";
    static final String CRL_LOCATION = "Authentication/CRL-A.crl";
    static final String OBU_X_CERTIFICATE_LOCATION = "Authentication/OBU-X-certificate0.crt";
    static final String DNS_CERTIFICATE_LOCATION = "Authentication/DNS-certificate.crt";
    static final String BLOOM_FILTER_LOCATION = "Authentication/DNS-bloom-filter.bf";

    /**
     * Handles the initialization of the program to see which experiment it is running.
     *
     * @param args input from the command line when running the program
     */
    public void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
            NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, CertificateException {
        int mode = Integer.parseInt(args[0]);
        int testAmount = Integer.parseInt(args[1]);
        System.out.println(System.getProperty("user.dir"));
        switch (mode) {
            case 1:
                System.out.println("running test 1");
                runFirstTest(testAmount);
                break;
            case 2:
                System.out.println("running test 2");
                runSecondTest(testAmount);
                break;
            case 3:
                System.out.println("running test 3");
                runThirdTest(testAmount, Integer.parseInt(args[2]));
                break;
            case 4:
                System.out.println("running test 4");
                runFourthTest(testAmount, Integer.parseInt(args[2]));
                break;
            case 0:
                System.out.println("running test 0");
                test(testAmount);
                break;
            case -1:
                System.out.println("running test -1");
                crlTest();
                break;
            case -2:
                System.out.println("running test -2");
                bloomFilterTest();
                break;
            case -3:
                System.out.println(System.getProperty("user.dir"));
        }
    }

    // https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    // https://www.developer.com/java/data/how-to-multicast-using-java-sockets.html

    /**
     * Sends query message to the 2 OBUs for the first test.
     *
     * @throws IOException
     */
    private void sendQueryTest1() throws IOException {
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress groupIP = InetAddress.getByName("225.0.0.0");
        multicastSocket.joinGroup(groupIP);
        Message query = new Message();
        query.putValue("Query", "Query");
        long currentTime = System.currentTimeMillis();
        String time = String.valueOf(currentTime);
        query.putValue("Time", time);
        byte[] data = CommunicationFunctions.messageToByteArray(query);
        int randomPort = multicastSocket.getLocalPort();
        DatagramPacket queryPacket = new DatagramPacket(data, data.length, groupIP, randomPort);
        multicastSocket.send(queryPacket);
        //System.out.println("query sent");
        multicastSocket.close();
    }

    // https://stackoverflow.com/questions/2275443/how-to-timeout-a-thread

    /**
     * Handles the first test.
     *
     * @param testAmount an integer specifying the amount of query to be sent
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public synchronized void runFirstTest(int testAmount) throws IOException, InterruptedException {
        TimeCounter timeCounter = new TimeCounter(1, testAmount);
        AnswerCounter answerCounter = new AnswerCounter(1);
        ValidityCounter validityCounter = new ValidityCounter(1);

        try {
            answerCounter.importJSONLog();
            validityCounter.importJSONLog();
			timeCounter.importJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatagramSocket serverSocket = new DatagramSocket(2021);
        ThreadCommunication threadCommunication = new ThreadCommunication(true);
        long startTime = System.currentTimeMillis();

        int counter = 0;
        long TSQStart;
        long TSQEnd;
        while (counter < testAmount) {
            try {
                if (threadCommunication.getReady()) {
                    if (counter != 0) {
                        serverSocket = new DatagramSocket(2021);
                    }
                    threadCommunication.setReady(false);
                    ReceiveAnswerOne receiveAnswerOne = new ReceiveAnswerOne(serverSocket, answerCounter,
                            validityCounter, timeCounter, counter, threadCommunication);
                    receiveAnswerOne.start();
                    TSQStart = System.currentTimeMillis();
                    sendQueryTest1();
                    TSQEnd = System.currentTimeMillis();
                    timeCounter.addTimeToSendQuery(TSQEnd - TSQStart);
                    timeCounter.addTimeToRawTSQData(TSQEnd - TSQStart);
                    counter++;
                    if (counter % 25 == 0) {
                        System.out.println("query number: " + counter);
                    }
                    startTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
                serverSocket.close();
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                //System.out.println("timeout");
                threadCommunication.setReady(true);
                counter--;
            }
            Thread.sleep(100);
        }

        Thread.sleep(1000);

        System.out.println(answerCounter.printAnswer());
        System.out.println(answerCounter.printMath());
        System.out.println(validityCounter.printValidity());
        System.out.println(validityCounter.printMath());
        System.out.println("ALL_AVG_TQR_TIME: "+timeCounter.getPercentage1());
        System.out.println("NOT_AVG_TQR_TIME: "+timeCounter.getBiasedPercentage1());
        System.out.println("ALL_AVG_TSQ_TIME: "+timeCounter.getPercentage2());
        System.out.println("NOT_AVG_TSQ_TIME: "+timeCounter.getBiasedPercentage2());
        System.out.println("ALL_AVG_TPR_TIME: "+timeCounter.getPercentage3());
        System.out.println("NOT_AVG_TPR_TIME: "+timeCounter.getBiasedPercentage3());

        answerCounter.logAnswers();
        validityCounter.logAnswers();
        timeCounter.logAnswers();
        try {
            answerCounter.exportJSONLog();
            validityCounter.exportJSONLog();
            timeCounter.exportJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends query message, hash, and certificate to the 2 OBUs
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     */
    private void sendQueryTest2()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        String userCertificate = AuthenticationFunctions.getCertificate(OWN_CERTIFICATE_LOCATION);
        PrivateKey userPrivateKey = AuthenticationFunctions.getPrivateKey(OWN_PRIVATE_KEY_LOCATION);
        String message = "Query";
        String hash = AuthenticationFunctions.hashMessage(message);
        String authentication = AuthenticationFunctions.encryptMessage(hash, userPrivateKey);
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress groupIP = InetAddress.getByName("225.0.0.0");
        multicastSocket.joinGroup(groupIP);
        Message query = new Message();
        query.putValue("Query", message);
        query.putValue("Certificate", userCertificate);
        query.putValue("Hash", authentication);
        long currentTime = System.currentTimeMillis();
        String time = String.valueOf(currentTime);
        query.putValue("Time", time);
        byte[] data = CommunicationFunctions.messageToByteArray(query);
        int randomPort = multicastSocket.getLocalPort();
        DatagramPacket queryPacket = new DatagramPacket(data, data.length, groupIP, randomPort);
        multicastSocket.send(queryPacket);
        //System.out.println("query sent");
        multicastSocket.close();
    }

    // https://stackoverflow.com/questions/2275443/how-to-timeout-a-thread

    /**
     * Handles the second test.
     *
     * @param testAmount an integer specifying the amount of query to be sent
     * @throws IOException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws ClassNotFoundException
     * @throws CertificateException
     */
    public void runSecondTest(int testAmount)
            throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException,
            InvalidKeySpecException, InterruptedException {
        TimeCounter timeCounter = new TimeCounter(2, testAmount);
        AnswerCounter answerCounter = new AnswerCounter(2);
        ValidityCounter validityCounter = new ValidityCounter(2);

        try {
            answerCounter.importJSONLog();
            validityCounter.importJSONLog();
			timeCounter.importJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new PrintWriter(CRL_LOCATION).close(); // empty the file
        String blacklistCertificate = AuthenticationFunctions.getCertificate(OBU_X_CERTIFICATE_LOCATION);
        AuthenticationFunctions.addToCRL(blacklistCertificate, CRL_LOCATION);
        DatagramSocket serverSocket = new DatagramSocket(2021);
        ThreadCommunication threadCommunication = new ThreadCommunication(true);
        long startTime = System.currentTimeMillis();

        int counter = 0;
        long TSQStart;
        long TSQEnd;
        while (counter < testAmount) {
            try {
                if (threadCommunication.getReady()) {
                    if (counter != 0) {
                        serverSocket = new DatagramSocket(2021);
                    }
                    threadCommunication.setReady(false);
                    ReceiveAnswerTwo receiveAnswerTwo = new ReceiveAnswerTwo(serverSocket, answerCounter,
                            validityCounter, timeCounter, counter, threadCommunication);
                    receiveAnswerTwo.start();
                    TSQStart = System.currentTimeMillis();
                    sendQueryTest2();
                    TSQEnd = System.currentTimeMillis();
                    timeCounter.addTimeToSendQuery(TSQEnd - TSQStart);
                    timeCounter.addTimeToRawTSQData(TSQEnd - TSQStart);
                    counter++;
                    if (counter % 25 == 0) {
                        System.out.println("query number: " + counter);
                    }
                    startTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
                serverSocket.close();
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                System.out.println("timeout");
                threadCommunication.setReady(true);
                counter--;
            }
            Thread.sleep(100);
        }

        Thread.sleep(1000);

        System.out.println(answerCounter.printAnswer());
        System.out.println(answerCounter.printMath());
        System.out.println(validityCounter.printValidity());
        System.out.println(validityCounter.printMath());
        System.out.println("ALL_AVG_TQR_TIME: "+timeCounter.getPercentage1());
        System.out.println("NOT_AVG_TQR_TIME: "+timeCounter.getBiasedPercentage1());
        System.out.println("ALL_AVG_TSQ_TIME: "+timeCounter.getPercentage2());
        System.out.println("NOT_AVG_TSQ_TIME: "+timeCounter.getBiasedPercentage2());
        System.out.println("ALL_AVG_TPR_TIME: "+timeCounter.getPercentage3());
        System.out.println("NOT_AVG_TPR_TIME: "+timeCounter.getBiasedPercentage3());

        answerCounter.logAnswers();
        validityCounter.logAnswers();
        timeCounter.logAnswers();
        try {
            answerCounter.exportJSONLog();
            validityCounter.exportJSONLog();
            timeCounter.exportJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends query message, hash, and certificate to the 2 OBUs. Same as the second one.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     */
    private void sendQueryTest3(int counter)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        String userCertificate = AuthenticationFunctions.getCertificate(OWN_CERTIFICATE_LOCATION);
        PrivateKey userPrivateKey = AuthenticationFunctions.getPrivateKey(OWN_PRIVATE_KEY_LOCATION);
        String message = "Query";
        String hash = AuthenticationFunctions.hashMessage(message);
        String authentication = AuthenticationFunctions.encryptMessage(hash, userPrivateKey);
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress groupIP = InetAddress.getByName("225.0.0.0");
        multicastSocket.joinGroup(groupIP);
        Message query = new Message();
        query.putValue("Query", message);
        query.putValue("Certificate", userCertificate);
        query.putValue("Hash", authentication);
        long currentTime = System.currentTimeMillis();
        String time = String.valueOf(currentTime);
        query.putValue("Time", time);
        query.putValue("TestNumber", String.valueOf(counter));
        byte[] data = CommunicationFunctions.messageToByteArray(query);
        int randomPort = multicastSocket.getLocalPort();
        DatagramPacket queryPacket = new DatagramPacket(data, data.length, groupIP, randomPort);
        multicastSocket.send(queryPacket);
        //System.out.println("query sent");
        multicastSocket.close();
    }

    // https://stackoverflow.com/questions/2275443/how-to-timeout-a-thread

    /**
     * Handles the third test.
     *
     * @param testAmount an integer specifying the amount of query to be sent
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws InterruptedException
     */
    public void runThirdTest(int testAmount, int rate)
            throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException,
            InvalidKeySpecException, InterruptedException {
        TimeCounter timeCounter = new TimeCounter(3, rate, testAmount);
        AnswerCounter answerCounter = new AnswerCounter(3, rate);
        ValidityCounter validityCounter = new ValidityCounter(3, rate);

        try {
            answerCounter.importJSONLog();
            validityCounter.importJSONLog();
			timeCounter.importJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new PrintWriter(CRL_LOCATION).close(); // empty the file
        String blacklistCertificate = AuthenticationFunctions.getCertificate(OBU_X_CERTIFICATE_LOCATION);
        AuthenticationFunctions.addToCRL(blacklistCertificate, CRL_LOCATION);
        DatagramSocket serverSocket = new DatagramSocket(2021);
        ThreadCommunication threadCommunication = new ThreadCommunication(true);
        long startTime = System.currentTimeMillis();

        int counter = 0;
        long TSQStart;
        long TSQEnd;
        while (counter < testAmount) {
            try {
                if (threadCommunication.getReady()) {
                    if (counter != 0) {
                        serverSocket = new DatagramSocket(2021);
                    }
                    threadCommunication.setReady(false);
                    ReceiveAnswerThree receiveAnswerThree = new ReceiveAnswerThree(serverSocket, answerCounter,
                            validityCounter, timeCounter, counter, threadCommunication);
                    receiveAnswerThree.start();
                    TSQStart = System.currentTimeMillis();
                    sendQueryTest3(counter);
                    TSQEnd = System.currentTimeMillis();
                    timeCounter.addTimeToSendQuery(TSQEnd - TSQStart);
                    timeCounter.addTimeToRawTSQData(TSQEnd - TSQStart);
                    counter++;
                    if (counter % 25 == 0) {
                        System.out.println("query number: " + counter);
                    }
                    startTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
                serverSocket.close();
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                System.out.println("timeout");
                threadCommunication.setReady(true);
                counter--;
            }
            Thread.sleep(100);
        }

        Thread.sleep(1000);

        System.out.println(answerCounter.printAnswer());
        System.out.println(answerCounter.printMath());
        System.out.println(validityCounter.printValidity());
        System.out.println(validityCounter.printMath());
        System.out.println("ALL_AVG_TQR_TIME: "+timeCounter.getPercentage1());
        System.out.println("NOT_AVG_TQR_TIME: "+timeCounter.getBiasedPercentage1());
        System.out.println("ALL_AVG_TSQ_TIME: "+timeCounter.getPercentage2());
        System.out.println("NOT_AVG_TSQ_TIME: "+timeCounter.getBiasedPercentage2());
        System.out.println("ALL_AVG_TPR_TIME: "+timeCounter.getPercentage3());
        System.out.println("NOT_AVG_TPR_TIME: "+timeCounter.getBiasedPercentage3());

        answerCounter.logAnswers();
        validityCounter.logAnswers();
        timeCounter.logAnswers();
        try {
            answerCounter.exportJSONLog();
            validityCounter.exportJSONLog();
            timeCounter.exportJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends query message, hash, and certificate to the 2 OBUs. Same as the second & third ones.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     */
    private void sendQueryTest4(int counter)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        String userCertificate = AuthenticationFunctions.getCertificate(OWN_CERTIFICATE_LOCATION);
        PrivateKey userPrivateKey = AuthenticationFunctions.getPrivateKey(OWN_PRIVATE_KEY_LOCATION);
        String message = "Query";
        String hash = AuthenticationFunctions.hashMessage(message);
        String authentication = AuthenticationFunctions.encryptMessage(hash, userPrivateKey);
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        InetAddress groupIP = InetAddress.getByName("225.0.0.0");
        multicastSocket.joinGroup(groupIP);
        Message query = new Message();
        query.putValue("Query", message);
        query.putValue("Certificate", userCertificate);
        query.putValue("Hash", authentication);
        long currentTime = System.currentTimeMillis();
        String time = String.valueOf(currentTime);
        query.putValue("Time", time);
        query.putValue("TestNumber", String.valueOf(counter));
        byte[] data = CommunicationFunctions.messageToByteArray(query);
        int randomPort = multicastSocket.getLocalPort();
        DatagramPacket queryPacket = new DatagramPacket(data, data.length, groupIP, randomPort);
        multicastSocket.send(queryPacket);
        //System.out.println("query sent");
        multicastSocket.close();
    }

    /**
     * Handles the fourth test
     *
     * @param testAmount an integer specifying the amount of query to be sent
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws InterruptedException
     */
    public void runFourthTest(int testAmount, int rate)
            throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException,
            InvalidKeySpecException, InterruptedException {
        TimeCounter timeCounter = new TimeCounter(4, rate, testAmount);
        AnswerCounter answerCounter = new AnswerCounter(4, rate);
        ValidityCounter validityCounter = new ValidityCounter(4, rate);

        try {
            answerCounter.importJSONLog();
            validityCounter.importJSONLog();
			timeCounter.importJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new PrintWriter(CRL_LOCATION).close(); // empty the file
        String blacklistCertifiate = AuthenticationFunctions.getCertificate(OBU_X_CERTIFICATE_LOCATION);
        AuthenticationFunctions.addToCRL(blacklistCertifiate, CRL_LOCATION);
        DNSBloomFilterFunctions.generateRandomBloomFilter(1000);

        DatagramSocket serverSocket = new DatagramSocket(2021);
        ThreadCommunication threadCommunication = new ThreadCommunication(true);
        long startTime = System.currentTimeMillis();

        int counter = 0;
        long TSQStart;
        long TSQEnd;
        while (counter < testAmount) {
            try {
                if (threadCommunication.getReady()) {
                    if (counter != 0) {
                        serverSocket = new DatagramSocket(2021);
                    }
                    threadCommunication.setReady(false);
                    ReceiveAnswerFour receiveAnswerFour = new ReceiveAnswerFour(serverSocket, answerCounter,
                            validityCounter, timeCounter, counter, threadCommunication);
                    receiveAnswerFour.start();
                    TSQStart = System.currentTimeMillis();
                    sendQueryTest4(counter);
                    TSQEnd = System.currentTimeMillis();
                    timeCounter.addTimeToSendQuery(TSQEnd - TSQStart);
                    timeCounter.addTimeToRawTSQData(TSQEnd - TSQStart);
                    counter++;
                    if (counter % 25 == 0) {
                        System.out.println("query number: " + counter);
                    }
                    startTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
                serverSocket.close();
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                System.out.println("timeout");
                threadCommunication.setReady(true);
                counter--;
            }
            Thread.sleep(100);
        }

        Thread.sleep(1000);

        System.out.println(answerCounter.printAnswer());
        System.out.println(answerCounter.printMath());
        System.out.println(validityCounter.printValidity());
        System.out.println(validityCounter.printMath());
        System.out.println("ALL_AVG_TQR_TIME: "+timeCounter.getPercentage1());
        System.out.println("NOT_AVG_TQR_TIME: "+timeCounter.getBiasedPercentage1());
        System.out.println("ALL_AVG_TSQ_TIME: "+timeCounter.getPercentage2());
        System.out.println("NOT_AVG_TSQ_TIME: "+timeCounter.getBiasedPercentage2());
        System.out.println("ALL_AVG_TPR_TIME: "+timeCounter.getPercentage3());
        System.out.println("NOT_AVG_TPR_TIME: "+timeCounter.getBiasedPercentage3());

        answerCounter.logAnswers();
        validityCounter.logAnswers();
        timeCounter.logAnswers();
        try {
            answerCounter.exportJSONLog();
            validityCounter.exportJSONLog();
            timeCounter.exportJSONLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void test(int testAmount) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException,
            CertificateException, ClassNotFoundException {
        DNSBloomFilter signedIPsRSU = DNSBloomFilterFunctions.generateRandomBloomFilter(10); // BF created by C&C
        signedIPsRSU.exportBloomFilter(BLOOM_FILTER_LOCATION);
        DNSBloomFilter signedIPsOBU = AuthenticationFunctions.getBloomFilter(BLOOM_FILTER_LOCATION); // BF obtained for test
        if (!AuthenticationFunctions.checkSignedAAAARecord("KTH.Thesis.V2X", signedIPsOBU)) {
            System.out.println("it seems the bloom filter worked..2");
            if (!AuthenticationFunctions.checkSignedAAAARecord("0000:1111:2222:3333:4444:5555:6666:7777", signedIPsOBU)) {
                System.out.println("it seems the bloom filter worked..1");
                if (AuthenticationFunctions.checkSignedAAAARecord("KTH.Thesis.V2X=0000:1111:2222:3333:4444:5555:6666:7777", signedIPsOBU)) {
                    System.out.println("it seems the bloom filter worked..0");
                    return;
                }
            }
        }
        System.out.println("it seems the bloom filter did not work..");
    }

    // test a certificate file for revocation, then test adding a certificate to CRL file
    public static void crlTest() throws IOException {
        String n_certificate = AuthenticationFunctions.getCertificate("Authentication/OBU-N-certificate.crt");
        String x_certificate = AuthenticationFunctions.getCertificate("Authentication/OBU-X-certificate.crt");
        new PrintWriter(CRL_LOCATION).close(); // empty the file
        if (!AuthenticationFunctions.checkRevocatedCertificate(n_certificate, CRL_LOCATION)) {
            System.out.println("it seems the bloom filter worked..2");
            if (!AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION)) {
                System.out.println("it seems the bloom filter worked..1");

                AuthenticationFunctions.addToCRL(x_certificate, CRL_LOCATION);

                if (AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION)) {
                    System.out.println("it seems the revocation list worked..0.5");
                }

                if (!AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION)) {
                    System.out.println("0.5 not working");
                }

                AuthenticationFunctions.addToCRL(n_certificate, CRL_LOCATION);

                if (AuthenticationFunctions.checkRevocatedCertificate(n_certificate, CRL_LOCATION)) {
                    System.out.println("it seems the revocation list worked..0.25");
                }

                if (!AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION)) {
                    System.out.println("0.25 not working");
                }

                if (AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION)) {
                    System.out.println("it seems the revocation list worked..0.125");
                }

                if (!AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION)) {
                    System.out.println("0.125 not working");
                }

                if (AuthenticationFunctions.checkRevocatedCertificate(x_certificate, CRL_LOCATION) && AuthenticationFunctions.checkRevocatedCertificate(n_certificate, CRL_LOCATION)) {
                    System.out.println("it seems the revocation list worked..0");
                }

                if (AuthenticationFunctions.checkRevocatedCertificate(x_certificate + n_certificate, CRL_LOCATION)) {
                    System.out.println("Y'all have a good one");
                }
            }
        }
        System.out.println("it seems the revocation list did not work..");
    }

    // test a bloom filter with one entry against 2 missing records and the one exisiting record
    public static void bloomFilterTest() throws IOException {
        DNSBloomFilter signedIPsRSU = new DNSBloomFilter(DNSBloomFilter.NUM_AAAA_RECORDS); // BF created by C&C
        signedIPsRSU.add(DNSBloomFilter.exampleAAAA);
        signedIPsRSU.exportBloomFilter(BLOOM_FILTER_LOCATION);
        DNSBloomFilter signedIPsOBU = AuthenticationFunctions.getBloomFilter(BLOOM_FILTER_LOCATION); // BF obtained for test
        if (!AuthenticationFunctions.checkSignedAAAARecord(DNSBloomFilter.exampleHostname, signedIPsOBU)) {
            System.out.println("it seems the bloom filter worked..2");
            if (!AuthenticationFunctions.checkSignedAAAARecord(DNSBloomFilter.exampleIPv6Addr, signedIPsOBU)) {
                System.out.println("it seems the bloom filter worked..1");
                if (AuthenticationFunctions.checkSignedAAAARecord(DNSBloomFilter.exampleAAAA, signedIPsOBU)) {
                    System.out.println("it seems the bloom filter worked..0");
                    return;
                }
            }
        }
        System.out.println("it seems the bloom filter did not work..");
    }
}