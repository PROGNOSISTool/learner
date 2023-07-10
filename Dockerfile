FROM openjdk:21-slim
COPY . /usr/src/learner
WORKDIR /usr/src/learner
ENV LD_BIND_NOW=1
RUN apt update && apt install -y bash curl graphviz ant
RUN curl -o /usr/bin/wait-for-it https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh && chmod +x /usr/bin/wait-for-it
RUN ant -f build.xml dist

ENTRYPOINT ["wait-for-it", "-t", "60", "adapter:3333", "-s", "--", "java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.io=ALL-UNNAMED", "--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED", "-cp", "dist/prognosisLearner.jar:lib/*", "learner.Main", "config.yaml"]
