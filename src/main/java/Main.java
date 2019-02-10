import org.apache.log4j.BasicConfigurator;

import static spark.Spark.*;

public class Main {
    static {
        BasicConfigurator.configure();
    }
    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        get("/", (req, res) -> "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"100\" height=\"100%\"> <circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"red\"> </svg>");

        get("report.svg" +

                "", (req, res) -> "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"100\" height=\"100%\"> <circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"red\"> </svg>");
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

}
