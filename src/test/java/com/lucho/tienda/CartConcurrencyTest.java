package com.lucho.tienda;

import com.lucho.tienda.dto.ProductOperationRequest;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Category;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.model.User;
import com.lucho.tienda.model.enums.Role;
import com.lucho.tienda.repository.*;
import com.lucho.tienda.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Removed the property override so schema.sql runs and ddl-auto: validate succeeds
@SpringBootTest
class CartConcurrencyTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DiscountRepository discountRepository;

    private Long cartId;
    private Long userId;
    private final String productCode = "CONCURRENCY-01";

    @BeforeEach
    void setUp() {
        // Clean DB respecting Foreign Key dependencies
        cartRepository.deleteAll();
        discountRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create user assigning the mandatory Role attribute
        User user = new User();
        user.setUsername("Test Concurrency User");
        user.setEmail("test-concurrency@test.com");
        user.setPassword("123456");
        user.setRole(Role.USER);
        user = userRepository.save(user);
        userId = user.getId();

        // 2. Create test category and product
        Category category = new Category();
        category.setName("Test Concurrency Category");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setCode(productCode);
        product.setName("Concurrent Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(100);
        product.setCategory(category);
        productRepository.save(product);

        // 3. Create cart for the user
        Cart cart = cartService.createCart(user.getId());
        cartId = cart.getId();
    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll();
        discountRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void addProduct_ConcurrentRequests_ShouldSerializeWithPessimisticLock()
            throws InterruptedException {

        int numberOfThreads = 3;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch =
                new CountDownLatch(numberOfThreads);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch doneLatch =
                new CountDownLatch(numberOfThreads);

        AtomicInteger concurrencyErrors =
                new AtomicInteger(0);

        ProductOperationRequest request =
                new ProductOperationRequest(
                        cartId,
                        productCode,
                        1
                );

        for (int i = 0; i < numberOfThreads; i++) {

            executorService.submit(() -> {
                try {

                    readyLatch.countDown();

                    // Todos los threads esperan acá
                    startLatch.await();

                    // PESSIMISTIC_WRITE serializa las modificaciones
                    // sobre el mismo carrito.
                    cartService.addProduct(userId, request);

                } catch (Exception e) {

                    concurrencyErrors.incrementAndGet();
                    e.printStackTrace();

                } finally {

                    doneLatch.countDown();
                }
            });
        }

        // Esperamos que todos los threads estén preparados
        readyLatch.await();

        // Los liberamos prácticamente al mismo tiempo
        startLatch.countDown();

        // Esperamos que terminen
        doneLatch.await();

        executorService.shutdown();

        // Ninguna operación debería fallar.
        assertEquals(
                0,
                concurrencyErrors.get(),
                "All concurrent operations should complete successfully."
        );

        Cart finalCart =
                cartRepository.findCartById(cartId)
                        .orElseThrow();

        CartItem finalItem =
                finalCart.getItems()
                        .stream()
                        .filter(item ->
                                item.getProduct()
                                        .getCode()
                                        .equals(productCode))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                3,
                finalItem.getQuantity(),
                "Three concurrent additions of quantity 1 must result in quantity 3."
        );
    }
}