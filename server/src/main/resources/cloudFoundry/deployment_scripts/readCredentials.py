import subprocess as sub
import sys
import json

database_uri = ""
database_username = ""
database_port = ""
database_password = ""
database_name = ""
database_host = ""

def main():
    strAppName = sys.argv[1]
    strService = sys.argv[2]
    strServiceType = str(sys.argv[3])
    if strServiceType == "mysql":
        print("Read credentials from mysql service " + strService)
        read_mysql_credentials(strAppName, strService)
        createConfigureFile()
    return

def read_mysql_credentials(appName, serviceName):
    # required environment variable names for the database connection.
    strDatabaseUri = "database_uri"
    strEnvDatabaseUser = "database_user"
    strEnvDatabaseName = "database_name"
    strEnvDatabaseHost = "database_host"
    strEnvDatabasePort = "database_port"
    strEnvDatabasePassword = "database_password"

    # find the VCAP_SERVICES block
    serviceBlock = get_Service_Env_Block_MySql(appName)

    # convert it to json and set environment variables
    jsonEnv = json.loads(serviceBlock)
    global database_uri
    global database_username
    global database_port
    global database_password
    global database_name
    global database_host

    database_uri = jsonEnv["VCAP_SERVICES"][serviceName][0]["credentials"]["uri"]
    database_username = jsonEnv["VCAP_SERVICES"][serviceName][0]["credentials"]["username"]
    database_port = jsonEnv["VCAP_SERVICES"][serviceName][0]["credentials"]["port"]
    database_password = jsonEnv["VCAP_SERVICES"][serviceName][0]["credentials"]["password"]
    database_name = jsonEnv["VCAP_SERVICES"][serviceName][0]["credentials"]["name"]
    database_host = jsonEnv["VCAP_SERVICES"][serviceName][0]["credentials"]["hostname"]

    # set environment variables in the CF container
    sub.call(["cf" ,"set-env",appName, strDatabaseUri, database_uri])
    sub.call(["cf" ,"set-env",appName, strEnvDatabaseUser, database_username])
    sub.call(["cf" ,"set-env",appName, strEnvDatabaseName, database_name])
    sub.call(["cf" ,"set-env",appName, strEnvDatabaseHost, database_host])
    sub.call(["cf" ,"set-env",appName, strEnvDatabasePort, database_port])
    sub.call(["cf" ,"set-env",appName, strEnvDatabasePassword, database_password])

    # ensure the env variables changes take effect
    sub.call(["cf", "restage", appName])
    return

def get_Service_Env_Block_MySql(appName):
    strBegin = "System-Provided:"
    strEnd = "VCAP_APPLICATION"
    env = sub.check_output(["cf","env", appName])
    begin = env.find(strBegin) + len(strBegin) + 1
    end = env.find(strEnd) - 5
    return env[begin:end]

def createConfigureFile():
    configFile = open("mysqlConfig.txt", "w")
    configFile.write("""{'user': '%s',
    'password': '%s',
    'host': '%s',
    'database': '%s',
    'raise_on_warnings': True,}""" %(database_username, database_password,
    database_host, database_name))

if __name__ == "__main__":
    main()