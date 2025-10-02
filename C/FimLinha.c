#include <stdio.h>

int main()
{
    int x;
    while ((scanf("%d", &x) == 1) != EOF)
    {
        printf("Consegui ler x = %d", x);
        return 0;
    }
}