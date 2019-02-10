
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
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

    public static void paint(Graphics2D g2d, String name, String studentId, Map<String, String> messages, String score, String maxScore) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 400, 800));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 32));
        g2d.drawString("ID: "+ studentId,20,60);
        g2d.drawString("Score: "+ score + " out of "+ maxScore,20,90);
    }

    public static void main(String[] args) {
        System.err.println("$$$GRADER$$$ | ADDSCORE | AMOUNT | REASON");
        System.err.println("$$$GRADER$$$ | KEY | VALUE");
        port(getHerokuAssignedPort());
        get("/help", (req, res) -> {
            return "$$$GRADER$$$ | ADDSCORE | AMOUNT | REASON \n" +
                    "$$$GRADER$$$ | SUBSCORE | AMOUNT | REASON \n" +
                    "$$$GRADER$$$ | KEY | VALUE | PRIORITY";
        });
        get("/", (req, res) -> {
//            res.header("Content-Encoding", "gzip");
//            res.header("Content-Type", "image/svg+xml");
//            res.header("Cache-Control","max-age=300");
//            res.status(200);
            res.type("image/svg+xml");
            // Get a DOMImplementation.
            DOMImplementation domImpl =
                    GenericDOMImplementation.getDOMImplementation();

            // Create an instance of org.w3c.dom.Document.
            String svgNS = "http://www.w3.org/2000/svg";
            Document document = domImpl.createDocument(svgNS, "svg", null);

            // Create an instance of the SVG Generator.
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

            // Ask the test to render into the SVG Graphics2D implementation.

            //paint(svgGenerator);

            // Finally, stream out SVG to the standard output using
            // UTF-8 encoding.
            boolean useCSS = true; // we want to use CSS style attributes
            svgGenerator.stream(res.raw().getWriter(), useCSS);
            return res;
        });

        get("report", (req, res) -> {
            System.out.println("req.params() = " + req.queryParams());
            String repo = req.queryParams("repo");
            String studentID = req.queryParams("id");
            String repoInfoAPI = "https://api.travis-ci.org/repos/kntu-java-spring-2019/"+repo;
            HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.get(repoInfoAPI).asJson();
            System.out.println("jsonNodeHttpResponse.getBody() = " + jsonNodeHttpResponse.getBody());
            String lastBuildId = String.valueOf(jsonNodeHttpResponse.getBody().getObject().getLong("last_build_id"));
            System.out.println("lastBuildId = " + lastBuildId);
            HttpResponse<JsonNode> buildInfo = Unirest.get("https://api.travis-ci.org/v3/build/" + lastBuildId).asJson();
            String jobOutputId = String.valueOf(buildInfo.getBody().getObject().getJSONArray("jobs").getJSONObject(0).getLong("id"));

            HttpResponse<JsonNode> jobOutput = Unirest.get("https://api.travis-ci.org/v3/job/" + jobOutputId + "/log").asJson();
            String content = jobOutput.getBody().getObject().getString("content");
            //https://api.travis-ci.org/v3/build/490438743 https://api.travis-ci.org/v3/build/
            //https://api.travis-ci.org/v3/job/490438744/log process this




            res.type("image/svg+xml");
            // Get a DOMImplementation.
            DOMImplementation domImpl =
                    GenericDOMImplementation.getDOMImplementation();

            // Create an instance of org.w3c.dom.Document.
            String svgNS = "http://www.w3.org/2000/svg";
            Document document = domImpl.createDocument(svgNS, "svg", null);

            // Create an instance of the SVG Generator.
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

            // Ask the test to render into the SVG Graphics2D implementation.

            paint(svgGenerator,repo,studentID,null,"20","30");

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
