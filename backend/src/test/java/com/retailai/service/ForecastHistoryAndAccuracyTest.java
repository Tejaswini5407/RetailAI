package com.retailai.service;

import com.retailai.domain.UserRole;
import com.retailai.dto.auth.AuthResponse;
import com.retailai.dto.auth.RegisterRequest;
import com.retailai.dto.forecast.*;
import com.retailai.dto.product.CreateProductRequestDto;
import com.retailai.dto.product.ProductResponseDto;
import com.retailai.dto.sale.CreateSaleRequestDto;
import com.retailai.dto.store.CreateStoreRequestDto;
import com.retailai.dto.store.StoreResponseDto;
import com.retailai.repository.*;
import com.retailai.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
public class ForecastHistoryAndAccuracyTest {

    @Autowired private ForecastService forecastService;
    @Autowired private AuthService authService;
    @Autowired private StoreService storeService;
    @Autowired private ProductService productService;
    @Autowired private SaleService saleService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ForecastHistoryRepository forecastHistoryRepository;

    @MockBean private RestTemplate restTemplate;

    private AuthResponse authTenant;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        saleRepository.deleteAll();
        forecastHistoryRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        RegisterRequest req = new RegisterRequest("FC-ACC-ORG", "Forecast Accuracy Org", "FC Admin", "admin@fcacc.com", "Password123!", UserRole.ADMIN);
        authTenant = authService.register(req);
        setSecurityContext(authTenant);

        Map<String, Object> mockMlResponse = Map.of(
            "model_name", "Random Forest",
            "model_version", "1.0.0",
            "predictions", List.of(Map.of("predicted_demand", 20.0, "rounded_demand", 20))
        );
        Mockito.when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(mockMlResponse));
    }

    private void setSecurityContext(AuthResponse auth) {
        CustomUserDetails userDetails = new CustomUserDetails(
            auth.getUser().getId(),
            auth.getUser().getEmail(),
            "passwordHash",
            auth.getUser().getOrganizationId(),
            auth.getUser().getOrganizationCode(),
            auth.getUser().getRole()
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("1. Forecast generation persists history record")
    void testForecastGenerationPersistsHistory() {
        StoreResponseDto store = storeService.createStore(new CreateStoreRequestDto("STORE-FC-1", "FC Store 1", "CA", "LA"));
        ProductResponseDto product = productService.createProduct(new CreateProductRequestDto("SKU-FC-1", "FC Product 1", "Cat", "Dept"));

        ForecastRequestDto req = new ForecastRequestDto(store.getId(), product.getId(), LocalDate.now().plusDays(1), 7, null);
        ForecastResponseDto response = forecastService.generateForecast(req);

        assertNotNull(response);
        assertEquals(20.0, response.getPredictedDemand());

        List<ForecastHistoryResponseDto> history = forecastService.getForecastHistory();
        assertFalse(history.isEmpty());
        assertEquals("STORE-FC-1", history.get(0).getStoreCode());
        assertEquals("SKU-FC-1", history.get(0).getSku());
        assertEquals(7, history.get(0).getHorizonDays());
    }

    @Test
    @DisplayName("2. Accuracy returns insufficient data notice when no actual sales match target date")
    void testAccuracyInsufficientDataNotice() {
        StoreResponseDto store = storeService.createStore(new CreateStoreRequestDto("STORE-FC-2", "FC Store 2", "CA", "LA"));
        ProductResponseDto product = productService.createProduct(new CreateProductRequestDto("SKU-FC-2", "FC Product 2", "Cat", "Dept"));

        // Generate forecast for future date
        ForecastRequestDto req = new ForecastRequestDto(store.getId(), product.getId(), LocalDate.now().plusDays(10), 7, null);
        forecastService.generateForecast(req);

        ForecastAccuracyResponseDto accuracy = forecastService.calculateForecastAccuracy();
        assertFalse(accuracy.isSufficientData());
        assertEquals("Accuracy will be available after actual sales data is recorded for the forecast period.", accuracy.getMessage());
    }

    @Test
    @DisplayName("3. Accuracy calculates MAE, RMSE, and WAPE when actual sales data exists")
    void testAccuracyCalculationWithActualSales() {
        StoreResponseDto store = storeService.createStore(new CreateStoreRequestDto("STORE-FC-3", "FC Store 3", "CA", "LA"));
        ProductResponseDto product = productService.createProduct(new CreateProductRequestDto("SKU-FC-3", "FC Product 3", "Cat", "Dept"));

        LocalDate targetDate = LocalDate.now().minusDays(1);

        // Generate forecast for targetDate (predictedDemand = 20.0)
        ForecastRequestDto req = new ForecastRequestDto(store.getId(), product.getId(), targetDate, 1, null);
        forecastService.generateForecast(req);

        // Record actual sale (unitsSold = 25)
        saleService.createSale(new CreateSaleRequestDto(store.getId(), product.getId(), targetDate, 25, java.math.BigDecimal.valueOf(4.99)));

        ForecastAccuracyResponseDto accuracy = forecastService.calculateForecastAccuracy();
        assertTrue(accuracy.isSufficientData());
        assertEquals(1, accuracy.getTotalEvaluated());
        assertEquals(5.0, accuracy.getMae()); // |25 - 20| = 5.0
        assertEquals(5.0, accuracy.getRmse());
        assertEquals(20.0, accuracy.getWape()); // (5 / 25) * 100 = 20.0%
    }

    @Test
    @DisplayName("4. Forecast explanation returns real feature factors")
    void testForecastExplanation() {
        StoreResponseDto store = storeService.createStore(new CreateStoreRequestDto("STORE-FC-4", "FC Store 4", "CA", "LA"));
        ProductResponseDto product = productService.createProduct(new CreateProductRequestDto("SKU-FC-4", "FC Product 4", "Cat", "Dept"));

        ForecastExplanationResponseDto exp = forecastService.getForecastExplanation(store.getId(), product.getId());
        assertNotNull(exp);
        assertEquals("STORE-FC-4", exp.getStoreCode());
        assertEquals("SKU-FC-4", exp.getSku());
        assertFalse(exp.getFactors().isEmpty());
    }
}
