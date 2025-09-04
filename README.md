# Migration for code-value registries

The source for registries is assumed to be the service running on https://luettelopalvelu.fi.
This service will be shutdown sometime 2026 and this tool will help migrate any registries in that service to the new one.

The target for registries:
- test: https://koodistot.test.yti.cloud.dvv.fi/
- prod: https://koodistot.suomi.fi/

## Settings

Copy the `settings.properties_template` as `settings.properties` or use ENV variables to configure the options.

All settings can be set either in `settings.properties` or as environment variable (using the same name except env variable name should be uppercase'd and with `_` as the word separator instead of `.`)

## Build with maven

Requires:
- Java 21+
- Maven

`mvn -f codetransform/pom.xml clean package`

## Run with java -jar

Requires:
- Java 21+

`java -jar codetransform/target/codetransform-jar-with-dependencies.jar -vv -u --dry-run kiintopisterekisteri.csv`

## Run behind corporate proxy

Add `-Dhttps.proxyHost={proxy domain} -Dhttps.proxyPort={proxy port}` to java-command.

## Command line arguments

Options:
* `-u`, to run in update mode (reload is default)
* `-v`/`-vv`, for (more) verbose reporting
* `--dry-run`, don't actually touch anything in koodistot.suomi.fi, just report what would happen
* `--set-valid`, required to actually set status to valid (by-default we stop at DRAFT status)

Last argument:
* `{configuration}.csv`, tab-separated configuration file listing the codelists to run

## CSV-format

The CSV-files are used for mapping which registries to migrate and includes values for registries that are not available on the luettelopalvelu.fi.

There are 3 CSV-files as examples for registries that have been migrated by NLS Finland.

TODO: add details about CSV format

## Getting an API-key

The source service doesn't need one, but to insert/update/delete registries in the target system you will need an API key. The API key is linked to your organization. You will need to be assigned a role `Koodistotyöntekijä` in the services listed below so your API key can be used to do write operations on the registries. To get the role, someone in your organization needs to have an admin role in the service that allows assigning roles to other users in the organization.

For test env: https://rhp.test.yti.cloud.dvv.fi/
For prod: https://rhp.suomi.fi/

Roles you have are listed in the userDetails path like https://rhp.suomi.fi/userDetails.

TODO: how to find the organization id for settings.properties/env-variable
