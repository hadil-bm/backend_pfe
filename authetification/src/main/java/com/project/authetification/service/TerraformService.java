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
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerraformService {

    // ✅ CORRECTION : Pointe vers /app/terraform par défaut dans Docker
    @Value("${terraform.vm.working-dir:/app/terraform}") 
    private String terraformWorkingDir;

    @Async
    public CompletableFuture<String> triggerVmCreation(DemandeVM demande) {
        log.info(">>> [TerraformService] Start provisioning VM ID: {}", demande.getId());
        
        try {
            // 1. Ecrire les variables (tfvars)
            writeTfVars(demande);

            // 2. Init
            log.info(">>> Executing: terraform init in {}", terraformWorkingDir);
            runTerraformCommand(List.of("terraform", "init"));

            // 3. Plan (Optionnel mais recommandé pour debug, on saute ici pour aller vite)
            
            // 4. Apply
            log.info(">>> Executing: terraform apply");
            // -auto-approve est CRUCIAL pour ne pas bloquer
            // -input=false empêche terraform d'attendre une entrée utilisateur
            runTerraformCommand(List.of("terraform", "apply", "-auto-approve", "-input=false"));

            // 5. Récupérer l'IP
            String ip = fetchPublicIp().orElse("IP_NOT_FOUND");
            log.info(">>> SUCCESS! VM IP: {}", ip);
            
            return CompletableFuture.completedFuture(ip);

        } catch (Exception e) {
            log.error(">>> FAILURE Provisioning for Request {}", demande.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void writeTfVars(DemandeVM demande) throws IOException {
        // ✅ CORRECTION : On s'assure que le dossier existe
        Path dirPath = Paths.get(terraformWorkingDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // ✅ CORRECTION : On utilise le chemin du conteneur (/app/terraform/terraform.tfvars)
        Path tfVarsPath = dirPath.resolve("terraform.tfvars");
        
        String osType = (demande.getOsType() != null) ? demande.getOsType() : "Ubuntu";
        
        // Construction du contenu du fichier
        // ⚠️ IMPORTANT : Ces noms de variables (vm_name, resource_group_name...) 
        // DOIVENT exister dans ton fichier variables.tf sinon ça plantera !
        String content = String.format(
            "vm_name = \"vm-%d\"\n" +
            "demande_id = \"%d\"\n" +
            "cpu_cores = %d\n" +
            "ram_gb = %d\n" +
            "os_type = \"%s\"\n" +
            "os_version = \"22.04\"\n" + 
            "create_vnet = true\n" +    
            "admin_username = \"azureuser\"\n" +
            "ssh_public_key = \"\"\n", // Idéalement, passer une vraie clé SSH ici
            
            demande.getId(),
            demande.getId(),
            demande.getCpu(), 
            demande.getRam(), 
            osType
        );
        
        log.info(">>> Writing terraform.tfvars to: {}", tfVarsPath);
        Files.writeString(tfVarsPath, content, StandardCharsets.UTF_8);
    }

    private String runTerraformCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        
        // ✅ CORRECTION : Exécution dans le dossier /app/terraform
        pb.directory(new File(terraformWorkingDir));
        pb.redirectErrorStream(true);

        // Passer les variables d'env pour Azure Login (Service Principal)
        Map<String, String> env = pb.environment();
        // On passe les credentials s'ils sont présents dans l'env du Pod
        if(System.getenv("ARM_CLIENT_ID") != null) env.put("ARM_CLIENT_ID", System.getenv("ARM_CLIENT_ID"));
        if(System.getenv("ARM_CLIENT_SECRET") != null) env.put("ARM_CLIENT_SECRET", System.getenv("ARM_CLIENT_SECRET"));
        if(System.getenv("ARM_SUBSCRIPTION_ID") != null) env.put("ARM_SUBSCRIPTION_ID", System.getenv("ARM_SUBSCRIPTION_ID"));
        if(System.getenv("ARM_TENANT_ID") != null) env.put("ARM_TENANT_ID", System.getenv("ARM_TENANT_ID"));

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // On loggue tout pour voir ce que Terraform dit
                log.info("[Terraform] " + line); 
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
