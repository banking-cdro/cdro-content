git clone https://github.com/electric-cloud-community/DSL-Samples.git
ectool --server xxxx login admin yyyyy
PluginConfigs/EC-Kubectl.sh
PluginConfigs/EC-ServiceNow.sh
PluginConfigs/EC-Github.sh
PluginConfigs/EC-Git.sh
ectool evalDsl --dslFile ServiceNowPollingEcGroovy.groovy
ectool evalDsl --dslFile DSL-Samples/attachPipelineRuntime.groovy
ectool evalDsl --dslFile BlogApp.groovy
ectool evalDsl --dslFile cdPipeline.groovy
ectool evalDsl --dslFile ReleasePipeline.groovy


kubectl -n cloudbees-sda exec -it $(kubectl get pods -n cloudbees-sda | grep flow-server | cut -f 1 -d" ") -- chown cbflow:cbflow -R /plugins-data

# TODO


