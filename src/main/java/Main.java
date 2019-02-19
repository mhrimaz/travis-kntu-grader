import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.util.List;

import static spark.Spark.get;
import static spark.Spark.port;

public class Main {
    private final static String TRAVIS_TOKEN = System.getenv("TRAVIS_TOKEN");
    private final static String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");

    static {
        BasicConfigurator.configure();
        System.out.println("TRAVIS_TOKEN = " + TRAVIS_TOKEN);
        System.out.println("GITHUB_TOKEN = " + GITHUB_TOKEN);


    }

    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        get("/", (req, res) -> {
            return "$$$GRADER$$$ | { type:\"SCORE\" , amount:# , reason:\"#\" }  | $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"TOTAL\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"TODO\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"#\" , value:# , priority:# }| $$$GRADER$$$ </br>";
        });
        get("grader", (req, res) -> {
            res.type("application/json");
            String repo = req.queryParams("repo");
            String dueDate = req.queryParams("due");

            String commitSHA = APIUtil.getSubmittedCommitSHA(APIUtil.DATE_FORMAT.parse(dueDate), repo, GITHUB_TOKEN);
            long buildID = APIUtil.getBuildIDForSubmitedCommit(repo, commitSHA, TRAVIS_TOKEN);
            long jobID = APIUtil.extractBuildJobID(buildID, TRAVIS_TOKEN);
            String buildLog = APIUtil.extractJobLog(jobID, TRAVIS_TOKEN);

            List<JSONObject> graderLog = GraderReportProcessUtil.tokenizeBuildLog(buildLog);
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
                GraderReportPainterUtil.paintError(svgGenerator, "INVALID STUDENT ID");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            if (repo == null || repo.isEmpty()) {
                GraderReportPainterUtil.paintError(svgGenerator, "MISSING PARAM: REPO");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            try {
                long buildID = APIUtil.extractLastBuildID(repo, TRAVIS_TOKEN);
                long jobID = APIUtil.extractBuildJobID(buildID, TRAVIS_TOKEN);
                String comitSHA = APIUtil.extractCommitSHAForJobID(jobID, TRAVIS_TOKEN);
                String logOutput = APIUtil.extractJobLog(jobID, TRAVIS_TOKEN);
                String status = APIUtil.getBuildStatus(buildID, TRAVIS_TOKEN);
                List<JSONObject> graderLogs = GraderReportProcessUtil.tokenizeBuildLog(logOutput);
                long totalScore = GraderReportProcessUtil.getTotalScore(graderLogs);
                long sumOfScores = GraderReportProcessUtil.getSumOfScores(graderLogs);

                GraderReportPainterUtil.paintMinimal(svgGenerator, repo, studentID, sumOfScores, totalScore, comitSHA, status);

                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            } catch (UnirestException ex) {
                GraderReportPainterUtil.paintError(svgGenerator, "Internal Error");
                svgGenerator.stream(res.raw().getWriter(), true);
                ex.printStackTrace();
                return res;
            } catch (Exception ex) {
                GraderReportPainterUtil.paintError(svgGenerator, "Exception");
                svgGenerator.stream(res.raw().getWriter(), true);
                ex.printStackTrace();
                return res;
            }

        });


    }

    static String getGithubToken() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("GITHUB_TOKEN") != null) {
            return processBuilder.environment().get("GITHUB_TOKEN");
        }
        return "";
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

}
