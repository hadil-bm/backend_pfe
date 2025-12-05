pipeline {
    agent any

    environment {
        DOCKERHUB = credentials('44248ad2-0b55-4dd8-9243-281f38fc118a')   // ID Jenkins
        DOCKER_USER = "hadilbenmasseoud"
        BACKEND_IMAGE = "backend"
        BUILD_TAG = "${env.BUILD_NUMBER}-${new Date().format('yyyyMMdd-HHmmss')}"
    }

    stages {

        /* ======== CLONE BACKEND ======== */
        stage('Checkout') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/hadil-bm/backend_pfe.git',
                    credentialsId: 'github-token'
            }
        }

        /* ======== BUILD SPRING BOOT ======== */
        stage('Build Backend') {
            steps {
                dir('backend/authetification') {
                    sh """
                        chmod +x mvnw || true
                        ./mvnw clean package -DskipTests
                    """
                }
            }
        }

        /* ======== BUILD DOCKER IMAGE ======== */
        stage('Docker Build') {
            steps {
                dir('backend/authetification') {
                    sh """
                        docker build --no-cache -t ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG} .
                        docker tag ${DOCKER_USER}/${BACKEND_IMAGE}:${BUILD_TAG} ${DOCKER_USER}/${BACKEND_IMAGE}:latest
                    """
                }
            }
        }

        /* ======== PUSH DOCKERHUB ======== */
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
        success {
            echo "✅ Backend CI pipeline completed successfully!"
        }
        failure {
            echo "❌ Backend CI pipeline failed!"
        }
    }
}
