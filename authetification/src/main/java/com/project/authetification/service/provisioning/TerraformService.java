package com.project.authetification.service.provisioning;

import com.project.authetification.model.DemandeStatus;
import com.project.authetification.model.DemandeVM;
import com.project.authetification.repository.DemandeVMRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service("vmTerraformService")
@RequiredArgsConstructor
public class TerraformService {

    private final DemandeVMRepository repository;

    @Value("${terraform.vm.working-dir:/home/hadilbenmasseoud/azure-tf-test}")
    private String terraformWorkingDir;

    /**
     * Launches VM creation asynchronously.
     */
    @Async("terraformExecutor")
    public void triggerVmCreation(DemandeVM demande) {
        try {
            writeTfVars(demande);
            runTerraform(List.of("terraform", "init"));
            String applyOutput = runTerraform(List.of("terraform", "apply", "-auto-approve"));
            String ip = fetchPublicIp().orElseGet(() -> parsePublicIp(applyOutput));

            demande.setIpAddress(ip);
            demande.setStatus(DemandeStatus.DEPLOYED);
            repository.save(demande);
        } catch (Exception e) {
            log.error("Terraform provisioning failed for demande {}", demande.getId(), e);
            demande.setStatus(DemandeStatus.ERROR);
            repository.save(demande);
            throw new RuntimeException("Terraform provisioning failed", e);
        }
    }

    private void writeTfVars(DemandeVM demande) throws IOException {
        Path tfVarsPath = Path.of(terraformWorkingDir, "terraform.tfvars");
        String tfVars = """
                vm_name   = "vm-%d"
                cpu_cores = %d
                ram_gb    = %d
                os_type   = "%s"
                """.formatted(demande.getId(), demande.getCpu(), demande.getRam(), demande.getOsType());
        Files.writeString(tfVarsPath, tfVars, StandardCharsets.UTF_8);
        log.info("terraform.tfvars written to {}", tfVarsPath);
    }

    private String runTerraform(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(new File(terraformWorkingDir))
                .redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        env.put("ARM_CLIENT_ID", "dummy-client-id");
        env.put("ARM_CLIENT_SECRET", "dummy-client-secret");
        env.put("ARM_SUBSCRIPTION_ID", "dummy-subscription-id");
        env.put("ARM_TENANT_ID", "dummy-tenant-id");

        Process process = pb.start();
        String output;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\n" + output);
        }

        log.info("Terraform command succeeded: {}", String.join(" ", command));
        return output;
    }

    private java.util.Optional<String> fetchPublicIp() {
        try {
            String output = runTerraform(List.of("terraform", "output", "-raw", "public_ip"));
            if (output != null && !output.isBlank()) {
                return java.util.Optional.of(output.trim());
            }
        } catch (Exception e) {
            log.warn("Unable to fetch public_ip via terraform output: {}", e.getMessage());
        }
        return java.util.Optional.empty();
    }

    private String parsePublicIp(String output) {
        if (output == null) {
            return null;
        }
        return output.lines()
                .filter(line -> line.contains("public_ip"))
                .map(line -> line.substring(line.indexOf('=') + 1).trim())
                .findFirst()
                .orElse(null);
    }
}

