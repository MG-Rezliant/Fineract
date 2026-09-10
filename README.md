# Apache Fineract

<!-- TODO Reactivate when there is a working CI-CD instance: [![Swagger Validation](https://validator.swagger.io/validator?url=https://sandbox.mifos.community/fineract-provider/swagger-ui/fineract.yaml)](https://validator.swagger.io/validator/debug?url=https://sandbox.mifos.community/fineract-provider/swagger-ui/fineract.yaml) -->
[![Build](https://github.com/apache/fineract/actions/workflows/build-postgresql.yml/badge.svg?branch=develop)](https://github.com/apache/fineract/actions/workflows/build-postgresql.yml)
[![Docker Hub](https://img.shields.io/docker/pulls/apache/fineract.svg?logo=Docker)](https://hub.docker.com/r/apache/fineract)
[![Docker Build](https://github.com/apache/fineract/actions/workflows/publish-dockerhub.yml/badge.svg)](https://github.com/apache/fineract/actions/workflows/publish-dockerhub.yml)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=apache_fineract&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=apache_fineract)
[![Verified DPG](https://img.shields.io/badge/Verified-DPG-3333AB)](https://www.digitalpublicgoods.net/r/apache-fineract "Apache Fineract is a verified Digital Public Good under the Digital Public Goods Alliance.")

Apache Fineract is an open-source core banking platform providing a
flexible, extensible foundation for a wide range of financial services. By
making robust banking technology openly available, it lowers barriers for
institutions and innovators to reach underserved and unbanked populations.

Have a look at the [documentation](https://fineract.apache.org/docs/stable), the [wiki](https://cwiki.apache.org/confluence/display/FINERACT) or at the [FAQ](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=91554327), if this README does not answer what you are looking for.


COMMUNITY
=========

If you are interested in contributing to this project, but perhaps don't quite know how and where to get started, please [join our developer mailing list](http://fineract.apache.org/#contribute) and [chat server](https://app.element.io/#/room/#apache-fineract-home:matrix.org), listen into our conversations, chime into threads, or just send us a "Hello!" introduction message; we're a friendly bunch, and look forward to hearing from you.

For the developer wiki, see [Contributor's Zone](https://cwiki.apache.org/confluence/display/FINERACT/Contributor%27s+Zone). Maybe [these how-to articles](https://cwiki.apache.org/confluence/display/FINERACT/How-to+articles) help you to get started.

In any case visit [our JIRA Dashboard](https://issues.apache.org/jira/secure/Dashboard.jspa?selectPageId=12335824) to find issues to work on, see what others are doing, or open new issues.

In the moment you get started writing code, please consult our [CONTRIBUTING](CONTRIBUTING.md) guidelines, where you will find more information on subjects like coding style, testing and pull requests.


REQUIREMENTS
============

See [Fineract Development Environment](https://fineract.apache.org/docs/develop/#_fineract_development_environment) in the docs for hardware, software, and tool prerequisites and recommendations.


SECURITY
============
For a list of known vulnerabilities, see [Apache Fineract Security Reports](https://fineract.apache.org/security.html).

If you believe you have found a new vulnerability, [let us know privately](https://fineract.apache.org/#contribute).

For details about security during development and deployment, see the documentation [here](https://fineract.apache.org/docs/stable/#_security).

PRIVATE FORKS
============

If you're running Apache Fineract on a private fork, you might want to consider **disabling GitHub Actions CI**. 
This is because the usage minutes and artifact costs (including the built workspace and logs generated during execution) could incur additional expenses, that you should be aware!

INSTRUCTIONS
============

Quick Start
---

Follow these steps to quickly set up and run Apache Fineract locally.

### Prerequisites

- [Java JDK](https://fineract.apache.org/docs/develop/#_fineract_development_environment)
- PostgreSQL running locally, listening on port 5432 with proper permissions (see [below](#database-and-tables) for how to run PostgreSQL in Docker)