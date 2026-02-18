package org.scaas.controllers;

import lombok.RequiredArgsConstructor;
import org.scaas.protocol.requests.CreateFunctionRequest;
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
    public FunctionResponse create(@RequestBody CreateFunctionRequest request) {
        return functionService.createFunction(request);
    }

    @GetMapping()
    public List<FunctionResponse> list() {
        return functionService.getFunctions();
    }

    @GetMapping("/{id}")
    public FunctionResponse getById(@PathVariable UUID id) {
        return functionService.getFunctionById(id);
    }
}
