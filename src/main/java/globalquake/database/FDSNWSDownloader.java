package globalquake.database;

import globalquake.main.Main;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FDSNWSDownloader {

    private static final String CHANNELS = "EHZ,SHZ,HHZ,BHZ";
    private static final SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final int TIMEOUT_SECONDS = 10;

    public static List<Network> downloadFDSNWS(StationSource stationSource) throws Exception {
        List<Network> result = new ArrayList<>();

        URL url = new URL("%squery?level=channel&endafter=%s&includerestricted=false&format=xml&channel=%s".formatted(stationSource.getUrl(), format1.format(new Date()), CHANNELS));

        System.out.println("Connecting to " + stationSource.getName());
        stationSource.getStatus().setString("Connecting to " + stationSource.getName());
        stationSource.getStatus().setValue(0);

        URLConnection con = url.openConnection();
        con.setConnectTimeout(TIMEOUT_SECONDS * 1000);
        con.setReadTimeout(TIMEOUT_SECONDS * 1000);
        InputStream inp = con.getInputStream();

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        f.setValidating(false);
        final CountInputStream in = new CountInputStream(inp);

        in.setEvent(() ->  stationSource.getStatus().setString("Downloading %dkB".formatted(in.getCount() / 1024)));

        stationSource.getStatus().setValue(25);
        System.out.printf("Downloading stations from %s (%s)%n", stationSource.getName(), url);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);

        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        stationSource.getStatus().setValue(50);
        stationSource.getStatus().setString("Parsing networks...");
        parseNetworks(result, stationSource, root);

        stationSource.getStatus().setValue(75);
        return result;
    }

    private static void parseNetworks(List<Network> result, StationSource stationSource, Element root) {
        NodeList networks = root.getElementsByTagName("Network");
        for (int i = 0; i < networks.getLength(); i++) {
            try {
                String networkCode = obtainAttribute(networks.item(i), "code", "unknown");
                if (networkCode.equalsIgnoreCase("unknown")) {
                    System.err.println("ERR: no network code wtf.");
                    continue;
                }
                String networkDescription = obtainElement(networks.item(i), "Description", "");
                parseStations(result, stationSource, networks, i, networkCode, networkDescription);
            } catch (Exception e) {
                Main.getErrorHandler().handleException(e);
            }
        }
    }

    private static void parseStations(List<Network> result, StationSource stationSource, NodeList networks, int i, String networkCode, String networkDescription) {
        NodeList stations = ((Element) networks.item(i)).getElementsByTagName("Station");
        for (int j = 0; j < stations.getLength(); j++) {
            Node stationNode = stations.item(j);
            String stationCode = stationNode.getAttributes().getNamedItem("code").getNodeValue();
            String stationSite = ((Element) stationNode).getElementsByTagName("Site").item(0).getTextContent();
            // todo station-specific lat lon alt
            parseChannels(result, stationSource, networkCode, networkDescription, (Element) stationNode, stationCode, stationSite);
        }
    }

    private static void parseChannels(List<Network> result, StationSource stationSource, String networkCode, String networkDescription, Element stationNode, String stationCode, String stationSite) {
        NodeList channels = stationNode.getElementsByTagName("Channel");
        for (int k = 0; k < channels.getLength(); k++) {
                // Necessary values: lat lon alt sampleRate, Other can fail

            Node channelNode = channels.item(k);
            String channel = channelNode.getAttributes().getNamedItem("code").getNodeValue();
            String locationCode = channelNode.getAttributes().getNamedItem("locationCode")
                    .getNodeValue();
            double lat = Double.parseDouble(
                    ((Element) channelNode).getElementsByTagName("Latitude").item(0).getTextContent());
            double lon = Double.parseDouble(
                    ((Element) channelNode).getElementsByTagName("Longitude").item(0).getTextContent());
            double alt = Double.parseDouble(
                    ((Element) channelNode).getElementsByTagName("Elevation").item(0).getTextContent());
            double sampleRate = Double.parseDouble(((Element) channelNode)
                    .getElementsByTagName("SampleRate").item(0).getTextContent());

            addChannel(result, stationSource, networkCode, networkDescription, stationCode, stationSite, channel,
                    locationCode, lat, lon, alt, sampleRate);
        }
    }

    private static void addChannel(List<Network> result, StationSource stationSource, String networkCode, String networkDescription, String stationCode, String stationSite, String channelCode, String locationCode, double lat, double lon, double alt, double sampleRate) {
        Network network = StationDatabase.getOrCreateNetwork(result, networkCode, networkDescription);
        Station station = StationDatabase.getOrCreateStation(network, stationCode, stationSite, lat, lon, alt);
        StationDatabase.getOrCreateChannel(station, channelCode, locationCode, lat, lon, alt, sampleRate, stationSource);
    }

    public static String obtainElement(Node item, String name, String defaultValue) {
        try {
            return ((Element) item).getElementsByTagName(name).item(0).getTextContent();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String obtainAttribute(Node item, String name, String defaultValue) {
        try {
            return item.getAttributes().getNamedItem(name).getNodeValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
