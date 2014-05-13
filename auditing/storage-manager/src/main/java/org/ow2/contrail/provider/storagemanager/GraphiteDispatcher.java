package org.ow2.contrail.provider.storagemanager;

import com.mongodb.BasicDBObject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Map;

public class GraphiteDispatcher {
    private static Logger log = Logger.getLogger(GraphiteDispatcher.class);
    private String carbonHostname;
    private int carbonPort;

    public GraphiteDispatcher() {
        this.carbonHostname = Conf.getInstance().getCarbonHost();
        this.carbonPort = Conf.getInstance().getCarbonPort();
    }

    public GraphiteDispatcher(String carbonHostname, int carbonPort) {
        this.carbonHostname = carbonHostname;
        this.carbonPort = carbonPort;
    }

    public void sendMetricsData(BasicDBObject data) throws IOException {
        String source = data.getString("source");
        String sid = data.getString("sid");
        String group = data.getString("group");
        Date timestamp = (Date) data.get("time");
        BasicDBObject metrics = (BasicDBObject) data.get("metrics");
        long timestampSec = Math.round(timestamp.getTime() / 1000);

        String message = "";
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            String metric = entry.getKey();
            Object value = entry.getValue();
            String strValue = value.toString();

            if (!isNumeric(strValue)) {  // the same if metric value is 'N/A'
                continue;
            }
            String metricPath = source + "." + sid + "." + group + "." + metric;
            message += metricPath + " " + strValue + " " + timestampSec + "\n";
        }

        if (message.length() > 0) {
            Socket socket = null;
            try {
                socket = new Socket(carbonHostname, carbonPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.print(message);
                out.close();
                if (log.isTraceEnabled()) {
                    log.trace("Metrics data sent to Graphite:\n" + message);
                }
            }
            finally {
                if (socket != null) {
                    try {
                        socket.close();
                    }
                    catch (Exception ignore) {
                    }
                }
            }
        }
    }

    public static boolean isNumeric(String string) {
        return string.matches("-?\\d+(\\.\\d+)?");
    }
}
