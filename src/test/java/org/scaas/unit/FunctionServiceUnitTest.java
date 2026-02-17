package org.scaas.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.protocol.mappers.ToFunctionResponse;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.security.CurrentUserService;
import org.scaas.services.impl.FunctionServiceImpl;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FunctionServiceUnitTest {

    @Mock
    private FunctionRepository functionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ToFunctionResponse mapper;

    @InjectMocks
    private FunctionServiceImpl functionService;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("hello@test.com")
                .build();
    }

    @Test
    void create_shouldSaveCurrentUserAsOwner(){

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        when(functionRepository.save(any(Function.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FunctionResponse fakeResponse = FunctionResponse.builder()
                .id(UUID.randomUUID())
                .name("Test1")
                .build();

        when(mapper.toFunctionResponse(any(Function.class))).thenReturn(fakeResponse);

        FunctionResponse result = functionService.createFunction(
                "Test1", "java17", "handler"
        );

        assertNotNull(result);
        assertEquals("Test1", result.getName());

        verify(currentUserService).getCurrentUser();
        verify(functionRepository).save(any(Function.class));
        verify(mapper).toFunctionResponse(any(Function.class));
    }

    @Test
    void listMyFunctions_shouldReturnMappedFunctions(){

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        Function function = Function.builder()
                .id(UUID.randomUUID())
                .name("Test1")
                .owner(mockUser)
                .build();

        when(functionRepository.findByOwner(mockUser)).thenReturn(List.of(function));

        FunctionResponse response = FunctionResponse.builder()
                .id(UUID.randomUUID())
                .name("Test1")
                .build();

        when(mapper.toFunctionResponse(any(Function.class))).thenReturn(response);

        List<FunctionResponse> result = functionService.getFunctions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test1", result.getFirst().getName());

        verify(functionRepository).findByOwner(mockUser);

    }
}
