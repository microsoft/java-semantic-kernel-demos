if (Test-Path -Path "/run/secrets/demo") {
    New-Item -ItemType Directory -Path "/home/deploy/.sk" -Force | Out-Null
    Copy-Item -Path "/run/secrets/demo" -Destination "/home/deploy/.sk/conf.properties"
}

java -jar quarkus-run.jar -Dquarkus.http.host=0.0.0.0