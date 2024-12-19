import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.TcpConnection;
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


@SuppressWarnings("unused")
public class ZC300Printer {
    //Comando que se recibe para identificar la conexion con la impresora
    private String ZCPCommand = "ZCP";
    private int lengthInnerMessage;
    private String innerCommand = "";

    private String answerToECR = "";
    private String answerFromPrinter = "";

    private String TAR1 = "";
    private String NAM1 = "";
    private String NAM2 = "";
    private String VIG1 = "";
    private String TRK1 = ""; 
    private String TR01 = "";
    
    private int[] octeto = new int[4];
    private String host = "";
    private int port;
    
    //inicializar los objetos de los logs 
    /*
    private SimpleLogger simpleLogger;
    private FileLogger logTrace;
    
    public ZC300Printer(SimpleLogger simpleLogger, FileLogger logTrace) {
        this.simpleLogger = simpleLogger;
        this.logTrace = logTrace;
    } 
    */
    
    /**
     * 
     * 
     * @param command
     * @return True: Si es una orden válida. <br/>
     *         False: Si no es una orden válida.
     * 
     */


    //EJEMPLO DE COMO LLEGA 
    //   0143EMB6246226133037269BEATRIZ ADRIANA PEREZ         RIVERA                        12/806246226133037269=80121014507269 486
    public String isValidOrder(String command) {
        //this.simpleLogger.printStream("ECR-->Java: " + command); 
        this.lengthInnerMessage = Integer.parseInt(command.substring(0, 4));
        this.innerCommand = command.substring(4, 7);
        
        int com1 = Integer.parseInt(command.substring(123, 126));
        int com2 = Integer.parseInt(command.substring(126, 129));
        int com3 = Integer.parseInt(command.substring(129, 132));
        int com4 = Integer.parseInt(command.substring(132, 135));

        this.host = com1 + "." + com2 + "." + com3 + "." + com4;
        this.port = 9100;

        if (this.innerCommand.equals(ZCPCommand)) {
            Connection connection = null;
            ZebraCardPrinter zebraCardPrinter = null;
            DiscoveredPrinter[] printers = null;

            long tiempoInicial = System.currentTimeMillis();
            boolean status = true;

            try {
                /* ------------------------------------------------------------
                    Establece la conexión con la impresora conectada por IP
                ------------------------------------------------------------ */
                connection = new TcpConnection(this.host, this.port);
                connection.open();
                zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);
                
                
                /* ------------------------------------------------------------
                    Establece la conexión con la impresora conectada por USB
                ------------------------------------------------------------ */
                //printers = UsbDiscoverer.getZebraUsbPrinters(new ZebraCardPrinterFilter());
                
                //Si hay al menos una impresora conectada se abre la conexión
                /*if (printers.length > 0) {
                    connection = printers[0].getConnection();
                    connection.open();
                    zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);
                }*/
            

                //Si no se encontró impresora conectada se muestra un mensaje y se retorna false
                if (zebraCardPrinter == null) {
                    System.out.println("No se encontraron impresoras conectadas");
                    return "No hay impresoras";
                }
    
                //validamos si la impresora es capaz de coficiar la banda magnetica
                if (zebraCardPrinter.hasMagneticEncoder()) {
                    ZebraTemplate zebraCardTemplate = new ZebraCardTemplate(zebraCardPrinter);

                    //Obtenemos el XML
                    String templateData = getTemplateData();
                    List<String> templateFields = zebraCardTemplate.getTemplateDataFields(templateData);

                    // Se envian los datos para llenar los campos de la plantilla
                    Map<String, String> fieldData = this.populateTemplateFieldData(templateFields, command);
        
                    // Generate template and send job
                    TemplateJob templateJob = zebraCardTemplate.generateTemplateDataJob(templateData, fieldData);
                    int jobId = zebraCardPrinter.printTemplate(1, templateJob);
        
                    // Poll job status
                    JobStatusInfo jobStatusInfo = pollJobStatus(jobId, zebraCardPrinter);
                    System.out.println(String.format(Locale.US, "Job %d completed with status '%s'", jobId, jobStatusInfo.printStatus));
                } else{
                    System.out.println("El dispositivo no tiene la opción de codificación.");
                    return "El dispositivo no tiene la opción de codificación.";
                }
                
                connection.close();
            } catch (ConnectionException e) {
                //simpleLogger.printStream(e.toString());
                //simpleLogger.printStream("", e);
                e.printStackTrace();
                System.out.println("Error discovering local printers: " + e.getMessage());
            } catch (Exception e) {
                //simpleLogger.printStream(e.toString());
                //simpleLogger.printStream("", e);
                e.printStackTrace();
                System.out.println("Error printing template: " + e.getMessage());
                e.printStackTrace();
            } finally {
                cleanUpQuietly(connection, zebraCardPrinter);

                // Se calcula el tiempo que tardó el proceso de impresion.
                long tiempoFinal = System.currentTimeMillis();
                long tiempoTranscurrido = tiempoFinal - tiempoInicial;

                long hora = tiempoTranscurrido / 3600000;
                long restohora = tiempoTranscurrido % 3600000;
                long minuto = restohora / 60000;
                long restominuto = restohora % 60000;
                long segundo = restominuto / 1000;
                long restosegundo = restominuto % 1000;

                //simpleLogger.printStream("------> Tiempo de impresión: " + hora + ":" + minuto + ":" + segundo + "." + restosegundo + " <------");
            }

            return "exito";
        }

        return "fallo";
    }

    //XML de la plantilla de la tarjeta
    private static String getTemplateData() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" 
            + "<template name=\"CIMACO\" card_type=\"2\" card_thickness=\"30\" delete=\"no\">\n" 
            + "  <fonts>\n" 
            + "    <font id=\"1\" name=\"Luxi Mono\" size=\"15\" bold=\"yes\" italic=\"no\" underline=\"no\"/>\n"  // Aumentar tamaño de fuente
            + "    <font id=\"2\" name=\"Luxi Mono\" size=\"11\" bold=\"yes\" italic=\"no\" underline=\"no\"/>\n"
            + "    <font id=\"3\" name=\"Luxi Mono\" size=\"10\" bold=\"yes\" italic=\"no\" underline=\"no\"/>\n"
            + "  </fonts>\n" 
            + "  <sides>\n" 

            //Parte frontal
            + "    <side name=\"front\" orientation=\"landscape\" rotation=\"0\" k_mode=\"text\">\n" 
            + "      <print_types>\n" 
            + "        <print_type type=\"mono\">\n" 
            + "          <text field=\"cardNumber\" font_id=\"1\" x=\"130\" y=\"350\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\" alignment=\"center\"/>\n"  // Número de tarjeta
            + "          <text field=\"name\" font_id=\"2\" x=\"150\" y=\"450\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\"/>\n" // Primera parte del nombre
            + "          <text field=\"name2\" font_id=\"2\" x=\"180\" y=\"510\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\"/>\n" // Segunda parte del nombre
            + "        </print_type>\n" 
            + "      </print_types>\n" 
            + "    </side>\n" 
            
            //Parte trasera
            + "    <side name=\"back\" orientation=\"landscape\" rotation=\"0\" k_mode=\"text\">\n" 
            + "      <print_types>\n" 
            + "        <print_type type=\"mono\">\n" 
            + "          <text field=\"securityCode\" font_id=\"3\" x=\"700\" y=\"355\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\"/>\n" // Numero de seguridad
            //+ "          <text field=\"expirationDate\" font_id=\"3\" x=\"800\" y=\"400\" width=\"0\" height=\"0\" angle=\"0\" color=\"0x000000\"/>\n" //fecha de vencimiento
            + "        </print_type>\n" 
            + "      </print_types>\n" 

            // Codificacion de la banda magnetica
            
            + "      <magdata format=\"iso\" coercivity=\"high\" verify=\"ascii\">\n" 
            + "        <track field=\"track1Data\" number=\"1\" format=\"ascii\"/>\n" 
            + "        <track field=\"track2Data\" number=\"2\" format=\"ascii\"/>\n" 
            + "        <track field=\"track3Data\" number=\"3\" format=\"ascii\"/>\n" 
            + "      </magdata>"
           
            + "    </side>\n"  
            + "  </sides>\n" 
            + "</template>";
    }

    //Llena los campos de la plantilla con los datos de la tarjeta
    private Map<String, String> populateTemplateFieldData(List<String> templateFields, String command) {
        Map<String, String> fieldData = new HashMap<String, String>();
        for (String fieldName : templateFields) {
            String fieldValue = "";

            TAR1 = command.substring(7, 23);    //NUMERO DE TARJETA
            NAM1 = command.substring(23, 53);   //PRIMERA PARTE DEL NOMBRE
            NAM2 = command.substring(53, 83);   //PRIMERA PARTE DEL NOMBRE
            VIG1 = command.substring(83, 88);   //VIGENCIA
            TRK1 = command.substring(88, 115);  //CONTENIDO DE LA BANDA MAGNETICA
            TR01 = command.substring(115, 123); //CODIGO DE ATRAS 

            String TRACK1 = "B " + NAM1.substring(0, 25);
            String TRACK2 = TRK1;
            String TRACK3 = "";

            NAM1 = NAM1.toUpperCase(); 
            NAM2 = NAM2.toUpperCase();

            if (fieldName.equals("cardNumber")) {
                fieldValue = formatCardNumber(TAR1);
                //fieldValue = "";
            } else if (fieldName.equals("name")) {
                fieldValue = centerText(NAM1, 30); 
                //fieldValue = "";
            } else if (fieldName.equals("name2")) {
                fieldValue = centerText(NAM2, 30); 
                //fieldValue = "";
            } else if (fieldName.equals("securityCode")) {
                fieldValue = TR01;
                //fieldValue = "";
            } else if (fieldName.equals("expirationDate")) {
                fieldValue = VIG1;
                //fieldValue = "";
            } 
            else if (fieldName.equals("track1Data")) {
                fieldValue = TRACK1;
            } else if (fieldName.equals("track2Data")) {
                fieldValue = TRACK2;
            } else if (fieldName.equals("track3Data")) {
                fieldValue = TRACK3;
            }

            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!fieldData.containsKey(fieldName)) {
                    fieldData.put(fieldName, fieldValue);
                }
            }
        }
        return fieldData;
    }

    // Formatea el número de tarjeta para que se muestre con espacios cada 4 dígitos
    private static String formatCardNumber(String cardNumber) {
        return cardNumber.replaceAll("(.{4})", "$1 ").trim();
    }
    
    // Ajusta el texto para que se alinee al centro 
    private static String centerText(String text, int totalLength) {
        // Elimina los espacios en blanco al final del texto
        text = text.trim();
    
        if (text.length() >= totalLength) {
            return text;
        }
    
        // Calcula los espacios que deben añadirse a cada lado
        int paddingTotal = totalLength - text.length();
        int paddingLeft = paddingTotal / 2;
        int paddingRight = paddingTotal - paddingLeft;
    
        // Añade espacios a la izquierda y a la derecha
        String format = "%" + paddingLeft + "s%s%" + paddingRight + "s";
        return String.format(format, "", text, "");
    }

    // Verifica el estado del trabajo de impresión 
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

    //Cierra la conexión de la impresora
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
    
