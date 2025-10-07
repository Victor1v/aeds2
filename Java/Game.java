import java.util.*;
import java.util.regex.*;

/**
 * Modela um jogo do dataset Steam (games.csv), com normalização dos campos.
 *
 * Observações:
 * - Use Game.fromCSV(Map<String,String>) quando você já tiver um mapa (coluna -> valor)
 *   a partir do cabeçalho do CSV.
 * - Os nomes de chaves esperados no mapa seguem convenções comuns nesse dataset:
 *   "appid" ou "id", "name", "release_date" ou "releaseDate", "estimated_owners",
 *   "price", "languages" (ou "supportedLanguages"), "metacritic_score",
 *   "user_score", "achievements", "publishers", "developers",
 *   "categories", "genres", "tags".
 * - Caso seu CSV use rótulos diferentes, basta ajustar os aliases abaixo.
 */
public class Game {
    // -------------------- Campos do modelo --------------------
    private int id;                                // Identificador único
    private String name;                           // Nome
    private String releaseDate;                    // dd/mm/aaaa (completando com 01)
    private int estimatedOwners;                   // inteiro (remove não numéricos)
    private float price;                           // float (Free to Play -> 0.0)
    private String[] supportedLanguages;           // array entre colchetes
    private int metacriticScore;                   // inteiro (vazio -> -1)
    private float userScore;                       // float (vazio ou tbd -> -1.0)
    private int achievements;                      // inteiro (vazio -> 0)
    private String[] publishers;                   // array separado por vírgulas
    private String[] developers;                   // array separado por vírgulas
    private String[] categories;                   // array entre colchetes
    private String[] genres;                       // array entre colchetes
    private String[] tags;                         // array entre colchetes

    // -------------------- Construtores --------------------
    public Game() {}

    public Game(int id, String name, String releaseDate, int estimatedOwners,
                float price, String[] supportedLanguages, int metacriticScore,
                float userScore, int achievements, String[] publishers,
                String[] developers, String[] categories, String[] genres,
                String[] tags) {
        this.id = id;
        this.name = safe(name);
        this.releaseDate = releaseDate;
        this.estimatedOwners = estimatedOwners;
        this.price = price;
        this.supportedLanguages = normArray(supportedLanguages);
        this.metacriticScore = metacriticScore;
        this.userScore = userScore;
        this.achievements = achievements;
        this.publishers = normArray(publishers);
        this.developers = normArray(developers);
        this.categories = normArray(categories);
        this.genres = normArray(genres);
        this.tags = normArray(tags);
    }

    // -------------------- Fábrica a partir do CSV --------------------
    public static Game fromCSV(Map<String, String> row) {
        // Aliases comuns de cabeçalhos no dataset
        String idStr         = pick(row, "id", "appid", "AppID");
        String nameStr       = pick(row, "name", "Name");
        String rdStr         = pick(row, "releaseDate", "release_date", "Release date");
        String ownersStr     = pick(row, "estimatedOwners", "estimated_owners", "owners");
        String priceStr      = pick(row, "price", "Price");
        String langsStr      = pick(row, "supportedLanguages", "languages", "Supported languages");
        String metaStr       = pick(row, "metacriticScore", "metacritic_score", "Metacritic score");
        String userStr       = pick(row, "userScore", "user_score", "User score");
        String achStr        = pick(row, "achievements", "Achievements");
        String pubsStr       = pick(row, "publishers", "publisher", "Publishers");
        String devsStr       = pick(row, "developers", "developer", "Developers");
        String catsStr       = pick(row, "categories", "Categories");
        String gensStr       = pick(row, "genres", "Genres");
        String tagsStr       = pick(row, "tags", "Tags");

        Game g = new Game();
        g.id = parseIntSafe(idStr, 0);
        g.name = safe(nameStr);
        g.releaseDate = normalizeDate(rdStr); // dd/mm/aaaa (com 01 em faltantes)
        g.estimatedOwners = normalizeEstimatedOwners(ownersStr);
        g.price = normalizePrice(priceStr);
        g.supportedLanguages = extractBracketedList(langsStr);
        g.metacriticScore = parseIntDefault(metaStr, -1);
        g.userScore = normalizeUserScore(userStr);
        g.achievements = parseIntDefault(achStr, 0);
        g.publishers = splitCommaList(pubsStr);
        g.developers = splitCommaList(devsStr);
        g.categories = extractBracketedList(catsStr);
        g.genres = extractBracketedList(gensStr);
        g.tags = extractBracketedList(tagsStr);
        return g;
    }

    // -------------------- Normalizações --------------------

    // 1) releaseDate -> dd/mm/aaaa; completar faltantes com "01"
    private static String normalizeDate(String raw) {
        if (isBlank(raw)) return "01/01/0001"; // valor neutro caso ausente
        String s = raw.trim();

        // Formatos comuns: "2015-03-10", "Mar 10, 2015", "10 Mar, 2015", "2015", "Mar 2015"
        // Estratégia:
        // - Tenta ISO yyyy-MM-dd
        // - Tenta dd/MM/yyyy já pronto
        // - Tenta com meses por extenso (en-US e pt-BR curtos)
        // - Se tiver só ano, vira 01/01/ano; se tiver mês e ano, vira 01/mm/ano
        // - fallback seguro: 01/01/0001
        // ISO yyyy-MM-dd
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = s.split("-");
            return String.format("%02d/%02d/%04d", parseIntSafe(p[2], 1), parseIntSafe(p[1], 1), parseIntSafe(p[0], 1));
        }
        // dd/MM/yyyy
        if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] p = s.split("/");
            int d = parseIntSafe(p[0], 1);
            int m = parseIntSafe(p[1], 1);
            int y = parseIntSafe(p[2], 1);
            return String.format("%02d/%02d/%04d", clamp(d,1,31), clamp(m,1,12), y);
        }
        // Padrões com mês por nome (en-US): "Mar 10, 2015" | "10 Mar, 2015" | "Mar 2015"
        // e pt-BR curtos: "10 mar 2015" | "mar 2015"
        String sClean = s.replace(",", " ").replaceAll("\\s+", " ").trim();
        String[] parts = sClean.split(" ");
        Map<String,Integer> mon = monthMap();

        if (parts.length == 3) {
            // Casos: "Mar 10 2015" || "10 Mar 2015" || "03 10 2015"
            Integer m = mon.get(normalizeMonthToken(parts[0]));
            Integer d = tryParseInt(parts[1]);
            Integer y = tryParseInt(parts[2]);
            if (m != null && d != null && y != null) {
                return String.format("%02d/%02d/%04d", clamp(d,1,31), clamp(m,1,12), y);
            }
            // inverso: "10 Mar 2015"
            d = tryParseInt(parts[0]);
            m = mon.get(normalizeMonthToken(parts[1]));
            if (d != null && m != null && y != null) {
                return String.format("%02d/%02d/%04d", clamp(d,1,31), clamp(m,1,12), y);
            }
        } else if (parts.length == 2) {
            // "Mar 2015" ou "03 2015"
            Integer m = mon.get(normalizeMonthToken(parts[0]));
            Integer y = tryParseInt(parts[1]);
            if (m != null && y != null) {
                return String.format("01/%02d/%04d", clamp(m,1,12), y);
            }
        } else if (parts.length == 1) {
            // Apenas ano
            Integer y = tryParseInt(parts[0]);
            if (y != null) return String.format("01/01/%04d", y);
        }

        // fallback
        return "01/01/0001";
    }

    // 2) estimatedOwners: remover não numéricos
    // Observação: alguns datasets trazem faixas ("200,000 .. 500,000").
    // A regra exigida pede "remova não numéricos" -> ficaria "200000500000".
    // Caso deseje outra política (ex.: menor valor), adapte aqui.
    private static int normalizeEstimatedOwners(String raw) {
        if (isBlank(raw)) return 0;
        String digits = raw.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 0;
        // Pode estourar int em entradas exorbitantes; aqui fazemos um clamp simples:
        try {
            long val = Long.parseLong(digits);
            if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return (int) val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 3) price: "Free to Play" -> 0.0; senão, float padrão (remove símbolos)
    private static float normalizePrice(String raw) {
        if (isBlank(raw)) return 0.0f;
        String s = raw.trim();
        if (s.equalsIgnoreCase("Free to Play") || s.equalsIgnoreCase("Free") || s.equalsIgnoreCase("Gratuito"))
            return 0.0f;
        // Remove moeda e normaliza decimal com ponto
        s = s.replaceAll("[^0-9,\\.]", "");
        // Se vier com vírgula decimal, troca por ponto (Brasil)
        if (s.contains(",") && !s.contains(".")) s = s.replace(',', '.');
        try { return Float.parseFloat(s); } catch (Exception e) { return 0.0f; }
    }

    // 4) userScore: vazio ou "tbd" -> -1.0; senão float
    private static float normalizeUserScore(String raw) {
        if (isBlank(raw)) return -1.0f;
        String s = raw.trim();
        if (s.equalsIgnoreCase("tbd")) return -1.0f;
        s = s.replace(',', '.');
        try { return Float.parseFloat(s); } catch (Exception e) { return -1.0f; }
    }

    // 5) Listas entre colchetes: [a, b, c]
    private static String[] extractBracketedList(String raw) {
        if (isBlank(raw)) return new String[0];
        String s = raw.trim();
        // tenta extrair conteúdo entre o primeiro '[' e o último ']'
        int i = s.indexOf('['), j = s.lastIndexOf(']');
        if (i >= 0 && j > i) {
            s = s.substring(i + 1, j);
        }
        // Agora divide por vírgulas respeitando aspas simples/duplas
        return splitCsvLike(s);
    }

    // 6) Listas separadas por vírgulas (sem colchetes)
    private static String[] splitCommaList(String raw) {
        if (isBlank(raw)) return new String[0];
        return splitCsvLike(raw);
    }

    // -------------------- Utilitários --------------------
    private static String pick(Map<String,String> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k) && row.get(k) != null) return row.get(k);
        }
        return null;
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static Integer tryParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private static int parseIntDefault(String s, int def) {
        if (isBlank(s)) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static String[] normArray(String[] a) {
        if (a == null) return new String[0];
        List<String> out = new ArrayList<>();
        for (String t : a) {
            if (!isBlank(t)) out.add(t.trim());
        }
        return out.toArray(new String[0]);
    }

    private static int clamp(int x, int lo, int hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static String normalizeMonthToken(String t) {
        if (t == null) return "";
        String s = t.trim().toLowerCase(Locale.ROOT);
        // remove ponto de abreviação: "sept." -> "sept"
        if (s.endsWith(".")) s = s.substring(0, s.length()-1);
        return s;
    }

    private static Map<String,Integer> monthMap() {
        Map<String,Integer> m = new HashMap<>();
        String[][] pairs = {
            {"jan","1"},{"january","1"},{"jan.","1"},{"jan","1"},
            {"feb","2"},{"february","2"},{"fev","2"},{"fevereiro","2"},
            {"mar","3"},{"march","3"},{"março","3"},{"marco","3"},
            {"apr","4"},{"april","4"},{"abr","4"},{"abril","4"},
            {"may","5"},{"mai","5"},
            {"jun","6"},{"june","6"},{"junho","6"},
            {"jul","7"},{"july","7"},{"julho","7"},
            {"aug","8"},{"august","8"},{"ago","8"},{"agosto","8"},
            {"sep","9"},{"sept","9"},{"september","9"},{"set","9"},{"setembro","9"},
            {"oct","10"},{"october","10"},{"out","10"},{"outubro","10"},
            {"nov","11"},{"november","11"},{"novembro","11"},
            {"dec","12"},{"december","12"},{"dez","12"},{"dezembro","12"}
        };
        for (String[] p : pairs) m.put(p[0], Integer.parseInt(p[1]));
        return m;
    }

    /**
     * Divide uma string por vírgulas respeitando aspas (simples e duplas).
     * Também remove chaves/colchetes residuais e aparas.
     */
    private static String[] splitCsvLike(String s) {
        if (isBlank(s)) return new String[0];
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && !inSingle) { inDouble = !inDouble; continue; }
            if (c == '\'' && !inDouble) { inSingle = !inSingle; continue; }
            if (c == ',' && !inSingle && !inDouble) {
                out.add(cleanToken(cur.toString()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cleanToken(cur.toString()));
        return out.stream().filter(t -> !t.isEmpty()).toArray(String[]::new);
    }

    private static String cleanToken(String t) {
        String u = t.trim();
        // remove colchetes residuais e aspas
        if (u.startsWith("[") && u.endsWith("]")) u = u.substring(1, u.length()-1).trim();
        if ((u.startsWith("\"") && u.endsWith("\"")) || (u.startsWith("'") && u.endsWith("'"))) {
            u = u.substring(1, u.length()-1).trim();
        }
        return u;
    }

    // -------------------- Getters --------------------
    public int getId() { return id; }
    public String getName() { return name; }
    public String getReleaseDate() { return releaseDate; }
    public int getEstimatedOwners() { return estimatedOwners; }
    public float getPrice() { return price; }
    public String[] getSupportedLanguages() { return supportedLanguages.clone(); }
    public int getMetacriticScore() { return metacriticScore; }
    public float getUserScore() { return userScore; }
    public int getAchievements() { return achievements; }
    public String[] getPublishers() { return publishers.clone(); }
    public String[] getDevelopers() { return developers.clone(); }
    public String[] getCategories() { return categories.clone(); }
    public String[] getGenres() { return genres.clone(); }
    public String[] getTags() { return tags.clone(); }

    // -------------------- Representação útil --------------------
    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", estimatedOwners=" + estimatedOwners +
                ", price=" + price +
                ", supportedLanguages=" + Arrays.toString(supportedLanguages) +
                ", metacriticScore=" + metacriticScore +
                ", userScore=" + userScore +
                ", achievements=" + achievements +
                ", publishers=" + Arrays.toString(publishers) +
                ", developers=" + Arrays.toString(developers) +
                ", categories=" + Arrays.toString(categories) +
                ", genres=" + Arrays.toString(genres) +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}
