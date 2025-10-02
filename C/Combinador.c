#include <stdio.h>

int main()
{
    char str1[101], str2[101], resultado[202];

    int i = 0, j = 0, k = 0;

    while (scanf("%s %s", str1, str2) != EOF)
    {
        i = 0, j = 0, k = 0;
        
        while (str1[i] != '\0' && str2[j] != '\0')
        {
            resultado[k++] = str1[i++];
            resultado[k++] = str2[j++];
        }

        while (str1[i] != '\0')
        {
            resultado[k++] = str1[i++];
        }

        while (str2[j] != '\0')
        {
            resultado[k++] = str2[j++];
        }

        resultado[k] = '\0';

        printf("%s\n", resultado);
    }
    return 0;
}
