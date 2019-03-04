package com.mhrimaz;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

public class TravisAPIUtil {

    /**
     * search in travis for corespondent build for a specific commit sha
     *
     * @param repo
     * @param commitSHA
     * @param apiKey
     * @return
     * @throws UnirestException
     */
    public static long getBuildIDForSubmitedCommit(String repo, String commitSHA, String apiKey, String organization) throws UnirestException {
        JSONArray builds = Unirest.get("https://api.travis-ci.com/repos/" + organization + "/" +
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

    public static long extractLastBuildID(String repo, String apiKey, String organization) throws UnirestException {
        String repoInfoAPI = "https://api.travis-ci.com/repos/" + organization + "/" + repo;
        HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.get(repoInfoAPI)
                .header("Content-Type", "application/json")
                .header("Authorization", "token " + apiKey)
                .header("cache-control", "no-cache")
                .asJson();
        return jsonNodeHttpResponse.getBody().getObject().getLong("last_build_id");
    }

    public static String extractLatestBuildLog(String repo, String apiKey, String organization) throws UnirestException {
        long lastBuildID = extractLastBuildID(repo, apiKey, organization);
        long jobID = extractBuildJobID(lastBuildID, apiKey);
        return extractJobLog(jobID, apiKey);
    }
}
