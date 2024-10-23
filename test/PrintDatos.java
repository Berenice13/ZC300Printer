package test;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.containers.GraphicsInfo;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.enumerations.CardSide;
import com.zebra.sdk.common.card.enumerations.GraphicType;
import com.zebra.sdk.common.card.enumerations.OrientationType;
import com.zebra.sdk.common.card.enumerations.PrintType;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.graphics.ZebraCardGraphics;
import com.zebra.sdk.common.card.graphics.ZebraGraphics;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.common.card.printer.discovery.ZebraCardPrinterFilter;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

public class PrintDatos {

    public static void main(String[] args) {
        Connection connection = null;
        ZebraCardPrinter zebraCardPrinter = null;
        DiscoveredPrinter[] printers = null;
        ZebraGraphics graphics = null;

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

            List<GraphicsInfo> graphicsData = new ArrayList<GraphicsInfo>();
            graphics = new ZebraCardGraphics(zebraCardPrinter);

            generatePrintJobImage(graphics, graphicsData);

            int jobId = zebraCardPrinter.print(1, graphicsData);
            pollJobStatus(zebraCardPrinter, jobId);

            JobStatusInfo jStatus = zebraCardPrinter.getJobStatus(jobId);
            System.out.println("Job complete: " + jStatus.printStatus);

        } catch (ConnectionException e) {
            System.out.println("Error discovering local printers: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanUp(connection, zebraCardPrinter, graphics);
        }
    }

    private static void generatePrintJobImage(ZebraGraphics graphics, List<GraphicsInfo> graphicsData) throws IOException {
        GraphicsInfo grInfo;

        // Inicializar gráfico para el frente de la tarjeta con dimensiones adecuadas
        int width = 1016;
        int height = 640;
        graphics.initialize(width, height, OrientationType.Landscape, PrintType.MonoK, Color.WHITE);

        // Imprimir datos en la parte frontal
        grInfo = new GraphicsInfo();
        grInfo.side = CardSide.Front;
        grInfo.printType = PrintType.MonoK;
        grInfo.graphicType = GraphicType.NA; // Se usará para texto

        // Dibujar texto en la tarjeta
        String nombreTitular = "BERENICE DE LA CRUZ";
        String numeroTarjeta = "1234 5678 9012 3456";
        String fechaVencimiento = "12/26";

        int x = 50;
        int y = 100;

        // Verificar las dimensiones del área de dibujo
        System.out.println("Drawing area dimensions: " + width + "x" + height);

        // Dibujar el texto en negro sobre un fondo blanco
        graphics.drawText(nombreTitular, x, y, Color.BLACK); // Nombre del titular
        graphics.drawText(numeroTarjeta, x, y + 40, Color.BLACK); // Número de tarjeta
        graphics.drawText(fechaVencimiento, x, y + 80, Color.BLACK); // Fecha de vencimiento

        grInfo.graphicData = graphics.createImage(null);
        graphicsData.add(grInfo);
    }

    static boolean pollJobStatus(ZebraCardPrinter device, int actionID) throws ConnectionException, ZebraCardException, ZebraIllegalArgumentException {
        boolean success = false;
        long dropDeadTime = System.currentTimeMillis() + 40000;
        long pollInterval = 500;

        // Poll job status
        JobStatusInfo jStatus = null;

        do {
            jStatus = device.getJobStatus(actionID);
            System.out.println(String.format("Job %d, Status:%s, Card Position:%s, " + "ReadyForNextJob:%s, Mag Status:%s, Contact Status:%s, Contactless Status:%s, "
                    + "Error Code:%d, Alarm Code:%d", actionID, jStatus.printStatus, jStatus.cardPosition, jStatus.readyForNextJob, jStatus.magneticEncoding, jStatus.contactSmartCard, jStatus.contactlessSmartCard, jStatus.errorInfo.value, jStatus.alarmInfo.value));

            if (jStatus.contactSmartCard.contains("station")) {
                success = true;
                break;
            } else if (jStatus.contactlessSmartCard.contains("station")) {
                success = true;
                break;
            } else if (jStatus.printStatus.contains("done_ok")) {
                success = true;
                break;
            } else if (jStatus.printStatus.contains("alarm_handling")) {
                System.out.println("Error Dectected: " + jStatus.alarmInfo.description);
            } else if (jStatus.printStatus.contains("error") || jStatus.printStatus.contains("cancelled")) {
                success = false;
                break;
            }

            if (System.currentTimeMillis() > dropDeadTime) {
                success = false;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (true);

        return success;
    }

    protected static void cleanUp(Connection connection, ZebraCardPrinter genericPrinter, ZebraGraphics graphics) {
        try {
            if (genericPrinter != null) {
                genericPrinter.destroy();
                genericPrinter = null;
            }
        } catch (ZebraCardException e) {
            e.printStackTrace();
        }

        if (graphics != null) {
            graphics.close();
            graphics = null;
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