package globalquake.core;

import java.awt.EventQueue;
import java.util.HashMap;

import globalquake.core.earthquake.Earthquake;
import globalquake.ui.settings.Settings;
import globalquake.ui.AlertWindow;
import globalquake.geo.GeoUtils;
import org.tinylog.Logger;

public class AlertManager {
    private final HashMap<Earthquake, Warning> warnedQuakes;

    public AlertManager() {
        this.warnedQuakes = new HashMap<>();
    }

    public void tick() {
        if (!Settings.enableAlarmDialogs) {
            return;
        }
        for (Earthquake quake : GlobalQuake.instance.getEarthquakeAnalysis().getEarthquakes()) {
            if (meetsConditions(quake) && !warnedQuakes.containsKey(quake)) {
                warnedQuakes.put(quake, new Warning());
                EventQueue.invokeLater(() -> {
                    try {
                        AlertWindow frame = new AlertWindow(quake);
                        frame.setVisible(true);
                    } catch (Exception e) {
                        Logger.error(e);
                    }
                });
            }
        }
    }

    public static boolean meetsConditions(Earthquake quake) {
        double distGC = GeoUtils.greatCircleDistance(quake.getLat(), quake.getLon(), Settings.homeLat,
                Settings.homeLon);
        double distGEO = GeoUtils.geologicalDistance(quake.getLat(), quake.getLon(), -quake.getDepth(),
                Settings.homeLat, Settings.homeLon, 0.0);
        double pgaHome = GeoUtils.pgaFunctionGen1(quake.getMag(), distGEO);

        if (distGC < 400) {
            return true;
        }

        if (distGC < 2000 && quake.getMag() >= 3.5) {
            return true;
        }

        if (quake.getMag() > 5.5) {
            return true;
        }

        return pgaHome >= 0.1;
    }

}

class Warning {

}
