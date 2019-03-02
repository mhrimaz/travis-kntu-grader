package com.mhrimaz;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.List;

import static spark.Spark.*;


public class Main {
    private static String TRAVIS_TOKEN = System.getenv("TRAVIS_TOKEN");
    private static String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");



    public static void main(String[] args) {
        BasicConfigurator.configure();
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
            try {
                JSONObject event = new JSONObject(req.body());
                if (event.getString("action").equalsIgnoreCase("created")) {
                    String destinationRepo = event.getJSONObject("repository").getString("name");
                    if (!destinationRepo.endsWith("starter") && destinationRepo.contains("-")) {
                        String sourceRepo = destinationRepo.substring(0, destinationRepo.lastIndexOf('-')) + "-starter";
                        return GitHubApiUtil.importRepo(GITHUB_TOKEN, sourceRepo, destinationRepo);
                    }
                }
            } catch (JSONException ex) {
                System.out.println("ex = " + ex);
            }
            return res;
        });
        get("importer", (req, res) -> {
            String sourceRepo = req.queryParams("source");
            String destinationRepo = req.queryParams("destination");
            if (destinationRepo == null || destinationRepo.isEmpty() || destinationRepo.endsWith("starter")) {
                return "BAD REQUEST";
            }
            if (sourceRepo == null || sourceRepo.isEmpty()) {
                sourceRepo = destinationRepo.substring(0, destinationRepo.lastIndexOf('-')) + "-starter";
            }

            return GitHubApiUtil.importRepo(GITHUB_TOKEN, sourceRepo, destinationRepo);
        });
        get("grader", (req, res) -> {
            res.type("application/json");
            String repo = req.queryParams("repo");
            String dueDate = req.queryParams("due");
            try {
                String commitSHA = GitHubApiUtil.getSubmittedCommitSHA(GitHubApiUtil.DATE_FORMAT.parse(dueDate), repo, GITHUB_TOKEN);
                long buildID = TravisAPIUtil.getBuildIDForSubmitedCommit(repo, commitSHA, TRAVIS_TOKEN);
                long jobID = TravisAPIUtil.extractBuildJobID(buildID, TRAVIS_TOKEN);
                String logOutput = TravisAPIUtil.extractJobLog(jobID, TRAVIS_TOKEN);
                List<JSONObject> graderLogs = GraderReportProcessUtil.tokenizeBuildLog(logOutput);
                return GraderReportProcessUtil.getSumOfScores(graderLogs);
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

            if (studentID == null || studentID.isEmpty()) {
                GraderReportPainterUtil.paintError(svgGenerator, "INVALID STUDENT ID");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            if (studentID.matches("\\d{7}") == false) {
                studentID = "UNKNOWN";
            }

            if (repo == null || repo.isEmpty()) {
                GraderReportPainterUtil.paintError(svgGenerator, "MISSING PARAM: REPO");
                svgGenerator.stream(res.raw().getWriter(), true);
                return res;
            }

            try {
                long buildID = TravisAPIUtil.extractLastBuildID(repo, TRAVIS_TOKEN);
                String status = TravisAPIUtil.getBuildStatus(buildID, TRAVIS_TOKEN);
                if (status.equalsIgnoreCase("started")) {
                    GraderReportPainterUtil.paintError(svgGenerator, "Build in Progress, wait...");
                    svgGenerator.stream(res.raw().getWriter(), true);
                    return res;
                }
                long jobID = TravisAPIUtil.extractBuildJobID(buildID, TRAVIS_TOKEN);
                String comitSHA = TravisAPIUtil.extractCommitSHAForJobID(jobID, TRAVIS_TOKEN);
                String logOutput = TravisAPIUtil.extractJobLog(jobID, TRAVIS_TOKEN);
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


    private static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

}
