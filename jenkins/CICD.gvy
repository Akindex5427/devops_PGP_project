pipeline {
    agent any
    stages {
        stage('compile') {
            steps {
                // Step 1: Compile the code
                echo 'compiling..'
                git url: 'https://github.com/Akindex5427/devops_PGP_project.git', branch: 'main'
                sh script: '/opt/maven/bin/mvn compile'
            }
        }
        stage('codereview-pmd') {
            steps {
                // Step 2: Code review with PMD
                echo 'codereview..'
                sh script: '/opt/maven/bin/mvn -P metrics pmd:pmd'
            }
            post {
                success {
                    recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
                }
            }		
        }
        stage('unit-test') {
            steps {
                // Step 3: Run unit tests
                echo 'unittest..'
                sh script: '/opt/maven/bin/mvn test'
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml'
                }
            }			
        }
        stage('codecoverage') {
            steps {
                echo 'codecoverage...'
                sh script: '/opt/maven/bin/mvn verify' // Fixed path typo
            }
            post {
                success {
                    cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: 'target/site/cobertura/coverage.xml', conditionalCoverageTargets: '70, 0, 0', failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false                  
                }
            }		
        }
        stage('package/build-war') {
            steps {
                // Step 5: Package the application
                echo 'package......'
                sh script: '/opt/maven/bin/mvn package'	
            }		
        }
        stage('build & push docker image') {
            steps {
                withDockerRegistry(credentialsId: 'DOCKER_HUB_LOGIN', url: 'https://index.docker.io/v1/') {
                    sh script: 'cd $WORKSPACE' // Ensure WORKSPACE is used correctly
                    sh script: "docker build --file Dockerfile --tag docker.io/FisayoAkinde/abctech:\${BUILD_NUMBER} ."
                    sh script: "docker push docker.io/FisayoAkinde/abctech:\${BUILD_NUMBER}"
                }	
            }		
        }
        stage('deploy-QA') {
            steps {
                sh script: "sudo ansible-playbook --inventory /tmp/myinv \$WORKSPACE/deploy/deploy-kube.yml --extra-vars \"env=qa build=\${BUILD_NUMBER}\""
            }		
        }
    }
}
