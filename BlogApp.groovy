def Org="cb-thunder"
def Project="Blog"
def Environments = ["Dev","QA","Pre-Prod","Prod"]
def Resource = 'k8s-agent'

project Project,{
	Environments.each { Env ->
		environment Env, {
			cluster environmentName, {
				definitionParameter = [
					config: 'Helm',
					namespace: '$[/javascript myEnvironment.toLowerCase()+"-"+myJob.launchedByUser]',
				]
				pluginKey = 'EC-Helm'
				}
			utilityResource 'API', {
				resourceName = Resource
			}
		}
	}

	application 'Blogging Application', {
		applicationType = 'microservice'
		microservice 'Backend', {
			definitionSource = 'git'
			definitionSourceParameter = [
				branch: '$[ScmBranch]',
				config: 'github-sandbox',
				repoUrl: (String) "https://github.com/${Org}/microblog-backend.git",
			]
			definitionType = 'helm'
			deployParameter = [
				additionalOptions: '''\
					--create-namespace
					--set image.repository=gcr.io/cb-thunder-v2/microblog-backend
					--set image.tag=1.0.2
					--set hostname=backend.$[/server/hostName]
				'''.stripIndent(),
				chart: './chart',
				releaseName: 'microblog-backend',
			]
			process 'Deploy Microservice Process', {
				description = 'System generated process for microservice deployment'
				microserviceName = 'Backend'
				processType = 'DEPLOY'
				processStep 'Retrieve Artifact', {
					processStepType = 'plugin'
					subprocedure = 'Source Provider'
					subproject = '/plugins/EC-Git/project'
					useUtilityResource = '0'
				}
				processStep 'Deploy Microservice', {
					processStepType = 'plugin'
					subprocedure = 'Deploy Service'
					subproject = '/plugins/EC-Helm/project'
					useUtilityResource = '0'
				}
				processDependency 'Retrieve Artifact', targetProcessStepName: 'Deploy Microservice', {
					branchType = 'ALWAYS'
				}
			}
		}

		microservice 'Frontend', {
			definitionSource = 'git'
			definitionSourceParameter = [
				'branch': '$[ScmBranch]',
				'config': 'github-sandbox',
				'repoUrl': (String) "https://github.com/${Org}/microblog-frontend.git",
			]
			definitionType = 'helm'
			deployParameter = [
				'additionalOptions': '''\
					--create-namespace
					--set hostname=frontend.$[/myJob/ec_microservice_deployment_parameters/$[/myMicroservice]/clusterDefinition/namespace].$[/server/hostName]
					--set backendUrl=http://backend.$[/server/hostName]
					--set image.repository=gcr.io/cb-thunder-v2/microblog-frontend
					--set image.tag=1.0.8
				'''.stripIndent(),
				'chart': './chart',
				'releaseName': 'microblog-frontend',
				'values': '',
			]
			process 'Deploy Microservice Process', {
				processStep 'Retrieve Artifact', {
					alwaysRun = '0'
					dependencyJoinType = 'and'
					errorHandling = 'abortJob'
					processStepType = 'plugin'
					subprocedure = 'Source Provider'
					subproject = '/plugins/EC-Git/project'
					useUtilityResource = '0'
				}
				processStep 'Deploy Microservice', {
					alwaysRun = '0'
					errorHandling = 'abortJob'
					processStepType = 'plugin'
					subprocedure = 'Deploy Service'
					subproject = '/plugins/EC-Helm/project'
					useUtilityResource = '0'
				}
				processStep 'Create application link', {
					actualParameter = [
						'commandToRun': '''\
							ectool setProperty "/myJob/report-urls/Micro Blog Application" "http://frontend.$[/myJob/ec_microservice_deployment_parameters/$[/myMicroservice]/clusterDefinition/namespace].$[/server/hostName]"								
								\$[/javascript
									// Only run in Pipeline context
									getProperty("/myStageRuntime")?"":"#"
								] ectool setProperty "/myStageRuntime/ec_summary/Micro Blog Application" "<html><a target="_blank" href='http://frontend.$[/myJob/ec_microservice_deployment_parameters/$[/myMicroservice]/clusterDefinition/namespace].$[/server/hostName]'>link</a></html>"
						'''.stripIndent(),
					]
					processStepType = 'command'
					subprocedure = 'RunCommand'
					subproject = '/plugins/EC-Core/project'
				}
				processDependency 'Retrieve Artifact', targetProcessStepName: 'Deploy Microservice', {
					branchType = 'ALWAYS'
				}
				processDependency 'Deploy Microservice', targetProcessStepName: 'Create application link', {
					branchType = 'ALWAYS'
				}
			}
		}

		process 'Deploy Application', {
			formalParameter 'ScmBranch', defaultValue: 'main', {
				expansionDeferred = '0'
				orderIndex = '1'
				required = '1'
				type = 'entry'
			}
			processStep 'Backend', {
				alwaysRun = '0'
				errorHandling = 'abortJob'
				processStepType = 'process'
				submicroservice = 'Backend'
				submicroserviceProcess = 'Deploy Microservice Process'
				useUtilityResource = '1'
			}

			processStep 'Delete git files', {
				actualParameter = [
					'commandToRun': 'rm -rf gitSources',
				]
				alwaysRun = '0'
				dependencyJoinType = 'and'
				errorHandling = 'abortJob'
				processStepType = 'command'
				subprocedure = 'RunCommand'
				subproject = '/plugins/EC-Core/project'
				useUtilityResource = '1'
			}

			processStep 'Frontend', {
				alwaysRun = '0'
				errorHandling = 'abortJob'
				processStepType = 'process'
				submicroservice = 'Frontend'
				submicroserviceProcess = 'Deploy Microservice Process'
				useUtilityResource = '1'
			}

			processDependency 'Backend', targetProcessStepName: 'Delete git files', {
				branchType = 'ALWAYS'
			}

			processDependency 'Delete git files', targetProcessStepName: 'Frontend', {
				branchType = 'ALWAYS'
			}

		}
		Environments.each { Env ->
			tierMap Env, {
				environmentName = Env
				environmentProjectName = projectName
				microserviceMapping "${Env}-Frontend", {
					clusterName = Env
					clusterProjectName = projectName
					microserviceName = 'Frontend'
				}
				microserviceMapping "${Env}-Backend", {
					clusterName = Env
					clusterProjectName = projectName
					microserviceName = 'Backend'
				}
			}
		}
	}
}