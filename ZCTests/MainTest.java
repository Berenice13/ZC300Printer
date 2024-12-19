
public class MainTest {
    static String fullOrder = "0143EMB6246226133037269BEATRIZ ADRIANA PEREZ         RIVERA                        12/806246226133037269=80121014507269 486";

    public static void main(String[] args) {
        ZC300Printer printer = new ZC300Printer();

        boolean status = printer.isValidOrder(fullOrder);

        System.out.println("Entró a ZC300 Printer");
        System.out.println(status);

        
    }
}
