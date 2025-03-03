package globalquake.ui.stationselect;

import globalquake.database.Network;
import globalquake.database.Station;
import globalquake.database.StationDatabaseManager;
import globalquake.geo.GeoUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DistanceFilterAction extends AbstractAction {

    private final StationDatabaseManager stationDatabaseManager;
    private final Window parent;

    public DistanceFilterAction(StationDatabaseManager stationDatabaseManager, Window parent) {
        super("Apply Distance Filter");
        this.stationDatabaseManager=stationDatabaseManager;
        this.parent=parent;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String input = JOptionPane.showInputDialog(parent, "Enter distance in kilometers:", "Distance Input", JOptionPane.PLAIN_MESSAGE);
        double minDist = 10;
        if (input != null) { // Check if user clicked OK or Cancel
            try {
                minDist = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(parent, "Invalid input. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        minDist = Math.max(0.1, minDist);
        this.setEnabled(false);

        double finalMinDist = minDist;
        new Thread(() -> {
            try{
                runAlgorithm(finalMinDist);
            } finally {
                DistanceFilterAction.this.setEnabled(true);
            }
        }).start();

    }

    private void runAlgorithm(double minDist){
        stationDatabaseManager.getStationDatabase().getDatabaseWriteLock().lock();
        try{

            List<FilterChannel> selectedAvailableChannels = new ArrayList<>();
            for(Network network : stationDatabaseManager.getStationDatabase().getNetworks()){
                for(Station station: network.getStations()){
                    if(station.getSelectedChannel() != null && station.getSelectedChannel().isAvailable()){
                        selectedAvailableChannels.add(new FilterChannel(station));
                    }
                }
            }

            selectedAvailableChannels.parallelStream().forEach(filterChannel -> filterChannel.calculateClosestChannel(new ArrayList<>(selectedAvailableChannels)));
            selectedAvailableChannels.sort(Comparator.comparingDouble(FilterChannel::getClosestChannel));

            while(true) {
                boolean removed = false;
                for (Iterator<FilterChannel> iterator = selectedAvailableChannels.iterator(); iterator.hasNext(); ) {
                    FilterChannel filterChannel = iterator.next();

                    if (filterChannel.getClosestChannel() < minDist) {
                        filterChannel.getStation().setSelectedChannel(null);
                        iterator.remove();
                        removed = true;
                        for(int i = 0; i < 2; i++){
                            if(!iterator.hasNext()){
                                break;
                            }
                            iterator.next();
                        }
                    }
                }
                if(!removed){
                    break;
                }

                selectedAvailableChannels.parallelStream().filter(filterChannel -> filterChannel.getClosestChannel() <= minDist).forEach(filterChannel -> filterChannel.calculateClosestChannel(new ArrayList<>(selectedAvailableChannels)));
                selectedAvailableChannels.sort(Comparator.comparingDouble(FilterChannel::getClosestChannel));
            }

            stationDatabaseManager.fireUpdateEvent();
        }finally {
            stationDatabaseManager.getStationDatabase().getDatabaseWriteLock().unlock();
        }
    }

    private static class FilterChannel {
        private final Station station;
        private double closestChannel = 999999;

        public FilterChannel(Station station) {
            this.station = station;
        }

        public Station getStation() {
            return station;
        }

        public double getClosestChannel() {
            return closestChannel;
        }

        public void calculateClosestChannel(List<FilterChannel> filterChannels){
            double closest = 99999999;
            for(FilterChannel filterChannel : filterChannels){
                if(filterChannel.equals(this)){
                    continue;
                }
                double dist = GeoUtils.greatCircleDistance(station.getLatitude(), station.getLongitude(), filterChannel.getStation().getLatitude(), filterChannel.getStation().getLongitude());
                if(dist  < closest){
                    closest = dist;
                }
            }
            this.closestChannel = closest;
        }

    }
}
