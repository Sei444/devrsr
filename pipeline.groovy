def url_repo = "https://github.com/andresmerida/academic-management-ui.git"

pipeline {
    agent {
        label 'jenkins_slave'
    }
    environment {
        VAR='NUEVO'
        workspace="/data/"
    }
    tools {
        jdk 'jdk21'
        nodejs 'nodejs'
        dockerTool 'docker'
    }
    parameters {
        string defaultValue: 'deploy', description: 'Colocar un branch a deployar', name: 'BRANCH', trim: false
        choice (name: 'SCAN_GRYPE', choices: ['YES', 'NO'], description: 'Activar escáner con grype')
    }
    stages {
        stage("create build name") {
            steps {
                script {
                    currentBuild.displayName = "frontend-" + currentBuild.number
                }
            }
        }
        stage("Limpiar") {
            steps {
                cleanWs()
            }
        }
        stage("Descargar proyecto") {
            steps {
                git credentialsId: 'git_credentials', branch: "${BRANCH}", url: "${url_repo}"
                echo "Proyecto descargado"
            }
        }
        stage('Compilar proyecto') {
            steps {
                sh "npm version"
                sh "pwd"
                sh "npm install"
                sh "npm run build"
                sh "tar -rf dist.tar dist/"
                archiveArtifacts artifacts: 'dist.tar',onlyIfSuccessful:true
                echo "Proyecto buildeado"
            }
        }
        stage('Build Docker Image') {
            
            steps {
                sh "docker --version"
                sh "pwd"
                sh "docker build -t prueba_proyecto:1.0 ."
                sh "docker tag prueba_proyecto:1.0 sei444/prueba_proyecto:0.0.1"
                withCredentials([usernamePassword(credentialsId: dockerhub_id, usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD"
                    sh "docker push sei444/prueba_proyecto2:0.0.1"
                }
                
            }
        }        
        stage("Test vulnerability") {
            when {
                expression { SCAN_GRYPE == 'YES' }
            }
            steps {
                sh "/grype node_modules/ > informe-scan-ui.txt"
                sh "pwd"
                archiveArtifacts artifacts: 'informe-scan.txt', onlyIfSuccessful: true 
            }
        }

        stage('sonarqube analysis'){
            steps{
                script{
                    sh "pwd"
                    writeFile encoding: 'UTF-8', file: 'sonar-project.properties', text: """sonar.projectKey=academy-front
                    sonar.projectName=academy-front
                    sonar.projectVersion=academy-front
                    sonar.sourceEncoding=UTF-8
                    sonar.sources=src/
                    sonar.exclusions=*/node_modules/,/.spec.js
                    sonar.language=js
                    sonar.scm.provider=git
                    """
                    withSonarQubeEnv('Sonar_CI') {
                        def scannerHome = tool 'Sonar_CI'
                        sh "${tool("Sonar_CI")}/bin/sonar-scanner -X"
                    }
                }
            }
        }
    }
}
