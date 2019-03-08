package com.mhrimaz;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GitHubApiUtil {
    static Logger log = Logger.getLogger(GitHubApiUtil.class.getName());
    public final static SimpleDateFormat DATE_FORMAT;

    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Extract submitted commit sha from github considering the submit due date
     *
     * @param untilDate
     * @param repo
     * @param githubToken
     * @return
     * @throws UnirestException
     * @throws ParseException
     */
    public static String getSubmittedCommitSHA(Date untilDate, String repo, String githubToken, String organization)
            throws UnirestException, ParseException {

        JSONArray commits = Unirest.get("http://api.github.com/repos/" + organization + "/" +
                repo + "/commits?until=" + DATE_FORMAT.format(untilDate))
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getArray();

        Date lastCommitDate = null;
        String submitedSHA = "";
        for (int i = 0; i < commits.length(); i++) {
            JSONObject commitJSON = commits.getJSONObject(i);
            String sha = commitJSON.getString("sha");
            Date date = DATE_FORMAT.parse(commitJSON.getJSONObject("commit")
                    .getJSONObject("author").getString("date"));
            if (lastCommitDate == null || lastCommitDate.compareTo(date) <= 0) {
                lastCommitDate = date;
                submitedSHA = sha;
            }

        }
        return submitedSHA;
    }

    public static String getLastCommitSHA(String githubToken, String repo, String organization)
            throws UnirestException {
        return Unirest.get("https://api.github.com/repos/" + organization + "/" +
                repo + "/commits/master")
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getString("sha");
    }

    public static int getRemainingRateLimit(String githubToken) throws UnirestException {
        return Unirest.get("https://api.github.com/rate_limit")
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject()
                .getJSONObject("rate").getInt("remaining");
    }


    public static JSONObject getRepoTree(String githubToken, String repo, String sha,
                                         String desinationRepo, final String organization) throws UnirestException {
        JSONArray tree = Unirest.get("https://api.github.com/repos/" + organization + "/" +
                repo + "/git/trees/" + sha + "?recursive=1")
                .header("Authorization", "token  " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getJSONArray("tree");
        JSONArray treeOfBlobs = new JSONArray();
        for (int i = 0; i < tree.length(); i++) {
            JSONObject jsonObject = tree.getJSONObject(i);

            if (jsonObject.getString("type").equalsIgnoreCase("blob")) {
                treeOfBlobs.put(jsonObject);
                String url = jsonObject.getString("url");
                String path = jsonObject.getString("path");
                String fileContent = new String(Base64.getMimeDecoder().decode(
                        getFileContent(githubToken, url)));
                if (path.equalsIgnoreCase("README.md")) {
                    fileContent = fileContent.replaceAll("\\(YOUR_GRADER_BADGE\\)",
                            "(https://kntu-grader.herokuapp.com/minimal?repo="
                                    + desinationRepo + "&id=YOUR_ID)");
                }
                jsonObject.put("content", fileContent);
                jsonObject.remove("url");
                jsonObject.remove("sha");
                jsonObject.remove("size");
                treeOfBlobs.put(jsonObject);
            }

        }
        JSONObject toReturn = new JSONObject();
        toReturn.put("tree", treeOfBlobs);
        return toReturn;
    }

    public static String getFileContent(String githubToken, String url) throws UnirestException {
        return Unirest.get(url)
                .header("Authorization", "token  " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getString("content");
    }

    public static boolean isRepoExist(String githubToken, String repo, String organization) throws UnirestException {
        return Unirest.get("https://api.github.com/repos/" + organization + "/" + repo)
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getStatus() == 200;
    }

    public static int importRepo(String githubToken, String sourceRepo, String destinationRepo,
                                 final String organization) throws UnirestException {
        if (!GitHubApiUtil.isRepoExist(githubToken, sourceRepo, organization) || !GitHubApiUtil.isRepoExist(githubToken, destinationRepo, organization)) {
            return 404;
        }
        int readmeStatus = Unirest.get("https://api.github.com/repos/" + organization + "/" +
                destinationRepo + "/contents/README.md")
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getStatus();
        if (readmeStatus == 200) {
            return 404;
        }
        createFile(githubToken, destinationRepo, ".initiator",
                "Sy4gTi4gVG9vc2kgVW5pdmVyc2l0eSBvZiBUZWNobm9sZ3k=", organization);
        String lastCommitSHA = getLastCommitSHA(githubToken, sourceRepo, organization);
        log.debug("COPY FROM " + sourceRepo + " INTO " + destinationRepo);
        JSONObject sourceRepoTree = getRepoTree(githubToken, sourceRepo, lastCommitSHA, destinationRepo, organization);

        String treeSHA = createTree(githubToken, destinationRepo, sourceRepoTree, organization);
        String commitSHA = createCommit(githubToken, destinationRepo, treeSHA, organization);

        updateHEAD(githubToken, destinationRepo, commitSHA, organization);
        return 200;
    }

    private static int updateHEAD(String githubToken, String repo, String commitSHA, String organization)
            throws UnirestException {
        return Unirest.patch("https://api.github.com/repos/" + organization + "/" +
                repo + "/git/refs/heads/master")
                .header("Authorization", "token " + githubToken)
                .header("Content-Type", "application/json")
                .header("cache-control", "no-cache")
                .body("{\"sha\":\"" + commitSHA + "\",\"force\":true\n}")
                .asJson().getStatus();
    }

    private static String createCommit(String githubToken, String repo, String treeSHA, String organization)
            throws UnirestException {
        return Unirest.post("https://api.github.com/repos/" + organization + "/" +
                repo + "/git/commits")
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .body("{\"message\":\"[KNTU_GRADER] File Creator\",\"tree\":\"" +
                        treeSHA + "\"}")
                .asJson().getBody().getObject().getString("sha");
    }

    private static String createTree(String githubToken, String repo, JSONObject tree, String organization)
            throws UnirestException {
        return Unirest.post("https://api.github.com/repos/" + organization + "/" + repo +
                "/git/trees")
                .header("Authorization", "token " + githubToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("cache-control", "no-cache")
                .body(tree.toString()).asJson().getBody().getObject().getString("sha");
    }

    private static String createFile(String githubToken, String repo, String path, String base64Content,
                                     String organization) throws UnirestException {
        return Unirest.put("https://api.github.com/repos/" + organization + "/" +
                repo + "/contents/" + path)
                .header("Authorization", "token  " + githubToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("cache-control", "no-cache")
                .body("{\"message\": \"[KNTU_GRADER] File Creator\", \"content\": \"" + base64Content + "\"}")
                .asJson().getBody().getObject().getJSONObject("commit").getString("sha");

    }
}
