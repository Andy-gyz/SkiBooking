package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.repository.BookingItemRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CorePersistenceIntegrationTests {

    private static final List<String> CORE_TABLES = List.of(
            "users",
            "resorts",
            "products",
            "lesson_sessions",
            "carts",
            "cart_items",
            "bookings",
            "booking_items",
            "payments");

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ResortRepository resortRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Test
    void flywayCreatesAllCoreTables() {
        Integer tableCount = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (:tableNames)
                """)
                .param("tableNames", CORE_TABLES)
                .query(Integer.class)
                .single();

        assertThat(tableCount).isEqualTo(CORE_TABLES.size());
    }

    @Test
    void repositoriesPersistAndQueryProductsByCategory() {
        Resort resort = new Resort();
        resort.setName("Test Alpine Resort");
        resort.setLocation("Victoria, Australia");
        resort.setStatus(ResortStatus.ACTIVE);
        resortRepository.saveAndFlush(resort);

        Product product = new Product();
        product.setResort(resort);
        product.setName("Test Full Day Lift Pass");
        product.setCategory(ProductCategory.LIFT_TICKET);
        product.setPrice(new BigDecimal("125.00"));
        product.setActive(true);
        productRepository.saveAndFlush(product);

        List<Product> products = productRepository
                .findByResortIdAndCategoryAndActiveTrueOrderByNameAsc(
                        resort.getId(),
                        ProductCategory.LIFT_TICKET);

        assertThat(products)
                .extracting(Product::getName)
                .containsExactly("Test Full Day Lift Pass");
        assertThat(product.getCreatedAt()).isNotNull();
    }

    @Test
    void dashboardCountQueryReturnsZeroWithoutBookings() {
        long quantity = bookingItemRepository.countReservedQuantity(
                ProductCategory.LESSON,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

        assertThat(quantity).isZero();
    }
}
