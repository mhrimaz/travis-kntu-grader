import org.apache.log4j.BasicConfigurator;

import static spark.Spark.*;

public class Main {
    static {
        BasicConfigurator.configure();
    }

    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        get("/", (req, res) -> "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"100\" height=\"100%\"> <circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"red\"> </svg>");

        get("report.svg" , (req, res) -> {
            res.type("image/svg+xml");
            String data =  "<svg width=\"580\" height=\"400\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                    " <!-- Created with Method Draw - http://github.com/duopixel/Method-Draw/ -->\n" +
                    " <g>\n" +
                    "  <title>background</title>\n" +
                    "  <rect fill=\"#fff\" id=\"canvas_background\" height=\"402\" width=\"582\" y=\"-1\" x=\"-1\"/>\n" +
                    "  <g display=\"none\" overflow=\"visible\" y=\"0\" x=\"0\" height=\"100%\" width=\"100%\" id=\"canvasGrid\">\n" +
                    "   <rect fill=\"url(#gridpattern)\" stroke-width=\"0\" y=\"0\" x=\"0\" height=\"100%\" width=\"100%\"/>\n" +
                    "  </g>\n" +
                    " </g>\n" +
                    " <g>\n" +
                    "  <title>Layer 1</title>\n" +
                    "  <rect id=\"svg_1\" height=\"355\" width=\"341\" y=\"32.4375\" x=\"87.5\" stroke-width=\"1.5\" stroke=\"#000\" fill=\"#bf0000\"/>\n" +
                    "  <text xml:space=\"preserve\" text-anchor=\"start\" font-family=\"Helvetica, Arial, sans-serif\" font-size=\"24\" id=\"svg_2\" y=\"187.4375\" x=\"145.5\" stroke-width=\"0\" stroke=\"#000\" fill=\"#000000\">Clean Code : 10/10</text>\n" +
                    " </g>\n" +
                    "</svg>";
            //data = data.replace("<svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"");

            return data;
        });
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

}
