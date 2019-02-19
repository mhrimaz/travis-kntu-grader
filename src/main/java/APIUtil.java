import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class APIUtil {

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
     * @param apiKey
     * @return
     * @throws UnirestException
     * @throws ParseException
     */
    public static String getSubmittedCommitSHA(Date untilDate, String repo, String apiKey) throws UnirestException, ParseException {

        JSONArray commits = Unirest.get("http://api.github.com/repos/k-n-toosi-university-of-technology/" +
                repo + "/commits?until=" + DATE_FORMAT.format(untilDate))
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson().getBody().getArray();

        Date lastCommitDate = null;
        String submitedSHA = "";
        for (int i = 0; i < commits.length(); i++) {
            JSONObject commitJSON = commits.getJSONObject(i);
            String sha = commitJSON.getString("sha");
            Date date = DATE_FORMAT.parse(commitJSON.getJSONObject("commit").getJSONObject("author").getString("date"));
            if (lastCommitDate == null || lastCommitDate.compareTo(date) <= 0) {
                lastCommitDate = date;
                submitedSHA = sha;
            }

        }
        return submitedSHA;
    }

    /**
     * search in travis for corespondent build for a specific commit sha
     *
     * @param repo
     * @param commitSHA
     * @param apiKey
     * @return
     * @throws UnirestException
     */
    public static long getBuildIDForSubmitedCommit(String repo, String commitSHA, String apiKey) throws UnirestException {
        JSONArray builds = Unirest.get("https://api.travis-ci.com/repos/k-n-toosi-university-of-technology/" +
                repo + "/builds")
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson().getBody().getArray();
        for (int i = 0; i < builds.length(); i++) {
            JSONObject buildJSON = builds.getJSONObject(i);
            String commit = buildJSON.getString("commit");
            if (commit.equalsIgnoreCase(commitSHA)) {
                return buildJSON.getLong("id");
            }
        }

        return 0;
    }

    public static long extractBuildJobID(long buildID, String apiKey) throws UnirestException {
        JSONObject buildInfo = Unirest.get("https://api.travis-ci.com/builds/" + buildID)
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject();
        long jobID = buildInfo.getJSONArray("matrix").getJSONObject(0).getLong("id");

        return jobID;
    }

    public static String extractCommitSHAForJobID(long jobID, String apiKey) throws UnirestException {
        return Unirest.get("https://api.travis-ci.com/jobs/" + jobID)
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getString("commit");
    }

    public static String extractJobLog(long jobID, String apiKey) throws UnirestException {
        HttpResponse<JsonNode> jobOutput = Unirest.get("https://api.travis-ci.com/v3/job/" + jobID + "/log")
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson();
        String content = jobOutput.getBody().getObject().getString("content");
        return content;
    }

    public static String getBuildStatus(long buildID, String apiKey) throws UnirestException {
        String status = Unirest.get("https://api.travis-ci.com/v3/build/" + buildID)
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getString("state");
        return status.toUpperCase();
    }

    public static long extractLastBuildID(String repo, String apiKey) throws UnirestException {
        String repoInfoAPI = "https://api.travis-ci.com/repos/k-n-toosi-university-of-technology/" + repo;
        HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.get(repoInfoAPI)
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson();
        return jsonNodeHttpResponse.getBody().getObject().getLong("last_build_id");
    }

    public static String extractLatestBuildLog(String repo, String apiKey) throws UnirestException {
        long lastBuildID = extractLastBuildID(repo, apiKey);
        long jobID = extractBuildJobID(lastBuildID, apiKey);
        return extractJobLog(jobID, apiKey);
    }
}
