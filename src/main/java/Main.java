import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static spark.Spark.get;
import static spark.Spark.port;

public class Main {
    static {

        BasicConfigurator.configure();

    }
    final static Pattern GRADER_PATTERN = Pattern.compile("\\$\\$\\$GRADER\\$\\$\\$ \\| (.*) \\| \\$\\$\\$GRADER\\$\\$\\$");
    public static void paintError(Graphics2D g2d, String message) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString(message, 40, 70);
    }

    public static void paintMinimal(Graphics2D g2d, String name, String studentId, long score, long maxScore) {
        if (score == maxScore) {
            g2d.setPaint(Color.GREEN);
        } else {
            g2d.setPaint(Color.ORANGE);
        }
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 24));
        g2d.drawString("Student ID: " + studentId, 45, 40);
        g2d.drawString(String.format("Score: %3d out of %3d", score, maxScore), 47, 80);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString("K. N. Toosi University of Technology", 25, 120);
        g2d.setStroke(new BasicStroke(4));
    }

    public static void paint(Graphics2D g2d, String name, String studentId, Map<String, List<String>> messages, long score, long maxScore) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 800, 900));
        if (score == maxScore) {
            g2d.setPaint(Color.GREEN);
        } else {
            g2d.setPaint(Color.ORANGE);
        }
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 24));
        g2d.drawString("Student ID: " + studentId, 45, 40);
        g2d.drawString(String.format("Score: %3d out of %3d", score, maxScore), 47, 80);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString("K. N. Toosi University of Technology", 25, 120);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(2,152,798,152);


        List<String> failReasons = messages.get("FAIL_REASON".toLowerCase());
        Iterator<String> iterator = failReasons.iterator();
        for (int i=0;i<14 && iterator.hasNext();i++){
            g2d.drawString(" + FAIL_REASON: " +iterator.next(), 10, 180 + (i*25));
        }

        List<String> todo = messages.get("TODO".toLowerCase());
        iterator = todo.iterator();
        g2d.drawLine(2,520,798,520);
        for (int i=0;i<14 && iterator.hasNext();i++){
            g2d.drawString(" +  TODO      : " +iterator.next(), 10, 545 + (i*25));
        }

    }

    public static List<JSONObject> tokenizeBuildLog(String log){
        List<JSONObject> buildLogs = new LinkedList<>();
        Matcher matcher = GRADER_PATTERN.matcher(log);
        while(matcher.find()) {
            JSONObject json = new JSONObject(matcher.group(1));
            buildLogs.add(json);
        }

        return buildLogs;
    }

    public static long getTotalScore(List<JSONObject> graderLogs){
        for(JSONObject graderLog:graderLogs){
            String msgType = graderLog.getString("type");
            if (msgType.equalsIgnoreCase("msg")){
                String key = graderLog.getString("key");
                if(key.equalsIgnoreCase("total")){
                    return graderLog.getLong("value");
                }
            }
        }
        return 0;
    }
    public static long getSumOfScorees(List<JSONObject> graderLogs){
        long sum = 0;
        for(JSONObject graderLog:graderLogs){
            String msgType = graderLog.getString("type");
            if (msgType.equalsIgnoreCase("score")){
                sum += graderLog.getLong("amount");
            }
        }
        return sum;
    }
    public static Map<String, List<String>> getGraderMessages(List<JSONObject> graderLogs){
        Map<String, List<String>> messages = new HashMap<>();
        for(JSONObject graderLog:graderLogs){
            String msgType = graderLog.getString("type");
            if (msgType.equalsIgnoreCase("msg")) {
                String key = graderLog.getString("key");
                if (key.equalsIgnoreCase("total")) {
                    String value = graderLog.getString("value");
                    int priority = graderLog.getInt("priority");
                    List<String> messageList = messages.getOrDefault(key.toLowerCase(), new LinkedList<>());
                    messageList.add(value);
                }
            }
        }
        return messages;
    }


    public static String extractBuildLog(String repo) throws UnirestException {
        String repoInfoAPI = "https://api.travis-ci.com/repos/k-n-toosi-university-of-technology/" + repo;
        HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.get(repoInfoAPI)
                .header("Content-Type", "application/json")
                .header("Authorization", "token ktGWI5d7wctH3NyObZq-aw")
                .header("cache-control", "no-cache")
                .asJson();
        String lastBuildId = String.valueOf(jsonNodeHttpResponse.getBody().getObject().getLong("last_build_id"));
        HttpResponse<JsonNode> buildInfo = Unirest.get("https://api.travis-ci.com/v3/build/" + lastBuildId)
                .header("Content-Type", "application/json")
                .header("Authorization", "token ktGWI5d7wctH3NyObZq-aw")
                .header("cache-control", "no-cache")
                .asJson();
        String jobOutputId = String.valueOf(buildInfo.getBody().getObject().getJSONArray("jobs").getJSONObject(0).getLong("id"));

        HttpResponse<JsonNode> jobOutput = Unirest.get("https://api.travis-ci.com/v3/job/" + jobOutputId + "/log")
                .header("Content-Type", "application/json")
                .header("Authorization", "token ktGWI5d7wctH3NyObZq-aw")
                .header("cache-control", "no-cache")
                .asJson();
        String content = jobOutput.getBody().getObject().getString("content");
        return content;
    }


    public static void main(String[] args) {
        port(getHerokuAssignedPort());

        get("/", (req, res) -> {
            return "$$$GRADER$$$ | { type:\"SCORE\" , amount:# , reason:\"#\" }  | $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"TOTAL\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"TODO\" , value:# , priority:# }| $$$GRADER$$$ </br>"+
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"#\" , value:# , priority:# }| $$$GRADER$$$ </br>";
        });
        get("grader", (req, res) -> {
            res.type("application/json");
            String repo = req.queryParams("repo");
            String studentID = req.queryParams("id");

            String buildLog = extractBuildLog(repo);
            List<JSONObject> graderLog = tokenizeBuildLog(buildLog);
            return new JSONArray(graderLog).toString();
        });
        get("minimal", (req, res) -> {
            res.header("Cache-Control", "no-cache");
            res.type("image/svg+xml");
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
            String svgNS = "http://www.w3.org/2000/svg";
            Document document = domImpl.createDocument(svgNS, "svg", null);
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);


            String repo = req.queryParams("repo");
            String studentID = req.queryParams("id");

            if (studentID == null || studentID.isEmpty() || studentID.matches("\\d{7}") == false) {
                paintError(svgGenerator, "INVALID STUDENT ID");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            if (repo == null || repo.isEmpty()) {
                paintError(svgGenerator, "MISSING PARAM: REPO");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            String logOutput = "";
            try {
                logOutput = extractBuildLog(repo);
            } catch (UnirestException ex) {
                paintError(svgGenerator, "Internal Error");
            } catch (Exception ex){
                paintError(svgGenerator, "Exception");
            }

            String buildLog = extractBuildLog(repo);
            List<JSONObject> graderLogs = tokenizeBuildLog(buildLog);
            long totalScore = getTotalScore(graderLogs);
            long sumOfScores = getSumOfScorees(graderLogs);

            paintMinimal(svgGenerator, repo, studentID, sumOfScores, totalScore);


            // Finally, stream out SVG to the standard output using
            // UTF-8 encoding.
            boolean useCSS = true; // we want to use CSS style attributes
            svgGenerator.stream(res.raw().getWriter(), useCSS);


            return res;
        });

        get("report", (req, res) -> {
            res.header("Cache-Control", "no-cache");
            res.type("image/svg+xml");
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
            String svgNS = "http://www.w3.org/2000/svg";
            Document document = domImpl.createDocument(svgNS, "svg", null);
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);


            String repo = req.queryParams("repo");
            String studentID = req.queryParams("id");

            if (studentID == null || studentID.isEmpty() || studentID.matches("\\d{7}") == false) {
                paintError(svgGenerator, "INVALID STUDENT ID");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            if (repo == null || repo.isEmpty()) {
                paintError(svgGenerator, "MISSING PARAM: REPO");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            String logOutput = "";
            try {
                logOutput = extractBuildLog(repo);
            } catch (UnirestException ex) {
                paintError(svgGenerator, "Internal Error");
            } catch (Exception ex){
                paintError(svgGenerator, "Exception");
            }

            String buildLog = extractBuildLog(repo);
            List<JSONObject> graderLogs = tokenizeBuildLog(buildLog);
            long totalScore = getTotalScore(graderLogs);
            long sumOfScorees = getSumOfScorees(graderLogs);
            Map<String, List<String>> graderMessages = getGraderMessages(graderLogs);

            paint(svgGenerator, repo, studentID, graderMessages, sumOfScorees, totalScore);


            // Finally, stream out SVG to the standard output using
            // UTF-8 encoding.
            boolean useCSS = true; // we want to use CSS style attributes
            svgGenerator.stream(res.raw().getWriter(), useCSS);


            return res;
        });
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

}
