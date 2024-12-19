package test;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.containers.TemplateJob;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.job.template.ZebraTemplate;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.common.card.printer.discovery.ZebraCardPrinterFilter;
import com.zebra.sdk.common.card.template.ZebraCardTemplate;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

public class PrintTemplateExample {

	public static void main(String[] args) {
		Connection connection = null;
		ZebraCardPrinter zebraCardPrinter = null;
		DiscoveredPrinter[] printers = null;

		try {
			printers = UsbDiscoverer.getZebraUsbPrinters(new ZebraCardPrinterFilter());
			if (printers.length > 0) {
				connection = printers[0].getConnection();
				connection.open();
				zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);
			}

			if (zebraCardPrinter == null) {
				return;
			}

			ZebraTemplate zebraCardTemplate = new ZebraCardTemplate(zebraCardPrinter);

			String templateData = getTemplateData();
			List<String> templateFields = zebraCardTemplate.getTemplateDataFields(templateData);
			Map<String, String> fieldData = populateTemplateFieldData(templateFields);

			// Generate template job
			TemplateJob templateJob = zebraCardTemplate.generateTemplateDataJob(templateData, fieldData);

			// Send job
			int jobId = zebraCardPrinter.printTemplate(1, templateJob);

			// Poll job status
			JobStatusInfo jobStatusInfo = pollJobStatus(jobId, zebraCardPrinter);
			System.out.println(String.format(Locale.US, "Job %d completed with status '%s'", jobId, jobStatusInfo.printStatus));
		} catch (ConnectionException e) {
			System.out.println("Error discovering local printers: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error printing template: " + e.getMessage());
			e.printStackTrace();
		} finally {
			cleanUpQuietly(connection, zebraCardPrinter);
		}
	}

	private static String getTemplateData() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
			+ "<template name=\"TemplateExample\" card_type=\"2\" card_thickness=\"30\" delete=\"no\">\n" 
			+ "  <fonts>\n" 
		   	+ "    <font id=\"1\" name=\"OCR A Extended\" size=\"17\" bold=\"yes\" italic=\"no\" underline=\"no\"/>\n"  // Aumentar tamaño de fuente
			+ "    <font id=\"2\" name=\"OCR A Extended\" size=\"13\" bold=\"yes\" italic=\"no\" underline=\"no\"/>\n" 
			+ "  </fonts>\n" 
			+ "  <sides>\n" 
			+ "    <side name=\"front\" orientation=\"landscape\" rotation=\"0\" k_mode=\"text\">\n" 
			+ "      <print_types>\n" 
			+ "        <print_type type=\"mono\">\n" 
			+ "          <text field=\"cardNumber\" font_id=\"1\" x=\"50\" y=\"350\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\" alignment=\"center\"/>\n"  // Número de tarjeta
			+ "          <text field=\"name\" font_id=\"2\" x=\"60\" y=\"450\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\"/>\n" // Nombre completo
			+ "          <text field=\"expiryDate\" font_id=\"2\" x=\"700\" y=\"530\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\"/>\n"  // Fecha de expiración
			+ "        </print_type>\n" 
			+ "      </print_types>\n" 
			+ "    </side>\n" 
			+ "  </sides>\n" 
			+ "</template>";
	}

	@SuppressWarnings("unused")
	private static Map<String, String> populateTemplateFieldData(List<String> templateFields) {
		Map<String, String> fieldData = new HashMap<String, String>();
		for (String fieldName : templateFields) {
			String fieldValue = "";

			String TAR1 = "1234567890123456";
			String NAM1 = "Berenice DE LA cruz";
			String NAM2 = "DE LA cruz";
			String VIG1 = "12/26";
			String TRK1 = "1234567890120000=8012101450";
			String TR01 = "7269 486";

			NAM1 = NAM1.toUpperCase(); 
			NAM2 = NAM2.toUpperCase();


			if (fieldName.equals("cardNumber")) {
				fieldValue = formatCardNumber(TAR1);
			} else if (fieldName.equals("name")) {
				fieldValue = centerText(NAM1, 25); 
			} else if (fieldName.equals("expiryDate")) {
				fieldValue = VIG1;
			}
	
			if (fieldValue != null && !fieldValue.isEmpty()) {
				if (!fieldData.containsKey(fieldName)) {
					fieldData.put(fieldName, fieldValue);
				}
			}
		}
		return fieldData;
	}

	private static String formatCardNumber(String cardNumber) {
		// Utiliza una expresión regular para insertar un espacio cada 4 dígitos
		return cardNumber.replaceAll("(.{4})", "$1 ").trim();
	}
	

	private static String centerText(String text, int totalLength) {
		if (text.length() >= totalLength) {
			return text;
		}
		System.out.println("text length = " + text.length());
		
		// Calcula los espacios que deben añadirse a cada lado
		int paddingTotal = totalLength - text.length();
		int paddingLeft = paddingTotal / 2;
		int paddingRight = paddingTotal - paddingLeft;
	
		// Añade espacios a la izquierda y a la derecha
		String format = "%" + paddingLeft + "s%s%" + paddingRight + "s";
		return String.format(format, "", text, "");
	}

	/*private static String centerText(String text, int totalLength) {
		if (text.length() >= totalLength) {
			return text;
		}
	
		// Calcula los puntos que deben añadirse a cada lado
		int paddingTotal = totalLength - text.length();
		int paddingLeft = paddingTotal / 2;
		int paddingRight = paddingTotal - paddingLeft;
	
		// Muestra la cantidad de puntos a la izquierda y derecha
		System.out.println("Puntos a la izquierda: " + paddingLeft);
		System.out.println("Puntos a la derecha: " + paddingRight);
	
		// Añade puntos a la izquierda y a la derecha
		String format = "%" + paddingLeft + "s%s%" + paddingRight + "s";
		return String.format(format, repeat(".", paddingLeft), text, repeat(".", paddingRight));
	}

	private static String repeat(String str, int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			builder.append(str);
		}
		return builder.toString();
	}*/
	

	private static JobStatusInfo pollJobStatus(int jobId, ZebraCardPrinter zebraCardPrinter) throws ConnectionException, ZebraCardException, ZebraIllegalArgumentException {
		long dropDeadTime = System.currentTimeMillis() + 40000;
		long pollInterval = 500;

		// Poll job status
		JobStatusInfo jobStatusInfo = new JobStatusInfo();

		do {
			jobStatusInfo = zebraCardPrinter.getJobStatus(jobId);

			String alarmDesc = jobStatusInfo.alarmInfo.value > 0 ? String.format(Locale.US, " (%s)", jobStatusInfo.alarmInfo.description) : "";
			String errorDesc = jobStatusInfo.errorInfo.value > 0 ? String.format(Locale.US, " (%s)", jobStatusInfo.errorInfo.description) : "";

			System.out.println(String.format("Job %d, Status:%s, Card Position:%s, Alarm Code:%d%s, Error Code:%d%s", jobId, jobStatusInfo.printStatus, jobStatusInfo.cardPosition, jobStatusInfo.alarmInfo.value, alarmDesc, jobStatusInfo.errorInfo.value, errorDesc));

			if (jobStatusInfo.printStatus.contains("done_ok")) {
				break;
			} else if (jobStatusInfo.printStatus.contains("alarm_handling")) {
				System.out.println("Alarm Dectected: " + jobStatusInfo.alarmInfo.description);
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
