import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) {
        final Pattern graderPattern = Pattern.compile("\\$\\$\\$GRADER\\$\\$\\$ \\| (.*) \\| \\$\\$\\$GRADER\\$\\$\\$");
        String log = "java.lang.AssertionError: \n" +
                "Unable to detect valid trinagle [CHECK OVERFLOW]\n" +
                "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:\"[CHECK OVERFLOW]\", priority:1  }  | $$$GRADER$$$\n" +
                "\tat ir.ac.kntu.style.TriangleTest.testValidTriangle(TriangleTest.java:87)\n" +
                "[ERROR] testIsoscelesTriangle(ir.ac.kntu.style.TriangleTest)  Time elapsed: 0.012 s  <<< FAILURE!\n" +
                "java.lang.AssertionError: \n" +
                "Unable to detect ISOSCELES trinagle [CHECK OVERFLOW]\n" +
                "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:\"[CHECK OVERFLOW]\", priority:1  }  | $$$GRADER$$$\n" +
                "\tat ir.ac.kntu.style.TriangleTest.testIsoscelesTriangle(TriangleTest.java:115)\n" +
                "[ERROR] testEquilateralTriangle(ir.ac.kntu.style.TriangleTest)  Time elapsed: 0.007 s  <<< FAILURE!\n" +
                "java.lang.AssertionError: \n" +
                "Unable to detect EQUILATERAL trinagle [CHECK OVERFLOW]\n" +
                "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:\"[CHECK OVERFLOW]\", priority:1  }  | $$$GRADER$$$\n" +
                "\tat ir.ac.kntu.style.TriangleTest.testEquilateralTriangle(TriangleTest.java:127)\n" +
                "[INFO] \n" +
                "[INFO] Results:\n" +
                "[INFO] \n" +
                "[ERROR] Failures: \n" +
                "[ERROR]   TriangleTest.testEquilateralTriangle:127 Unable to detect EQUILATERAL trinagle [CHECK OVERFLOW]\n" +
                "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:\"[CHECK OVERFLOW]\", priority:1  }  | $$$GRADER$$$\n" +
                "[ERROR]   TriangleTest.testIsoscelesTriangle:115 Unable to detect ISOSCELES trinagle [CHECK OVERFLOW]\n" +
                "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:\"[CHECK OVERFLOW]\", priority:1  }  | $$$GRADER$$$\n" +
                "[ERROR]   TriangleTest.testValidTriangle:87 Unable to detect valid trinagle [CHECK OVERFLOW]\n" +
                "$$$GRADER$$$ | { type:\"MSG\" , key:\"FAIL_REASON\" , value:\"[CHECK OVERFLOW]\", priority:1  }  | $$$GRADER$$$";
        Matcher matcher = graderPattern.matcher(log);
        while(matcher.find()) {
            System.out.println("found: " + matcher.group(1));
        }
    }
}
