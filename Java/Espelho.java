import java.util.*;

public class Espelho {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextInt()) {
            int inicio = sc.nextInt();
            int fim = sc.nextInt();

            StringBuilder sequencia = new StringBuilder();
            for (int i = inicio; i <= fim; i++) {
                sequencia.append(i);
            }

            System.out.println(sequencia.toString() + sequencia.reverse().toString());
        }
        sc.close();
    }
}
