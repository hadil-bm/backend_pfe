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

        // Nmap target
        NETWORK_TARGET = "192.168.1.0/24"
    }

    stages {

        /* ---------------- CHECKOUT ---------------- */
        stage('Checkout') {
            steps {
                cleanWs()
                git branch: 'main',
                    url: 'https://github.com/hadil-bm/backend_pfe.git',
                    credentialsId: 'github-token'
            }
        }

        /* ---------------- BUILD MAVEN ---------------- */
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

        /* ---------------- DOCKER BUILD ---------------- */
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

        /* ---------------- PUSH DOCKERHUB ---------------- */
        stage('Push DockerHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds',
                                                  usernameVariable: 'DOCKERHUB_USR',
                                                  passwordVariable: 'DOCKERHUB_PSW')]) {
                    sh """
                        echo $DOCKERHUB_PSW | docker login -u $DOCKERHUB_USR --password-stdin
                        docker push ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG}
                        docker push ${DOCKER_USER}/${BACKEND_IMAGE}:latest
                    """
                }
            }
        }

        /* ---------------- SONARQUBE ---------------- */
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
                                  -Dsonar.login=$SONAR_AUTH_TOKEN
                            """
                        }
                    }
                }
            }
        }

        /* ---------------- NMAP NETWORK SCAN (INVERTED FIRST) ---------------- */
        stage('Nmap Network Scan') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh """
                        if ! command -v nmap >/dev/null 2>&1; then
                            echo "❌ Nmap n'est pas installé sur l'agent Jenkins."
                            exit 1
                        fi

                        nmap -sV -oN nmap_scan_result.txt ${NETWORK_TARGET}
                        echo "✅ Scan Nmap terminé."
                    """
                }
            }
        }

        stage('OWASP Dependency-Check') {
    steps {
        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            // Injection correcte de la clé NVD
            withCredentials([string(credentialsId: 'nvd-api-key', variable: 'nvd-api-key')]) {

                // Créer le dossier de sortie
                sh 'mkdir -p dependency-check-report'

                // Lancer le scan avec la variable correctement échappée pour shell
                sh """
                dependency-check \
                  --project "MonProjet" \
                  --scan ./authetification \
                  --out ./dependency-check-report \
                  --prettyPrint \
                  --format ALL \
                  --nvdApiKey \$nvd-api-key
                """

                // Lister les fichiers générés pour debug
                sh 'ls -l dependency-check-report'
            }

            // Publier les rapports
            dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml'
        }
    }
}


    post {
        success {
            echo "✅ Pipeline terminé avec succès !"
            archiveArtifacts artifacts: 'nmap_scan_result.txt', allowEmptyArchive: true
            archiveArtifacts artifacts: 'dependency-check-report/*', allowEmptyArchive: true
        }
        failure {
            echo "❌ Pipeline échoué !"
        }
    }
}
