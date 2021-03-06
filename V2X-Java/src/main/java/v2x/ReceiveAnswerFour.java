package v2x;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.Callable;

// https://stackoverflow.com/questions/2275443/how-to-timeout-a-thread

/**
 * Waits for an answer that is authenticated and returns it for the third test. If the message is untrustworhy, the
 * certificate is put into the revocation list.
 */
public class ReceiveAnswerFour extends Thread {
    static final int UNICAST_PORT = 2021;
    static final String CA_CERTIFICATE_LOCATION = "Authentication/CA-certificate.crt";
    static final String CRL_LOCATION = "Authentication/CRL-A.crl";
    static final String BLOOM_FILTER_LOCATION = "Authentication/DNS-bloom-filter.bf";
    static final String DNS_CERTIFICATE_LOCATION = "Authentication/DNS-certificate.crt";

    private final DatagramSocket serverSocket;
    private AnswerCounter answerCounter;
    private ValidityCounter validityCounter;
    private TimeCounter timeCounter;
    private int counter;
    private ThreadCommunication threadCommunication;

    public ReceiveAnswerFour(DatagramSocket serverSocket,
                             AnswerCounter answerCounter,
                             ValidityCounter validityCounter,
                             TimeCounter timeCounter, int counter,
                             ThreadCommunication threadCommunication) {
        this.serverSocket = serverSocket;
        this.answerCounter = answerCounter;
        this.validityCounter = validityCounter;
        this.timeCounter = timeCounter;
        this.counter = counter;
        this.threadCommunication = threadCommunication;
    }

    @Override
    public void run() {

        byte[] buffer = new byte[65508];

        int counter = 0;
        boolean run = true;
        long TPRStart;
        long TPREnd;

        while (run) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            try {
                serverSocket.receive(receivePacket);
                TPRStart = System.currentTimeMillis();
                Message outerMessage = CommunicationFunctions.byteArrayToMessage(buffer);
                String outerAnswer = outerMessage.getValue("Answer");

                String outerCertificate = outerMessage.getValue("Certificate");
                String outerEncryptedHash = outerMessage.getValue("Hash");

                boolean outerAuthentication = AuthenticationFunctions.authenticateMessage(
                        outerAnswer, outerEncryptedHash, outerCertificate, CA_CERTIFICATE_LOCATION);
                boolean outerRevoked = AuthenticationFunctions.checkRevocatedCertificate(
                        outerCertificate, CRL_LOCATION);

                if (outerAuthentication && !outerRevoked) {
                    byte[] decodedInnerAnswer = Base64.getDecoder().decode(outerAnswer);
                    Message innerMessage = CommunicationFunctions.byteArrayToMessage(decodedInnerAnswer);

                    String innerAnswer = innerMessage.getValue("Answer");

                    try {
                        DNSBloomFilter signedIPs = AuthenticationFunctions.getBloomFilter(BLOOM_FILTER_LOCATION);
                        boolean innerAuthentication = AuthenticationFunctions
                                .checkSignedAAAARecord(innerAnswer, signedIPs);

                        if (innerAuthentication) {
                            long endTime = System.currentTimeMillis();
                            String time = outerMessage.getValue("Time");
                            long startTime = Long.parseLong(time);
                            long totalTime = endTime - startTime;

//                    System.out.println("start time" + startTime);
//                    System.out.println("end time" + endTime);
                            //System.out.println("total time " + totalTime);
                            timeCounter.addTimeToQueryResolve(totalTime);
                            timeCounter.addTimeToRawTQRData(totalTime);

                            boolean isResponseMalicious = !DNSBloomFilterFunctions.getFixedAAAA().equals(innerAnswer);
                            String answer = isResponseMalicious ? "1" : "0";

                            answerCounter.addAnswer(answer);
                            validityCounter.addValidity("2");

                            TPREnd = System.currentTimeMillis();
                            timeCounter.addTimeToProcessResponse(TPREnd - TPRStart);
                            timeCounter.addTimeToRawTPRData(TPREnd - TPRStart);

//                            System.out.println("counter " + counter);

//                            if (counter >= testAmount - 1) {
//                                run = false;
//                            }
//
                            counter++;
//                            buffer = new byte[65508];
                            run = false;
                            serverSocket.close();
                            threadCommunication.setReady(true);
                        } else {
                            AuthenticationFunctions.addToCRL(outerCertificate, CRL_LOCATION);
                            validityCounter.addValidity("1");

                            TPREnd = System.currentTimeMillis();
                            timeCounter.addTimeToProcessResponse(TPREnd - TPRStart);
                            timeCounter.addTimeToRawTPRData(TPREnd - TPRStart);
                        }
                    } catch (Exception e) {
                        AuthenticationFunctions.addToCRL(outerCertificate, CRL_LOCATION);
                        validityCounter.addValidity("1");

                        TPREnd = System.currentTimeMillis();
                        timeCounter.addTimeToProcessResponse(TPREnd - TPRStart);
                        timeCounter.addTimeToRawTPRData(TPREnd - TPRStart);
                    }
                } else {
                    validityCounter.addValidity("0");

                    TPREnd = System.currentTimeMillis();
                    timeCounter.addTimeToProcessResponse(TPREnd - TPRStart);
                    timeCounter.addTimeToRawTPRData(TPREnd - TPRStart);
                }
            } catch (SocketException e) {
                //System.out.println("Thread ended");
                run = false;
            } catch (Exception e) {
                //System.out.println("error two");
                e.printStackTrace();
            }
        }
    }
}
