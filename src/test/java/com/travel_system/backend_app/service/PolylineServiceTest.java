package com.travel_system.backend_app.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PolylineServiceTest {
    /*
    ====== ORGANIZAÇÃO DOS TESTES ======
    - Os testes de cada method devem ser isolados por classes anotadas com @nested

    - Em casos de muitos testes em uma mesma classe, criar classes especificas com @nested para cenários de
    success e throw exception.

    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer

    - Em cenários de Success, os métodos devem conter "With Success" em seus respectivos nomes.

    - Em cenários de Error, os métodos devem conter "ShouldThrowExceptionWhen..." em seus respectivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @Nested
    class formattedPolyline {
        @DisplayName("Deve fazer a formatação da polyline com sucesso")
        @Test
        void shouldFormattedPolylineWithSuccess() {}
    }
}