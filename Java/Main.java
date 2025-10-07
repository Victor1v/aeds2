import java.io.*;
import java.util.*;

public class Main {

    // ------------------------- Leitor rápido -------------------------
    static class FastScanner {
        private final InputStream in;
        private final byte[] buffer = new byte[1 << 16];
        private int ptr = 0, len = 0;

        FastScanner(InputStream is) { in = is; }

        private int read() throws IOException {
            if (ptr >= len) {
                len = in.read(buffer);
                ptr = 0;
                if (len <= 0) return -1;
            }
            return buffer[ptr++];
        }

        Integer nextIntOrNull() throws IOException {
            int c, sgn = 1, x = 0;

            // pular espaços
            do {
                c = read();
                if (c == -1) return null; // EOF antes de achar dígito
            } while (c <= ' ');

            // sinal?
            if (c == '-') { sgn = -1; c = read(); }

            // ler dígitos
            while (c > ' ') {
                if (c < '0' || c > '9') throw new IOException("Entrada inválida");
                x = x * 10 + (c - '0');
                c = read();
            }
            return x * sgn;
        }
    }

    // ---------------------- MergeSort para inversões ----------------------
    static long countInversions(int[] a) {
        int n = a.length;
        int[] aux = new int[n];
        return mergeSortCount(a, aux, 0, n - 1);
    }

    static long mergeSortCount(int[] a, int[] aux, int l, int r) {
        if (l >= r) return 0;
        int m = (l + r) >>> 1;
        long inv = 0;
        inv += mergeSortCount(a, aux, l, m);
        inv += mergeSortCount(a, aux, m + 1, r);

        // se já está ordenado, economiza merge
        if (a[m] <= a[m + 1]) return inv;

        // merge contando cruzamentos
        int i = l, j = m + 1, k = l;
        while (i <= m && j <= r) {
            if (a[i] <= a[j]) aux[k++] = a[i++];
            else { // a[i] > a[j] => todos [i..m] formam inversões com a[j]
                aux[k++] = a[j++];
                inv += (m - i + 1);
            }
        }
        while (i <= m) aux[k++] = a[i++];
        while (j <= r) aux[k++] = a[j++];
        System.arraycopy(aux, l, a, l, r - l + 1);
        return inv;
    }

    public static void main(String[] args) throws Exception {
        FastScanner fs = new FastScanner(System.in);
        StringBuilder out = new StringBuilder();

        while (true) {
            Integer nObj = fs.nextIntOrNull();
            if (nObj == null) break; // EOF
            int n = nObj;

            int[] gridLargada = new int[n];
            for (int i = 0; i < n; i++) gridLargada[i] = fs.nextIntOrNull();

            int[] gridChegada = new int[n];
            for (int i = 0; i < n; i++) gridChegada[i] = fs.nextIntOrNull();

            // mapa: competidor -> posição na largada
            int[] pos = new int[n + 1]; // ids são 1..N
            for (int i = 0; i < n; i++) pos[gridLargada[i]] = i;

            // transforma a ordem de chegada em um array de posições da largada
            int[] arr = new int[n];
            for (int i = 0; i < n; i++) arr[i] = pos[gridChegada[i]];

            // o número mínimo de ultrapassagens = # inversões em arr
            long inversoes = countInversions(arr);
            out.append(inversoes).append('\n');
        }

        System.out.print(out.toString());
    }
}
