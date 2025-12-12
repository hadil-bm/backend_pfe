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

        /* ---------------- OWASP DEPENDENCY CHECK ---------------- */
        stage('OWASP Dependency-Check') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                        
                        dependencyCheck additionalArguments: """
                            -o './dependency-check-report' \
                            -s './authetification' \
                            --prettyPrint \
                            --format ALL \
                            --nvdApiKey=${NVD_API_KEY}
                        """, odcInstallation: 'dependency-check'

                        dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml'
                    }
                }
            }
        }

        /* ---------------- NMAP NETWORK SCAN ---------------- */
        stage('Nmap Network Scan') {
            steps {
                sh "nmap -st -p 80,443,90 app.4.251.133.114.nip.io"
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
