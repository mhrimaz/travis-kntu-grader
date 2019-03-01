package com.mhrimaz;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GraderReportPainterUtil {
    public static void paintError(Graphics2D g2d, String message) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString(message, 40, 70);
    }

    public static void paintMinimal(Graphics2D g2d, String name, String studentId, long score, long maxScore
            , String commitSHA, String buildStatus) {
        if (score == maxScore) {
            if (score == 0) {
                g2d.setPaint(Color.MAGENTA);
            } else {
                g2d.setPaint(Color.GREEN);
            }
        } else {
            g2d.setPaint(Color.ORANGE);
        }
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 24));
        g2d.drawString("Student ID: " + studentId, 45, 30);
        g2d.drawString(String.format("Score: %3d out of %3d", score, maxScore), 47, 60);
        g2d.drawString(String.format("Build Status: %13s", buildStatus), 20, 90);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 18));
        g2d.drawString("For Commit: " + commitSHA.substring(0, 8), 60, 115);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString("K. N. Toosi University of Technology", 25, 140);
    }

    public static void paint(Graphics2D g2d, String name, String studentId, Map<String, java.util.List<String>> messages, long score, long maxScore) {
        g2d.setPaint(Color.LIGHT_GRAY);
        g2d.fill(new Rectangle(0, 0, 800, 900));
        if (score == maxScore) {
            g2d.setPaint(Color.GREEN);
        } else {
            g2d.setPaint(Color.ORANGE);
        }
        g2d.fill(new Rectangle(0, 0, 300, 150));
        g2d.setPaint(Color.BLACK);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 24));
        g2d.drawString("Student ID: " + studentId, 45, 40);
        g2d.drawString(String.format("Score: %3d out of %3d", score, maxScore), 47, 80);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 16));
        g2d.drawString("K. N. Toosi University of Technology", 25, 120);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(2, 152, 798, 152);


        java.util.List<String> failReasons = messages.get("FAIL_REASON".toLowerCase());
        Iterator<String> iterator = failReasons.iterator();
        for (int i = 0; i < 14 && iterator.hasNext(); i++) {
            g2d.drawString(" + FAIL_REASON: " + iterator.next(), 10, 180 + (i * 25));
        }

        List<String> todo = messages.get("TODO".toLowerCase());
        iterator = todo.iterator();
        g2d.drawLine(2, 520, 798, 520);
        for (int i = 0; i < 14 && iterator.hasNext(); i++) {
            g2d.drawString(" +  TODO      : " + iterator.next(), 10, 545 + (i * 25));
        }

    }

}
