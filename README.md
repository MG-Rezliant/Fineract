# get code
git clone https://github.com/apache/fineract.git
cd fineract

# create dbs
./gradlew createPGDB -PdbName=fineract_tenants
./gradlew createPGDB -PdbName=fineract_default

# start backend
./gradlew devRun