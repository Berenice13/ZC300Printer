public class MainTest {
    static String fullOrder = "0143ZCP1234567890123456BERENICE DE LA CRUZ           DE LA CRUZ                    12/261234567890123456=80121016667269 4860100010030359100";

    public static void main(String[] args) {
        ZC300Printer printer = new ZC300Printer();

        String status = printer.isValidOrder(fullOrder);

        System.out.println("Entro a ZC300 Printer");
        System.out.println(status);
    }

}
