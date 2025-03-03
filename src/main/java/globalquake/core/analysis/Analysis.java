package globalquake.core.analysis;

import edu.sc.seis.seisFile.mseed.DataRecord;
import globalquake.core.station.AbstractStation;
import globalquake.core.earthquake.Event;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Analysis {
	private long lastRecord;
	private final AbstractStation station;
	private double sampleRate;
	private final List<Event> detectedEvents;
	public long numRecords;
	public long latestLogTime;
	public double _maxRatio;
	public boolean _maxRatioReset;
	public final Object previousLogsLock;
	private final ArrayList<Log> previousLogs;
	private AnalysisStatus status;
	
	public Analysis(AbstractStation station) {
		this.station = station;
		this.sampleRate = -1;
		detectedEvents = new CopyOnWriteArrayList<>();
		previousLogsLock = new Object();
		previousLogs = new ArrayList<>();
		status = AnalysisStatus.IDLE;
	}

	public long getLastRecord() {
		return lastRecord;
	}

	public AbstractStation getStation() {
		return station;
	}

	public void analyse(DataRecord dr) {
		if (sampleRate == -1) {
			sampleRate = dr.getSampleRate();
			reset();
		}
		long time = dr.getLastSampleBtime().toInstant().toEpochMilli();
        if (time >= lastRecord) {
            decode(dr);
            lastRecord = time;
        } // TODO ERROR BACKWARDS TIME
    }

	private void decode(DataRecord dataRecord) {
		long time = dataRecord.getStartBtime().toInstant().toEpochMilli();
		long gap = lastRecord != 0 ? (time - lastRecord) : -1;
		if (gap > getGapTreshold()) {
			reset();
		}
		int[] data;
		try {
			data = dataRecord.decompress().getAsInt();
			for (int v : data) {
				nextSample(v, time);
				time += (long) (1000 / getSampleRate());
			}
		} catch (Exception e) {
			System.err.println("Crash occurred at station " + getStation().getStationCode() + ", thread continues.");
			Logger.error(e);
        }
	}

	public abstract void nextSample(int v, long time);

	@SuppressWarnings("SameReturnValue")
	public abstract long getGapTreshold();

	public void reset() {
		station.reset();
	}

	public double getSampleRate() {
		return sampleRate;
	}

	public abstract void second();

	public List<Event> getDetectedEvents() {
		return detectedEvents;
	}

	public Event getLatestEvent() {
		if (detectedEvents.isEmpty()) {
			return null;
		} else {
			return detectedEvents.get(0);
		}
	}

	public long getNumRecords() {
		return numRecords;
	}
	
	public ArrayList<Log> getPreviousLogs() {
		return previousLogs;
	}
	
	public AnalysisStatus getStatus() {
		return status;
	}
	
	public void setStatus(AnalysisStatus status) {
		this.status = status;
	}
}
