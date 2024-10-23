package test;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.UsbConnection;
import com.zebra.sdk.common.card.printer.discovery.ZebraCardPrinterFilter;
import com.zebra.sdk.printer.discovery.DiscoveredUsbPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

public class UsbDiscovererExample {
    public static void main(String[] args) {
        try {
            // Descubrir impresoras USB
            for (DiscoveredUsbPrinter printer : UsbDiscoverer.getZebraUsbPrinters(new ZebraCardPrinterFilter())) {
                System.out.println("Discovered USB printer: " + printer.address);

                // Crear una conexión a la impresora
                UsbConnection connection = new UsbConnection(printer.address);

                // Abrir la conexión
                connection.open();

                // Verificar si la conexión se ha realizado correctamente
                if (connection.isConnected()) {
                    System.out.println("Successfully connected to the printer!");

                    // Aquí puedes realizar operaciones adicionales con la impresora, como imprimir
                } else {
                    System.out.println("Failed to connect to the printer.");
                }

                // Cerrar la conexión
                connection.close();
            }
        } catch (ConnectionException e) {
            System.out.println("Error discovering or connecting to USB printers: " + e.getMessage());
        }

        System.out.println("Done discovering USB printers");
    }


    
}
