pipeline {
    agent any

    environment {
        // Java 21
        JAVA_HOME = "/usr/lib/jvm/java-21-openjdk-amd64"
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        // DockerHub
        DOCKER_USER = "hadilbenmasseoud"
        BACKEND_IMAGE = "backend"
        BUILD_TAG = "${env.BUILD_NUMBER}-${new Date().format('yyyyMMdd-HHmmss')}"

        // SonarQube
        SONARQUBE_URL = "http://48.220.33.106:9000"

        // Nmap
        NETWORK_TARGET = "192.168.1.0/24"  // Remplacer par IP ou réseau à scanner
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                git branch: 'main',
                    url: 'https://github.com/hadil-bm/backend_pfe.git',
                    credentialsId: 'github-token'
            }
        }

        stage('Build Backend') {
            steps {
                dir('authetification') {
                    sh """
                        java -version
                        chmod +x mvnw
                        ./mvnw clean package -DskipTests
                    """
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir('authetification') {
                    sh """
                        docker build --no-cache -t ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG} .
                        docker tag ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG} ${DOCKER_USER}/${BACKEND_IMAGE}:latest
                    """
                }
            }
        }

        stage('Push DockerHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', usernameVariable: 'DOCKERHUB_USR', passwordVariable: 'DOCKERHUB_PSW')]) {
                    sh """
                        echo $DOCKERHUB_PSW | docker login -u $DOCKERHUB_USR --password-stdin
                        docker push ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG}
                        docker push ${DOCKER_USER}/${BACKEND_IMAGE}:latest
                    """
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    script {
                        def scannerHome = tool name: 'sonarqube', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                        dir('authetification') {
                            sh """
                                ${scannerHome}/bin/sonar-scanner \
                                  -Dsonar.projectKey=ooredoo \
                                  -Dsonar.sources=. \
                                  -Dsonar.java.binaries=target/classes \
                                  -Dsonar.host.url=${SONARQUBE_URL} \
                                  -Dsonar.login=${SONARQUBE_TOKEN}
                            """
                        }
                    }
                }
            }
        }

        stage('OWASP Dependency-Check') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                    script {
                        // Utilisation sécurisée de NVD_API_KEY
                        def additionalArgs = "-o './dependency-check-report' -s './authetification' -f 'ALL' --prettyPrint --nvdApiKey ${env.NVD_API_KEY}"
                        dependencyCheck additionalArguments: additionalArgs, odcInstallation: 'dependency-check'
                        dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml'
                    }
                }
            }
        }

        stage('Nmap Network Scan') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh """
                        # Installer Nmap si nécessaire
                        if ! command -v nmap &> /dev/null
                        then
                            sudo apt-get update && sudo apt-get install -y nmap
                        fi

                        # Lancer le scan réseau
                        nmap -sV -oN nmap_scan_result.txt ${NETWORK_TARGET}
                        echo "✅ Scan Nmap terminé. Résultats dans nmap_scan_result.txt"
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline terminé avec succès !"
            archiveArtifacts artifacts: 'nmap_scan_result.txt', allowEmptyArchive: true
        }
        failure {
            echo "❌ Pipeline échoué !"
            archiveArtifacts artifacts: 'nmap_scan_result.txt', allowEmptyArchive: true
        }
    }
}
