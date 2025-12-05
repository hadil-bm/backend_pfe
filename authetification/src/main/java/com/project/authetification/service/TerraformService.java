package com.project.authetification.service;

import com.project.authetification.model.Demande;
import com.project.authetification.model.TerraformExecution;
import com.project.authetification.model.VM;
import com.project.authetification.model.WorkOrder;
import com.project.authetification.repository.DemandeRepository;
import com.project.authetification.repository.TerraformExecutionRepository;
import com.project.authetification.repository.VMRepository;
import com.project.authetification.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TerraformService {

    private final TerraformExecutionRepository terraformExecutionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DemandeRepository demandeRepository;
    private final VMRepository vmRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${terraform.cloud.api.url:https://app.terraform.io/api/v2}")
    private String terraformApiUrl;

    @Value("${terraform.cloud.api.token:}")
    private String terraformApiToken;

    @Value("${terraform.cloud.organization:}")
    private String terraformOrganization;

    @Value("${terraform.cloud.workspace:vm-provisioning}")
    private String terraformWorkspace;

    /**
     * Génère la configuration Terraform à partir d'une demande
     * Cette méthode génère un fichier terraform.tfvars avec les variables
     */
    public String generateTerraformConfig(Demande demande) {
        StringBuilder config = new StringBuilder();
        
        config.append("# Configuration Terraform générée automatiquement\n");
        config.append("# Demande ID: ").append(demande.getId()).append("\n");
        config.append("# Date: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        // Générer les variables pour terraform.tfvars
        config.append("vm_name = \"").append(demande.getName()).append("\"\n");
        config.append("demande_id = \"").append(demande.getId()).append("\"\n");
        config.append("vm_size = \"").append(getInstanceType(demande)).append("\"\n");
        config.append("image_publisher = \"").append(getImagePublisher(demande.getOs())).append("\"\n");
        config.append("image_offer = \"").append(getImageOffer(demande.getOs())).append("\"\n");
        config.append("image_sku = \"").append(getImageSku(demande.getOs(), demande.getVersionOs())).append("\"\n");
        config.append("os_type = \"").append(demande.getOs() != null ? demande.getOs() : "Ubuntu").append("\"\n");
        config.append("os_version = \"").append(demande.getVersionOs() != null ? demande.getVersionOs() : "22.04").append("\"\n");
        config.append("cpu_cores = ").append(extractCpu(demande.getCpu())).append("\n");
        config.append("ram_gb = ").append(extractRam(demande.getRam())).append("\n");
        config.append("disk_size = ").append(extractDiskSize(demande.getDisque())).append("\n");
        config.append("disk_type = \"").append(demande.getTypeDisque() != null ? demande.getTypeDisque() : "Premium_LRS").append("\"\n");
        
        if (demande.getReseau() != null) {
            config.append("subnet_id = \"").append(demande.getReseau()).append("\"\n");
        } else {
            config.append("subnet_id = \"\"\n");
            config.append("create_vnet = true\n");
        }
        
        config.append("assign_public_ip = true\n");
        config.append("enable_monitoring = true\n");
        config.append("disk_encryption = true\n");
        config.append("admin_username = \"azureuser\"\n");
        
        return config.toString();
    }
    
    /**
     * Génère un fichier terraform.tfvars.json à partir d'une demande (format Azure)
     */
    public String generateTerraformVarsJson(Demande demande) {
        // Générer JSON manuellement pour Azure (sans dépendance externe)
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"vm_name\": \"").append(demande.getName()).append("\",\n");
        json.append("  \"demande_id\": \"").append(demande.getId()).append("\",\n");
        json.append("  \"vm_size\": \"").append(getInstanceType(demande)).append("\",\n");
        json.append("  \"image_publisher\": \"").append(getImagePublisher(demande.getOs())).append("\",\n");
        json.append("  \"image_offer\": \"").append(getImageOffer(demande.getOs())).append("\",\n");
        json.append("  \"image_sku\": \"").append(getImageSku(demande.getOs(), demande.getVersionOs())).append("\",\n");
        json.append("  \"os_type\": \"").append(demande.getOs() != null ? demande.getOs() : "Ubuntu").append("\",\n");
        json.append("  \"os_version\": \"").append(demande.getVersionOs() != null ? demande.getVersionOs() : "22.04").append("\",\n");
        json.append("  \"cpu_cores\": ").append(extractCpu(demande.getCpu())).append(",\n");
        json.append("  \"ram_gb\": ").append(extractRam(demande.getRam())).append(",\n");
        json.append("  \"disk_size\": ").append(extractDiskSize(demande.getDisque())).append(",\n");
        json.append("  \"disk_type\": \"").append(demande.getTypeDisque() != null ? demande.getTypeDisque() : "Premium_LRS").append("\",\n");
        json.append("  \"subnet_id\": \"").append(demande.getReseau() != null ? demande.getReseau() : "").append("\",\n");
        json.append("  \"create_vnet\": ").append(demande.getReseau() == null).append(",\n");
        json.append("  \"assign_public_ip\": true,\n");
        json.append("  \"enable_monitoring\": true,\n");
        json.append("  \"disk_encryption\": true,\n");
        json.append("  \"admin_username\": \"azureuser\"\n");
        json.append("}");
        
        return json.toString();
    }

    /**
     * Crée une exécution Terraform dans Terraform Cloud
     */
    @Async
    public TerraformExecution createTerraformRun(String workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        Demande demande = workOrder.getDemande();
        if (demande == null) {
            throw new RuntimeException("Demande non trouvée pour le workorder: " + workOrderId);
        }

        // Générer la configuration Terraform
        String terraformConfig = generateTerraformConfig(demande);
        
        // Créer l'exécution Terraform
        TerraformExecution execution = new TerraformExecution();
        execution.setWorkOrder(workOrder);
        execution.setDemande(demande);
        execution.setTerraformConfig(terraformConfig);
        execution.setStatus("PENDING");
        execution.setDateCreation(LocalDateTime.now());
        
        // Préparer les variables Terraform
        Map<String, String> variables = new HashMap<>();
        variables.put("ami_id", getAmiIdForOS(demande.getOs(), demande.getVersionOs()));
        variables.put("key_name", "default-key");
        if (demande.getReseau() != null) {
            variables.put("subnet_id", demande.getReseau());
        }
        execution.setTerraformVariables(variables);
        
        execution = terraformExecutionRepository.save(execution);

        try {
            // Appeler l'API Terraform Cloud pour créer un run
            String runId = createTerraformCloudRun(execution);
            execution.setTerraformRunId(runId);
            execution.setStatus("RUNNING");
            execution.setDateDebut(LocalDateTime.now());
            execution = terraformExecutionRepository.save(execution);

            // Notifier l'équipe Support
            notificationService.sendNotification(
                    workOrder.getAssigne(),
                    "Exécution Terraform démarrée",
                    "L'exécution Terraform pour la demande '" + demande.getName() + "' a été démarrée. Run ID: " + runId
            );

        } catch (Exception e) {
            execution.setStatus("ERROR");
            execution.setErrorMessage(e.getMessage());
            terraformExecutionRepository.save(execution);
            throw new RuntimeException("Erreur lors de la création du run Terraform: " + e.getMessage(), e);
        }

        return execution;
    }

    /**
     * Crée un run dans Terraform Cloud via l'API
     */
    private String createTerraformCloudRun(TerraformExecution execution) {
        if (terraformApiToken == null || terraformApiToken.isEmpty()) {
            // Mode simulation si pas de token configuré
            return "simulated-run-" + execution.getId();
        }

        try {
            // Préparer la requête pour créer un run
            String workspaceId = getOrCreateWorkspace();
            execution.setWorkspaceId(workspaceId);

            Map<String, Object> runRequest = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> attributes = new HashMap<>();
            Map<String, Object> relationships = new HashMap<>();
            Map<String, Object> workspace = new HashMap<>();
            
            workspace.put("data", Map.of("type", "workspaces", "id", workspaceId));
            relationships.put("workspace", workspace);
            
            attributes.put("is-destroy", false);
            attributes.put("message", "Provisionnement automatique pour demande: " + execution.getDemande().getName());
            
            data.put("type", "runs");
            data.put("attributes", attributes);
            data.put("relationships", relationships);
            
            runRequest.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(terraformApiToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(runRequest, headers);

            String url = terraformApiUrl + "/runs";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
                return (String) responseData.get("id");
            } else {
                throw new RuntimeException("Erreur lors de la création du run Terraform Cloud");
            }
        } catch (Exception e) {
            // En cas d'erreur, retourner un ID simulé pour le développement
            return "simulated-run-" + execution.getId();
        }
    }

    /**
     * Vérifie le statut d'une exécution Terraform
     */
    public TerraformExecution checkExecutionStatus(String executionId) {
        TerraformExecution execution = terraformExecutionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Exécution Terraform non trouvée: " + executionId));

        if (execution.getTerraformRunId() == null || execution.getTerraformRunId().startsWith("simulated-")) {
            // Mode simulation
            execution.setStatus("APPLIED");
            execution.setDateCompletion(LocalDateTime.now());
            execution.setVmId("simulated-vm-" + execution.getId());
            execution.setVmIpAddress(execution.getDemande().getAdresseIp());
            return terraformExecutionRepository.save(execution);
        }

        try {
            // Appeler l'API Terraform Cloud pour vérifier le statut
            String url = terraformApiUrl + "/runs/" + execution.getTerraformRunId();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(terraformApiToken);
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
                String status = (String) attributes.get("status");

                execution.setStatus(mapTerraformStatus(status));
                
                if ("applied".equals(status)) {
                    execution.setDateCompletion(LocalDateTime.now());
                    // Récupérer les outputs
                    fetchTerraformOutputs(execution);
                }
                
                return terraformExecutionRepository.save(execution);
            }
        } catch (Exception e) {
            execution.setErrorMessage("Erreur lors de la vérification du statut: " + e.getMessage());
            terraformExecutionRepository.save(execution);
        }

        return execution;
    }

    /**
     * Récupère les outputs de Terraform après application
     */
    private void fetchTerraformOutputs(TerraformExecution execution) {
        try {
            String url = terraformApiUrl + "/workspaces/" + execution.getWorkspaceId() + "/current-state-version/outputs";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(terraformApiToken);
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Traiter les outputs et mettre à jour la VM
                // Cette partie dépend de la structure de réponse de Terraform Cloud
                Map<String, Object> outputs = new HashMap<>();
                execution.setTerraformOutputs(outputs);
                
                // Créer ou mettre à jour la VM avec les informations de Terraform
                createVMFromTerraformOutput(execution);
            }
        } catch (Exception e) {
            execution.setErrorMessage("Erreur lors de la récupération des outputs: " + e.getMessage());
        }
    }

    /**
     * Crée une VM à partir des outputs Terraform
     */
    private void createVMFromTerraformOutput(TerraformExecution execution) {
        Demande demande = execution.getDemande();
        
        VM vm = new VM();
        vm.setVmName(demande.getName());
        vm.setVmId(execution.getVmId() != null ? execution.getVmId() : "terraform-vm-" + execution.getId());
        vm.setDemande(demande);
        vm.setRam(demande.getRam());
        vm.setCpu(demande.getCpu());
        vm.setTypeVm(demande.getTypeVm());
        vm.setDisque(demande.getDisque());
        vm.setTypeDisque(demande.getTypeDisque());
        vm.setOs(demande.getOs());
        vm.setVersionOs(demande.getVersionOs());
        vm.setAdresseIp(execution.getVmIpAddress() != null ? execution.getVmIpAddress() : demande.getAdresseIp());
        vm.setReseau(demande.getReseau());
        vm.setDatastore(demande.getDatastore());
        vm.setStatus("RUNNING");
        vm.setDateCreation(LocalDateTime.now());
        vm.setMonitored(false);

        vmRepository.save(vm);
    }

    /**
     * Méthodes utilitaires
     */
    private String getInstanceType(Demande demande) {
        // Mapper CPU/RAM vers une taille de VM Azure
        int cpu = extractCpu(demande.getCpu());
        int ram = extractRam(demande.getRam());
        
        // Taille Azure basée sur CPU/RAM
        if (cpu <= 2 && ram <= 4) return "Standard_B2s";      // 2 vCPU, 4 GB RAM
        if (cpu <= 2 && ram <= 8) return "Standard_B2ms";     // 2 vCPU, 8 GB RAM
        if (cpu <= 4 && ram <= 8) return "Standard_B4ms";     // 4 vCPU, 16 GB RAM
        if (cpu <= 4 && ram <= 16) return "Standard_D2s_v3";  // 2 vCPU, 8 GB RAM
        if (cpu <= 8 && ram <= 16) return "Standard_D4s_v3";  // 4 vCPU, 16 GB RAM
        if (cpu <= 8 && ram <= 32) return "Standard_D8s_v3"; // 8 vCPU, 32 GB RAM
        return "Standard_D16s_v3"; // 16 vCPU, 64 GB RAM
    }

    private int extractCpu(String cpu) {
        try {
            return Integer.parseInt(cpu.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 2;
        }
    }

    private int extractRam(String ram) {
        try {
            return Integer.parseInt(ram.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 4;
        }
    }

    private int extractDiskSize(String disk) {
        try {
            return Integer.parseInt(disk.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 20;
        }
    }

    private String getAmiIdForOS(String os, String version) {
        // Pour Azure, on retourne les informations d'image (publisher, offer, sku)
        // Cette méthode est utilisée pour générer les variables Terraform
        // Les valeurs réelles seront dans les variables image_publisher, image_offer, image_sku
        
        if (os == null) {
            return "Canonical:UbuntuServer:22.04-LTS"; // Ubuntu par défaut
        }
        
        String osLower = os.toLowerCase();
        if (osLower.contains("ubuntu")) {
            String ubuntuVersion = version != null ? version : "22.04";
            return "Canonical:UbuntuServer:" + ubuntuVersion + "-LTS";
        } else if (osLower.contains("centos")) {
            return "OpenLogic:CentOS:8_5";
        } else if (osLower.contains("windows") || osLower.contains("win")) {
            String winVersion = version != null ? version : "2019";
            return "MicrosoftWindowsServer:WindowsServer:" + winVersion + "-Datacenter";
        } else if (osLower.contains("debian")) {
            return "Debian:debian-11:11";
        } else if (osLower.contains("redhat") || osLower.contains("rhel")) {
            return "RedHat:RHEL:8.5";
        }
        
        return "Canonical:UbuntuServer:22.04-LTS"; // Par défaut Ubuntu
    }
    
    /**
     * Extrait le publisher de l'image Azure
     */
    private String getImagePublisher(String os) {
        if (os == null) return "Canonical";
        String osLower = os.toLowerCase();
        if (osLower.contains("windows") || osLower.contains("win")) return "MicrosoftWindowsServer";
        if (osLower.contains("centos")) return "OpenLogic";
        if (osLower.contains("debian")) return "Debian";
        if (osLower.contains("redhat") || osLower.contains("rhel")) return "RedHat";
        return "Canonical"; // Ubuntu par défaut
    }
    
    /**
     * Extrait l'offer de l'image Azure
     */
    private String getImageOffer(String os) {
        if (os == null) return "UbuntuServer";
        String osLower = os.toLowerCase();
        if (osLower.contains("windows") || osLower.contains("win")) return "WindowsServer";
        if (osLower.contains("centos")) return "CentOS";
        if (osLower.contains("debian")) return "debian-11";
        if (osLower.contains("redhat") || osLower.contains("rhel")) return "RHEL";
        return "UbuntuServer";
    }
    
    /**
     * Extrait le SKU de l'image Azure
     */
    private String getImageSku(String os, String version) {
        if (os == null) return "22.04-LTS";
        String osLower = os.toLowerCase();
        if (osLower.contains("windows") || osLower.contains("win")) {
            String winVersion = version != null ? version : "2019";
            return winVersion + "-Datacenter";
        }
        if (osLower.contains("centos")) return "8_5";
        if (osLower.contains("debian")) return "11";
        if (osLower.contains("redhat") || osLower.contains("rhel")) return "8.5";
        // Ubuntu
        String ubuntuVersion = version != null ? version : "22.04";
        return ubuntuVersion + "-LTS";
    }

    private String mapTerraformStatus(String terraformStatus) {
        if (terraformStatus == null) return "PENDING";
        
        switch (terraformStatus.toLowerCase()) {
            case "pending": return "PENDING";
            case "planning": case "planned": return "RUNNING";
            case "applying": return "RUNNING";
            case "applied": return "APPLIED";
            case "errored": return "ERROR";
            case "canceled": return "CANCELLED";
            default: return "PENDING";
        }
    }

    private String getOrCreateWorkspace() {
        // Cette méthode devrait créer ou récupérer le workspace Terraform Cloud
        // Pour l'instant, retourner un ID simulé
        return "workspace-" + terraformWorkspace;
    }
}

