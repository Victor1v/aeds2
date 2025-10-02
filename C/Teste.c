#include <stdio.h>

int contar_maiusculas(char *str)
{
    int contador = 0;
    for (int i = 0; str[i] != '\0'; i++)
    {
        if (str[i] >= 'A' && str[i] <= 'Z')
        {
            contador++;
        }
    }
    return contador;
}

int main()
{
    char teste[10];
    //int temp = 0;

    printf("Digite uma palavra: ");

    scanf(" %[^\n]", teste);


    while (teste[0] != 'F' && teste[1] != 'I' && teste[2] != 'M' && teste[3] != '\0')
    {
        //int num_maiusculas = contar_maiusculas(teste);
        //temp += num_maiusculas;
        printf("Número de maiúsculas na string: %d\n", contar_maiusculas(teste));
        scanf("%[^\n]", teste);
    }

    //printf("Numero total de maiúsculas: %d\n", temp);

    return 0;
}