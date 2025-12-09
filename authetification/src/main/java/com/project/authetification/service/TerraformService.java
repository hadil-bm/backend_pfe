package com.project.authetification.service;

import com.project.authetification.model.DemandeVM;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerraformService {

    // Assure-toi que ce dossier existe dans le conteneur Docker !
    @Value("${terraform.vm.working-dir:/app/terraform}") 
    private String terraformWorkingDir;

    @Async
    public CompletableFuture<String> triggerVmCreation(DemandeVM demande) {
        log.info(">>> [TerraformService] Start provisioning VM ID: {}", demande.getId());
        
        try {
            // 1. Ecrire les variables
            writeTfVars(demande);

            // 2. Init
            log.info(">>> Executing: terraform init");
            runTerraformCommand(List.of("terraform", "init"));

            // 3. Apply
            log.info(">>> Executing: terraform apply");
            // -auto-approve est CRUCIAL pour ne pas bloquer
            runTerraformCommand(List.of("terraform", "apply", "-auto-approve"));

            // 4. Récupérer l'IP
            String ip = fetchPublicIp().orElse("IP_NOT_FOUND");
            log.info(">>> SUCCESS! VM IP: {}", ip);
            
            return CompletableFuture.completedFuture(ip);

        } catch (Exception e) {
            log.error(">>> FAILURE Provisioning for Request {}", demande.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void writeTfVars(DemandeVM demande) throws IOException {
        // ... (création dossier) ...

        Path tfVarsPath = Path.of(terraformWorkingDir, "terraform.tfvars");
        String osType = (demande.getOsType() != null) ? demande.getOsType() : "Ubuntu";
        
        // CORRECTION : On envoie toutes les variables requises par variables.tf
        String content = String.format(
            "vm_name = \"vm-%d\"\n" +
            "demande_id = \"%d\"\n" +  // Ajouté
            "cpu_cores = %d\n" +
            "ram_gb = %d\n" +
            "os_type = \"%s\"\n" +
            "os_version = \"22.04\"\n" + // Valeur par défaut forcée ici
            "create_vnet = true\n" +    // Pour créer le réseau
            "admin_username = \"azureuser\"\n" +
            "ssh_public_key = \"\"\n", // Mettre une clé SSH publique réelle ici si possible
            
            demande.getId(),
            demande.getId(), // demande_id
            demande.getCpu(), 
            demande.getRam(), 
            osType
        );
        Files.writeString(tfVarsPath, content, StandardCharsets.UTF_8);
    }

    private String runTerraformCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(terraformWorkingDir));
        pb.redirectErrorStream(true);

        // Passer les variables d'env pour Azure Login (Service Principal)
        Map<String, String> env = pb.environment();
        if(System.getenv("ARM_CLIENT_ID") != null) env.put("ARM_CLIENT_ID", System.getenv("ARM_CLIENT_ID"));
        if(System.getenv("ARM_CLIENT_SECRET") != null) env.put("ARM_CLIENT_SECRET", System.getenv("ARM_CLIENT_SECRET"));
        if(System.getenv("ARM_SUBSCRIPTION_ID") != null) env.put("ARM_SUBSCRIPTION_ID", System.getenv("ARM_SUBSCRIPTION_ID"));
        if(System.getenv("ARM_TENANT_ID") != null) env.put("ARM_TENANT_ID", System.getenv("ARM_TENANT_ID"));

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[TF Output] " + line); // Log en debug pour ne pas polluer
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Terraform Error (Code " + exitCode + ")\nOutput:\n" + output.toString());
        }

        return output.toString();
    }

    private Optional<String> fetchPublicIp() {
        try {
            String rawOutput = runTerraformCommand(List.of("terraform", "output", "-raw", "vm_public_ip"));
            if (rawOutput != null && !rawOutput.isBlank() && !rawOutput.contains("No outputs")) {
                return Optional.of(rawOutput.trim());
            }
        } catch (Exception e) {
            log.warn("IP fetch warning: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
