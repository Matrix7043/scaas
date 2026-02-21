package org.scaas.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scaas.domain.entites.Function;
import org.scaas.domain.entites.User;
import org.scaas.domain.enumerations.Runtime;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.exceptions.ResourceNotFoundException;
import org.scaas.protocol.mappers.ToFunctionResponse;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.security.CurrentUserService;
import org.scaas.services.impl.FunctionServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FunctionServiceUnitTest {

    @Mock
    private FunctionRepository functionRepository;

    @Mock
    private CurrentUserService currentUserService;

    private final ToFunctionResponse mapper =  new ToFunctionResponse();

    private FunctionServiceImpl functionService;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("hello@test.com")
                .build();
        functionService = new FunctionServiceImpl(currentUserService,
                functionRepository,
                mapper);
    }

    @Test
    void create_shouldSaveCurrentUserAsOwner(){

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        when(functionRepository.save(any(Function.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateFunctionRequest request = CreateFunctionRequest.builder()
                .name("Test1")
                .runtime(Runtime.PYTHON)
                .entryPoint("main")
                .build();

        FunctionResponse result = functionService.createFunction(request);

        assertNotNull(result);
        assertEquals("Test1", result.name());

        verify(currentUserService).getCurrentUser();
        verify(functionRepository).save(any(Function.class));
    }

    @Test
    void listMyFunctions_shouldReturnMappedFunctionsWithPagination(){

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        Function function = Function.builder()
                .id(UUID.randomUUID())
                .name("Test1")
                .owner(mockUser)
                .build();

        Function function2 = Function.builder()
                .id(UUID.randomUUID())
                .name("Test2")
                .owner(mockUser)
                .build();

        Pageable pageable = PageRequest.of(0, 2);
        Page<Function> page = new PageImpl<>(List.of(function, function2), pageable, 5);

        when(functionRepository.findByOwnerAndDeletedAtIsNull(mockUser, pageable)).thenReturn(page);

        Page<FunctionResponse> result = functionService.getFunctions(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals("Test1", result.getContent().getFirst().name());

        verify(functionRepository).findByOwnerAndDeletedAtIsNull(mockUser, pageable);

    }

    @Test
    void updateFunctionById_shouldUpdateFunction(){

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        Function request = Function.builder()
                        .id(UUID.randomUUID())
                        .name("Test1")
                        .runtime(Runtime.PYTHON)
                        .entryPoint("handler")
                        .build();
        when(functionRepository.findByIdAndOwnerAndDeletedAtIsNull(any(UUID.class), eq(mockUser))).thenReturn(Optional.of(request));
        when(functionRepository.save(any(Function.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UpdateFunctionRequest updateFunctionRequest = UpdateFunctionRequest.builder()
                .name("Test2")
                .entryPoint("main")
                .runtime(Runtime.PYTHON)
                .build();

        FunctionResponse updated = functionService.updateFunctionById(UUID.randomUUID(), updateFunctionRequest);

        assertNotNull(updated);
        assertEquals("Test2", updated.name());
        assertEquals("main", updated.entryPoint());
        verify(functionRepository).findByIdAndOwnerAndDeletedAtIsNull(any(UUID.class), eq(mockUser));
        verify(functionRepository).save(any(Function.class));

    }

    @Test
    void deleteFunctionById_shouldDeleteFunction(){

        when(currentUserService.getCurrentUser()).thenReturn(mockUser);
        UUID functionId = UUID.randomUUID();

        Function function = Function.builder()
                .id(functionId)
                .name("Test1")
                .owner(mockUser)
                .runtime(Runtime.PYTHON)
                .entryPoint("main")
                .build();

        when(functionRepository.findByIdAndOwnerAndDeletedAtIsNull(eq(functionId), eq(mockUser))).thenReturn(Optional.of(function));

        functionService.deleteFunctionById(functionId);
        verify(functionRepository).findByIdAndOwnerAndDeletedAtIsNull(eq(functionId), eq(mockUser));
        verify(functionRepository).save(eq(function));

    }

    @Test
    void findById_shouldReturnFunction(){
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID id = UUID.randomUUID();
        Function function = Function.builder()
                .id(id)
                .name("Test1")
                .owner(mockUser)
                .runtime(Runtime.PYTHON)
                .entryPoint("handler")
                .build();

        when(functionRepository.findByIdAndOwnerAndDeletedAtIsNull(eq(id), eq(mockUser))).thenReturn(Optional.of(function));

        FunctionResponse functionResponse = functionService.getFunctionById(id);

        assertNotNull(functionResponse);
        assertEquals("Test1", functionResponse.name());
        verify(functionRepository).findByIdAndOwnerAndDeletedAtIsNull(eq(id), eq(mockUser));
    }

    @Test
    void findById_unauthenticated_shouldReturnFunction(){
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID id = UUID.randomUUID();

        when(functionRepository.findByIdAndOwnerAndDeletedAtIsNull(eq(id), eq(mockUser))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                ()-> functionService.getFunctionById(id)
        );
    }

    @Test
    void deleteById_shouldThrowResourceNotFoundExceptionIfResourceNotFound(){
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID id = UUID.randomUUID();
        when(functionRepository.findByIdAndOwnerAndDeletedAtIsNull(eq(id), eq(mockUser))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> functionService.deleteFunctionById(id));
    }

    @Test
    void updateById_shouldThrowResourceNotFoundExceptionIfResourceNotFound(){
        when(currentUserService.getCurrentUser()).thenReturn(mockUser);

        UUID id = UUID.randomUUID();
        UpdateFunctionRequest request = mock(UpdateFunctionRequest.class);
        when(functionRepository.findByIdAndOwnerAndDeletedAtIsNull(eq(id), eq(mockUser))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> functionService.updateFunctionById(id, request));
    }


}
