def call () {
    pipeline {
        agent any
        parameters {
            booleanParam(name: 'UPDATE_PARAMS', defaultValue: false, description: '!!! Check this box if only the configuration files have been changed !!!')
            choice(name: 'SERVER', choices: ['192.168.1.40'], description: 'Choose your server')
            booleanParam(name: 'DEPLOY_JUPYTERHUB', defaultValue: false, description: 'Deploy JupyterHub ')
            booleanParam(name: 'RESTART_JUPYTERHUB', defaultValue: false, description: 'Restart service JupyterHub')
            booleanParam(name: 'VERBOSE', defaultValue: false, description: 'Run ansible-playbook with verbose mode (-vv)')
            booleanParam(name: 'CHECK_MODE', defaultValue: false, description: 'Run ansible-playbook in check mode (--check)')
            booleanParam(name: 'INSTALL_TAG', defaultValue: false, description: 'Apply install tag')
            booleanParam(name: 'UNINSTALL_TAG', defaultValue: false, description: 'Apply uninstall tag')
            booleanParam(name: 'LAUNCH_SERVICE_TAG', defaultValue: false, description: 'Launch services with Ansible')
            string(name: 'JUPYTER_ADMIN_TOKEN', defaultValue: '', description: 'Admin token for JupyterHub')



        }

        environment {
            SSH_CREDENTIALS_ID = 'ssh_cred_srv'
            CREDENTIALS_ID_GIT = 'GIT'
            PROJECT = 'MikhailKalikin'
            BRANCH = 'main'
            COMPONENT = 'ansible-role-jupyterhub-install'
            ANSIBLE_HOST = "${params.SERVER}"

        }
        stages {

            stage('Check JENKINS_URL') {
                steps {
                    script {
                        echo "JENKINS_URL: ${env.JENKINS_URL}"
                    }
                }
            }


            stage('🔄Updates parameters pipeline') {
                steps {
                    script {
                        if (params.UPDATE_PARAMS) {
                            echo "Skipping all stages"
                        }
                    }
                }
            }


            stage('Check Ansible Version') {
                when {
                    expression { params.UPDATE_PARAMS == true }
                }
                steps {
                    sh 'ansible --version'
                }
            }



/*
            stage('🛠️Initialize contour') {
                when {
                    expression { !params.UPDATE_PARAMS }
                }
                steps {
                    script {
                        def contour = 'UNKNOWN'
                        if (env.JENKINS_URL.contains('setup')) {
                            contour = 'DEV'
                            credentialsId = env.DEV_CREDENTIALS_ID
                        } else if (env.JENKINS_URL.contains('setup')) {
                            contour = 'IFT'
                        }

                        if (!credentialsId.isEmpty()) {
                            withCredentials([usernamePassword(credentialsId: credentialsId, passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                                echo "Using tech user for contour $contour: $USERNAME"
                            }

                        } else {
                            echo "Contour is not recognized or credentials are not set: $contour"
                        }
                    }
                }
            }
            
 */
            stage('🗂Create dir') {
                when {
                    expression { !params.UPDATE_PARAMS }
                }
                steps {
                    script {
                        if (!fileExists('jupyterhub_roles')) {
                            sh 'mkdir -p jupyterhub_roles'
                            sh 'pwd'
                        }
                    }
                }
            }

            stage('⚙️Clone Playbooks') {
                when {
                    expression { !params.UPDATE_PARAMS }
                }
                steps {
                    dir('jupyterhub_roles') {
                        checkout([
                                $class           : 'GitSCM',
                                branches         : [[name: "*/${BRANCH}"]],
                                userRemoteConfigs: [[
                                    credentialsId: "${CREDENTIALS_ID_GIT}",
                                    url: "https://github.com/${PROJECT}/${COMPONENT}.git"
                        ]]
                    ])
                    }
                }
            }


            /*stage('🗂️Create Directories') {
                when {
                    expression { !params.UPDATE_PARAMS }
                }
                steps {
                    script {
                        sh '''
                        mkdir -p distr/dir
                        ls -la
                        pwd
                        '''
                    }
                }
            }

             */


            stage('🎨Prepare Inventory') {
                when {
                    expression { !params.UPDATE_PARAMS }
                }
                steps {
                    script {
                            dir('jupyterhub_roles') {
                                sh """
                                pwd
                                ls -la
                                sed -i 's/JUPYTER_HOST/${params.SERVER}/' inventory.ini
                                cat inventory.ini
                            """
                            }

                    }
                }
            }



            stage('🚀Deploy JupyterHub') {
                when {
                    expression { params.DEPLOY_JUPYTERHUB == true && !params.UPDATE_PARAMS }
                }
                steps {
                    script {
                        sshagent(credentials: [SSH_CREDENTIALS_ID]) {
                            dir('jupyterhub_roles') {
                                def verboseParam = params.VERBOSE ? "-vv" : ""
                                def checkModeParam = params.CHECK_MODE ? "--check" : ""
                                def tagsParam = params.LAUNCH_SERVICE_TAG ? "launch_services" : ""

                                if (params.INSTALL_TAG) {
                                    tagsParam += tagsParam ? ",install" : "install"
                                }
                                if (params.UNINSTALL_TAG) {
                                    tagsParam += tagsParam ? ",uninstall" : "uninstall"
                                }

                                def extraVars = "jupyter_admin_token=${params.JUPYTER_ADMIN_TOKEN}"

                                sh """
                                ansible-playbook ${verboseParam} ${checkModeParam} --tags ${tagsParam} --extra-vars "${extraVars}" -i inventory.ini install_jupyterhub.yaml
                                """
                            }
                        }
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