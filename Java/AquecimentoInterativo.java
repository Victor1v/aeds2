import java.util.*;

class AquecimentoInterativo {

   public static boolean testeMaiusculas(char c) {
      return (c >= 'A' && c <= 'Z');
   }

   public static boolean testeFim(String s) {
      return (s.length() == 3 && s.charAt(0) == 'F' && s.charAt(1) == 'I' && s.charAt(2) == 'M');
   }

   public static int contarMaiusculas(String s, int pos) {
      int resp = 0;
      if (pos < s.length()) {
         if (testeMaiusculas(s.charAt(pos)) == true) {
            resp = 1 + contarMaiusculas(s, pos + 1);
         } else {
            resp = contarMaiusculas(s, pos + 1);
         }
      }
      return resp;
   }

   public static void main(String[] args) {
      String[] entrada = new String[1000];
      int numEntrada = 0;

      do {
         entrada[numEntrada] = MyIO.readLine();
      } while (testeFim(entrada[numEntrada++]) == false);

      for (int i = 0; i < numEntrada-1; i++) {
         MyIO.println(contarMaiusculas(entrada[i], 0));
      }
   }
}