def call () {
    pipeline {
        agent any
        parameters {
            booleanParam(name: 'UPDATE_PARAMS', defaultValue: false, description: '!!! Check this box if only the configuration files have been changed !!!')
            booleanParam(name: 'CHECK_CONNECTION', defaultValue: true, description: 'Check SSH Connection')
            choice(name: 'SERVER', choices: ['192.168.1.40'], description: 'Выберите сервер для установки')


        }
        environment {
            SSH_CREDENTIALS_ID = 'ssh_cred_srv'
            CREDENTIALS_ID_GIT = 'git'

        }
        stages {

            stage('Check Ansible Version') {
                when {
                    expression { params.UPDATE_PARAMS == true }
                }
                steps {
                    sh 'ansible --version'
                }
            }

            stage('Check SSH Connection') {
                when {
                    allOf {
                        expression { params.CHECK_CONNECTION }
                        not { expression { params.UPDATE_PARAMS } }
                    }
                }
                steps {
                    sshagent(credentials: [SSH_CREDENTIALS_ID]) {
                        sh """
                        if ssh -o StrictHostKeyChecking=no user@${params.SERVER} "echo SSH connection successful"; then
                            echo "SSH connection to ${params.SERVER} successful."
                        else
                            echo "Failed to connect to ${params.SERVER} via SSH."
                        fi
                        """
                    }

                }

            }


        }
        post {
            always {
                cleanWs()
            }
        }
    }
}
