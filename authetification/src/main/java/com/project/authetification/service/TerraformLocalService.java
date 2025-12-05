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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerraformLocalService {

    private final TerraformExecutionRepository terraformExecutionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DemandeRepository demandeRepository;
    private final VMRepository vmRepository;
    private final NotificationService notificationService;

    @Value("${terraform.local.path:/opt/terraform/vm-template}")
    private String terraformPath;

    @Value("${terraform.local.working.dir:/opt/terraform/vm-template}")
    private String terraformWorkingDir;

    @Value("${terraform.binary.path:terraform}")
    private String terraformBinaryPath;

    /**
     * Crée une exécution Terraform et lance la création de la VM localement
     */
    @Async("terraformExecutor")
    public TerraformExecution createVMLocally(String workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder non trouvé: " + workOrderId));

        Demande demande = workOrder.getDemande();
        if (demande == null) {
            throw new RuntimeException("Demande non trouvée pour le workorder: " + workOrderId);
        }

        // Créer l'exécution Terraform
        TerraformExecution execution = new TerraformExecution();
        execution.setWorkOrder(workOrder);
        execution.setDemande(demande);
        execution.setStatus("PENDING");
        execution.setDateCreation(LocalDateTime.now());
        execution = terraformExecutionRepository.save(execution);

        try {
            log.info("Démarrage de la création de VM pour la demande: {}", demande.getId());

            // 1. Préparer le répertoire de travail Terraform
            Path workingDir = prepareTerraformDirectory(execution.getId());

            // 2. Générer les fichiers Terraform
            generateTerraformFiles(demande, workingDir);

            // 3. Générer le fichier terraform.tfvars
            generateTerraformVarsFile(demande, workingDir);

            // 4. Exécuter terraform init
            execution.setStatus("RUNNING");
            execution.setDateDebut(LocalDateTime.now());
            execution = terraformExecutionRepository.save(execution);

            log.info("Exécution de terraform init...");
            TerraformResult initResult = executeTerraformCommand(workingDir, "init", "-upgrade");
            if (!initResult.isSuccess()) {
                throw new RuntimeException("Erreur lors de terraform init: " + initResult.getError());
            }

            // 5. Exécuter terraform apply
            log.info("Exécution de terraform apply...");
            TerraformResult applyResult = executeTerraformCommand(workingDir, "apply", "-auto-approve");
            
            if (applyResult.isSuccess()) {
                // 6. Récupérer les outputs
                log.info("Récupération des outputs Terraform...");
                Map<String, String> outputs = getTerraformOutputs(workingDir);
                
                execution.setStatus("APPLIED");
                execution.setDateCompletion(LocalDateTime.now());
                execution.setOutput(applyResult.getOutput());
                execution.setTerraformOutputs(convertOutputsToMap(outputs));
                
                // Extraire les informations de la VM depuis les outputs
                String vmId = outputs.getOrDefault("vm_id", "");
                String vmPublicIp = outputs.getOrDefault("vm_public_ip", "");
                String vmPrivateIp = outputs.getOrDefault("vm_private_ip", "");
                
                execution.setVmId(vmId);
                execution.setVmIpAddress(vmPublicIp != null && !vmPublicIp.isEmpty() ? vmPublicIp : vmPrivateIp);
                
                execution = terraformExecutionRepository.save(execution);

                // 7. Créer la VM dans la base de données
                createVMFromTerraformOutput(execution, outputs);

                // 8. Notifier l'équipe Support
                notificationService.sendNotification(
                        workOrder.getAssigne(),
                        "VM créée avec succès",
                        "La VM '" + demande.getName() + "' a été créée avec succès dans Azure. IP: " + execution.getVmIpAddress()
                );

                // 9. Notifier le client
                if (demande.getDemandeur() != null) {
                    notificationService.sendNotification(
                            demande.getDemandeur(),
                            "VM créée",
                            "Votre VM '" + demande.getName() + "' a été créée avec succès. IP: " + execution.getVmIpAddress()
                    );
                }

                log.info("VM créée avec succès. ID: {}, IP: {}", vmId, execution.getVmIpAddress());
            } else {
                execution.setStatus("ERROR");
                execution.setErrorMessage(applyResult.getError());
                execution.setOutput(applyResult.getOutput());
                terraformExecutionRepository.save(execution);
                
                log.error("Erreur lors de terraform apply: {}", applyResult.getError());
                throw new RuntimeException("Erreur lors de la création de la VM: " + applyResult.getError());
            }

        } catch (Exception e) {
            log.error("Erreur lors de la création de la VM: ", e);
            execution.setStatus("ERROR");
            execution.setErrorMessage(e.getMessage());
            execution.setOutput(e.toString());
            terraformExecutionRepository.save(execution);
            
            // Notifier l'erreur
            notificationService.sendNotification(
                    workOrder.getAssigne(),
                    "Erreur lors de la création de la VM",
                    "Une erreur est survenue lors de la création de la VM '" + demande.getName() + "': " + e.getMessage()
            );
        }

        return execution;
    }

    /**
     * Prépare le répertoire de travail Terraform pour cette exécution
     */
    private Path prepareTerraformDirectory(String executionId) throws IOException {
        Path workingDir = Paths.get(terraformWorkingDir, "executions", executionId);
        Files.createDirectories(workingDir);
        
        // Copier les fichiers Terraform de base depuis le template (sans copier le dossier executions)
        Path templateDir = Paths.get(terraformPath);
        if (Files.exists(templateDir)) {
            try (var stream = Files.list(templateDir)) {
                for (Path sourcePath : stream.toList()) {
                    if (Files.isRegularFile(sourcePath) && !sourcePath.getFileName().toString().startsWith(".")) {
                        Path targetPath = workingDir.resolve(sourcePath.getFileName());
                        Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        log.debug("Copié {} vers {}", sourcePath.getFileName(), targetPath);
                    }
                }
            }
        }
        
        // Copier la clé SSH si elle existe
        Path sshKeySource = Paths.get(terraformPath, "id_rsa.pub");
        if (Files.exists(sshKeySource)) {
            Path sshKeyTarget = workingDir.resolve("id_rsa.pub");
            Files.copy(sshKeySource, sshKeyTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.debug("Clé SSH copiée vers {}", sshKeyTarget);
        } else {
            log.warn("Clé SSH non trouvée dans {}. La création de VM Linux pourrait échouer.", sshKeySource);
        }
        
        return workingDir;
    }


    /**
     * Génère les fichiers Terraform à partir de la demande
     */
    private void generateTerraformFiles(Demande demande, Path workingDir) throws IOException {
        // Générer main.tf
        String mainTf = generateMainTf(demande);
        Files.write(workingDir.resolve("main.tf"), mainTf.getBytes());

        // Générer variables.tf (si nécessaire)
        String variablesTf = generateVariablesTf();
        Files.write(workingDir.resolve("variables.tf"), variablesTf.getBytes());

        // Générer outputs.tf
        String outputsTf = generateOutputsTf();
        Files.write(workingDir.resolve("outputs.tf"), outputsTf.getBytes());
    }

    /**
     * Génère le fichier terraform.tfvars
     */
    private void generateTerraformVarsFile(Demande demande, Path workingDir) throws IOException {
        StringBuilder vars = new StringBuilder();
        vars.append("# Variables générées automatiquement pour la demande: ").append(demande.getId()).append("\n\n");
        vars.append("vm_name = \"").append(demande.getName()).append("\"\n");
        vars.append("demande_id = \"").append(demande.getId()).append("\"\n");
        vars.append("vm_size = \"").append(getInstanceType(demande)).append("\"\n");
        vars.append("image_publisher = \"").append(getImagePublisher(demande.getOs())).append("\"\n");
        vars.append("image_offer = \"").append(getImageOffer(demande.getOs())).append("\"\n");
        vars.append("image_sku = \"").append(getImageSku(demande.getOs(), demande.getVersionOs())).append("\"\n");
        vars.append("os_type = \"").append(demande.getOs() != null ? demande.getOs() : "Ubuntu").append("\"\n");
        vars.append("os_version = \"").append(demande.getVersionOs() != null ? demande.getVersionOs() : "22.04").append("\"\n");
        vars.append("cpu_cores = ").append(extractCpu(demande.getCpu())).append("\n");
        vars.append("ram_gb = ").append(extractRam(demande.getRam())).append("\n");
        vars.append("disk_size = ").append(extractDiskSize(demande.getDisque())).append("\n");
        vars.append("disk_type = \"").append(demande.getTypeDisque() != null ? demande.getTypeDisque() : "Premium_LRS").append("\"\n");
        
        if (demande.getReseau() != null && !demande.getReseau().isEmpty()) {
            vars.append("subnet_id = \"").append(demande.getReseau()).append("\"\n");
            vars.append("create_vnet = false\n");
        } else {
            vars.append("subnet_id = \"\"\n");
            vars.append("create_vnet = true\n");
        }
        
        vars.append("assign_public_ip = true\n");
        vars.append("enable_monitoring = true\n");
        vars.append("disk_encryption = true\n");
        vars.append("admin_username = \"azureuser\"\n");
        vars.append("azure_location = \"West Europe\"\n");

        Files.write(workingDir.resolve("terraform.tfvars"), vars.toString().getBytes());
    }

    /**
     * Génère le fichier main.tf
     */
    private String generateMainTf(Demande demande) {
        // Utiliser le template main.tf.simple ou main.tf existant
        String template = readTemplateFile("main.tf.simple");
        if (template.isEmpty()) {
            template = readTemplateFile("main.tf");
        }
        if (template.isEmpty()) {
            template = getDefaultTemplate("main.tf");
        }
        return template;
    }

    /**
     * Génère le fichier variables.tf
     */
    private String generateVariablesTf() {
        String template = readTemplateFile("variables.tf");
        if (template.isEmpty()) {
            template = readTemplateFile("variables.tf.simple");
        }
        return template.isEmpty() ? getDefaultVariablesTf() : template;
    }

    /**
     * Génère le fichier outputs.tf
     */
    private String generateOutputsTf() {
        String template = readTemplateFile("outputs.tf.simple");
        if (template.isEmpty()) {
            template = readTemplateFile("outputs.tf");
        }
        return template.isEmpty() ? getDefaultOutputsTf() : template;
    }

    /**
     * Lit un fichier template depuis le dossier terraform
     */
    private String readTemplateFile(String filename) {
        try {
            Path templatePath = Paths.get(terraformPath, filename);
            if (Files.exists(templatePath)) {
                return Files.readString(templatePath);
            }
            // Si le fichier n'existe pas, retourner un template par défaut
            return getDefaultTemplate(filename);
        } catch (IOException e) {
            log.warn("Impossible de lire le template {}, utilisation du template par défaut", filename);
            return getDefaultTemplate(filename);
        }
    }

    /**
     * Retourne un template par défaut si le fichier n'existe pas
     */
    private String getDefaultTemplate(String filename) {
        if ("main.tf".equals(filename)) {
            return """
                terraform {
                  required_version = ">= 1.0"
                  required_providers {
                    azurerm = {
                      source  = "hashicorp/azurerm"
                      version = "~> 3.0"
                    }
                  }
                }
                
                provider "azurerm" {
                  features {}
                }
                
                # Resource Group
                resource "azurerm_resource_group" "vm_rg" {
                  name     = "rg-${var.vm_name}-${var.demande_id}"
                  location = var.azure_location
                  tags = {
                    DemandeID = var.demande_id
                  }
                }
                
                # Virtual Network
                resource "azurerm_virtual_network" "vm_vnet" {
                  count               = var.create_vnet ? 1 : 0
                  name                = "vnet-${var.vm_name}"
                  address_space       = [var.vnet_address_space]
                  location            = azurerm_resource_group.vm_rg.location
                  resource_group_name = azurerm_resource_group.vm_rg.name
                }
                
                # Subnet
                resource "azurerm_subnet" "vm_subnet" {
                  count                = var.create_vnet ? 1 : 0
                  name                 = "subnet-${var.vm_name}"
                  resource_group_name  = azurerm_resource_group.vm_rg.name
                  virtual_network_name = azurerm_virtual_network.vm_vnet[0].name
                  address_prefixes     = [var.subnet_address_prefix]
                }
                
                # Network Security Group
                resource "azurerm_network_security_group" "vm_nsg" {
                  name                = "nsg-${var.vm_name}"
                  location            = azurerm_resource_group.vm_rg.location
                  resource_group_name = azurerm_resource_group.vm_rg.name
                  
                  security_rule {
                    name                       = "SSH"
                    priority                   = 1000
                    direction                  = "Inbound"
                    access                     = "Allow"
                    protocol                   = "Tcp"
                    source_port_range          = "*"
                    destination_port_range     = "22"
                    source_address_prefix       = "*"
                    destination_address_prefix  = "*"
                  }
                }
                
                # Public IP
                resource "azurerm_public_ip" "vm_public_ip" {
                  count               = var.assign_public_ip ? 1 : 0
                  name                = "pip-${var.vm_name}"
                  location            = azurerm_resource_group.vm_rg.location
                  resource_group_name = azurerm_resource_group.vm_rg.name
                  allocation_method   = "Static"
                  sku                 = "Standard"
                }
                
                # Network Interface
                resource "azurerm_network_interface" "vm_nic" {
                  name                = "nic-${var.vm_name}"
                  location            = azurerm_resource_group.vm_rg.location
                  resource_group_name = azurerm_resource_group.vm_rg.name
                  
                  ip_configuration {
                    name                          = "internal"
                    subnet_id                     = var.create_vnet ? azurerm_subnet.vm_subnet[0].id : var.subnet_id
                    private_ip_address_allocation = "Dynamic"
                    public_ip_address_id          = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].id : null
                  }
                }
                
                # Association NSG
                resource "azurerm_network_interface_security_group_association" "vm_nic_nsg" {
                  network_interface_id      = azurerm_network_interface.vm_nic.id
                  network_security_group_id = azurerm_network_security_group.vm_nsg.id
                }
                
                # Linux Virtual Machine
                resource "azurerm_linux_virtual_machine" "vm" {
                  count               = var.os_type != "Windows" ? 1 : 0
                  name                = var.vm_name
                  location            = azurerm_resource_group.vm_rg.location
                  resource_group_name = azurerm_resource_group.vm_rg.name
                  size                = var.vm_size
                  admin_username      = var.admin_username
                  
                  network_interface_ids = [azurerm_network_interface.vm_nic.id]
                  
                  admin_ssh_key {
                    username   = var.admin_username
                    public_key = file("${path.module}/id_rsa.pub")
                  }
                  
                  os_disk {
                    name                 = "osdisk-${var.vm_name}"
                    caching              = "ReadWrite"
                    storage_account_type = var.disk_type
                    disk_size_gb         = var.disk_size
                  }
                  
                  source_image_reference {
                    publisher = var.image_publisher
                    offer     = var.image_offer
                    sku       = var.image_sku
                    version   = "latest"
                  }
                }
                
                # Windows Virtual Machine
                resource "azurerm_windows_virtual_machine" "vm_windows" {
                  count               = var.os_type == "Windows" ? 1 : 0
                  name                = var.vm_name
                  location            = azurerm_resource_group.vm_rg.location
                  resource_group_name = azurerm_resource_group.vm_rg.name
                  size                = var.vm_size
                  admin_username      = var.admin_username
                  admin_password      = var.admin_password
                  
                  network_interface_ids = [azurerm_network_interface.vm_nic.id]
                  
                  os_disk {
                    name                 = "osdisk-${var.vm_name}"
                    caching              = "ReadWrite"
                    storage_account_type = var.disk_type
                    disk_size_gb         = var.disk_size
                  }
                  
                  source_image_reference {
                    publisher = var.image_publisher
                    offer     = var.image_offer
                    sku       = var.image_sku
                    version   = "latest"
                  }
                }
                """;
        }
        return "";
    }

    /**
     * Template par défaut pour variables.tf
     */
    private String getDefaultVariablesTf() {
        return """
            variable "vm_name" { type = string }
            variable "demande_id" { type = string }
            variable "vm_size" { type = string; default = "Standard_B2s" }
            variable "image_publisher" { type = string; default = "Canonical" }
            variable "image_offer" { type = string; default = "UbuntuServer" }
            variable "image_sku" { type = string; default = "22.04-LTS" }
            variable "os_type" { type = string; default = "Ubuntu" }
            variable "os_version" { type = string; default = "22.04" }
            variable "cpu_cores" { type = number; default = 2 }
            variable "ram_gb" { type = number; default = 4 }
            variable "disk_size" { type = number; default = 30 }
            variable "disk_type" { type = string; default = "Premium_LRS" }
            variable "azure_location" { type = string; default = "West Europe" }
            variable "environment" { type = string; default = "production" }
            variable "subnet_id" { type = string; default = "" }
            variable "create_vnet" { type = bool; default = true }
            variable "vnet_address_space" { type = string; default = "10.0.0.0/16" }
            variable "subnet_address_prefix" { type = string; default = "10.0.1.0/24" }
            variable "assign_public_ip" { type = bool; default = true }
            variable "admin_username" { type = string; default = "azureuser" }
            variable "admin_password" { type = string; default = ""; sensitive = true }
            variable "user_data" { type = string; default = "" }
            """;
    }

    /**
     * Template par défaut pour outputs.tf
     */
    private String getDefaultOutputsTf() {
        return """
            output "vm_id" {
              value = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].id : azurerm_windows_virtual_machine.vm_windows[0].id
            }
            output "vm_public_ip" {
              value = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].ip_address : null
            }
            output "vm_private_ip" {
              value = azurerm_network_interface.vm_nic.private_ip_address
            }
            output "vm_fqdn" {
              value = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].fqdn : null
            }
            output "vm_location" {
              value = azurerm_resource_group.vm_rg.location
            }
            output "vm_resource_group_name" {
              value = azurerm_resource_group.vm_rg.name
            }
            output "vm_name" {
              value = var.vm_name
            }
            output "demande_id" {
              value = var.demande_id
            }
            """;
    }

    /**
     * Exécute une commande Terraform
     */
    private TerraformResult executeTerraformCommand(Path workingDir, String... args) {
        List<String> command = new ArrayList<>();
        command.add(terraformBinaryPath);
        command.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);

        // Ajouter les variables d'environnement Azure
        Map<String, String> env = pb.environment();
        env.put("ARM_CLIENT_ID", System.getenv("ARM_CLIENT_ID"));
        env.put("ARM_CLIENT_SECRET", System.getenv("ARM_CLIENT_SECRET"));
        env.put("ARM_TENANT_ID", System.getenv("ARM_TENANT_ID"));
        env.put("ARM_SUBSCRIPTION_ID", System.getenv("ARM_SUBSCRIPTION_ID"));

        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try {
            Process process = pb.start();

            // Lire la sortie
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.info("Terraform: {}", line);
                }
            }

            // Attendre la fin du processus (max 30 minutes)
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                error.append("Timeout: Terraform a pris plus de 30 minutes");
                return new TerraformResult(false, output.toString(), error.toString());
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                error.append("Terraform a échoué avec le code: ").append(exitCode);
            }

            return new TerraformResult(exitCode == 0, output.toString(), error.toString());

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de Terraform: ", e);
            error.append("Erreur: ").append(e.getMessage());
            return new TerraformResult(false, output.toString(), error.toString());
        }
    }

    /**
     * Récupère les outputs Terraform
     */
    private Map<String, String> getTerraformOutputs(Path workingDir) {
        // Utiliser directement la méthode individuelle qui est plus fiable
        // car elle récupère chaque output séparément avec -raw
        return getTerraformOutputsIndividually(workingDir);
    }

    /**
     * Récupère les outputs individuellement (méthode fiable)
     */
    private Map<String, String> getTerraformOutputsIndividually(Path workingDir) {
        Map<String, String> outputs = new HashMap<>();
        String[] outputNames = {"vm_id", "vm_public_ip", "vm_private_ip", "vm_fqdn", "vm_location", "vm_resource_group_name", "vm_name", "demande_id"};
        
        for (String outputName : outputNames) {
            try {
                TerraformResult result = executeTerraformCommand(workingDir, "output", "-raw", outputName);
                if (result.isSuccess()) {
                    String value = result.getOutput().trim();
                    // Nettoyer la valeur (enlever les guillemets si présents, les retours à la ligne, etc.)
                    value = value.replaceAll("^\"|\"$", "").replaceAll("\n", "").trim();
                    // Ignorer les valeurs null ou vides
                    if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                        outputs.put(outputName, value);
                        log.debug("Output récupéré: {} = {}", outputName, value);
                    } else {
                        log.debug("Output {} est null ou vide, ignoré", outputName);
                    }
                } else {
                    log.debug("Impossible de récupérer l'output: {} - {}", outputName, result.getError());
                }
            } catch (Exception e) {
                log.warn("Erreur lors de la récupération de l'output {}: {}", outputName, e.getMessage());
            }
        }
        
        log.info("Outputs Terraform récupérés: {} outputs trouvés", outputs.size());
        return outputs;
    }
    
    /**
     * Parse les outputs Terraform depuis JSON (méthode alternative si nécessaire)
     * Format JSON Terraform: {"vm_id": {"value": "...", "type": "string", "sensitive": false}, ...}
     */
    @SuppressWarnings("unused")
    private Map<String, String> parseTerraformOutputs(String json) {
        Map<String, String> outputs = new HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return outputs;
        }
        
        try {
            // Parser manuellement le JSON simple
            // Format: {"vm_id": {"value": "xxx", "type": "string"}, ...}
            json = json.trim();
            
            // Enlever les accolades externes
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
            }
            
            // Parser chaque output
            // Format attendu: "vm_id": {"value": "...", ...}
            String[] parts = json.split(",\\s*\"");
            for (String part : parts) {
                if (part.contains("\"value\"")) {
                    // Extraire le nom de l'output
                    int colonIndex = part.indexOf(':');
                    if (colonIndex > 0) {
                        String outputName = part.substring(0, colonIndex)
                                .replace("\"", "")
                                .replace("{", "")
                                .trim();
                        
                        // Extraire la valeur
                        int valueIndex = part.indexOf("\"value\"");
                        if (valueIndex > 0) {
                            String valuePart = part.substring(valueIndex);
                            int valueStart = valuePart.indexOf(":\"") + 2;
                            int valueEnd = valuePart.indexOf("\"", valueStart);
                            if (valueEnd > valueStart) {
                                String value = valuePart.substring(valueStart, valueEnd);
                                outputs.put(outputName, value);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erreur lors du parsing JSON des outputs: {}", e.getMessage());
        }
        
        return outputs;
    }


    /**
     * Convertit les outputs en Map pour stockage
     */
    private Map<String, Object> convertOutputsToMap(Map<String, String> outputs) {
        return new HashMap<>(outputs);
    }

    /**
     * Crée une VM dans la base de données à partir des outputs Terraform
     */
    private void createVMFromTerraformOutput(TerraformExecution execution, Map<String, String> outputs) {
        Demande demande = execution.getDemande();
        
        VM vm = new VM();
        vm.setVmName(demande.getName());
        vm.setVmId(outputs.getOrDefault("vm_id", execution.getVmId()));
        vm.setDemande(demande);
        vm.setRam(demande.getRam());
        vm.setCpu(demande.getCpu());
        vm.setTypeVm(demande.getTypeVm());
        vm.setDisque(demande.getDisque());
        vm.setTypeDisque(demande.getTypeDisque());
        vm.setOs(demande.getOs());
        vm.setVersionOs(demande.getVersionOs());
        vm.setAdresseIp(execution.getVmIpAddress());
        vm.setReseau(demande.getReseau());
        vm.setDatastore(demande.getDatastore());
        vm.setStatus("RUNNING");
        vm.setDateCreation(LocalDateTime.now());
        vm.setMonitored(false);

        vmRepository.save(vm);
        
        // Mettre à jour la demande
        demande.setStatus("PROVISIONNEE");
        demande.setDateCompletion(LocalDateTime.now());
        demandeRepository.save(demande);
    }

    /**
     * Méthodes utilitaires (copiées depuis TerraformService)
     */
    private String getInstanceType(Demande demande) {
        int cpu = extractCpu(demande.getCpu());
        int ram = extractRam(demande.getRam());
        
        if (cpu <= 2 && ram <= 4) return "Standard_B2s";
        if (cpu <= 2 && ram <= 8) return "Standard_B2ms";
        if (cpu <= 4 && ram <= 8) return "Standard_B4ms";
        if (cpu <= 4 && ram <= 16) return "Standard_D2s_v3";
        if (cpu <= 8 && ram <= 16) return "Standard_D4s_v3";
        if (cpu <= 8 && ram <= 32) return "Standard_D8s_v3";
        return "Standard_D16s_v3";
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

    private String getImagePublisher(String os) {
        if (os == null) return "Canonical";
        String osLower = os.toLowerCase();
        if (osLower.contains("windows") || osLower.contains("win")) return "MicrosoftWindowsServer";
        if (osLower.contains("centos")) return "OpenLogic";
        if (osLower.contains("debian")) return "Debian";
        if (osLower.contains("redhat") || osLower.contains("rhel")) return "RedHat";
        return "Canonical";
    }

    private String getImageOffer(String os) {
        if (os == null) return "UbuntuServer";
        String osLower = os.toLowerCase();
        if (osLower.contains("windows") || osLower.contains("win")) return "WindowsServer";
        if (osLower.contains("centos")) return "CentOS";
        if (osLower.contains("debian")) return "debian-11";
        if (osLower.contains("redhat") || osLower.contains("rhel")) return "RHEL";
        return "UbuntuServer";
    }

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
        String ubuntuVersion = version != null ? version : "22.04";
        return ubuntuVersion + "-LTS";
    }

    /**
     * Classe interne pour stocker le résultat d'une exécution Terraform
     */
    private static class TerraformResult {
        private final boolean success;
        private final String output;
        private final String error;

        public TerraformResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
    }
}

