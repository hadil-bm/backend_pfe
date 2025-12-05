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
                // ATTENTION : Orthographe "authetification" comme sur votre GitHub
                dir('authetification') { 
                    sh """
                        # Debug : vérifie la version de Java utilisée
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
    }
    
    post {
        success { echo "✅ Success!" }
        failure { echo "❌ Failed!" }
    }
}
