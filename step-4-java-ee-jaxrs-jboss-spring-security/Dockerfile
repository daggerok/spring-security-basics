FROM daggerok/jboss-eap-7.3:7.3.0-debian
EXPOSE 8080
HEALTHCHECK --retries=33 \
            --timeout=1s \
            --interval=1s \
            --start-period=3s \
            CMD ( wget -q --spider http://127.0.0.1:8080/ ) || exit 1
COPY --chown=jboss ./target/*.war ${JBOSS_HOME}/standalone/deployments/app.war
