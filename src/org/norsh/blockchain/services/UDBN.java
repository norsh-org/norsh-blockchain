package org.norsh.blockchain.services;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UDBN {

    /**
     * Converte o prefixo alfanumérico em valores numéricos.
     * Números (0-9) são mantidos, letras (A-Z) são convertidas de 10 a 35.
     */
    public static List<Integer> convertPrefixToNumeric(String prefix) {
        List<Integer> numericValue = new LinkedList<>();
        for (char c : prefix.toCharArray()) {
            if (c >= '0' && c <= '9') {
                numericValue.add(c - '0');
            } else if (c >= 'A' && c <= 'Z') {
                numericValue.add(c - 'A' + 10);
            }
        }
        return numericValue;
    }

    /**
     * Calcula o Dígito Verificador (DV) utilizando a soma direta dos números e Módulo 11.
     * Formato: PREFIXO_NUMÉRICO + TIPO (02 dígitos) + SEQUENCIAL
     */
    public static String generateUDBN(String prefix, int type, String sequentialStr) {
        // Garantir que o tipo seja formatado com 2 dígitos
        String typeStr = String.format("%02d", type);

        List<Integer> numericPrefix = convertPrefixToNumeric(prefix);

        // Criar lista completa de números para o cálculo
        List<Integer> fullNumber = new ArrayList<>(numericPrefix);

        // Adicionar TIPO (formatado com 2 dígitos)
        for (char c : typeStr.toCharArray()) {
            fullNumber.add(Character.getNumericValue(c));
        }

        // Adicionar SEQUENCIAL convertido em lista de dígitos
        for (char c : sequentialStr.toCharArray()) {
            fullNumber.add(Character.getNumericValue(c));
        }

        // Somar todos os números
        int sum = fullNumber.stream().mapToInt(Integer::intValue).sum();

        // Calcular Dígito Verificador aplicando Módulo 11
        int dv = sum % 11;

        // Se DV for 10, retorna 'X'
        String dvStr = (dv == 10) ? "0" : String.valueOf(dv);

        // Retorna o UDBN completo
        return prefix + "-" + typeStr + dvStr + "-" + sequentialStr;
    }

    /**
     * Valida um UDBN, verificando se o Dígito Verificador está correto.
     */
    public static boolean validateUDBN(String udbn) {
        // Quebrar UDBN em partes
        String[] parts = udbn.split("-");
        if (parts.length != 3) {
            return false; // Formato inválido
        }

        String prefix = parts[0];
        String typeAndDV = parts[1];
        String sequential = parts[2];

        // Extração do tipo
        int type;
        try {
            type = Integer.parseInt(typeAndDV.substring(0, 2)); // Pega os dois primeiros dígitos
        } catch (NumberFormatException e) {
            return false;
        }

        // Obtém o dígito verificador fornecido
        char providedDV = typeAndDV.charAt(2);

        // Recalcula o DV esperado
        String recalculatedUDBN = generateUDBN(prefix, type, sequential);
        char expectedDV = recalculatedUDBN.split("-")[1].charAt(2);

        // Compara os valores
        return providedDV == expectedDV;
    }

    public static void main(String[] args) {
        // Exemplo de geração de UDBN
        String prefix = "NSH";
        int type = 3;
        String sequential = "1"; 

        String generatedUDBN = generateUDBN(prefix, type, sequential);
        System.out.println("UDBN Gerado: " + generatedUDBN);

        // Exemplo de validação de UDBN
        boolean isValid = validateUDBN(generatedUDBN);
        System.out.println("Validação: " + (isValid ? "Válido" : "Inválido"));
    }
}
