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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service("vmTerraformService")
@RequiredArgsConstructor
public class TerraformService {

    @Value("${terraform.vm.working-dir:/home/hadilbenmasseoud/azure-tf-test}")
    private String terraformWorkingDir;

    @Async
    public CompletableFuture<String> triggerVmCreation(DemandeVM demande) {
        log.info(">>> [TerraformService] Start provisioning VM ID: {}", demande.getId());
        
        try {
            writeTfVars(demande);
            log.info(">>> Executing: terraform init");
            runTerraformCommand(List.of("terraform", "init"));
            log.info(">>> Executing: terraform apply");
            String applyOutput = runTerraformCommand(List.of("terraform", "apply", "-auto-approve"));
            String ip = fetchPublicIp().orElse("IP_NOT_FOUND");
            log.info(">>> SUCCESS! VM IP: {}", ip);
            return CompletableFuture.completedFuture(ip);
        } catch (Exception e) {
            log.error(">>> FAILURE Provisioning for Request {}", demande.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void writeTfVars(DemandeVM demande) throws IOException {
        Path tfVarsPath = Path.of(terraformWorkingDir, "terraform.tfvars");
        String osType = (demande.getOsType() != null) ? demande.getOsType() : "Ubuntu";
        String content = String.format(
            "vm_name = \"vm-%d\"\n" +
            "cpu_cores = %d\n" +
            "ram_gb = %d\n" +
            "os_type = \"%s\"\n",
            demande.getId(), 
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

        // --- ICI C'EST LA PARTIE CORRIGÉE ---
        Map<String, String> env = pb.environment();
        env.put("ARM_CLIENT_ID", System.getenv("ARM_CLIENT_ID"));
        env.put("ARM_CLIENT_SECRET", System.getenv("ARM_CLIENT_SECRET"));
        env.put("ARM_SUBSCRIPTION_ID", System.getenv("ARM_SUBSCRIPTION_ID"));
        env.put("ARM_TENANT_ID", System.getenv("ARM_TENANT_ID"));
        // ------------------------------------

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Terraform] " + line);
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Terraform Error (Code " + exitCode + ")");
        }

        return output.toString();
    }

    private java.util.Optional<String> fetchPublicIp() {
        try {
            String rawOutput = runTerraformCommand(List.of("terraform", "output", "-raw", "vm_public_ip"));
            if (rawOutput != null && !rawOutput.isBlank() && !rawOutput.contains("No outputs")) {
                return java.util.Optional.of(rawOutput.trim());
            }
        } catch (Exception e) {
            log.warn("IP fetch warning: {}", e.getMessage());
        }
        return java.util.Optional.empty();
    }
}
