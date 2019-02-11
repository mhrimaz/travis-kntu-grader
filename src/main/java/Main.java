
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.BasicConfigurator;

import java.awt.*;
import java.util.Map;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

import static spark.Spark.*;

public class Main {
    static {

        BasicConfigurator.configure();

    }

    public static void paintError(Graphics2D g2d, String message) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString(message, 40, 70);
    }

    public static void paint(Graphics2D g2d, String name, String studentId, Map<String, String> messages, String score, String maxScore) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 800, 800));
        if (score.equals(maxScore)) {
            g2d.setPaint(Color.GREEN);
        } else {
            g2d.setPaint(Color.ORANGE);
        }
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 24));
        g2d.drawString("Student ID: " + studentId, 45, 40);
        g2d.drawString(String.format("Score: %3s out of %3s", score, maxScore), 47, 80);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString("K. N. Toosi University of Technology", 25, 120);
    }

    public static String extractBuildLog(String repo) throws UnirestException {
        String repoInfoAPI = "https://api.travis-ci.org/repos/kntu-java-spring-2019/" + repo;
        HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.get(repoInfoAPI).asJson();
        String lastBuildId = String.valueOf(jsonNodeHttpResponse.getBody().getObject().getLong("last_build_id"));
        HttpResponse<JsonNode> buildInfo = Unirest.get("https://api.travis-ci.org/v3/build/" + lastBuildId).asJson();
        String jobOutputId = String.valueOf(buildInfo.getBody().getObject().getJSONArray("jobs").getJSONObject(0).getLong("id"));

        HttpResponse<JsonNode> jobOutput = Unirest.get("https://api.travis-ci.org/v3/job/" + jobOutputId + "/log").asJson();
        String content = jobOutput.getBody().getObject().getString("content");
        System.out.println("content = " + content);
        return content;
    }

    public static void main(String[] args) {
        port(getHerokuAssignedPort());

        get("/", (req, res) -> {
            return "$$$GRADER$$$ | ADDSCORE | AMOUNT | REASON \n | $$$GRADER$$$" +
                    "$$$GRADER$$$ | SUBSCORE | AMOUNT | REASON | $$$GRADER$$$ \n" +
                    "$$$GRADER$$$ | KEY | VALUE | PRIORITY | $$$GRADER$$$";
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

                paint(svgGenerator, repo, studentID, null, "20", "30");


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
