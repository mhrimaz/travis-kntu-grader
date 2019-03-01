package com.mhrimaz;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GitHubApiUtil {
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
    public static String getSubmittedCommitSHA(Date untilDate, String repo, String githubToken) throws UnirestException, ParseException {

        JSONArray commits = Unirest.get("http://api.github.com/repos/k-n-toosi-university-of-technology/" +
                repo + "/commits?until=" + DATE_FORMAT.format(untilDate))
                .header("Authorization", "token " + githubToken)
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

    public static String getLastCommitSHA(String githubToken, String repo) throws UnirestException {
        return Unirest.get("https://api.github.com/repos/k-n-toosi-university-of-technology/" +
                repo + "/commits/master")
                .header("Authorization", "token " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getString("sha");
    }

    public static int getRemainingRateLimit(String githubToken) throws UnirestException {
        return Unirest.get("https://api.github.com/rate_limit")
                .header("Authorization", "token  " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject()
                .getJSONObject("rate").getInt("remaining");
    }

    public static JSONArray getRepoFilesList(String githubToken, String repo, String sha) throws UnirestException {
        JSONArray tree = Unirest.get("https://api.github.com/repos/k-n-toosi-university-of-technology/" +
                repo + "/git/trees/" + sha + "?recursive=1")
                .header("Authorization", "token  " + githubToken)
                .header("cache-control", "no-cache")
                .asJson().getBody().getObject().getJSONArray("tree");
        JSONArray treeOfBlobs = new JSONArray();
        for (int i = 0; i < tree.length(); i++) {
            JSONObject jsonObject = tree.getJSONObject(i);
            if (jsonObject.getString("type").equalsIgnoreCase("blob")) {
                treeOfBlobs.put(jsonObject);
            }
        }
        return treeOfBlobs;
    }
}
