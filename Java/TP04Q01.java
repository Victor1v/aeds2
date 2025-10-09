import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TP04Q01 {

    static class Game {
        int id;
        String name;
        String releaseDate;                 // dd/MM/yyyy
        int estimatedOwners;
        float price;
        String[] supportedLanguages;
        int metacriticScore;
        float userScore;
        int achievements;
        String[] publishers;
        String[] developers;
        String[] categories;
        String[] genres;
        String[] tags;

private static String arrToString(String[] arr) {
    if (arr == null) return "[]";
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < arr.length; i++) {
        sb.append(arr[i]);
        if (i + 1 < arr.length) sb.append(", ");
    }
    sb.append(']');
    // garante espaço depois de toda vírgula, mesmo quando o campo veio como um item único
    return sb.toString().replaceAll(",\\s*", ", ");
}


        private static String safe(String s) { return s == null ? "" : s; }

@Override
public String toString() {
    String user = (userScore == -1.0f) ? "-1.0" : String.format(java.util.Locale.US, "%.1f", userScore);
    return "=> " + id + " ## " + safe(name) + " ## " + safe(releaseDate) + " ## " +
           estimatedOwners + " ## " + String.format(java.util.Locale.US, "%.2f", price) + " ## " +
           arrToString(supportedLanguages) + " ## " + metacriticScore + " ## " + user + " ## " +
           achievements + " ## " + arrToString(publishers) + " ## " + arrToString(developers) + " ## " +
           arrToString(categories) + " ## " + arrToString(genres) + " ## " + arrToString(tags) + " ##";
}


        // --------- Fábrica a partir das colunas do seu CSV ----------
        public static Game fromCSV(String[] c) {
            // Índices EXATOS do seu arquivo:
            final int iId           = 0;  // AppID
            final int iName         = 1;  // Name
            final int iRelease      = 2;  // Release date
            final int iOwners       = 3;  // Estimated owners
            final int iPrice        = 4;  // Price
            final int iLangs        = 5;  // Supported languages [ ... ]
            final int iMetacritic   = 6;  // Metacritic score
            final int iUserScore    = 7;  // User score
            final int iAchiev       = 8;  // Achievements
            final int iPublishers   = 9;  // Publishers
            final int iDevelopers   = 10; // Developers
            final int iCategories   = 11; // Categories [ ... ]
            final int iGenres       = 12; // Genres [ ... ]
            final int iTags         = 13; // Tags [ ... ]

            Game g = new Game();
            g.id = parseIntSafe(get(c, iId));
            g.name = trimQuotes(get(c, iName));
            g.releaseDate = normalizeDate(get(c, iRelease));
            g.estimatedOwners = normalizeOwners(get(c, iOwners));
            g.price = normalizePrice(get(c, iPrice));
            g.supportedLanguages = parseBracketList(get(c, iLangs));
            g.metacriticScore = normalizeIntOrDefault(get(c, iMetacritic), -1);
            g.userScore = normalizeUserScore(get(c, iUserScore));
            g.achievements = normalizeIntOrDefault(get(c, iAchiev), 0);
            g.publishers = splitCompanies(get(c, iPublishers));
            g.developers = splitCompanies(get(c, iDevelopers));
            g.categories = parseBracketList(get(c, iCategories));
            g.genres = parseBracketList(get(c, iGenres));
            g.tags = parseBracketList(get(c, iTags));
            return g;
        }

        // ---------------- Helpers de normalização ----------------
        private static String get(String[] cols, int idx) {
            return (idx < 0 || idx >= cols.length) ? "" : cols[idx];
        }

        private static String trimQuotes(String s) {
            if (s == null) return "";
            s = s.trim();
            if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
                s = s.substring(1, s.length() - 1).trim();
            }
            return s;
        }

        private static int parseIntSafe(String s) {
            try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
        }

        private static int normalizeIntOrDefault(String s, int def) {
            s = s == null ? "" : s.trim();
            if (s.isEmpty()) return def;
            try { return Integer.parseInt(s); } catch (Exception e) { return def; }
        }

        private static float normalizeUserScore(String s) {
            s = s == null ? "" : s.trim().toLowerCase();
            if (s.isEmpty() || s.equals("tbd")) return -1.0f;
            try { return Float.parseFloat(s.replace(',', '.')); } catch (Exception e) { return -1.0f; }
        }

        private static float normalizePrice(String s) {
            if (s == null) return 0.0f;
            s = s.trim();
            if (s.isEmpty()) return 0.0f;
            String lower = s.toLowerCase();
            if (lower.contains("free to play") || lower.equals("free") || lower.equals("gratuito")) return 0.0f;
            s = s.replaceAll("[^0-9,\\.]", "");
            if (s.isEmpty()) return 0.0f;
            s = s.replace(',', '.');
            try { return Float.parseFloat(s); } catch (Exception e) { return 0.0f; }
        }

        private static int normalizeOwners(String s) {
            if (s == null) return 0;
            s = s.replace(",", "");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(s);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignore) {}
            }
            String only = s.replaceAll("\\D+", "");
            if (only.isEmpty()) return 0;
            try { return Integer.parseInt(only); } catch (Exception e) { return 0; }
        }

private static String[] parseBracketList(String s) {
    if (s == null) return new String[0];
    int l = s.indexOf('['), r = s.lastIndexOf(']');
    if (l >= 0 && r > l) s = s.substring(l + 1, r);
    s = s.trim();
    if (s.isEmpty()) return new String[0];

    // Divide itens considerando vírgulas fora de aspas
    List<String> parts = splitCSVLike(s, ',');
    List<String> out = new ArrayList<>();
    for (String p : parts) {
        String t = p.trim()
                   .replaceAll("^[\"']+", "")   // remove aspas simples/duplas do início
                   .replaceAll("[\"']+$", "");  // remove aspas simples/duplas do final
        if (!t.isEmpty()) out.add(t);
    }
    return out.toArray(new String[0]);
}


        private static String[] splitCompanies(String s) {
            if (s == null) return new String[0];
            s = trimQuotes(s);
            if (s.isEmpty()) return new String[0];
            List<String> parts = splitCSVLike(s, ',');
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                String t = trimQuotes(p).trim();
                if (!t.isEmpty()) out.add(t);
            }
            return out.toArray(new String[0]);
        }

        private static List<String> splitCSVLike(String s, char delimiter) {
            List<String> out = new ArrayList<>();
            if (s == null || s.isEmpty()) return out;
            StringBuilder cur = new StringBuilder();
            boolean inSingle = false, inDouble = false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\'' && !inDouble) inSingle = !inSingle;
                else if (c == '"' && !inSingle) inDouble = !inDouble;

                if (c == delimiter && !inSingle && !inDouble) {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
            out.add(cur.toString());
            return out;
        }

        private static String normalizeDate(String raw) {
            if (raw == null) return "01/01/1970";
            raw = trimQuotes(raw).trim();
            if (raw.matches("\\d{2}/\\d{2}/\\d{4}")) return raw;

            String day = "01", month = "01", year = "1970";
            Map<String,String> mon = new HashMap<>();
            mon.put("jan","01"); mon.put("feb","02"); mon.put("mar","03"); mon.put("apr","04");
            mon.put("may","05"); mon.put("jun","06"); mon.put("jul","07"); mon.put("aug","08");
            mon.put("sep","09"); mon.put("oct","10"); mon.put("nov","11"); mon.put("dec","12");

            String s = raw.toLowerCase().replace(",", "").trim();
            String[] tk = s.split("\\s+");
            try {
                if (tk.length == 3) { // "nov 20 2015"
                    month = mon.getOrDefault(tk[0].substring(0,3), "01");
                    String d = tk[1].replaceAll("\\D+","");
                    day = d.isEmpty() ? "01" : (d.length()==1 ? "0"+d : d);
                    year = tk[2].replaceAll("\\D+","");
                } else if (tk.length == 2) { // "nov 2015"
                    month = mon.getOrDefault(tk[0].substring(0,3), "01");
                    year = tk[1].replaceAll("\\D+","");
                } else if (tk.length == 1) { // "2015"
                    year = tk[0].replaceAll("\\D+","");
                } else {
                    SimpleDateFormat in = new SimpleDateFormat("dd/MM/yyyy");
                    in.setLenient(false);
                    return in.format(in.parse(raw));
                }
            } catch (Exception ignore) {}
            if (month.length()==1) month = "0"+month;
            if (year.isEmpty()) year = "1970";
            return day + "/" + month + "/" + year;
        }
    }

    // ---------------- CSV loader ----------------
    private static String[][] loadCSV(String path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // pula cabeçalho
                rows.add(splitCSVLine(line));
            }
        }
        return rows.toArray(new String[0][]);
    }

    // Divide respeitando aspas (simples/duplas)
    private static String[] splitCSVLine(String s) {
        List<String> cols = new ArrayList<>();
        if (s == null) return new String[0];
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;

            if (c == ',' && !inSingle && !inDouble) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        for (int i = 0; i < cols.size(); i++) cols.set(i, cols.get(i).trim());
        return cols.toArray(new String[0]);
    }

    private static Map<Integer,String[]> indexById(String[][] table) {
        Map<Integer,String[]> map = new HashMap<>();
        for (String[] row : table) {
            if (row.length == 0) continue;
            try {
                int id = Integer.parseInt(row[0].trim().replaceAll("\\D+",""));
                map.put(id, row);
            } catch (Exception ignore) {}
        }
        return map;
    }

    private static String findCsvPath() {
        String p1 = "/tmp/games.csv";
        String p2 = "games.csv";
        if (Files.exists(Paths.get(p1))) return p1;
        return p2;
    }

    public static void main(String[] args) throws Exception {
        String csvPath = findCsvPath();
        String[][] table = loadCSV(csvPath);
        Map<Integer,String[]> byId = indexById(table);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.equals("FIM")) break;
            if (line.isEmpty()) continue;
            int id;
            try { id = Integer.parseInt(line); } catch (Exception e) { continue; }
            String[] cols = byId.get(id);
            if (cols == null) continue;
            Game g = Game.fromCSV(cols);
            System.out.println(g.toString());
        }
    }
}
