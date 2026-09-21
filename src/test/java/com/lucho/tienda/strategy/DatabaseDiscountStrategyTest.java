package com.lucho.tienda.strategy;

import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.Category;
import com.lucho.tienda.model.Discount;
import com.lucho.tienda.model.Product;
import com.lucho.tienda.repository.DiscountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseDiscountStrategyTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DatabaseDiscountStrategy discountStrategy;

    private Cart cart;
    private Category mainCategory;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setItems(new HashSet<>());

        mainCategory = new Category();
        mainCategory.setId(1L);
        mainCategory.setName("Proteinas");

        Product product = new Product();
        product.setCode("P01");
        product.setCategory(mainCategory);
        product.setPrice(new BigDecimal("1000.00"));

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2); // Quantity is 2, but discounts are calculated strictly per unit

        cart.getItems().add(cartItem);
    }

    // --- TESTS FOR calculateDiscount(Cart cart) ---

    @Test
    void calculateDiscount_AppliesUnitDiscount_WhenCategoryMatches() {
        Discount discount = new Discount();
        discount.setCategory(mainCategory);
        discount.setPercentage(new BigDecimal("10.00"));
        discount.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        // Expecting 100.00 (10% of 1000.00 base unit price)
        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void calculateDiscount_ReturnsZero_WhenNoActiveDiscounts() {
        when(discountRepository.findByActiveTrue()).thenReturn(List.of());
        BigDecimal result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateDiscount_ReturnsZero_WhenCartIsNull() {
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateDiscount(null)));
    }

    @Test
    void calculateDiscount_ReturnsZero_WhenCartItemsAreNullOrEmpty() {
        cart.setItems(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateDiscount(cart)));

        cart.setItems(new HashSet<>());
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateDiscount(cart)));
    }

    @Test
    void calculateDiscount_UsesFrozenUnitPrice_WhenAvailable() {
        CartItem item = cart.getItems().iterator().next();
        item.setUnitPrice(new BigDecimal("500.00")); // Frozen price lower than catalog (1000.00)

        Discount discount = new Discount();
        discount.setCategory(mainCategory);
        discount.setPercentage(new BigDecimal("10.00"));
        discount.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        // 500.00 * 10% = 50.00 per unit
        BigDecimal result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, new BigDecimal("50.00").compareTo(result));
    }

    @Test
    void calculateDiscount_SkipsInvalidItems_Safely() {
        CartItem badItemProductNull = new CartItem();
        badItemProductNull.setProduct(null);
        cart.getItems().add(badItemProductNull);

        cart.getItems().add(null);

        CartItem badItemCategoryNull = new CartItem();
        Product pWithoutCategory = new Product();
        pWithoutCategory.setPrice(new BigDecimal("100.00"));
        badItemCategoryNull.setProduct(pWithoutCategory);
        cart.getItems().add(badItemCategoryNull);

        Discount discount = new Discount();
        discount.setCategory(mainCategory);
        discount.setPercentage(new BigDecimal("10.00"));
        discount.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        // Expecting 100.00 for the only valid item in the cart
        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void calculateDiscount_HandlesNullOrInvalidQuantity_Safely() {
        cart.getItems().iterator().next().setQuantity(null);

        Discount discount = new Discount();
        discount.setCategory(mainCategory);
        discount.setPercentage(new BigDecimal("10.00"));
        discount.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, new BigDecimal("100.00").compareTo(result));

        cart.getItems().iterator().next().setQuantity(-5);
        result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void calculateDiscount_ReturnsZero_WhenBasePriceIsNullOrZeroOrNegative() {
        Product p = cart.getItems().iterator().next().getProduct();
        p.setPrice(null);

        Discount discount = new Discount();
        discount.setCategory(mainCategory);
        discount.setPercentage(new BigDecimal("10.00"));
        discount.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateDiscount(cart)));

        p.setPrice(BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateDiscount(cart)));

        p.setPrice(new BigDecimal("-50.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateDiscount(cart)));
    }

    @Test
    void calculateDiscount_IgnoresDiscounts_WithNullCategoryOrPercentage() {
        Discount d1 = new Discount();
        d1.setCategory(mainCategory);
        d1.setPercentage(null);
        d1.setActive(true);

        Discount d2 = new Discount();
        d2.setCategory(null);
        d2.setPercentage(new BigDecimal("10.00"));
        d2.setActive(true);

        Discount d3 = new Discount();
        Category catWithoutId = new Category();
        d3.setCategory(catWithoutId);
        d3.setPercentage(new BigDecimal("10.00"));
        d3.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(d1, d2, d3));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateDiscount_IgnoresDiscounts_WithZeroOrNegativePercentageOrInactive() {
        Discount d1 = new Discount();
        d1.setCategory(mainCategory);
        d1.setPercentage(BigDecimal.ZERO);
        d1.setActive(true);

        Discount d2 = new Discount();
        d2.setCategory(mainCategory);
        d2.setPercentage(new BigDecimal("-10.00"));
        d2.setActive(true);

        Discount d3 = new Discount();
        d3.setCategory(mainCategory);
        d3.setPercentage(new BigDecimal("10.00"));
        d3.setActive(false);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(d1, d2, d3));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateDiscount_HandlesDuplicateCategoriesInDiscounts_ResolvesFirst() {
        Discount d1 = new Discount();
        d1.setCategory(mainCategory);
        d1.setPercentage(new BigDecimal("10.00"));
        d1.setActive(true);

        Discount d2 = new Discount();
        d2.setCategory(mainCategory);
        d2.setPercentage(new BigDecimal("20.00"));
        d2.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(d1, d2));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        // Expecting 100.00 based on the first matched percentage (10%)
        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void calculateDiscount_ReturnsZero_WhenCategoryDoesNotMatchAnyDiscount() {
        Category differentCategory = new Category();
        differentCategory.setId(99L);

        Discount discount = new Discount();
        discount.setCategory(differentCategory);
        discount.setPercentage(new BigDecimal("10.00"));
        discount.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(discount));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateDiscount_AppliesMultipleDiscounts_ForDifferentCategories() {
        Category cat2 = new Category();
        cat2.setId(2L);

        Product p2 = new Product();
        p2.setCode("V01");
        p2.setCategory(cat2);
        p2.setPrice(new BigDecimal("500.00"));

        CartItem item2 = new CartItem();
        item2.setProduct(p2);
        item2.setQuantity(1);
        cart.getItems().add(item2);

        Discount d1 = new Discount();
        d1.setCategory(mainCategory);
        d1.setPercentage(new BigDecimal("10.00"));
        d1.setActive(true);

        Discount d2 = new Discount();
        d2.setCategory(cat2);
        d2.setPercentage(new BigDecimal("20.00"));
        d2.setActive(true);

        when(discountRepository.findByActiveTrue()).thenReturn(List.of(d1, d2));

        BigDecimal result = discountStrategy.calculateDiscount(cart);
        // Item 1: 1000 * 10% = 100.00
        // Item 2: 500 * 20% = 100.00
        // Total expected: 200.00
        assertEquals(0, new BigDecimal("200.00").compareTo(result));
    }

    // --- TESTS FOR calculateItemDiscount(CartItem item, Map<Long, BigDecimal> activeDiscountsMap) ---

    @Test
    void calculateItemDiscount_ReturnsZero_WhenItemProductOrCategoryIsNull() {
        Map<Long, BigDecimal> map = Map.of(1L, new BigDecimal("10.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateItemDiscount(null, map)));

        CartItem itemNullProduct = new CartItem();
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateItemDiscount(itemNullProduct, map)));

        CartItem itemNullCategory = new CartItem();
        itemNullCategory.setProduct(new Product());
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateItemDiscount(itemNullCategory, map)));
    }

    @Test
    void calculateItemDiscount_ReturnsZero_WhenDiscountsMapIsEmpty() {
        CartItem item = cart.getItems().iterator().next();

        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateItemDiscount(item, Map.of())));
    }

    @Test
    void calculateItemDiscount_ThrowsNullPointerException_WhenMapIsNull() {
        CartItem item = cart.getItems().iterator().next();

        assertThrows(NullPointerException.class, () -> discountStrategy.calculateItemDiscount(item, null));
    }

    @Test
    void calculateItemDiscount_ReturnsZero_WhenBasePriceInvalid() {
        CartItem item = cart.getItems().iterator().next();
        item.getProduct().setPrice(null);

        Map<Long, BigDecimal> map = Map.of(1L, new BigDecimal("10.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateItemDiscount(item, map)));

        item.getProduct().setPrice(new BigDecimal("-10.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(discountStrategy.calculateItemDiscount(item, map)));
    }

    @Test
    void calculateItemDiscount_AppliesDiscount_WithFrozenUnitPrice() {
        CartItem item = cart.getItems().iterator().next();
        item.setUnitPrice(new BigDecimal("500.00"));
        item.setQuantity(null); // Ignored in unit-discount calculation

        Map<Long, BigDecimal> map = Map.of(1L, new BigDecimal("10.00"));

        // 500.00 * 10% = 50.00
        BigDecimal result = discountStrategy.calculateItemDiscount(item, map);
        assertEquals(0, new BigDecimal("50.00").compareTo(result));
    }

    @Test
    void calculateItemDiscount_FiltersOutInvalidOrNonMatchingDiscounts() {
        CartItem item = cart.getItems().iterator().next();

        // Map representing active discounts for category ID 1L (15%) and non-matching 99L (10%)
        Map<Long, BigDecimal> map = Map.of(
                1L, new BigDecimal("15.00"),
                99L, new BigDecimal("10.00")
        );

        // 1000.00 * 15% = 150.00
        BigDecimal result = discountStrategy.calculateItemDiscount(item, map);
        assertEquals(0, new BigDecimal("150.00").compareTo(result));
    }
}