def url_repo = "https://github.com/andresmerida/academic-management-ui.git"
pipeline {
    agent {
        label 'jenkins_slave'
    }
    tools {
        jdk 'jdk21'
        nodejs 'nodejs'
        dockerTool 'docker'
    }
    parameters {
        string(defaultValue: 'dev', description: 'Colocar un brach a deployar', name: 'BRANCH', trim: false)
    }
    environment {
        VAR = 'NUEVO'
    }
    stages {
        stage("create build name") {
            steps {
                script {
                    currentBuild.displayName = "service_back-" + currentBuild.number
                }
            }
        }
        stage("Clean") {
            steps {
                cleanWs()
            }
        }
        stage("download proyect") {
            steps {
                git credentialsId: 'git_credentials', branch: "${BRANCH}", url: "${url_repo}"
                echo "proyecto ui descargado"
            }
        }
        stage("build proyect"){
            steps{
                echo "iniciando el build"
                sh "npm version"
                sh "pwd"
                sh "npm install"
                sh "pwd"
                sh "npm run build"
                sh "tar -rf dist.tar dist/"
                archiveArtifacts artifacts: 'dist.tar',onlyIfSuccessful:true
            }
        }
        stage("Test vulnerability"){
            steps{
                sh "/grype node_modules/ > informe-scan-ui.txt"
                sh "pwd"
                archiveArtifacts artifacts: 'informe-scan-ui.txt', onlyIfSuccessful: true
            }
        }
	stage('Build Docker Image') {
            steps {
                sh "docker --version"
                sh "pwd"
                sh "docker build -t prueba_proyecto:1.0 ."
                sh "docker tag prueba_proyecto:1.0 dafnec/prueba_proyecto:1.0"
                sh "docker login -u s3rgi0444@gmail.com -p Delax4445."
                sh "docker push sei444/prueba_proyecto:1.0"
                
            }
        }
        stage('sonarqube analysis'){
            steps{
               script{
                   sh "pwd"
						writeFile encoding: 'UTF-8', file: 'sonar-project.properties', text: """sonar.projectKey=academy-ui
						sonar.projectName=academy-ui
						sonar.projectVersion=academy-ui
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
