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

    @Value("${terraform.vm.working-dir:/app/terraform}") 
    private String terraformWorkingDir;

    @Async
    public CompletableFuture<String> triggerVmCreation(DemandeVM demande) {
        log.info(">>> [TerraformService] Start provisioning VM ID: {}", demande.getId());
        
        try {
            // 1. Ecrire les variables
            writeTfVars(demande);

            // 2. Init
            log.info(">>> Executing: terraform init in {}", terraformWorkingDir);
            runTerraformCommand(List.of("terraform", "init"));

            // 3. Apply
            log.info(">>> Executing: terraform apply");
            runTerraformCommand(List.of("terraform", "apply", "-auto-approve", "-input=false"));

            // 4. Récupérer l'IP (Privée maintenant, car Publique est N/A)
            String ip = fetchIpAddress().orElse("IP_NOT_FOUND");
            log.info(">>> SUCCESS! VM Private IP: {}", ip);
            
            return CompletableFuture.completedFuture(ip);

        } catch (Exception e) {
            log.error(">>> FAILURE Provisioning for Request {}", demande.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void writeTfVars(DemandeVM demande) throws IOException {
        Path dirPath = Paths.get(terraformWorkingDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path tfVarsPath = dirPath.resolve("terraform.tfvars");
        
        // --- TA CLÉ SSH (Indispensable pour Azure Linux) ---
        String maCleSSH = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDSPx8QQmscKlBcSRLsh3ROkBFuG5jG06vmzwpuYBN07XHKBMIyIedlOB3o1yQlTn/dzTapavCBBK/cFvNDm+n0M2iBZo+R9rMZQwyeFj3FQk1hNmAKsGXry+8RiKg2OfJQIUNbOrQcG7dkJlR+jBCt6CKga73YokP1EMttEMF2CZfac0bvnTy1HtdpeUh2Yzkpd4k//z7+fJhRgGpRBC+6THGm9j0eK/jsPo12ICssoQGC7JFhS2MDdgtxWR3Clz7GXb6RkOBn/NHuzEbyjmkYrOC6eJ/XxSo3H6N6mLN9LLqy5gqL1AxZ8BgAInSMxB10stSJIe1o4q/6J7Tcgi19SnuSBT53XyuLA2PfMNm1wgQjXueZpXTCcytcG7wIdud8u/2vlNkmzUtIlKgbKVmEt0hhyYPSTkpLOS63Fhfxa2J07WMMwbRQD8eUqzdr1E0vRtZdTwTgDM3N+20tRNZZIzTYvbgSHp7NYHULox+PGX0nxh/uOznQivixB5pAml1YNvhXChLf+4dHbcIAkqqEggmyOkHUURxKveEwiUWGXvP6v6YDHh9OLv1Na+dJu01mNIia39O5Bl+w8sWRGEkRlP9vOZHJ03u8MhcLzSWre3gCV8ak5348KPw9Gw5aFGvXraVxxJOoh1sstpQb3TvSUuA/VSrZUgU8f2S51NgTkQ== hadilbenmasseoud@SandboxHost-639009763172304965";

        String osType = (demande.getOsType() != null) ? demande.getOsType() : "Ubuntu";
        
        // Calcul simple de la taille VM (Mapping RAM -> Azure SKU)
        String azureSize = "Standard_B2s"; // Par défaut (4GB RAM)
        if (demande.getRam() > 4) {
            azureSize = "Standard_D2s_v3"; // Plus gros si besoin (8GB RAM)
        }

        String content = String.format(
            "vm_name = \"vm-%d\"\n" +
            "demande_id = \"%d\"\n" +
            "cpu_cores = %d\n" +
            "ram_gb = %d\n" +
            "vm_size = \"%s\"\n" +     // On force la taille Azure
            "os_type = \"%s\"\n" +
            "os_version = \"22.04\"\n" + 
            "create_vnet = true\n" +    
            "admin_username = \"azureuser\"\n" +
            "ssh_public_key = \"%s\"\n", // La clé est injectée ici
            
            demande.getId(),
            demande.getId(),
            demande.getCpu(), 
            demande.getRam(), 
            azureSize,
            osType,
            maCleSSH
        );
        
        log.info(">>> Writing terraform.tfvars to: {}", tfVarsPath);
        Files.writeString(tfVarsPath, content, StandardCharsets.UTF_8);
    }

    private String runTerraformCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(terraformWorkingDir));
        pb.redirectErrorStream(true);

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

    // Renommé pour chercher l'IP Privée car l'IP publique est "N/A"
    private Optional<String> fetchIpAddress() {
        try {
            // On cherche "vm_private_ip" défini dans outputs.tf
            String rawOutput = runTerraformCommand(List.of("terraform", "output", "-raw", "vm_private_ip"));
            if (rawOutput != null && !rawOutput.isBlank() && !rawOutput.contains("No outputs")) {
                return Optional.of(rawOutput.trim());
            }
        } catch (Exception e) {
            log.warn("IP fetch warning: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
