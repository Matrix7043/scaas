package org.scaas.testdata;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class MatrixGenerator {
    static Stream<Arguments> actionPairs() {
        return Stream.of(
                Arguments.of(Action.DEPLOY, Action.UPDATE, 1, 2),
                Arguments.of(Action.UPDATE, Action.DEPLOY, 1, 2),
                Arguments.of(Action.UPDATE, Action.UPDATE, 1, 1),
                Arguments.of(Action.DEPLOY, Action.DEPLOY, 2, 2)
        );
    }
}
