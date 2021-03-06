package v2x;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class AnswerCounter {
    private int answerNull = 0;
    private int answerZero = 0;
    private int answerOne = 0;
    private static final String LOG_FILE_NAME= "v2x-data-log";
    private static final String PRINT_LOG_FILE_NAME= "v2x-data-print-log";
    private static final String LOG_FILE_EXTENSION= ".txt";
    private int testNumber;
    private int pseudoRate;
    private JSONArray log = new JSONArray();

    /**
     *
     * @param testnum which test is being run
     */
    public AnswerCounter(int testnum) {
        this.testNumber = testnum;
        this.pseudoRate = 0;
    }

    /**
     *
     * @param testnum which test
     * @param rate pseudo change rate
     */
    public AnswerCounter(int testnum, int rate) {
        this.testNumber = testnum;
        this.pseudoRate = rate;
    }

    /**
     * Takes in answer and counts how much is correct, incorrect, and missing and answer.
     *
     * @param answer a string of the answer received
     */
    public void addAnswer(String answer) {
        int answerInt = Integer.parseInt(answer);

        switch (answerInt) {
            case -1:
                this.answerNull++;
                break;
            case 0:
                this.answerZero++;
                break;
            case 1:
                this.answerOne++;
                break;
        }
    }

    /**
     * Calculates the percentages of the answers received
     *
     * @return <code>int[]</code> an array of the percentages
     */
    public double[] getPercentage() {
        double[] answer = new double[3];
        double totalAnswers = this.answerZero + this.answerOne;
        answer[0] = (this.answerZero / totalAnswers) * 100;
        answer[1] = (this.answerOne / totalAnswers) * 100;

        return answer;
    }

    public void exportLogOutput() throws IOException {
        StringBuilder stringBuilder = new StringBuilder("\n ^(o.o)^ ^(o.o)^ ^(o.o)^ ^(o.o)^ TEST START ^(o.o)^ ^(o.o)^ ^(o.o)^ ^(o.o)^ \n");
        stringBuilder.append(printAnswer());
        stringBuilder.append(printMath());
        String str = stringBuilder.toString();
        BufferedWriter writer = new BufferedWriter(new FileWriter(PRINT_LOG_FILE_NAME+this.testNumber+LOG_FILE_EXTENSION, true));
        writer.append(str);

        writer.close();
    }

    /**
     * Prints the math related answers
     */
    public String printMath() {
        StringBuilder stringBuilder = new StringBuilder();
        double[] answer = getPercentage();

        for (int i = 0; i < answer.length; i++) {
            stringBuilder.append("Percentage of answer type ").append(i + 1).append(":\n");
            stringBuilder.append(answer[i]).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Prints out the answers that it has been given.
     */
    public String printAnswer() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Total answers received:\n");
        int totalAnswers = this.answerZero + this.answerOne;
        stringBuilder.append(totalAnswers).append("\n");
        stringBuilder.append("Answer type 0 amount:\n");
        stringBuilder.append(this.answerZero).append("\n");
        stringBuilder.append("Answer type 1 amount:\n");
        stringBuilder.append(this.answerOne).append("\n");
        stringBuilder.append("No answers received amount:\n");
        stringBuilder.append(this.answerNull).append("\n");
        return stringBuilder.toString();
    }

    /**
     * Replaces and fills the JSON log with the current state of answers
     */
    public void logAnswers() {
        double[] answer = getPercentage();
        int totalAnswers = this.answerZero + this.answerOne;
        JSONObject jo = new JSONObject();
        if (this.pseudoRate > 0) {
            jo.put("PSEUDO_RATE", this.pseudoRate);
        }
        jo.put("TOTAL", totalAnswers);
        for (int i = 0; i < answer.length; i++) {
            jo.put("DATA"+i, answer[i]);
        }
        log.put(jo);
    }

    /**
     *
     * @return JSONArray containing answer percentages
     */
    private JSONArray getLog() {
        return this.log;
    }

    /**
     * imports the JSON log, but applicability is scarce in our scenario. Able to store different runs of test I suppose
     * @throws IOException
     */
    public void importJSONLog() throws IOException {
        File jsonFile = new File(LOG_FILE_NAME+this.testNumber+LOG_FILE_EXTENSION);
        InputStream in = new FileInputStream(jsonFile);

        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }

        try {
            log = new JSONArray(textBuilder.toString());
        } catch (JSONException e) {
            System.err.println("Empty file isn't quite an empty JSON file");
        }

        in.close();
    }

    /**
     * Writes the log in JSON to a test specific file
     * @throws IOException
     */
    public void exportJSONLog() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME+this.testNumber+LOG_FILE_EXTENSION));
        writer.write(log.toString());

        writer.close();
    }

    public static void main(String[] args) {
        AnswerCounter answerCounter1 = new AnswerCounter(3, 10);
        AnswerCounter answerCounter2 = new AnswerCounter(3, 10);
        int notSoRandomNumber = DNSBloomFilterFunctions.generateRandomHostname().length() * DNSBloomFilterFunctions.generateRandomHostname().length();
        for(int i = 0; i < notSoRandomNumber; i++) {
            answerCounter1.addAnswer(Integer.valueOf(i % 2).toString());
        }
        answerCounter1.printAnswer();
        answerCounter1.printMath();
        answerCounter1.logAnswers();
        try {
            System.out.println("Expected: "+answerCounter1.getLog().toString());
            answerCounter1.exportJSONLog();
            answerCounter2.importJSONLog();
            System.out.println("Actual: "+answerCounter2.getLog().toString());
        } catch (IOException e) {
            System.out.println("Check if "+LOG_FILE_NAME+answerCounter1.testNumber+LOG_FILE_EXTENSION+" exists");
            System.err.println(e);
        }
    }
}
