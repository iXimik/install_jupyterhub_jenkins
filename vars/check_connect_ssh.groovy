def call () {
    pipeline {
        agent any
        parameters {
            booleanParam(name: 'CHECK_CONNECTION', defaultValue: true, description: 'Check SSH Connection')
            choice(name: 'SERVER', choices: ['setup'], description: 'Выберите сервер')


        }
        environment {
            SSH_CREDENTIALS_ID = 'ssh_cred_box_dev'
            CREDENTIALS_ID_GIT = 'git'

        }
        stages {
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
    }
}
