package org.scaas.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.services.FunctionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    @PostMapping()
    public FunctionResponse create(@Valid @RequestBody CreateFunctionRequest request) {
        return functionService.createFunction(request);
    }

    @GetMapping()
    public List<FunctionResponse> list() {
        return functionService.getFunctions();
    }

    @PutMapping("/{id}")
    public FunctionResponse updateById(@PathVariable UUID id,@Valid @RequestBody UpdateFunctionRequest request) {
        return functionService.updateFunctionById(id, request);
    }

    @DeleteMapping("/{id}")
    public FunctionResponse deleteById(@PathVariable UUID id) {
        return functionService.deleteFunctionById(id);
    }

    @GetMapping("/{id}")
    public FunctionResponse getById(@PathVariable UUID id) {
        return functionService.getFunctionById(id);
    }
}
