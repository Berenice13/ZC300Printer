package test;

import java.util.Locale;

import com.zebra.sdk.comm.*;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.jobSettings.ZebraCardJobSettingNames;
import com.zebra.sdk.common.card.printer.*;
import com.zebra.sdk.device.ZebraIllegalArgumentException;

public class MagneticEncodeExample {

	public static void main(String[] args) {
		Connection connection = null;
		ZebraCardPrinter zebraCardPrinter = null;

		try {
			connection = new TcpConnection("1.2.3.4", 9100);
			connection.open();

			zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);
 
			if (zebraCardPrinter.hasMagneticEncoder()) {
				// Configure magnetic encoding settings
				zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.MAG_ENCODING_TYPE, "ISO"); // ISO=default
				zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.MAG_COERCIVITY, "High"); // High=default
				zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.MAG_VERIFY, "yes"); // yes=default

				// Send job
				int jobId = zebraCardPrinter.magEncode(1, "Zebra Technologies", "2222222222", "3333333333");

				// Poll job status
				JobStatusInfo jobStatusInfo = pollJobStatus(jobId, zebraCardPrinter);
				System.out.println(String.format(Locale.US, "Job %d completed with status '%s'", jobId, jobStatusInfo.printStatus));
			} else {
				System.out.println("No magnetic encoder installed.");
			}
		} catch (Exception e) {
			System.out.println("Error encoding magnetic card: " + e.getMessage());
		} finally {
			cleanUpQuietly(connection, zebraCardPrinter);
		}
	}

	private static JobStatusInfo pollJobStatus(int jobId, ZebraCardPrinter zebraCardPrinter) throws ConnectionException, ZebraCardException, ZebraIllegalArgumentException {
		long dropDeadTime = System.currentTimeMillis() + 40000;
		long pollInterval = 500;

		// Poll job status
		JobStatusInfo jobStatusInfo = new JobStatusInfo();

		do {
			jobStatusInfo = zebraCardPrinter.getJobStatus(jobId);

			String alarmDesc = jobStatusInfo.alarmInfo.value > 0 ? String.format(Locale.US, " (%s)", jobStatusInfo.alarmInfo.description) : "";
			String errorDesc = jobStatusInfo.errorInfo.value > 0 ? String.format(Locale.US, " (%s)", jobStatusInfo.errorInfo.description) : "";

			System.out.println(String.format("Job %d, Status:%s, Card Position:%s, Mag Status:%s, Alarm Code:%d%s, Error Code:%d%s", jobId, jobStatusInfo.printStatus, jobStatusInfo.cardPosition, jobStatusInfo.magneticEncoding, jobStatusInfo.alarmInfo.value, alarmDesc,
					jobStatusInfo.errorInfo.value, errorDesc));

			if (jobStatusInfo.printStatus.contains("done_ok")) {
				break;
			} else if (jobStatusInfo.printStatus.contains("alarm_handling")) {
				System.out.println("Alarm Detected: " + jobStatusInfo.alarmInfo.description);
			} else if (jobStatusInfo.printStatus.contains("error") || jobStatusInfo.printStatus.contains("cancelled")) {
				break;
			} else if (jobStatusInfo.errorInfo.value > 0) {
				System.out.println(String.format(Locale.US, "The job encountered an error [%s] and was cancelled.", jobStatusInfo.errorInfo.description));
				zebraCardPrinter.cancel(jobId);
			}

			if (System.currentTimeMillis() > dropDeadTime) {
				break;
			}

			try {
				Thread.sleep(pollInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} while (true);

		return jobStatusInfo;
	}

	private static void cleanUpQuietly(Connection connection, ZebraCardPrinter genericPrinter) {
		try {
			if (genericPrinter != null) {
				genericPrinter.destroy();
				genericPrinter = null;
			}
		} catch (ZebraCardException e) {
			e.printStackTrace();
		}

		if (connection != null) {
			try {
				connection.close();
				connection = null;
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
		}
	}
}