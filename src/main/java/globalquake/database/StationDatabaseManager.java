package globalquake.database;

import globalquake.exception.FatalIOException;
import globalquake.main.Main;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StationDatabaseManager {

    private static final File STATIONS_FOLDER = new File(Main.MAIN_FOLDER, "/stationDatabase/");
    private StationDatabase stationDatabase;

    private final List<Runnable> updateListeners = new CopyOnWriteArrayList<>();

    private final List<Runnable> statusListeners = new CopyOnWriteArrayList<>();
    private boolean updating = false;

    public void load() throws FatalIOException{
        File file = getDatabaseFile();
        if (!file.getParentFile().exists()) {
            if(!file.getParentFile().mkdirs()){
               throw new FatalIOException("Unable to create database file directory!", null);
            }
        }

        if(file.exists()){
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                stationDatabase = (StationDatabase) in.readObject();
                in.close();

                System.out.println("Load successfull");
            } catch (ClassNotFoundException | IOException e) {
                Main.getErrorHandler().handleException(new FatalIOException("Unable to read station database!", e));
            }
        }

        if(stationDatabase == null){
            System.out.println("A new database created");
            stationDatabase = new StationDatabase();
        }

    }

    public void save() throws FatalIOException{
        File file = getDatabaseFile();
        if (!file.getParentFile().exists()) {
            if(!file.getParentFile().mkdirs()){
                throw new FatalIOException("Unable to create database file directory!", null);
            }
        }

        if(stationDatabase == null){
            return;
        }

        stationDatabase.getDatabaseReadLock().lock();
        try{
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(stationDatabase);
            out.close();
        }catch(IOException e){
            throw new FatalIOException("Unable to save station database!", e);
        } finally {
            stationDatabase.getDatabaseReadLock().unlock();
        }
    }

    public void addUpdateListener(Runnable runnable){
        this.updateListeners.add(runnable);
    }

    public void addStatusListener(Runnable runnable){
        this.statusListeners.add(runnable);
    }

    public void fireUpdateEvent(){
        for(Runnable runnable: updateListeners){
            runnable.run();
        }
    }

    private void fireStatusChangeEvent(){
        for(Runnable runnable: statusListeners){
            runnable.run();
        }
    }

    public void runUpdate(List<StationSource> toBeUpdated, Runnable onFinish) {
        this.updating = true;
        fireStatusChangeEvent();

        new Thread(() -> {
            toBeUpdated.parallelStream().forEach(stationSource -> {
                stationSource.getStatus().setString("Updating...");
                try {
                    List<Network> networkList = FDSNWSDownloader.downloadFDSNWS(stationSource);
                    stationSource.getStatus().setString("Updating database...");
                    StationDatabaseManager.this.acceptNetworks(networkList);
                    stationSource.getStatus().setString("Done");
                    stationSource.getStatus().setValue(100);
                    stationSource.setLastUpdate(LocalDateTime.now());
                } catch (Exception e) {
                    stationSource.getStatus().setString("Error!");
                    stationSource.getStatus().setValue(0);
                }finally {
                    fireUpdateEvent();
                }
            });

            this.updating = false;
            fireStatusChangeEvent();
            if (onFinish != null) {
                onFinish.run();
            }
        }).start();
    }

    private void acceptNetworks(List<Network> networkList) {
        stationDatabase.getDatabaseWriteLock().lock();
        try{
            for(Network network:networkList){
                for(Station station: network.getStations()){
                    for(Channel channel:station.getChannels()){
                        stationDatabase.acceptChannel(network, station, channel);
                    }
                }
            }
        }finally {
            System.out.println(stationDatabase.getNetworks().size());
            stationDatabase.getDatabaseWriteLock().unlock();
        }
    }

    private static File getDatabaseFile() {
        return new File(STATIONS_FOLDER, "database.dat");
    }

    public StationDatabase getStationDatabase() {
        return stationDatabase;
    }

    public void runAvailabilityCheck(List<SeedlinkNetwork> toBeUpdated, Runnable onFinish) {
        this.updating = true;
        fireStatusChangeEvent();
        new Thread(() -> {
            toBeUpdated.parallelStream().forEach(seedlinkNetwork -> {
                        try {
                            SeedlinkCommunicator.runAvailabilityCheck(seedlinkNetwork, stationDatabase);
                        } catch (Exception e) {
                            seedlinkNetwork.getStatus().setString("Error!");
                            seedlinkNetwork.getStatus().setValue(0);
                        }finally {
                            fireUpdateEvent();
                        }
                    }
            );
            this.updating = false;
            fireStatusChangeEvent();
            if (onFinish != null) {
                onFinish.run();
            }
        }).start();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isUpdating() {
        return updating;
    }

    public void removeAllSeedlinks(List<SeedlinkNetwork> toBeRemoved) {
        for(Network network: getStationDatabase().getNetworks()){
            for(Station station: network.getStations()){
                for(Channel channel: station.getChannels()){
                    toBeRemoved.forEach(channel.getSeedlinkNetworks()::remove);
                }
            }
        }
        getStationDatabase().getSeedlinkNetworks().removeAll(toBeRemoved);
        fireUpdateEvent();
    }

    public void removeAllStationSources(List<StationSource> toBeRemoved) {
        for (Iterator<Network> networkIterator = getStationDatabase().getNetworks().iterator(); networkIterator.hasNext(); ) {
            Network network = networkIterator.next();
            for(Iterator<Station> stationIterator = network.getStations().iterator(); stationIterator.hasNext(); ){
                Station station = stationIterator.next();
                for (Iterator<Channel> channelIterator = station.getChannels().iterator(); channelIterator.hasNext(); ) {
                    Channel channel = channelIterator.next();
                    toBeRemoved.forEach(channel.getStationSources()::remove);
                    if (channel.getStationSources().isEmpty()) {
                        channelIterator.remove();
                    }
                }
                if(station.getChannels().isEmpty()){
                    stationIterator.remove();
                } else if(station.getSelectedChannel() != null){
                    if(!station.getChannels().contains(station.getSelectedChannel())){
                        station.selectBestAvailableChannel();
                    }
                }
            }
            if(network.getStations().isEmpty()){
                networkIterator.remove();
            }
        }

        getStationDatabase().getStationSources().removeAll(toBeRemoved);

        fireUpdateEvent();
    }
}
