FROM solita/ubuntu-systemd
RUN apt-get update -y && \
    apt-get install -y wget sudo curl
RUN cd bin/ && curl -Lo kubectl https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && chmod +x kubectl && cd ..
ADD install-docker.sh /
RUN sh install-docker.sh
ADD install-minikube.sh /
RUN sh install-minikube.sh
RUN minikube start --vm-driver=none
