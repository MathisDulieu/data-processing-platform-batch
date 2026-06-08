package com.dataprocessing.batch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DatabasePropertiesTest {

    @Test
    void shouldNotThrow_whenAllPropertiesAreProvided() {
        //Arrange
        DatabaseProperties properties = new DatabaseProperties("url", "username", "password");

        //Act & Assert
        assertThatNoException().isThrownBy(properties::afterPropertiesSet);
    }

    @ParameterizedTest
    @MethodSource("invalidProperties")
    void shouldThrow_whenRequiredPropertyIsMissingOrBlank(String url, String username, String password, String expectedMessage) {
        //Arrange
        DatabaseProperties properties = new DatabaseProperties(url, username, password);

        //Act & Assert
        assertThatThrownBy(properties::afterPropertiesSet)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expectedMessage);
    }

    private static Stream<Arguments> invalidProperties() {
        return Stream.of(
            arguments(null, "username", "password", "batch.datasource.url must be given"),
            arguments(" ", "username", "password", "batch.datasource.url must be given"),
            arguments("url", null, "password", "batch.datasource.username must be given"),
            arguments("url", " ", "password", "batch.datasource.username must be given"),
            arguments("url", "username", null, "batch.datasource.password must be given"),
            arguments("url", "username", " ", "batch.datasource.password must be given")
        );
    }
}
