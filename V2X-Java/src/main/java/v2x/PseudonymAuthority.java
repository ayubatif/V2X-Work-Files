package v2x;

import java.io.File;
import java.io.IOException;

public class PseudonymAuthority {
    static int CERTIFICATE_AMOUNT = 1000;

    //TODO Can use pre gen pseudonyms if a la carte no work
    public static synchronized void genPseudonymsX() throws IOException, InterruptedException {
        File f = new File("Authentication");
        if (!f.mkdir()) {
            System.err.println("Couldn't create dir...");
        }
        String[][] cmdsX1 = new String[CERTIFICATE_AMOUNT][5];
        String[][] cmdsX2 = new String[CERTIFICATE_AMOUNT][9];
        String[][] cmdsX3 = new String[CERTIFICATE_AMOUNT][15];
        String[][] cmdsX4 = new String[CERTIFICATE_AMOUNT][12];
        for(int c = 0; c < CERTIFICATE_AMOUNT; c++) {
            cmdsX1[c][0] = ("openssl");
            cmdsX1[c][1] = ("genrsa");
            cmdsX1[c][2] = ("-out");
            cmdsX1[c][3] = ("OBU-X-private-key"+c+".pem");
            cmdsX1[c][4] = ("2048");

            cmdsX2[c][0] = ("openssl");
            cmdsX2[c][1] = ("req");
            cmdsX2[c][2] = ("-new");
            cmdsX2[c][3] = ("-key");
            cmdsX2[c][4] = ("OBU-X-private-key"+c+".pem");
            cmdsX2[c][5] = ("-out");
            cmdsX2[c][6] = ("OBU-X"+c+".csr");
            cmdsX2[c][7] = ("-subj");
            cmdsX2[c][8] = ("\"/C=SE/ST=Stockholm/L=Stockholm/O=KTH Thesis/OU=V2X Thesis/CN=test/emailAddress=arieltan@kth.se\"");

            cmdsX3[c][0] = ("openssl");
            cmdsX3[c][1] = ("x509");
            cmdsX3[c][2] = ("-req");
            cmdsX3[c][3] = ("-days");
            cmdsX3[c][4] = ("365");
            cmdsX3[c][5] = ("-in");
            cmdsX3[c][6] = ("OBU-X"+c+".csr");
            cmdsX3[c][7] = ("-CA");
            cmdsX3[c][8] = ("CA-certificate.crt");
            cmdsX3[c][9] = ("-CAkey");
            cmdsX3[c][10] = ("CA-private-key.pem");
            cmdsX3[c][11] = ("-CAcreateserial");
            cmdsX3[c][12] = ("-out");
            cmdsX3[c][13] = ("OBU-X-certificate"+c+".crt");
            cmdsX3[c][14] = ("-sha256");

            cmdsX4[c][0] = ("openssl");
            cmdsX4[c][1] = ("pkcs8");
            cmdsX4[c][2] = ("-nocrypt");
            cmdsX4[c][3] = ("-topk8");
            cmdsX4[c][4] = ("-inform");
            cmdsX4[c][5] = ("PEM");
            cmdsX4[c][6] = ("-in");
            cmdsX4[c][7] = ("OBU-X-private-key"+c+".pem");
            cmdsX4[c][8] = ("-outform");
            cmdsX4[c][9] = ("DER");
            cmdsX4[c][10] = ("-out");
            cmdsX4[c][11] = ("OBU-X-private-key"+c+".der");
        }

        ProcessBuilder builder;
        Process currentProcess;
        for(int c = 0; c < CERTIFICATE_AMOUNT; c++) {
            builder= new ProcessBuilder(cmdsX1[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsX2[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsX3[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsX4[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();
        }

    }

    public static synchronized void genPseudonymsN() throws IOException, InterruptedException {
        File f = new File("Authentication");
        if (!f.mkdir()) {
            System.err.println("Couldn't create dir...");
        }
        String[][] cmdsN1 = new String[CERTIFICATE_AMOUNT][5];
        String[][] cmdsN2 = new String[CERTIFICATE_AMOUNT][9];
        String[][] cmdsN3 = new String[CERTIFICATE_AMOUNT][15];
        String[][] cmdsN4 = new String[CERTIFICATE_AMOUNT][12];
        for(int c = 0; c < CERTIFICATE_AMOUNT; c++) {
            cmdsN1[c][0] = ("openssl");
            cmdsN1[c][1] = ("genrsa");
            cmdsN1[c][2] = ("-out");
            cmdsN1[c][3] = ("OBU-N-private-key"+c+".pem");
            cmdsN1[c][4] = ("2048");

            cmdsN2[c][0] = ("openssl");
            cmdsN2[c][1] = ("req");
            cmdsN2[c][2] = ("-new");
            cmdsN2[c][3] = ("-key");
            cmdsN2[c][4] = ("OBU-N-private-key"+c+".pem");
            cmdsN2[c][5] = ("-out");
            cmdsN2[c][6] = ("OBU-N"+c+".csr");
            cmdsN2[c][7] = ("-subj");
            cmdsN2[c][8] = ("\"/C=SE/ST=Stockholm/L=Stockholm/O=KTH Thesis/OU=V2X Thesis/CN=test/emailAddress=arieltan@kth.se\"");

            cmdsN3[c][0] = ("openssl");
            cmdsN3[c][1] = ("x509");
            cmdsN3[c][2] = ("-req");
            cmdsN3[c][3] = ("-days");
            cmdsN3[c][4] = ("365");
            cmdsN3[c][5] = ("-in");
            cmdsN3[c][6] = ("OBU-N"+c+".csr");
            cmdsN3[c][7] = ("-CA");
            cmdsN3[c][8] = ("CA-certificate.crt");
            cmdsN3[c][9] = ("-CAkey");
            cmdsN3[c][10] = ("CA-private-key.pem");
            cmdsN3[c][11] = ("-CAcreateserial");
            cmdsN3[c][12] = ("-out");
            cmdsN3[c][13] = ("OBU-N-certificate"+c+".crt");
            cmdsN3[c][14] = ("-sha256");

            cmdsN4[c][0] = ("openssl");
            cmdsN4[c][1] = ("pkcs8");
            cmdsN4[c][2] = ("-nocrypt");
            cmdsN4[c][3] = ("-topk8");
            cmdsN4[c][4] = ("-inform");
            cmdsN4[c][5] = ("PEM");
            cmdsN4[c][6] = ("-in");
            cmdsN4[c][7] = ("OBU-N-private-key"+c+".pem");
            cmdsN4[c][8] = ("-outform");
            cmdsN4[c][9] = ("DER");
            cmdsN4[c][10] = ("-out");
            cmdsN4[c][11] = ("OBU-N-private-key"+c+".der");
        }

        ProcessBuilder builder;
        Process currentProcess;
        for(int c = 0; c < CERTIFICATE_AMOUNT; c++) {
            builder = new ProcessBuilder(cmdsN1[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsN2[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsN3[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsN4[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();
        }

    }

    public static synchronized void genPseudonymsA() throws InterruptedException, IOException {
        File f = new File("Authentication");
        if (!f.mkdir()) {
            System.err.println("Couldn't create dir...");
        }
        String[][] cmdsA1 = new String[1][5];
        String[][] cmdsA2 = new String[1][9];
        String[][] cmdsA3 = new String[1][15];
        String[][] cmdsA4 = new String[1][12];
        for(int c = 0; c < 1; c++) {
            cmdsA1[c][0] = ("openssl");
            cmdsA1[c][1] = ("genrsa");
            cmdsA1[c][2] = ("-out");
            cmdsA1[c][3] = ("OBU-A-private-key"+c+".pem");
            cmdsA1[c][4] = ("2048");

            cmdsA2[c][0] = ("openssl");
            cmdsA2[c][1] = ("req");
            cmdsA2[c][2] = ("-new");
            cmdsA2[c][3] = ("-key");
            cmdsA2[c][4] = ("OBU-A-private-key"+c+".pem");
            cmdsA2[c][5] = ("-out");
            cmdsA2[c][6] = ("OBU-A"+c+".csr");
            cmdsA2[c][7] = ("-subj");
            cmdsA2[c][8] = ("\"/C=SE/ST=Stockholm/L=Stockholm/O=KTH Thesis/OU=V2X Thesis/CN=test/emailAddress=arieltan@kth.se\"");

            cmdsA3[c][0] = ("openssl");
            cmdsA3[c][1] = ("x509");
            cmdsA3[c][2] = ("-req");
            cmdsA3[c][3] = ("-days");
            cmdsA3[c][4] = ("365");
            cmdsA3[c][5] = ("-in");
            cmdsA3[c][6] = ("OBU-A"+c+".csr");
            cmdsA3[c][7] = ("-CA");
            cmdsA3[c][8] = ("CA-certificate.crt");
            cmdsA3[c][9] = ("-CAkey");
            cmdsA3[c][10] = ("CA-private-key.pem");
            cmdsA3[c][11] = ("-CAcreateserial");
            cmdsA3[c][12] = ("-out");
            cmdsA3[c][13] = ("OBU-A-certificate"+c+".crt");
            cmdsA3[c][14] = ("-sha256");

            cmdsA4[c][0] = ("openssl");
            cmdsA4[c][1] = ("pkcs8");
            cmdsA4[c][2] = ("-nocrypt");
            cmdsA4[c][3] = ("-topk8");
            cmdsA4[c][4] = ("-inform");
            cmdsA4[c][5] = ("PEM");
            cmdsA4[c][6] = ("-in");
            cmdsA4[c][7] = ("OBU-A-private-key"+c+".pem");
            cmdsA4[c][8] = ("-outform");
            cmdsA4[c][9] = ("DER");
            cmdsA4[c][10] = ("-out");
            cmdsA4[c][11] = ("OBU-A-private-key"+c+".der");
        }

        ProcessBuilder builder;
        Process currentProcess;
        for(int c = 0; c < 1; c++) {
            builder = new ProcessBuilder(cmdsA1[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsA2[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsA3[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();

            builder = new ProcessBuilder(cmdsA4[c]);
            builder.directory(new File("Authentication").getAbsoluteFile() ); // this is where you set the root folder for the executable to run with
            builder.redirectErrorStream(true);
            currentProcess =  builder.start();
            currentProcess.waitFor();
        }
    }

    public static void main(String[] args) {
        try {
            switch (args[0]) {
                case "x":
                case "X":
                    genPseudonymsX();
                    break;
                case "n":
                case "N":
                    genPseudonymsN();
                    break;
                case "a":
                case "A":
                    genPseudonymsA();
                    break;
                default:
                    System.err.println("PLEASE TYPE ARG a, A, x, X, n, OR N");
                    break;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error running cmd");
            System.err.println(e);
        }
    }
}
