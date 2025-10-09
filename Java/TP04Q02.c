#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#define CSV_PATH1 "/tmp/games.csv"
#define CSV_PATH2 "games.csv"
#define MAX_LINE  16384
#define MAX_COLS  32

typedef struct {
    int   id;
    char *name;
    char *releaseDate;            // dd/MM/yyyy
    int   estimatedOwners;
    float price;
    char *supportedLanguages;     // já no formato [a, b, c]
    int   metacriticScore;        // -1 se vazio
    float userScore;              // -1.0 se vazio ou tbd
    int   achievements;           // 0 se vazio
    char *publishers;             // [a, b]
    char *developers;             // [a, b]
    char *categories;             // [a, b]
    char *genres;                 // [a, b]
    char *tags;                   // [a, b]
} Game;

/* ====================== utilidades ====================== */

static char *strdup_safe(const char *s){
    if(!s) s = "";
    size_t n = strlen(s);
    char *r = (char*)malloc(n+1);
    memcpy(r, s, n+1);
    return r;
}
static void rtrim(char *s){
    if(!s) return;
    int i=(int)strlen(s)-1;
    while(i>=0 && (unsigned char)s[i] <= 32){ s[i]=0; i--; }
}
static void trim_inplace(char *s){
    if(!s) return;
    rtrim(s);
    size_t i=0;
    while(s[i] && (unsigned char)s[i] <= 32) i++;
    if(i>0) memmove(s, s+i, strlen(s+i)+1);
}
static void strip_quotes_edges(char *s){
    size_t n=strlen(s);
    if(n>=2){
        char a=s[0], b=s[n-1];
        if( (a=='"' && b=='"') || (a=='\'' && b=='\'')){
            memmove(s, s+1, n-2);
            s[n-2]=0;
        }
    }
}
/* divide linha CSV respeitando aspas simples/duplas */
static int split_csv_line(const char *line, char **outCols, int maxcols){
    int n=0, inS=0, inD=0;
    const char *p=line, *start=line;
    while(*p){
        char c=*p;
        if(c=='\'' && !inD) inS = !inS;
        else if(c=='"' && !inS) inD = !inD;
        if(c==',' && !inS && !inD){
            size_t len=p-start;
            char *cell=(char*)malloc(len+1);
            memcpy(cell,start,len); cell[len]=0; trim_inplace(cell);
            outCols[n++]=cell; if(n==maxcols) return n;
            start=p+1;
        }
        p++;
    }
    size_t len=p-start;
    char *cell=(char*)malloc(len+1);
    memcpy(cell,start,len); cell[len]=0; trim_inplace(cell);
    outCols[n++]=cell;
    return n;
}

/* ================= normalizações ================== */

static int parse_int_default(const char *s, int def){
    if(!s) return def;
    while(*s && (unsigned char)*s<=32) s++;
    if(!*s) return def;
    char *end; long v=strtol(s,&end,10);
    if(end==s) return def;
    return (int)v;
}
static float parse_float_default(const char *s, float def){
    if(!s) return def;
    char buf[128]; int j=0;
    for(int i=0;s[i] && j<120;i++){
        char c=s[i];
        if((c>='0'&&c<='9')||c=='.'||c==',') buf[j++]=(c==',')?'.':c;
    }
    buf[j]=0;
    if(!buf[0]) return def;
    char *end; float v=strtof(buf,&end);
    if(end==buf) return def;
    return v;
}
static int normalize_owners(const char *s){
    if(!s) return 0;
    // pega o primeiro bloco numérico (ex.: "1,000,000 - 2,000,000" -> 1000000)
    char buf[64]; int j=0;
    for(int i=0;s[i] && j<60;i++){
        if(isdigit((unsigned char)s[i])) buf[j++]=s[i];
        else if(j>0) break;
    }
    buf[j]=0;
    if(!buf[0]) return 0;
    return atoi(buf);
}
static float normalize_price(const char *s){
    if(!s) return 0.0f;
    char low[256]; snprintf(low,sizeof(low),"%s",s);
    for(char *p=low;*p;p++) *p=(char)tolower((unsigned char)*p);
    trim_inplace(low);
    if(!strcmp(low,"free to play") || !strcmp(low,"free") || !strcmp(low,"gratuito"))
        return 0.0f;
    return parse_float_default(s,0.0f);
}
static float normalize_user_score(const char *s){
    if(!s) return -1.0f;
    char low[128]; snprintf(low,sizeof(low),"%s",s);
    for(char *p=low;*p;p++) *p=(char)tolower((unsigned char)*p);
    trim_inplace(low);
    if(!low[0] || !strcmp(low,"tbd")) return -1.0f;
    return parse_float_default(low,-1.0f);
}

/* --- datas (aceita: "Aug 13, 2018" | "13 Aug, 2018" | "Nov 2015" | "2015") --- */
static void month_from_token(const char *m_tok, char out_mon[3]){
    static const char *mons[][2]={
        {"jan","01"},{"january","01"},{"feb","02"},{"february","02"},
        {"mar","03"},{"march","03"},{"apr","04"},{"april","04"},
        {"may","05"},{"jun","06"},{"june","06"},{"jul","07"},{"july","07"},
        {"aug","08"},{"august","08"},{"sep","09"},{"sept","09"},{"september","09"},
        {"oct","10"},{"october","10"},{"nov","11"},{"november","11"},
        {"dec","12"},{"december","12"}
    };
    char m[64]; int i=0;
    for(; m_tok[i] && i<62; i++) m[i]=(char)tolower((unsigned char)m_tok[i]);
    m[i]=0;
    strcpy(out_mon,"01");
    for(size_t k=0;k<sizeof(mons)/sizeof(mons[0]);k++){
        size_t L=strlen(mons[k][0]);
        if(strncmp(m, mons[k][0], L)==0){ strcpy(out_mon, mons[k][1]); break; }
    }
}
static void take_year(const char *x, char out[5]){
    int q=0; for(int i=0;x[i] && q<4;i++) if(isdigit((unsigned char)x[i])) out[q++]=x[i];
    out[(q<4)?q:4]=0; if(!out[0]) strcpy(out,"1970");
}
static void take_day_2(const char *x, char out[3]){
    int q=0; for(int i=0;x[i] && q<2;i++) if(isdigit((unsigned char)x[i])) out[q++]=x[i];
    if(q==1){ out[2]=0; out[1]=out[0]; out[0]='0'; }
    else if(q==2){ out[2]=0; }
    else strcpy(out,"01");
}
static char *normalize_date(const char *raw){
    if(!raw) return strdup_safe("01/01/1970");

    char s[MAX_LINE]; snprintf(s,sizeof(s),"%s",raw);
    trim_inplace(s);

    // remove aspas ao redor
    strip_quotes_edges(s);
    trim_inplace(s);

    // 1) tentar dd/MM/yyyy com sscanf (tolerante a espaços)
    int d=0,m=0,y=0;
    if(sscanf(s, " %d / %d / %d ", &d, &m, &y) == 3 && d>0 && m>0 && y>0){
        char *out = (char*)malloc(11);
        if(d<1||d>31) d=1; if(m<1||m>12) m=1;
        snprintf(out,11,"%02d/%02d/%04d", d, m, y);
        return out;
    }

    // 2) remover vírgulas e baixar caixa para tratar “Aug 13, 2018”, etc.
    char t[MAX_LINE]; int j=0;
    for(int i=0;s[i] && j<MAX_LINE-1;i++){
        unsigned char c=(unsigned char)s[i];
        if(c!=',') t[j++]=(char)tolower(c);
    }
    t[j]=0;

    // 3) tokenizar por espaço
    char *tok[4]={0}; int nt=0; char *ctx=NULL;
    char *p=strtok_r(t," \t",&ctx);
    while(p && nt<4){ tok[nt++]=p; p=strtok_r(NULL," \t",&ctx); }

    char day[3]="01", mon[3]="01", year[5]="1970";

    if(nt==3){
        if(isalpha((unsigned char)tok[0][0]) && isdigit((unsigned char)tok[1][0])){   // "aug 13 2018"
            month_from_token(tok[0], mon);
            take_day_2(tok[1], day);
            take_year(tok[2], year);
        } else if(isdigit((unsigned char)tok[0][0]) && isalpha((unsigned char)tok[1][0])){ // "13 aug 2018"
            take_day_2(tok[0], day);
            month_from_token(tok[1], mon);
            take_year(tok[2], year);
        }
    } else if(nt==2){ // "nov 2015"
        month_from_token(tok[0], mon);
        take_year(tok[1], year);
    } else if(nt==1){ // "2015"
        take_year(tok[0], year);
    }

    char *out=(char*)malloc(11);
    snprintf(out,11,"%s/%s/%s", day, mon, year);
    return out;
}

/* ----- listas: colchetes -> itens normalizados e ", " garantido ----- */
static char *join_with_brackets(char **items, int n){
    size_t cap=2; // []
    for(int i=0;i<n;i++) cap += strlen(items[i]) + 2;
    char *r=(char*)malloc(cap+1); r[0]='['; r[1]=0;
    for(int i=0;i<n;i++){
        strcat(r, items[i]);
        if(i+1<n) strcat(r, ", ");
    }
    strcat(r, "]");
    return r;
}

static void ensure_comma_space(char *s) {
    char temp[MAX_LINE];
    int j = 0;
    for (int i = 0; s[i] != '\0' && j < MAX_LINE - 2; i++) {
        temp[j++] = s[i];
        if (s[i] == ',') {
            // adiciona um espaço se não existir logo depois
            if (s[i+1] != ' ' && s[i+1] != '\0' && s[i+1] != ']')
                temp[j++] = ' ';
        }
    }
    temp[j] = '\0';
    strcpy(s, temp);
}


static char *normalize_list_brackets(const char *s){
    if(!s) return strdup_safe("[]");
    const char *l=strchr(s,'['), *r=strrchr(s,']');
    char work[MAX_LINE]; work[0]=0;
    if(l && r && r>l){
        size_t len=r-(l+1); if(len>=MAX_LINE) len=MAX_LINE-1;
        memcpy(work,l+1,len); work[len]=0;
    }else{
        snprintf(work,sizeof(work),"%s",s);
    }

    char *tmp=strdup_safe(work);
    int inS=0,inD=0; char *parts[1024]; int n=0;
    char *start=tmp;
    for(char *q=tmp; *q; q++){
        if(*q=='\'' && !inD) inS=!inS;
        else if(*q=='"' && !inS) inD=!inD;
        if(*q==',' && !inS && !inD){
            *q=0; trim_inplace(start); strip_quotes_edges(start);
            parts[n++]=start; start=q+1;
        }
    }
    trim_inplace(start); strip_quotes_edges(start); parts[n++]=start;

    // remove vazios
    char *clean[1024]; int m=0;
    for(int i=0;i<n;i++) if(parts[i] && parts[i][0]) clean[m++]=parts[i];

    /* -------- fallback: se veio tudo como um único item com vírgulas, separa agora -------- */
    if(m==1 && strchr(clean[0], ',') != NULL){
        char *one = strdup_safe(clean[0]);
        char *tok2[1024]; int n2=0;
        char *ctx2=NULL; char *t=strtok_r(one, ",", &ctx2);
        while(t && n2<1024){
            trim_inplace(t); strip_quotes_edges(t);
            if(*t) tok2[n2++]=t;
            t=strtok_r(NULL, ",", &ctx2);
        }
        char *res2 = join_with_brackets(tok2, n2);  // já coloca ", "
        free(one);
        free(tmp);
        return res2;
    }

    char *res = join_with_brackets(clean, m);       // já coloca ", "
    free(tmp);
    return res;
}



/* publishers/developers vêm sem [], mas imprimimos com [] */
static char *normalize_companies(const char *s){
    if(!s) return strdup_safe("[]");
    char *tmp=strdup_safe(s);
    char *tok[512]; int n=0; 
    char *ctx=NULL; 
    char *p=strtok_r(tmp,",",&ctx);
    while(p && n<512){
        trim_inplace(p);
        strip_quotes_edges(p);
        if(*p) tok[n++]=p;
        p=strtok_r(NULL,",",&ctx);
    }
    char *res = join_with_brackets(tok, n);
    free(tmp);
    return res;
}



/* ===================== construção do Game ===================== */

static Game make_game_from_cols(char **c, int ncols){
    Game g; memset(&g,0,sizeof(g));
    const int iId=0, iName=1, iRelease=2, iOwners=3, iPrice=4, iLangs=5,
              iMetacritic=6, iUser=7, iAch=8, iPublish=9, iDevs=10,
              iCats=11, iGenres=12, iTags=13;

    g.id = parse_int_default( (iId<ncols? c[iId]:""), 0);
    g.name = strdup_safe( (iName<ncols? c[iName]:"") );
    g.releaseDate = normalize_date( (iRelease<ncols? c[iRelease]:"") );
    g.estimatedOwners = normalize_owners( (iOwners<ncols? c[iOwners]:"") );
    g.price = normalize_price( (iPrice<ncols? c[iPrice]:"") );
    g.supportedLanguages = normalize_list_brackets( (iLangs<ncols? c[iLangs]:"") );
    g.metacriticScore = parse_int_default( (iMetacritic<ncols? c[iMetacritic]:""), -1 );
    g.userScore = normalize_user_score( (iUser<ncols? c[iUser]:"") );
    g.achievements = parse_int_default( (iAch<ncols? c[iAch]:""), 0 );
    g.publishers = normalize_companies( (iPublish<ncols? c[iPublish]:"") );
    g.developers = normalize_companies( (iDevs<ncols? c[iDevs]:"") );
    g.categories = normalize_list_brackets( (iCats<ncols? c[iCats]:"") );
    g.genres = normalize_list_brackets( (iGenres<ncols? c[iGenres]:"") );
    g.tags = normalize_list_brackets( (iTags<ncols? c[iTags]:"") );
    return g;
}
static void free_game(Game *g){
    free(g->name); free(g->releaseDate); free(g->supportedLanguages);
    free(g->publishers); free(g->developers); free(g->categories);
    free(g->genres); free(g->tags);
}

/* ============== carregar CSV e indexar por id ============== */

typedef struct { Game *v; int sz, cap; } Vec;
static void vec_push(Vec *vec, Game g){
    if(vec->sz==vec->cap){ vec->cap = vec->cap? vec->cap*2:256; vec->v=(Game*)realloc(vec->v, vec->cap*sizeof(Game)); }
    vec->v[vec->sz++] = g;
}
static int cmp_id(const void *a, const void *b){
    const Game *ga=a, *gb=b; return (ga->id - gb->id);
}
static int binsearch(Game *arr, int n, int id){
    int l=0,r=n-1;
    while(l<=r){
        int m=(l+r)/2;
        if(arr[m].id==id) return m;
        if(arr[m].id<id) l=m+1; else r=m-1;
    }
    return -1;
}
static int load_csv_all(const char *path, Vec *vec){
    FILE *f=fopen(path,"r");
    if(!f) return 0;
    char line[MAX_LINE];
    if(!fgets(line,sizeof(line),f)){ fclose(f); return 0; } // cabeçalho
    while(fgets(line,sizeof(line),f)){
        rtrim(line);
        char *cols[MAX_COLS]={0}; int ncols=split_csv_line(line, cols, MAX_COLS);
        Game g = make_game_from_cols(cols, ncols);
        for(int i=0;i<ncols;i++) free(cols[i]);
        vec_push(vec, g);
    }
    fclose(f);
    qsort(vec->v, vec->sz, sizeof(Game), cmp_id);
    return 1;
}

/* ========================= impressão ========================= */

static void print_game(const Game *g){
    char userbuf[32];
    if(g->userScore < 0) strcpy(userbuf, "-1.0");
    else snprintf(userbuf, sizeof(userbuf), "%.1f", g->userScore);

    printf("=> %d ## %s ## %s ## %d ## %.2f ## %s ## %d ## %s ## %d ## %s ## %s ## %s ## %s ## %s ##\n",
           g->id, g->name, g->releaseDate, g->estimatedOwners, g->price,
           g->supportedLanguages, g->metacriticScore, userbuf, g->achievements,
           g->publishers, g->developers, g->categories, g->genres, g->tags);
}

/* =========================== main =========================== */

int main(void){
    Vec vec={0};
    if(!load_csv_all(CSV_PATH1,&vec)){
        if(!load_csv_all(CSV_PATH2,&vec)){
            fprintf(stderr,"Nao consegui abrir %s nem %s\n", CSV_PATH1, CSV_PATH2);
            return 1;
        }
    }

    char buf[256];
    while(fgets(buf,sizeof(buf),stdin)){
        rtrim(buf);
        if(strcmp(buf,"FIM")==0) break;
        if(!buf[0]) continue;
        int id = atoi(buf);
        int idx = binsearch(vec.v, vec.sz, id);
        if(idx>=0) print_game(&vec.v[idx]);
    }

    for(int i=0;i<vec.sz;i++) free_game(&vec.v[i]);
    free(vec.v);
    return 0;
}
