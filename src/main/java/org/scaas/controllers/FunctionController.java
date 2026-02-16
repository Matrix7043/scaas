package org.scaas.controllers;

import lombok.RequiredArgsConstructor;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.services.FunctionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/function")
@RequiredArgsConstructor
public class FunctionController {

    FunctionService functionService;

    @PostMapping()
    public FunctionResponse create(@RequestBody CreateFunctionRequest request) {
        return functionService.createFunction(
                request.name(),
                request.runtime(),
                request.entryPoint()
        );
    }

    @GetMapping()
    public List<FunctionResponse> list() {
        return functionService.getFunctions();
    }
}
