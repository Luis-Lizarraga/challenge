package com.lucho.tienda.controller;

import com.lucho.tienda.annotation.ApiStandardErrorResponses;
import com.lucho.tienda.constant.ApiEndpointConstants;
import com.lucho.tienda.constant.MessageConstants;
import com.lucho.tienda.constant.OpenApiMessageConstants;
import com.lucho.tienda.dto.*;
import com.lucho.tienda.model.Cart;
import com.lucho.tienda.model.CartItem;
import com.lucho.tienda.model.enums.CartStatus;
import com.lucho.tienda.security.UserDetailsImpl;
import com.lucho.tienda.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lucho.tienda.constant.ApiEndpointConstants.*;
import static com.lucho.tienda.constant.ApiFieldConstants.FIELD_PRODUCT_CODE;
import static com.lucho.tienda.constant.ApiFieldConstants.PARAM_CART_ID;
import static com.lucho.tienda.constant.OpenApiMessageConstants.*;

@RestController
@RequestMapping(ApiEndpointConstants.ENDPOINT_CARTS)
@RequiredArgsConstructor
@ApiStandardErrorResponses
@Tag(name = "Cart Controller", description = OpenApiMessageConstants.TAG_CART_DESC)
public class CartController {

    private final CartService cartService;

    @Operation(summary = OP_CREATE_CART, description = OP_CREATE_CART_DESC)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = CREATED)
    })
    @PostMapping
    public ResponseEntity<CartResponse> createCart(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Retrieve the user ID directly from the authenticated principal in memory
        CartResponse cart = cartService.createCart(userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @Operation(summary = OP_ADD_PRODUCT)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK_PRODUCT),
            @ApiResponse(responseCode = "409", description = CONFLICT)
    })
    @PostMapping(SUB_ENDPOINT_CART_PRODUCTS)
    public ResponseEntity<CartResponse> addProduct(
            @PathVariable(PARAM_CART_ID) Long cartId,
            @RequestBody @Validated AddProductRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Passes the dynamic quantity requested by the user
        ProductOperationRequest serviceRequest = new ProductOperationRequest(
                cartId,
                request.productCode(),
                request.quantity()
        );
        CartResponse cart = cartService.addProduct(userDetails.getId(), serviceRequest);
        return ResponseEntity.ok(cart);
    }
    @Operation(summary = OP_UPDATE_PRODUCT_QTY)
    @PutMapping(SUB_ENDPOINT_REMOVE_PRODUCT)
    public ResponseEntity<CartResponse> updateProductQuantity(
            @PathVariable(PARAM_CART_ID) Long cartId,
            @PathVariable(FIELD_PRODUCT_CODE) String productCode,
            @RequestBody @Validated UpdateQuantityRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        ProductOperationRequest serviceRequest = new ProductOperationRequest(
                cartId,
                productCode,
                request.quantity()
        );
        CartResponse cart = cartService.updateProductQuantity(userDetails.getId(), serviceRequest);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = OP_REMOVE_PRODUCT)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK_PRODUCT),
            @ApiResponse(responseCode = "409", description = CONFLICT)
    })
    @DeleteMapping(SUB_ENDPOINT_REMOVE_PRODUCT)
    public ResponseEntity<CartResponse> removeProduct(
            @PathVariable(PARAM_CART_ID) Long cartId,
            @PathVariable(FIELD_PRODUCT_CODE) String productCode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        CartResponse cart = cartService.removeProduct(userDetails.getId(), cartId, productCode);

        return ResponseEntity.ok(cart);
    }

    @GetMapping(SUB_ENDPOINT_CART_PRODUCTS)
    public ResponseEntity<List<CartItemResponse>> getCartProducts(@PathVariable(PARAM_CART_ID) Long cartId,
                                                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<CartItemResponse> items = cartService.getCartProducts(userDetails.getId(), cartId);
        return ResponseEntity.ok(items);
    }

    @Operation(summary = OP_GET_USER_CARTS)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK_LIST)
    })
    @GetMapping
    public ResponseEntity<List<CartResponse>> getUserCarts(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                           @RequestParam(required = false) CartStatus status) {
        // Fully decoupled from token parsing
        List<CartResponse> carts = cartService.getUserCarts(userDetails.getId(), status);
        return ResponseEntity.ok(carts);
    }

    @Operation(summary = OP_PROCESS_ORDER)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = ACCEPTED),
            @ApiResponse(responseCode = "409", description = CONFLICT)
    })
    @PostMapping(SUB_ENDPOINT_PROCESS_ORDER)
    public ResponseEntity<AsyncOrderResponse> processOrder(@PathVariable(PARAM_CART_ID) Long cartId,
                                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        cartService.initiateCheckout(userDetails.getId(), cartId);
        AsyncOrderResponse response = new AsyncOrderResponse(MessageConstants.PROCESSING_ORDER);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = OP_GET_CART)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = OK_CART)
    })
    @GetMapping(SUB_ENDPOINT_GET_CART)
    public ResponseEntity<CartResponse> getCart(@PathVariable(PARAM_CART_ID) Long cartId,
                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse cart = cartService.getCartById(userDetails.getId(), cartId);
        return ResponseEntity.ok(cart);
    }

    @GetMapping(SUB_ENDPOINT_CHECKOUT_STATUS)
    public ResponseEntity<CheckoutStatusResponse> getCheckoutStatus(
            @PathVariable(PARAM_CART_ID) Long cartId,
            @AuthenticationPrincipal @NonNull UserDetailsImpl userDetails) {

        Cart cart = cartService.getCheckoutStatus(
                userDetails.getId(),
                cartId
        );

        return ResponseEntity.ok(
                CheckoutStatusResponse.fromEntity(cart)
        );
    }
}