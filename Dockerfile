FROM openjdk:16-alpine
COPY . /usr/src/learner
WORKDIR /usr/src/learner
ENV LD_BIND_NOW=1
RUN apk add --no-cache bash apache-ant curl graphviz
RUN curl -o /usr/bin/wait-for-it https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh && chmod +x /usr/bin/wait-for-it
RUN ant -f Learner/build.xml dist

ENTRYPOINT ["wait-for-it", "adapter:3333", "-s", "--", "java", "--illegal-access=permit", "-cp", "Learner/dist/prognosisLearner.jar:Learner/lib/*", "learner.Main", "config.yaml"]
