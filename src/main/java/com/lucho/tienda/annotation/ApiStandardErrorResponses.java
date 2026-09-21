package com.lucho.tienda.annotation;

import com.lucho.tienda.constant.OpenApiMessageConstants;
import com.lucho.tienda.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE}) // It allows to use it in classes or methods
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = OpenApiMessageConstants.BAD_REQUEST, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = OpenApiMessageConstants.UNAUTHORIZED, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = OpenApiMessageConstants.NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = OpenApiMessageConstants.ERROR, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public @interface ApiStandardErrorResponses {
}