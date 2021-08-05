def CurrentUser = "admin"
def ProjectName = "Legacy Blog"
def KubeConfig = "fidelity-sda-sandbox"
def Environments = ["QA", "Pre-Prod", "Prod"]
def KubernetesHost = "agent-flow-agent-0.agent-flow-agents.cloudbees-sda"
//resourcePool "kubectl"
def Organization = "cb-f-sandbox"
def GcpProject = "cb-thunder-v2"
def GitHubConfig = "git-sandbox"
def kubectl = "kubectl"
def domain = "fidelity-sda.cb-demos.io"

def dest = (String) "\$[/myApplication/\$[/myComponent/componentName]/image_name]"
def GitRepo = (String) "\$[/myApplication/\$[/myComponent/componentName]/yaml_repo]"

def EnvironmentResponse

Environments.each { Env ->
	project ProjectName,{
		EnvironmentResponse = environment Env, {
			['Back End','Database','Front End'].each { Tier ->
				environmentTier Tier, {
					resource "${ProjectName}-${environmentName}-${environmentTierName}", hostName: KubernetesHost //, zoneName: "zone2", resourcePools: "kubectl"
				}
			}
			
			// Custom properties
			property "namespace", value: "${Env.toLowerCase()}-${CurrentUser}"
		}
	} // project
} // each
	
project ProjectName,{
	//property "/myJob/report-urls/$Env Environment", value: "/flow/#environments/${EnvironmentResponse.environmentId}"
	
	application 'Blogging Application', {

		applicationTier 'Front End', {
			component 'microblog-frontend', {
				description = 'Template for deploying a container to k8s'
				pluginKey = 'EC-Artifact'

				process 'Install', {
					processType = 'DEPLOY'

					processStep 'Create Artifact Placeholder', {
						actualParameter = [
							'commandToRun': """\
								artifact artifactKey: "\$[/myComponent/ec_content_details/artifactName]", groupId: "group"
							""".stripIndent(),
							'shellToUse': 'ectool evalDsl --dslFile',
						]
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processStep 'Get yaml deploy file(s)', {
						actualParameter = [
							'clone': '1',
							'commit': '',
							'config': GitHubConfig,
							'depth': '',
							'dest': dest,
							'GitBranch': '',
							'GitRepo': GitRepo,
							'overwrite': '0',
							'tag': '',
						]
						dependencyJoinType = 'and'
						processStepType = 'plugin'
						subprocedure = 'CheckoutCode'
						subproject = '/plugins/ECSCM-Git/project'
					}

					processStep 'Undeploy any existing', {
						actualParameter = [
							'commandToRun': """\
								set -x
								
								sed \'s,REPLACE_IMAGE,\$[/myApplication/\$[/myComponent/componentName]/image_name]:\$[\$[/myComponent/componentName]_version],; s,REPLACE_HOSTNAME,\$[/myEnvironment/namespace].\$[target-subdomain].${domain},;s,REPLACE_REPO_OWNER,${CurrentUser},\' \\
								\$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] \\
								| tee "\$[/myComponent/componentName]-undeploy.yaml" |$kubectl -n \$[/myEnvironment/namespace] delete -f - || echo ok
								cat "\$[/myComponent/componentName]-undeploy.yaml"
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processStep 'Deploy', {
						actualParameter = [
							'commandToRun': """\
								set -x

								

								sed \'s,REPLACE_IMAGE,\$[/myApplication/\$[/myComponent/componentName]/image_name]:\$[\$[/myComponent/componentName]_version],; s,REPLACE_HOSTNAME,\$[target-subdomain].\$[/myEnvironment/namespace].${domain},;s,REPLACE_REPO_OWNER,${CurrentUser},\' \\
								\$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] \\
								| tee "\$[/myComponent/componentName]-deploy.yaml" |$kubectl -n \$[/myEnvironment/namespace] create  -f -
								cat "\$[/myComponent/componentName]-deploy.yaml"
								
								ectool setProperty "/myJob/report-urls/Micro Blog Application" "http://\$[target-subdomain].\$[/myEnvironment/namespace].${domain}"								
								\$[/javascript
									// Only run in Pipeline context
									getProperty("/myStageRuntime")?"":"#"
								] ectool setProperty "/myStageRuntime/ec_summary/Micro Blog Application" "<html><a target="_blank" href='http://\$[target-subdomain].\$[/myEnvironment/namespace].${domain}'>link</a></html>"

								""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processDependency 'Create Artifact Placeholder', targetProcessStepName: 'Get yaml deploy file(s)'

					processDependency 'Get yaml deploy file(s)', targetProcessStepName: 'Undeploy any existing'

					processDependency 'Undeploy any existing', targetProcessStepName: 'Deploy'
				}

				process 'Uninstall', {
					processType = 'UNDEPLOY'

					processStep 'Get yaml deploy file(s)', {
						actualParameter = [
							'clone': '1',
							'commit': '',
							'config': GitHubConfig,
							'depth': '',
							'dest': dest,
							'GitBranch': '',
							'GitRepo': GitRepo,
							'overwrite': '0',
							'tag': '',
						]
						dependencyJoinType = 'and'
						processStepType = 'plugin'
						subprocedure = 'CheckoutCode'
						subproject = '/plugins/ECSCM-Git/project'
					}

					processStep 'Uninstall', {
						actualParameter = [
							'commandToRun': """\
								set -x
								
								sed \'s,REPLACE_IMAGE,\$[/myApplication/\$[/myComponent/componentName]/image_name],; s,REPLACE_HOSTNAME,\$[/myEnvironment/namespace].\$[target-subdomain].${domain},;s,REPLACE_REPO_OWNER,${CurrentUser},\' \\
								\$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] \\
								| tee "\$[/myComponent/componentName]-uninstall.yaml" |$kubectl -n \$[/myEnvironment/namespace] delete -f - || echo ok
								cat "\$[/myComponent/componentName]-uninstall.yaml"
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processDependency 'Get yaml deploy file(s)', targetProcessStepName: 'Uninstall'
				}

				// Custom properties

				property 'ec_content_details', {

					// Custom properties

					property 'artifactName', value: 'microblog-frontend'
					artifactVersionLocationProperty = "/myJob/retrievedArtifactVersions/\$[assignedResourceName]"
					filterList = ''

					property 'overwrite', value: 'update'
					pluginProcedure = 'Retrieve'

					property 'pluginProjectName', value: 'EC-Artifact'
					retrieveToDirectory = ''

					property 'versionRange', value: "\$[microblog-frontend_version]"
				}
			}
		}

		applicationTier 'Back End', {

			component 'microblog-backend', {
				description = 'Template for deploying a container to k8s'
				pluginKey = 'EC-Artifact'

				process 'Install', {
					processType = 'DEPLOY'

					processStep 'Create Artifact Placeholder', {
						actualParameter = [
							'commandToRun': """\
								artifact artifactKey: "\$[/myComponent/ec_content_details/artifactName]", groupId: "group"
							""".stripIndent(),
							'shellToUse': 'ectool evalDsl --dslFile',
						]
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processStep 'Get yaml deploy file(s)', {
						actualParameter = [
							'clone': '1',
							'commit': '',
							'config': GitHubConfig,
							'depth': '',
							'dest': dest,
							'GitBranch': '',
							'GitRepo': GitRepo,
							'overwrite': '1',
							'tag': '',
						]
						dependencyJoinType = 'and'
						processStepType = 'plugin'
						subprocedure = 'CheckoutCode'
						subproject = '/plugins/ECSCM-Git/project'
					}

					processStep 'Undeploy any existing', {
						actualParameter = [
							'commandToRun': """\
								set -x
								
								sed \'s,REPLACE_IMAGE,\$[/myApplication/\$[/myComponent/componentName]/image_name]:\$[\$[/myComponent/componentName]_version],; s,REPLACE_HOSTNAME,\$[/myEnvironment/namespace].\$[target-subdomain].${domain},; s,REPLACE_REPO_OWNER,${CurrentUser},\' \\
								\$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] \\
								| tee "\$[/myComponent/componentName]-undeploy.yaml" |$kubectl -n \$[/myEnvironment/namespace] delete -f - || echo ok
								cat "\$[/myComponent/componentName]-undeploy.yaml"
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processStep 'Deploy', {
						actualParameter = [
								'commandToRun': """\
									set -x
									
									sed \'s,REPLACE_IMAGE,\$[/myApplication/\$[/myComponent/componentName]/image_name]:\$[\$[/myComponent/componentName]_version],; s,REPLACE_HOSTNAME,\$[target-subdomain].\$[/myEnvironment/namespace].${domain},;s,REPLACE_REPO_OWNER,${CurrentUser},\' \\
									\$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] \\
									| tee "\$[/myComponent/componentName]-deploy.yaml" |$kubectl create -n \$[/myEnvironment/namespace] -f -
									cat "\$[/myComponent/componentName]-deploy.yaml"
									
								""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processDependency 'Create Artifact Placeholder', targetProcessStepName: 'Get yaml deploy file(s)'

					processDependency 'Get yaml deploy file(s)', targetProcessStepName: 'Undeploy any existing'

					processDependency 'Undeploy any existing', targetProcessStepName: 'Deploy'
				}

				process 'Uninstall', {
					processType = 'UNDEPLOY'

					processStep 'Get yaml deploy file(s)', {
						actualParameter = [
							'clone': '1',
							'commit': '',
							'config': GitHubConfig,
							'depth': '',
							'dest': dest,
							'GitBranch': '',
							'GitRepo': GitRepo,
							'overwrite': '0',
							'tag': '',
						]
						dependencyJoinType = 'and'
						processStepType = 'plugin'
						subprocedure = 'CheckoutCode'
						subproject = '/plugins/ECSCM-Git/project'
					}

					processStep 'Uninstall', {
						actualParameter = [
							'commandToRun': """\
								set -x
								
								sed \'s,REPLACE_IMAGE,\$[/myApplication/\$[/myComponent/componentName]/image_name],; s,REPLACE_HOSTNAME,\$[/myEnvironment/namespace].\$[target-subdomain].${domain},; s,REPLACE_REPO_OWNER,${CurrentUser},\' \\
								\$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] \\
								| tee "\$[/myComponent/componentName]-uninstall.yaml" |$kubectl -n \$[/myEnvironment/namespace] delete -f - || echo ok
								cat "\$[/myComponent/componentName]-uninstall.yaml" 
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processDependency 'Get yaml deploy file(s)', targetProcessStepName: 'Uninstall'
				}

				// Custom properties

				property 'ec_content_details', {

					// Custom properties

					property 'artifactName', value: 'microblog-backend'
					artifactVersionLocationProperty = "/myJob/retrievedArtifactVersions/\$[assignedResourceName]"
					filterList = ''

					property 'overwrite', value: 'update'
					pluginProcedure = 'Retrieve'

					property 'pluginProjectName', value: 'EC-Artifact'
					retrieveToDirectory = ''

					property 'versionRange', value: "\$[microblog-backend_version]"
				}
			}
		}

		applicationTier 'Database', {

			component 'microblog-db', {
				description = 'Template for deploying a container to k8s'
				pluginKey = 'EC-Artifact'

				process 'Install', {
					processType = 'DEPLOY'

					processStep 'Create Artifact Placeholder', {
						actualParameter = [
							'commandToRun': """\
								artifact artifactKey: "\$[/myComponent/ec_content_details/artifactName]", groupId: "group"
							""".stripIndent(),
							'shellToUse': 'ectool evalDsl --dslFile',
						]
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processStep 'Get yaml deploy file(s)', {
						actualParameter = [
							'clone': '1',
							'commit': '',
							'config': GitHubConfig,
							'depth': '',
							'dest': dest,
							'GitBranch': '',
							'GitRepo': GitRepo,
							'overwrite': '1',
							'tag': '',
						]
						dependencyJoinType = 'and'
						processStepType = 'plugin'
						subprocedure = 'CheckoutCode'
						subproject = '/plugins/ECSCM-Git/project'
					}

					processStep 'Undeploy any existing', {
						actualParameter = [
							'commandToRun': """\
								set -x

								

								cat \$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] | tee "\$[/myComponent/componentName]-undeploy.yaml" |$kubectl -n \$[/myEnvironment/namespace] delete -f -	|| echo ok
								cat "\$[/myComponent/componentName]-undeploy.yaml"
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processStep 'Deploy', {
						actualParameter = [
							'commandToRun': """\
								set -x

								

								cat \$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file]	| tee "\$[/myComponent/componentName]-deploy.yaml" |$kubectl -n \$[/myEnvironment/namespace] create -f -
								cat "\$[/myComponent/componentName]-deploy.yaml"
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processDependency 'Create Artifact Placeholder', targetProcessStepName: 'Get yaml deploy file(s)'

					processDependency 'Get yaml deploy file(s)', targetProcessStepName: 'Undeploy any existing'

					processDependency 'Undeploy any existing', targetProcessStepName: 'Deploy'
				}

				process 'Uninstall', {
					processType = 'UNDEPLOY'

					processStep 'Get yaml deploy file(s)', {
						actualParameter = [
							'clone': '1',
							'commit': '',
							'config': GitHubConfig,
							'depth': '',
							'dest': dest,
							'GitBranch': '',
							'GitRepo': GitRepo,
							'overwrite': '0',
							'tag': '',
						]
						dependencyJoinType = 'and'
						processStepType = 'plugin'
						subprocedure = 'CheckoutCode'
						subproject = '/plugins/ECSCM-Git/project'
					}

					processStep 'Uninstall', {
						actualParameter = [
							'commandToRun': """\
								
								sed \'s,image: .*,image: \$[/myApplication/\$[/myComponent/componentName]/image_name],\' \$[/myApplication/\$[/myComponent/componentName]/image_name]/.kubernetes/\$[/myApplication/\$[/myComponent/componentName]/yaml_file] | tee "\$[/myComponent/componentName]-uninstall.yaml" |$kubectl -n \$[/myEnvironment/namespace] delete -f -	|| echo ok
								cat "\$[/myComponent/componentName]-uninstall.yaml"
							""".stripIndent(),
						]
						dependencyJoinType = 'and'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}

					processDependency 'Get yaml deploy file(s)', targetProcessStepName: 'Uninstall'
				}

				// Custom properties

				property 'ec_content_details', {

					// Custom properties

					property 'artifactName', value: 'postgress'
					artifactVersionLocationProperty = "/myJob/retrievedArtifactVersions/\$[assignedResourceName]"
					filterList = ''

					property 'overwrite', value: 'update'
					pluginProcedure = 'Retrieve'

					property 'pluginProjectName', value: 'EC-Artifact'
					retrieveToDirectory = ''

					property 'versionRange', value: "\$[microblog-db_version]"
				}
			}
		}

		process 'Deploy', {
			exclusiveEnvironment = '0'
			processType = 'OTHER'

			formalParameter 'microblog-backend_version', defaultValue: '1.0.2', {
				orderIndex = '1'
				required = '1'
				type = 'entry'
				label = "Backend image version"
			}

			formalParameter 'microblog-frontend_version', defaultValue: '1.0.2', {
				orderIndex = '2'
				required = '1'
				type = 'entry'
				label = "Frontend image version"
			}

			formalParameter 'microblog-db_version', defaultValue: '12.1-alpine', {
				orderIndex = '3'
				required = '1'
				type = 'entry'
				label = "DB image version"
			}

			formalParameter 'target-subdomain', defaultValue: 'blog', {
				description = 'Subdomain to set your ingresses to use'
				orderIndex = '4'
				required = '1'
				type = 'entry'
			}

			formalParameter 'ec_enforceDependencies', defaultValue: '0'

			formalParameter 'ec_microblog-backend-run', defaultValue: '1', {
				expansionDeferred = '1'
				type = 'checkbox'
			}

			formalParameter 'ec_microblog-backend-version', defaultValue: "\$[/projects/BeeZone Demo/applications/Blogging Application/components/microblog-backend/ec_content_details/versionRange]", {
				expansionDeferred = '1'
				type = 'entry'
			}

			formalParameter 'ec_microblog-db-run', defaultValue: '1', {
				expansionDeferred = '1'
				type = 'checkbox'
			}

			formalParameter 'ec_microblog-db-version', defaultValue: "\$[/projects/BeeZone Demo/applications/Blogging Application/components/microblog-db/ec_content_details/versionRange]", {
				expansionDeferred = '1'
				type = 'entry'
			}

			formalParameter 'ec_microblog-frontend-run', defaultValue: '1', {
				expansionDeferred = '1'
				type = 'checkbox'
			}

			formalParameter 'ec_microblog-frontend-version', defaultValue: "\$[/projects/BeeZone Demo/applications/Blogging Application/components/microblog-frontend/ec_content_details/versionRange]", {
				expansionDeferred = '1'
				type = 'entry'
			}

			formalParameter 'ec_smartDeployOption', defaultValue: '0'

			formalParameter 'ec_stageArtifacts', defaultValue: '0'

			processStep 'Front End', {
				applicationTierName = 'Front End'
				dependencyJoinType = 'and'
				processStepType = 'process'
				subcomponent = 'microblog-frontend'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Install'
			}

			processStep 'Deploy Database', {
				applicationTierName = 'Database'
				dependencyJoinType = 'and'
				processStepType = 'process'
				subcomponent = 'microblog-db'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Install'

				// Custom properties

				property 'ec_deploy', {

					// Custom properties
					ec_notifierStatus = '0'
				}
			}

			processStep 'Deploy Backend', {
				applicationTierName = 'Back End'
				dependencyJoinType = 'and'
				processStepType = 'process'
				subcomponent = 'microblog-backend'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Install'

				// Custom properties

				property 'ec_deploy', {

					// Custom properties
					ec_notifierStatus = '0'
				}
			}

			processStep 'Run Smoke Test', {
				actualParameter = [
					'commandToRun': 'echo "Smoke Testing..."',
				]
				applicationTierName = 'Front End'
				dependencyJoinType = 'and'
				processStepType = 'command'
				subprocedure = 'RunCommand'
				subproject = '/plugins/EC-Core/project'

				// Custom properties

				property 'ec_deploy', {

					// Custom properties
					ec_notifierStatus = '0'
				}
			}

			processDependency 'Deploy Database', targetProcessStepName: 'Deploy Backend'

			processDependency 'Deploy Backend', targetProcessStepName: 'Run Smoke Test'

			processDependency 'Front End', targetProcessStepName: 'Run Smoke Test'
		}

		process 'Undeploy', {
			processType = 'OTHER'
			
			formalParameter 'target-subdomain', defaultValue: 'blog', {
				description = 'Subdomain to set your ingresses to use'
				required = '1'
				type = 'entry'
			}
			formalParameter 'ec_enforceDependencies', defaultValue: '0'

			formalParameter 'ec_smartDeployOption', defaultValue: '1'

			formalParameter 'ec_stageArtifacts', defaultValue: '0'

			processStep 'frontend', {
				applicationTierName = 'Front End'
				processStepType = 'process'
				subcomponent = 'microblog-frontend'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Uninstall'
			}
			
			processStep 'backend', {
				applicationTierName = 'Back End'
				processStepType = 'process'
				subcomponent = 'microblog-backend'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Uninstall'
			}
			processStep 'db', {
				applicationTierName = 'Database'
				processStepType = 'process'
				subcomponent = 'microblog-db'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Uninstall'
			}			
		}

		
		Environments.each { Env -> 
			tierMap "${applicationName}-$Env",
				environmentProjectName: projectName,
				environmentName: Env,
				tierMapping: ['Front End':'Front End','Back End':'Back End','Database':'Database']			
		} // each Env		

		property 'microblog-backend', {

			// Custom properties
			image_name = "gcr.io/${GcpProject}/${Organization}/microblog-backend"
			// image_name = "gcr.io/${GcpProject}/microblog-backend"
			yaml_file = 'backend.yaml'
			yaml_repo = "https://github.com/${Organization}/microblog-backend.git"
		}

		property 'microblog-db', {

			// Custom properties
			image_name = 'postgres'
			yaml_file = 'postgres.yaml'
			yaml_repo = "https://github.com/${Organization}/microblog-backend.git"
		}

		property 'microblog-frontend', {

			// Custom properties
			image_name = "gcr.io/${GcpProject}/${Organization}/microblog-frontend"
			// image_name = "gcr.io/${GcpProject}/microblog-frontend"
			yaml_file = 'frontend.yaml'
			yaml_repo = "https://github.com/${Organization}/microblog-frontend.git"
		}

	} // application
} // project