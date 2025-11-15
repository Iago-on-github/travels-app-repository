package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class PolylineServiceTest {
    /*
    ====== ORGANIZAÇÃO DOS TESTES ======
    - Os testes de cada method devem ser isolados por classes anotadas com @nested

    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.

    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer

    - Em cenários de Success, os métodos devem conter "With Success" nos seus respetivos nomes.

    - Em cenários de Error, os métodos devem conter "ShouldThrowExceptionWhen…" nos seus respetivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    // !!! WARNING: !!!
    /*
    aqui nao tem simulação, é tudo chamada real no metodo pq estava dando problema com o Mock do metodo decode() de PolylineUtils
    pelo mesmo ser static e eu nao achei jeito de resolver isso
    */
    // !!! WARNING: !!!

    private final PolylineService polylineService = new PolylineService();

    @Nested
    class formattedPolyline {
        @DisplayName("Deve fazer a formatação da polyline com sucesso")
        @Test
        void shouldFormattedPolylineWithSuccess() {
            String inputPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"; // simulação
            List<Point> result = polylineService.formattedPolyline(inputPolyline);

            assertFalse(result.isEmpty());
            assertEquals(3, result.size());
        }

        @DisplayName("Deve lançar exceção quando o polyline for vazio")
        @Test
        void throwExceptionWhenPolylineIsEmpty() {
            String inputPolyline = "";

            assertThrows(NoSuchCoordinates.class, () -> {
                polylineService.formattedPolyline(inputPolyline);
            });


        }
    }
}