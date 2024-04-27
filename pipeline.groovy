def url_repo = "https://github.com/andresmerida/academic-management-ui.git"

pipeline {
    agent {
        label 'jenkins_slave'
    }
    environment {
        VAR='NUEVO'
    }
    tools {
        nodejs 'nodejs'
    }
    parameters {
        string defaultValue: 'main', description: 'Colocar un branch a deployar', name: 'BRANCH', trim: false
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
        stage('Instalar dependencias') {
            steps {
                sh 'npm install'
            }
        }
        stage('Compilar proyecto') {
            steps {
                sh 'npm run build'
                archiveArtifacts artifacts: '**/dist/**', onlyIfSuccessful: true
            }
        }
        stage("Test vulnerability") {
            when {
                expression { SCAN_GRYPE == 'YES' }
            }
            steps {
                sh "grype ${WORKSPACE}/dist/* > informe-scan.txt" 
                archiveArtifacts artifacts: 'informe-scan.txt', onlyIfSuccessful: true 
            }
        }
    }
}
