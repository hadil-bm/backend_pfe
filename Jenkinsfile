pipeline {
    agent any

    environment {
        DOCKERHUB = credentials('docker-hub-creds')
        DOCKER_USER = "hadilbenmasseoud"
        BACKEND_IMAGE = "backend"
        BUILD_TAG = "${env.BUILD_NUMBER}-${new Date().format('yyyyMMdd-HHmmss')}"
    }

    stages {
        stage('Checkout') {
            steps {
                // Force le nettoyage pour être sûr d'avoir la dernière version
                cleanWs()
                git branch: 'main',
                    url: 'https://github.com/hadil-bm/backend_pfe.git',
                    credentialsId: 'github-token'
            }
        }

        stage('Build Backend') {
            steps {
                // ATTENTION : J'utilise ici votre orthographe exacte "authetification" (sans 'n')
                dir('authetification') { 
                    sh """
                        # On rend le wrapper exécutable (juste au cas où)
                        chmod +x mvnw
                        # On lance le build
                        ./mvnw clean package -DskipTests
                    """
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Même chose ici : orthographe exacte du dossier git
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
