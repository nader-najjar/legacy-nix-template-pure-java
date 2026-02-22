package io.template.samplebusinesslayer;

import io.template.samplebusinesslayer.exceptions.CalculationException;
import io.template.samplebusinesslayer.models.CalculationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    private Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    @Test
    void calculatesAddition() {
        CalculationRequest request = new CalculationRequest(2.0, 3.0, "add");

        double result = calculator.calculate(request).result();

        assertEquals(5.0, result);
    }

    @Test
    void calculatesDivision() {
        CalculationRequest request = new CalculationRequest(10.0, 4.0, "DIVIDE");

        double result = calculator.calculate(request).result();

        assertEquals(2.5, result);
    }

    @Test
    void throwsOnDivisionByZero() {
        CalculationRequest request = new CalculationRequest(10.0, 0.0, "divide");

        assertThrows(CalculationException.class, () -> calculator.calculate(request));
    }

    @Test
    void calculatesSubtraction() {
        CalculationRequest request = new CalculationRequest(10.0, 4.0, "SUBTRACT");

        double result = calculator.calculate(request).result();

        assertEquals(6.0, result);
    }

    @Test
    void calculatesMultiplication() {
        CalculationRequest request = new CalculationRequest(3.0, 4.0, "multiply");

        double result = calculator.calculate(request).result();

        assertEquals(12.0, result);
    }

    @Test
    void throwsOnUnknownOperation() {
        CalculationRequest request = new CalculationRequest(10.0, 5.0, "UNKNOWN");

        assertThrows(CalculationException.class, () -> calculator.calculate(request));
    }
}
