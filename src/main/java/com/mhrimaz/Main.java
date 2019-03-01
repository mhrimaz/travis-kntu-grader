package com.mhrimaz;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.*;


public class Main {
    private static String TRAVIS_TOKEN = System.getenv("TRAVIS_TOKEN");
    private static String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");

    static {
        BasicConfigurator.configure();
    }

    public static void main(String[] args) {
        if (args != null && args.length == 2) {
            System.out.println(Arrays.toString(args));
            TRAVIS_TOKEN = args[0];
            GITHUB_TOKEN = args[1];
        }
        System.out.println("TRAVIS_TOKEN = " + TRAVIS_TOKEN);
        System.out.println("GITHUB_TOKEN = " + GITHUB_TOKEN);
        port(getHerokuAssignedPort());
        get("", (req, res) -> {
            return "$$$GRADER$$$ | { type:\"SCORE\" , amount:# , reason:\"#\" }  | $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"TOTAL\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"TODO\" , value:# , priority:# }| $$$GRADER$$$ </br>" +
                    "$$$GRADER$$$ | { type:\"MSG\" , key:\"#\" , value:# , priority:# }| $$$GRADER$$$ </br>";
        });
        post("repohook", (req, res) -> {
            // Update something
            System.out.println(req);
            return res;
        });
        get("grader", (req, res) -> {
            res.type("application/json");
            String repo = req.queryParams("repo");
            String dueDate = req.queryParams("due");
            try {
                String commitSHA = APIUtil.getSubmittedCommitSHA(APIUtil.DATE_FORMAT.parse(dueDate), repo, GITHUB_TOKEN);
                long buildID = APIUtil.getBuildIDForSubmitedCommit(repo, commitSHA, TRAVIS_TOKEN);
                long jobID = APIUtil.extractBuildJobID(buildID, TRAVIS_TOKEN);
                String logOutput = APIUtil.extractJobLog(jobID, TRAVIS_TOKEN);
                List<JSONObject> graderLogs = GraderReportProcessUtil.tokenizeBuildLog(logOutput);
                long sumOfScores = GraderReportProcessUtil.getSumOfScores(graderLogs);
                return sumOfScores;
            } catch (Exception ex) {
                return 0;
            }

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
                String status = APIUtil.getBuildStatus(buildID, TRAVIS_TOKEN);
                if (status.equalsIgnoreCase("started")) {
                    GraderReportPainterUtil.paintError(svgGenerator, "Build in Progress, wait...");
                    svgGenerator.stream(res.raw().getWriter(), true);
                    return res;
                }
                long jobID = APIUtil.extractBuildJobID(buildID, TRAVIS_TOKEN);
                String comitSHA = APIUtil.extractCommitSHAForJobID(jobID, TRAVIS_TOKEN);
                String logOutput = APIUtil.extractJobLog(jobID, TRAVIS_TOKEN);
                List<JSONObject> graderLogs = GraderReportProcessUtil.tokenizeBuildLog(logOutput);
                long totalScore = GraderReportProcessUtil.getTotalScore(graderLogs);
                long sumOfScores = GraderReportProcessUtil.getSumOfScores(graderLogs);

                GraderReportPainterUtil.paintMinimal(svgGenerator, repo, studentID, sumOfScores, totalScore, comitSHA, status);

                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            } catch (Exception ex) {
                GraderReportPainterUtil.paintError(svgGenerator, "We ain't living in a perfect world");
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
