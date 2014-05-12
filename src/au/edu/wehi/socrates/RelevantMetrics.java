package au.edu.wehi.socrates;

import java.io.File;
import java.util.List;

import net.sf.picard.analysis.CollectInsertSizeMetrics;
import net.sf.picard.analysis.InsertSizeMetrics;
import net.sf.picard.analysis.MetricAccumulationLevel;
import net.sf.picard.analysis.directed.InsertSizeMetricsCollector;
import net.sf.picard.metrics.MetricBase;
import net.sf.picard.metrics.MetricsFile;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.util.CollectionUtil;

import com.google.common.collect.ImmutableList;

public class RelevantMetrics {
	private Log log = Log.getInstance(RelevantMetrics.class);
	private InsertSizeMetrics insertSize = null;
	/**
	 * Creates a metric collector to record metrics required by Socrates
	 * @param header SAM header of file to process
	 * @return metric collector
	 */
	public static InsertSizeMetricsCollector createCollector(
			SAMFileHeader header) {
		List<SAMReadGroupRecord> rg = ImmutableList.<SAMReadGroupRecord>of();
		if (header != null) {
			rg = header.getReadGroups();
		}
		return new InsertSizeMetricsCollector(
    			CollectionUtil.makeSet(MetricAccumulationLevel.ALL_READS, MetricAccumulationLevel.SAMPLE),
    			rg,
				// match CollectInsertSizeMetrics defaults
				 new CollectInsertSizeMetrics().MINIMUM_PCT,
				 new CollectInsertSizeMetrics().HISTOGRAM_WIDTH,
				 new CollectInsertSizeMetrics().DEVIATIONS);
	}
	public static void save(
			InsertSizeMetricsCollector metrics,
			MetricsFile<InsertSizeMetrics, Integer> metricsFile,
			File file) {
		metrics.finish();
		metrics.addAllLevelsToFile(metricsFile);
		metricsFile.write(file);
	}
	public RelevantMetrics(File file) {
		for (MetricBase metric : MetricsFile.readBeans(file)) {
			if (metric.getClass() == InsertSizeMetrics.class) {
				InsertSizeMetrics m = (InsertSizeMetrics)metric;
				if (m.SAMPLE == null &&
					m.LIBRARY == null &&
					m.READ_GROUP == null) {
					insertSize = m;
				}
			}
		}
		if (insertSize == null) {
			insertSize = new InsertSizeMetrics();
			log.error(String.format("No pair-end insert size metrics found in %s.", file));
		}
	}
	protected RelevantMetrics() {
		insertSize = new InsertSizeMetrics();
	}
	/**
	 * Gets the median fragment size
	 * @return median fragment size
	 */
	public double getMedianFragmentSize() {
		// TODO: is this 5' difference or frag size?
		return insertSize.MEDIAN_INSERT_SIZE;
	}
	/**
	 * Gets the standard deviation of the fragment size
	 * @return fragment size standard deviation
	 */
	public double getFragmentSizeStdDev() {
		return 1.4826 * insertSize.MEDIAN_ABSOLUTE_DEVIATION;
	}
	/**
	 * Gets the maximum expected fragment size
	 * @return longest expected fragment size
	 */
	public int getMaxFragmentSize() {
		// TODO: is this 5' difference or frag size?
		return (int)Math.ceil(getMedianFragmentSize() + 3 * getFragmentSizeStdDev());
	}
}