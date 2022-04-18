FROM registry.fedoraproject.org/fedora:35 AS builder
RUN dnf install -y bison flex g++
ADD https://people.csail.mit.edu/asolar/sketch-1.7.6.tar.gz /tmp/sketch.tar.gz
RUN mkdir -p /opt/sketch && tar -x --no-same-owner --strip-components=1 -f /tmp/sketch.tar.gz -C /opt/sketch
RUN cd /opt/sketch/sketch-backend && ./configure -- && make -j
RUN rm -rf /opt/sketch/sketch-frontend/{LanguageReference.pdf,customcodegen,release_benchmarks,src,test,testrunner.mk}

FROM registry.fedoraproject.org/fedora-minimal:35

RUN mkdir -p /opt/sketch/sketch-backend/src/SketchSolver
COPY --from=builder /opt/sketch/sketch-backend/src/SketchSolver/cegis /opt/sketch/sketch-backend/src/SketchSolver

RUN mkdir -p /opt/sketch/sketch-frontend
COPY --from=builder /opt/sketch/sketch-frontend /opt/sketch/sketch-frontend
RUN microdnf install --setopt=install_weak_deps=0 --nodocs -y which java-1.8.0-openjdk-headless && \
    microdnf clean all -y

RUN echo 'export PATH=$PATH:/opt/sketch/sketch-frontend' >> /root/.bashrc && \
    echo 'export SKETCH_HOME=/opt/sketch/sketch-frontend/runtime' >> /root/.bashrc
CMD /bin/bash -i
