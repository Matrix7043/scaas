package org.scaas.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.DeploymentResponse;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.services.FunctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final FunctionService functionService;

    @PostMapping()
    public ResponseEntity<FunctionResponse> create(@Valid @RequestBody CreateFunctionRequest request) {
        FunctionResponse response = functionService.createFunction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping()
    public ResponseEntity<Page<FunctionResponse>> list(Pageable pageable) {
        Page<FunctionResponse> response = functionService.getFunctions(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FunctionResponse> updateById(@PathVariable UUID id,
            @Valid @RequestBody UpdateFunctionRequest request) {
        FunctionResponse response = functionService.updateFunctionById(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/artifacts")
    public ResponseEntity<Void> upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        functionService.replaceArtifact(id, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{id}/artifacts")
    public ResponseEntity<String> getArtifactContent(@PathVariable UUID id) {
        String content = functionService.getArtifactContent(id);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(content);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        functionService.deleteFunctionById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FunctionResponse> getById(@PathVariable UUID id) {
        FunctionResponse response = functionService.getFunctionById(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeploymentResponse> deploy(@PathVariable UUID id) {
        DeploymentResponse response = functionService.deployFunction(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
