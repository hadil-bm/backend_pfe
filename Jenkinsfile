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
                git branch: 'main',
                    url: 'https://github.com/hadil-bm/backend_pfe.git',
                    credentialsId: 'github-token'
            }
        }

        stage('Build Backend') {
            steps {
                // CORRECTION ICI : Vérifiez l'orthographe exacte de votre dossier !
                // J'ai corrigé 'authetification' -> 'authentication' (avec un 'n')
                dir('authentication') { 
                    sh """
                        # On liste les fichiers pour être sûr que mvnw est là (pour le debug)
                        ls -la
                        
                        chmod +x mvnw || true
                        ./mvnw clean package -DskipTests
                    """
                }
            }
        }

        stage('Docker Build') {
            steps {
                // CORRECTION ICI AUSSI
                dir('authentication') {
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
