package com.rafaelagamero.backend.service;

import com.rafaelagamero.backend.dto.ShippingCalculationDTO;
import com.rafaelagamero.backend.dto.ShippingOptionDTO;
import com.rafaelagamero.backend.dto.ShippingResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class ShippingService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("299.00");
    private static final String DEFAULT_ORIGIN_CEP = "01001000"; // São Paulo - SP

    @Value("${melhorenvio.token:}")
    private String melhorEnvioToken;

    @Value("${melhorenvio.sandbox:true}")
    private boolean melhorEnvioSandbox;

    @Value("${store.origin.cep:01001000}")
    private String originCep;

    private final RestTemplate restTemplate;

    public ShippingService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(6));
        this.restTemplate = new RestTemplate(factory);
    }

    public ShippingResponseDTO calculateShipping(ShippingCalculationDTO request) {
        if (request == null || request.getPostalCode() == null || request.getPostalCode().isBlank()) {
            throw new IllegalArgumentException("O CEP é obrigatório para o cálculo do frete.");
        }

        String sanitizedCep = request.getPostalCode().replaceAll("\\D", "");
        if (sanitizedCep.length() != 8) {
            throw new IllegalArgumentException("CEP inválido. O CEP deve conter exatamente 8 dígitos.");
        }

        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;

        // 1. Consultar endereço no ViaCEP
        Map<String, Object> viaCepData = fetchAddressByCep(sanitizedCep);
        String state = viaCepData.get("uf") != null ? viaCepData.get("uf").toString().toUpperCase() : "SP";
        String city = viaCepData.get("localidade") != null ? viaCepData.get("localidade").toString() : "";
        String neighborhood = viaCepData.get("bairro") != null ? viaCepData.get("bairro").toString() : "";
        String street = viaCepData.get("logradouro") != null ? viaCepData.get("logradouro").toString() : "";

        // 2. Tentar cotação na API do Melhor Envio (se houver token configurado)
        List<ShippingOptionDTO> options = null;
        if (melhorEnvioToken != null && !melhorEnvioToken.isBlank()) {
            options = tryMelhorEnvioCalculation(sanitizedCep, subtotal);
        }

        // 3. Fallback: Se não houver token ou falhar, utilizar o Motor Regional de Frete Brasileiro
        if (options == null || options.isEmpty()) {
            options = calculateRegionalShipping(sanitizedCep, state, subtotal);
        }

        return ShippingResponseDTO.builder()
                .postalCode(sanitizedCep)
                .street(street)
                .neighborhood(neighborhood)
                .city(city)
                .state(state)
                .options(options)
                .build();
    }

    private Map<String, Object> fetchAddressByCep(String cep) {
        try {
            String url = "https://viacep.com.br/ws/" + cep + "/json/";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                if (Boolean.TRUE.equals(body.get("erro")) || "true".equals(String.valueOf(body.get("erro")))) {
                    throw new IllegalArgumentException("CEP " + cep + " não foi localizado pelos Correios.");
                }
                return body;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Aviso ao consultar ViaCEP para {}: {}", cep, e.getMessage());
        }
        return Collections.emptyMap();
    }

    private List<ShippingOptionDTO> tryMelhorEnvioCalculation(String destinationCep, BigDecimal subtotal) {
        try {
            String baseUrl = melhorEnvioSandbox
                    ? "https://sandbox.melhorenvio.com.br/api/v2/me/shipment/calculate"
                    : "https://melhorenvio.com.br/api/v2/me/shipment/calculate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(melhorEnvioToken.trim());
            headers.set("User-Agent", "RafaelaShop (contato@rafaelashop.com)");

            // Payload padrão para moda fitness / semijoias (pacote médio de 350g, 20x15x5cm)
            Map<String, Object> packageDim = new HashMap<>();
            packageDim.put("weight", 0.35);
            packageDim.put("width", 15);
            packageDim.put("height", 5);
            packageDim.put("length", 20);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", Map.of("postal_code", originCep != null ? originCep : DEFAULT_ORIGIN_CEP));
            payload.put("to", Map.of("postal_code", destinationCep));
            payload.put("package", packageDim);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<List> response = restTemplate.postForEntity(baseUrl, entity, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) response.getBody();
                List<ShippingOptionDTO> options = new ArrayList<>();

                boolean eligibleForFreeShipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;

                for (Map<String, Object> item : list) {
                    if (item.containsKey("error")) continue;

                    String name = (String) item.get("name");
                    Map<String, Object> company = (Map<String, Object>) item.get("company");
                    String carrierName = company != null ? (String) company.get("name") : "Transportadora";

                    Object customPriceObj = item.get("custom_price") != null ? item.get("custom_price") : item.get("price");
                    if (customPriceObj == null) continue;

                    BigDecimal originalPrice = new BigDecimal(customPriceObj.toString()).setScale(2, RoundingMode.HALF_UP);
                    Integer deliveryDays = item.get("custom_delivery_time") != null
                            ? Integer.parseInt(item.get("custom_delivery_time").toString())
                            : 5;

                    boolean isEconomic = name != null && (name.toLowerCase().contains("pac") || name.toLowerCase().contains("package"));
                    boolean isFree = eligibleForFreeShipping && isEconomic;
                    BigDecimal price = isFree ? BigDecimal.ZERO : originalPrice;

                    options.add(ShippingOptionDTO.builder()
                            .id(item.get("id") != null ? item.get("id").toString() : UUID.randomUUID().toString())
                            .name(isFree ? name + " (Frete Grátis)" : name)
                            .carrier(carrierName)
                            .price(price)
                            .originalPrice(originalPrice)
                            .deliveryDays(deliveryDays)
                            .deliveryRange(deliveryDays + " a " + (deliveryDays + 2) + " dias úteis")
                            .isFree(isFree)
                            .description(isEconomic ? "Econômico" : "Expresso")
                            .build());
                }

                if (!options.isEmpty()) {
                    options.sort(Comparator.comparing(ShippingOptionDTO::getPrice));
                    return options;
                }
            }
        } catch (Exception e) {
            log.warn("API do Melhor Envio indisponível ou token não configurado: {}. Acionando motor de frete regional.", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Motor Regional Brasileiro (Fallback Inteligente)
     * Calcula valores reais e prazos para todo o Brasil baseado na localidade e UF de destino.
     */
    private List<ShippingOptionDTO> calculateRegionalShipping(String cep, String state, BigDecimal subtotal) {
        List<ShippingOptionDTO> options = new ArrayList<>();
        boolean eligibleForFreeShipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;

        BigDecimal pacPrice;
        int pacDays;

        BigDecimal sedexPrice;
        int sedexDays;

        switch (state) {
            case "SP":
                // Região metropolitana de SP vs Interior
                int prefix = Integer.parseInt(cep.substring(0, 2));
                if (prefix >= 1 && prefix <= 9) { // Grande SP
                    pacPrice = new BigDecimal("14.90");
                    pacDays = 3;
                    sedexPrice = new BigDecimal("20.90");
                    sedexDays = 1;
                } else { // Interior SP
                    pacPrice = new BigDecimal("17.90");
                    pacDays = 4;
                    sedexPrice = new BigDecimal("24.90");
                    sedexDays = 2;
                }
                break;

            case "RJ":
            case "MG":
            case "ES":
                pacPrice = new BigDecimal("21.90");
                pacDays = 5;
                sedexPrice = new BigDecimal("34.90");
                sedexDays = 2;
                break;

            case "PR":
            case "SC":
            case "RS":
                pacPrice = new BigDecimal("25.90");
                pacDays = 6;
                sedexPrice = new BigDecimal("42.90");
                sedexDays = 3;
                break;

            case "DF":
            case "GO":
            case "MS":
            case "MT":
                pacPrice = new BigDecimal("27.90");
                pacDays = 7;
                sedexPrice = new BigDecimal("46.90");
                sedexDays = 3;
                break;

            case "BA":
            case "PE":
            case "CE":
            case "RN":
            case "PB":
            case "AL":
            case "SE":
            case "PI":
            case "MA":
                pacPrice = new BigDecimal("33.90");
                pacDays = 9;
                sedexPrice = new BigDecimal("58.90");
                sedexDays = 4;
                break;

            case "AM":
            case "PA":
            case "RO":
            case "AC":
            case "RR":
            case "AP":
            case "TO":
            default:
                pacPrice = new BigDecimal("39.90");
                pacDays = 11;
                sedexPrice = new BigDecimal("69.90");
                sedexDays = 5;
                break;
        }

        // Opção 1: PAC / Econômico (com suporte a Frete Grátis)
        boolean isFree = eligibleForFreeShipping;
        options.add(ShippingOptionDTO.builder()
                .id("pac")
                .name(isFree ? "PAC - Correios (Frete Grátis)" : "PAC - Correios")
                .carrier("Correios")
                .price(isFree ? BigDecimal.ZERO : pacPrice)
                .originalPrice(pacPrice)
                .deliveryDays(pacDays)
                .deliveryRange((pacDays - 1) + " a " + (pacDays + 2) + " dias úteis")
                .isFree(isFree)
                .description("Entrega econômica")
                .build());

        // Opção 2: SEDEX / Expresso
        options.add(ShippingOptionDTO.builder()
                .id("sedex")
                .name("SEDEX - Correios Expresso")
                .carrier("Correios")
                .price(sedexPrice)
                .originalPrice(sedexPrice)
                .deliveryDays(sedexDays)
                .deliveryRange(sedexDays + " a " + (sedexDays + 1) + " dias úteis")
                .isFree(false)
                .description("Entrega rápida prioritária")
                .build());

        return options;
    }
}
