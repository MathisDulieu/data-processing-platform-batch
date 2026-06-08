package com.dataprocessing.batch.reader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileParserFactoryTest {

    @Mock
    private CsvFileParser csvFileParser;

    @Mock
    private JsonFileParser jsonFileParser;

    @InjectMocks
    private FileParserFactory factory;

    @ParameterizedTest
    @ValueSource(strings = {"text/csv", "application/csv"})
    void shouldReturnCsvParser_whenMimeTypeIsCsv(String mimeType) {
        //Arrange

        //Act
        FileParser result = factory.getParser(mimeType);

        //Assert
        assertThat(result).isSameAs(csvFileParser);
    }

    @Test
    void shouldReturnJsonParser_whenMimeTypeIsJson() {
        //Arrange

        //Act
        FileParser result = factory.getParser("application/json");

        //Assert
        assertThat(result).isSameAs(jsonFileParser);
    }

    @Test
    void shouldThrowIllegalArgumentException_whenMimeTypeIsUnsupported() {
        //Arrange

        //Act & Assert
        assertThatThrownBy(() -> factory.getParser("application/xml"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported file type: application/xml");
    }
}
