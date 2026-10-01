# How to generate the keys for the different processes
# (Assuming here that there are only 3 processes)

keytool -genkey -alias node1 -keyalg Ed25519 -validity 365 -keystore node1.ks -storetype pkcs12
keytool -genkey -alias node2 -keyalg Ed25519 -validity 365 -keystore node2.ks -storetype pkcs12
keytool -genkey -alias node3 -keyalg Ed25519 -validity 365 -keystore node3.ks -storetype pkcs12
keytool -genkey -alias node4 -keyalg Ed25519 -validity 365 -keystore node4.ks -storetype pkcs12

## I used the password 'password' (without quotes) and maintained default values for all fields.

# Next step is to extract the public key certificate for each node

keytool -exportcert -alias node1 -keystore node1.ks -file node1.cert
keytool -exportcert -alias node2 -keystore node2.ks -file node2.cert
keytool -exportcert -alias node3 -keystore node3.ks -file node3.cert
keytool -exportcert -alias node4 -keystore node4.ks -file node4.cert

# Next and final step in preparation cryptographic material is generate a truststore with all public key certificates

keytool -importcert -alias node1 -file node1.cert -keystore truststore.ks
keytool -importcert -alias node2 -file node2.cert -keystore truststore.ks
keytool -importcert -alias node3 -file node3.cert -keystore truststore.ks
keytool -importcert -alias node4 -file node4.cert -keystore truststore.ks

## I used 'password' (without quotations) as the password for the truststore file

# Compiling

## In the root of the project use maven to generate an executable jar by doing:

mvn clean package

# Starting the processes

## In different terminal windows execute the following commands:

java -Dlog4j2.configurationFile=./log4j2-base.xml -jar target/lab1-1.0.jar crypto_name=node1
java -Dlog4j2.configurationFile=./log4j2-base.xml -jar target/lab1-1.0.jar crypto_name=node2
java -Dlog4j2.configurationFile=./log4j2-base.xml -jar target/lab1-1.0.jar crypto_name=node3
java -Dlog4j2.configurationFile=./log4j2-base.xml -jar target/lab1-1.0.jar crypto_name=node4
