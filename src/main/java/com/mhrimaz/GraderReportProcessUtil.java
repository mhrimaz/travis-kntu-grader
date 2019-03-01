package com.mhrimaz;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraderReportProcessUtil {
    final static Pattern GRADER_PATTERN = Pattern.compile("\\$\\$\\$GRADER\\$\\$\\$ \\| (.*) \\| \\$\\$\\$GRADER\\$\\$\\$");

    public static List<JSONObject> tokenizeBuildLog(String log) {
        List<JSONObject> buildLogs = new LinkedList<>();
        Matcher matcher = GRADER_PATTERN.matcher(log);
        while (matcher.find()) {
            JSONObject json = new JSONObject(matcher.group(1));
            buildLogs.add(json);
        }

        return buildLogs;
    }

    public static long getTotalScore(List<JSONObject> graderLogs) {
        for (JSONObject graderLog : graderLogs) {
            String msgType = graderLog.getString("type");
            if (msgType.equalsIgnoreCase("msg")) {
                String key = graderLog.getString("key");
                if (key.equalsIgnoreCase("total")) {
                    return graderLog.getLong("value");
                }
            }
        }
        return 0;
    }

    public static long getSumOfScores(List<JSONObject> graderLogs) {
        long sum = 0;
        for (JSONObject graderLog : graderLogs) {
            String msgType = graderLog.getString("type");
            if (msgType.equalsIgnoreCase("score")) {
                sum += graderLog.getLong("amount");
            }
        }
        return sum;
    }

    public static Map<String, List<String>> getGraderMessages(List<JSONObject> graderLogs) {
        Map<String, List<String>> messages = new HashMap<>();
        for (JSONObject graderLog : graderLogs) {
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
}
