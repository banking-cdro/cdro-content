# Create EC-Kubectl configuration

KUBE_CONFIG=kubectl

printf "bogus\n" | ectool runProcedure /plugins/EC-Kubectl/project \
  --procedureName CreateConfiguration \
  --actualParameter \
      config="${KUBE_CONFIG}" \
	  kubeconfigSource="kubeconfigDefault" \
	  kubectlPath="/usr/local/bin/kubectl" \
	  credential="${KUBE_CONFIG}" \
  --credential "${KUBE_CONFIG}"="bogususer"