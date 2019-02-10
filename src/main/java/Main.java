import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;

import java.io.InputStream;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import static spark.Spark.*;

public class Main {
    static {

        BasicConfigurator.configure();

    }
    public static void paint(Graphics2D g2d) {
        g2d.setPaint(Color.red);
        g2d.fill(new Rectangle(10, 10, 100, 100));
    }
    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        get("/", (req, res) -> {
//            res.header("Content-Encoding", "gzip");
//            res.header("Content-Type", "image/svg+xml");
//            res.header("Cache-Control","max-age=300");
//            res.status(200);
            res.type("image/svg+xml");
            // Get a DOMImplementation.
            DOMImplementation domImpl =
                    GenericDOMImplementation.getDOMImplementation();

            // Create an instance of org.w3c.dom.Document.
            String svgNS = "http://www.w3.org/2000/svg";
            Document document = domImpl.createDocument(svgNS, "svg", null);

            // Create an instance of the SVG Generator.
            SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

            // Ask the test to render into the SVG Graphics2D implementation.

            paint(svgGenerator);

            // Finally, stream out SVG to the standard output using
            // UTF-8 encoding.
            boolean useCSS = true; // we want to use CSS style attributes
            svgGenerator.stream(res.raw().getWriter(), useCSS);
            return res;
        });

        get("report.svg" , (req, res) -> {
            res.header("Content-Encoding", "gzip");
            res.header("Content-Type", "image/svg+xml");
            res.header("Cache-Control","max-age=300");
            res.status(200);

            res.type("image/svg+xml");

            String data =
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"413\" height=\"28\"><g shape-rendering=\"crispEdges\"><path fill=\"#555\" d=\"M0 0h146v28H0z\"/><path fill=\"#e05d44\" d=\"M146 0h267v28H146z\"/></g><g fill=\"#fff\" text-anchor=\"middle\" font-family=\"DejaVu Sans,Verdana,Geneva,sans-serif\" font-size=\"100\"><image x=\"9\" y=\"7\" width=\"14\" height=\"14\" xlink:href=\"data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjMDBCM0UwIiByb2xlPSJpbWciIHZpZXdCb3g9IjAgMCAyNCAyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48dGl0bGU+QXBwVmV5b3IgaWNvbjwvdGl0bGU+PHBhdGggZD0iTSAxMiwwIEMgMTguNiwwIDI0LDUuNCAyNCwxMiAyNCwxOC42IDE4LjYsMjQgMTIsMjQgNS40LDI0IDAsMTguNiAwLDEyIDAsNS40IDUuNCwwIDEyLDAgWiBtIDIuOTQsMTQuMzQgQyAxNi4yNiwxMi42NiAxNi4wOCwxMC4yNiAxNC40LDkgMTIuNzgsNy43NCAxMC4zOCw4LjA0IDksOS43MiA3LjY4LDExLjQgNy44NiwxMy44IDkuNTQsMTUuMDYgYyAxLjY4LDEuMjYgNC4wOCwwLjk2IDUuNCwtMC43MiB6IG0gLTYuNDIsNy44IGMgMC43MiwwLjMgMi4yOCwwLjYgMy4wNiwwLjYgbCA1LjIyLC03LjU2IGMgMS42OCwtMi41MiAxLjI2LC01Ljk0IC0xLjA4LC03LjggLTIuMSwtMS42OCAtNS4wNCwtMS42MiAtNy4xNCwwIGwgLTcuMjYsNS41OCBjIDAuMTgsMS45MiAwLjcyLDIuODggMC43MiwyLjk0IGwgNC4xNCwtNC41IGMgLTAuMywxLjk4IDAuNDIsNC4wMiAyLjEsNS4yOCAxLjQ0LDEuMTQgMy4xOCwxLjQ0IDQuODYsMS4wOCB6Ii8+PC9zdmc+Cg==\"/> <text x=\"815\" y=\"175\" transform=\"scale(.1)\" textLength=\"1050\">CUSTOM BADGE</text><text x=\"2795\" y=\"175\" font-weight=\"bold\" transform=\"scale(.1)\" textLength=\"2430\">INVALID QUERY PARAMETER: URL</text></g> </svg>";
            //data = data.replace("<svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"");

            return data.getBytes();
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
