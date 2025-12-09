package com.project.authetification.controller.vm;

import com.project.authetification.model.DemandeStatus;
import com.project.authetification.model.DemandeVM;
import com.project.authetification.repository.DemandeVMRepository;
import com.project.authetification.service.provisioning.TerraformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController("vmDemandeController")
@RequestMapping("/api/demande")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeVMRepository repository;
    private final TerraformService terraformService;

    @PostMapping
    public ResponseEntity<DemandeVM> create(@RequestBody DemandeVM body) {
        body.setStatus(DemandeStatus.PENDING_CLOUD);
        DemandeVM saved = repository.save(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/validate-support")
    public ResponseEntity<DemandeVM> validateSupport(@PathVariable Long id) {
        DemandeVM demande = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DemandeVM not found"));

        demande.setStatus(DemandeStatus.APPROVED);
        repository.save(demande);
        terraformService.triggerVmCreation(demande);

        return ResponseEntity.ok(demande);
    }
}

