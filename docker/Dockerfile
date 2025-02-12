# Single-machine LynxKite image.
FROM openjdk:11

# Add system dependencies.
RUN \
  apt-get update && \
  apt-get install -y build-essential busybox git-core && \
  busybox --install && \
  rm -rf /var/lib/apt/lists/* && \
  wget -nv 'https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh' -O conda.sh && \
  bash conda.sh -b -p /opt/conda && \
  rm conda.sh
ENV PATH /opt/conda/bin:$PATH

# Install dependencies early so they are in a low layer.
COPY stage/tools/runtime-env.yml /lynxkite/tools/runtime-env.yml
RUN conda env update -n base --file /lynxkite/tools/runtime-env.yml
COPY stage/tools/install_spark.sh /lynxkite/tools/install_spark.sh
COPY stage/conf/SPARK_VERSION /lynxkite/conf/SPARK_VERSION
RUN /lynxkite/tools/install_spark.sh /spark
# We add Conda to LD_LIBRARY_PATH to make NetworKit available.
# But then system binaries start using the wrong libtinfo.so.6.
RUN rm /opt/conda/lib/libtinfo.so.6
ENV LD_LIBRARY_PATH /opt/conda/lib
ENV ADD_TO_PYTHON_JAIL /opt/conda/lib

# The user has to configure these.
ENV KITE_MASTER_MEMORY_MB 1024
ENV SPHYNX_CACHED_ENTITIES_MAX_MEM_MB 1024
VOLUME /data
VOLUME /metadata

# Install LynxKite.
COPY stage /lynxkite
RUN touch /prefix_definitions.txt
COPY run.sh /run.sh

ENTRYPOINT ["/run.sh"]
CMD ["interactive"]

EXPOSE 2200
