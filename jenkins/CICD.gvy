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
        echo 'unittest..'
        sh script: '/opt/maven/bin/mvn test'
    }
    post {
        success {
            script {
                // Check if report files exist before parsing
                def reportFiles = findFiles(glob: 'target/surefire-reports/*.xml')
                if (reportFiles) {
                    junit 'target/surefire-reports/*.xml'
                } else {
                    echo 'No test reports found, skipping JUnit parsing.'
                }
            }
        }
    }			
}
        stage('codecoverage') {
    tools {
        jdk 'java1.8'
    }
    steps {
        echo 'codecoverage..'
        sh script: '/opt/maven/bin/mvn verify' // Triggers jacoco:report from pom.xml
    }
    post {
        success {
            jacoco(execPattern: 'target/jacoco.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java', exclusionPattern: 'src/test*')
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
                    // Note: cd $WORKSPACE is redundant but kept as comment for clarity if Dockerfile relies on it
                    // sh script: 'cd ${WORKSPACE}'
                    sh script: "docker build --file Dockerfile --tag docker.io/FisayoAkinde/abctech:\${BUILD_NUMBER} ."
                    sh script: "docker push docker.io/FisayoAkinde/abctech:\${BUILD_NUMBER}"
                }	
            }		
        }
        stage('deploy-QA') {
            steps {
                sh script: "sudo ansible-playbook --inventory /tmp/myinv \${WORKSPACE}/deploy/deploy-kube.yml --extra-vars \"env=qa build=\${BUILD_NUMBER}\""
            }		
        }
    }
}
