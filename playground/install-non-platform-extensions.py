import os
import shutil
import yaml

baseDir = os.getcwd()
tmpDir = os.path.realpath('tmp')

if os.path.exists(tmpDir):
    shutil.rmtree(tmpDir)
os.mkdir(tmpDir)

with open(r'quarkus-extensions-repo.yaml') as file:
    extensionsDir = 'extensions'
    os.chdir(tmpDir)
    os.mkdir(extensionsDir)
    os.chdir(extensionsDir)
    document = yaml.full_load(file)
    individualExtensions = document['individual-extensions']
    for ext in individualExtensions:
        for version in ext.get('versions'):
          os.system('mvn io.quarkus:quarkus-maven-plugin:1.3.1.Final:create-extension -DartifactId=' + ext.get('artifact-id')
             + ' -DgroupId=' + ext.get('group-id')
             + ' -Dversion=' + version
             + ' -Dquarkus.basedir=' + ext.get('group-id') + '/' + ext.get('artifact-id') + '/' + version)
          os.system('mvn install -f ' + ext.get('group-id') + '/' + ext.get('artifact-id') + '/' + version + '/' + ext.get('artifact-id') + '/pom.xml')

