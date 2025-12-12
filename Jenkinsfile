pipeline {
    agent any

    environment {
        // Force le chemin vers Java 21 (Chemin standard sur Ubuntu)
        JAVA_HOME = "/usr/lib/jvm/java-21-openjdk-amd64"
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        
        DOCKERHUB = credentials('docker-hub-creds')
        DOCKER_USER = "hadilbenmasseoud"
        BACKEND_IMAGE = "backend"
        BUILD_TAG = "${env.BUILD_NUMBER}-${new Date().format('yyyyMMdd-HHmmss')}"

        // Variables SonarQube
        SONARQUBE_URL = "http://48.220.33.106:9000"   // Remplace par ton URL SonarQube
        SONARQUBE_TOKEN = credentials('sonarqube')   // ID du token stocké dans Jenkins Credentials
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
                sh """
                    echo ${DOCKERHUB_PSW} | docker login -u ${DOCKERHUB_USR} --password-stdin
                    docker push ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG}
                    docker push ${DOCKER_USER}/${BACKEND_IMAGE}:latest
                """
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = "${SONARQUBE_SCANNER_HOME}"
                    withSonarQubeEnv('ooredoo') {
                        sh """
                        ${scannerHome}/bin/sonar-scanner \
                          -Dsonar.projectKey=ooredoo \
                          -Dsonar.sources=. \
                          -Dsonar.host.url=${SONARQUBE_URL} \
                          -Dsonar.login=${SONARQUBE_TOKEN}
                        """
                    }
                }
            }
        }
    }
    
    post {
        success { echo "✅ Success!" }
        failure { echo "❌ Failed!" }
    }
}
