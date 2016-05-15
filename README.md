# Jaggr - the Jenkins aggregator

![Jaggr](resources/public/img/jaggr-logo-and-text.png?raw=true)

**Jaggr** is a build monitor for Jenkins that makes use of the
[Jenkins Claim plugin](https://wiki.jenkins-ci.org/display/JENKINS/Claim+plugin)
. It shows a single aggregated status (red, yellow, green) for all jobs of a
project and takes claimed builds into account.

## Why?

Most Jenkins jobs are owned by the whole team and should never be broken for a
long time. Many teams have a "stop the world"-agreement to fix broken builds
before doing anything else.

However, there are less critical jobs, like build jobs for experimental branches
or long living feature branches owned by individual developers, where a failed
job should not stop the world.

The Jenkins Claim plugin can help with the team-owned jobs: Jenkins jobs can be
configured to be claimable, so any developer can claim a broken build, telling
the others that they can continue doing something else.

Unfortunately, this is not reflected in the build monitors. Failed jobs owned by
individuals cannot be distinguished from problems that the whole team should
handle. Claimed builds cannot be distinguished from unclaimed builds. Developers
get used to seeing lots of red end yellow jobs on their build monitor, hoping
somebody else cares.

**Jaggr** tries to solve this problem.

## Claims and job status semantics

**Jaggr** presumes, that all critical, team-owned jobs are configured to be
claimable, so one team member can claim a broken build and all others can
continue working. Non-critical jobs should be not claimable, since there is no
full team ownership.

**Jaggr** will watch all _claimable_ jobs of a given project and show an overall
status for all jobs:

* **RED**

    At least one job has failed (status red or yellow) and the broken build has
    not been claimed. Stop work immediately until somebody claims the broken
    build.

* **YELLOW**

    Some jobs have failed, but all of them have been claimed. Claimers fix the
    broken builds, everybody else checks in and merges with extra care.

* **GREEN**

    Hooray, all jobs that are in your teams responsibility, are in a healthy
    state!

## Installation and usage

### Jenkins

Install the [Jenkins Claim plugin](https://wiki.jenkins-ci.org/display/JENKINS/Claim+plugin)

Make all team-owned jobs claimable

### Jaggr

Install [java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

Download the [latest Jaggr release](https://github.com/puffedo/jaggr/releases)

In the project base directory:

```sh
java -jar jaggr-<version>-standalone.jar --base-url http://my-jenkins:8081/jenkins/my-project/
```

If your Jenkins instance is configured to accept only authenticated clients,
add the parameters

```sh
-- user jenkins-user --user-token ABDCE12345
```

The user token can be obtained from the Configuration page in your Jenkins user profile.

If you don't want to type the config parameters repeatedly, you can also create a
file named `default.config` (or copy and rename
[example.config](https://github.com/puffedo/jaggr/blob/master/example.config) )
and set the parameters there:

```
{
   :user        "my-user"
   :user-token  "xyzabcdef"
   :base-url    "http://my-jenkins:8081/jenkins/my-project/"
}
```

Parameters:

* `base-url`

    the url of the page that shows all jobs to be monitored (mandatory)

* `user`

    a jenkins user with privileges to see the project's jobs

* `user-token`

    the users api-token. It  can be obtained from the jenkins user profile
    configuration page

* `port`

    defaults to 3000

* `refresh-rate`

    defaults to 60s

* `config-file`

    location and name of the config file (default: `./default.config`)

All parameters can also be specified as environment variables (`USER`,
`USER_TOKEN`, `BASE_URL`, ...)

Parameters specified via the command line override config file parameters.
Config file parameters override environment variables.

### Background images

Background images are automatically loaded from lorempixel.com. You can place
custom images next to the executable into folders images/red, images/yellow,
images/green or images/error. Images are selected randomly. It works best
with grayscale images!

## Development

[![Build Status](https://travis-ci.org/puffedo/jaggr.svg?branch=master)](https://travis-ci.org/puffedo/jaggr)
[![Dependencies Status](https://jarkeeper.com/puffedo/jaggr/status.svg)](https://jarkeeper.com/puffedo/jaggr)
[![Coverage Status](https://coveralls.io/repos/github/puffedo/jaggr/badge.svg?branch=master)](https://coveralls.io/github/puffedo/jaggr?branch=master)
[![Issue Count](https://codeclimate.com/github/puffedo/jaggr/badges/issue_count.svg)](https://codeclimate.com/github/puffedo/jaggr)

### Install

* [java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [leiningen](http://leiningen.org/#install)

### Run in DEV mode

Set environment variable `BASE_URL` (and `USER` and `USER_TOKEN` if required)
or provide a `default.config` file (see above)

Build and start the server with leiningen:

```sh
lein ring server
```

Changes in the code will be visible in the browser after a page reload.

### Build

You can build the executable jar with

```sh
lein uberjar
```

### Contribute

Contributions welcome!

There is also a [Trello board](https://trello.com/b/uzKqvnY8/Jaggr) for features
and ideas.

## License

Copyright (C) 2016 Steffen Frank

licensed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html)

## Kudos

The logo is based on an illustration by
[vectorportal.com](http://www.vectorportal.com/subcategory/167/MICK-JAGGER-VECTOR-ILLUSTRATION.eps/ifile/10647/detailtest.asp)
thanks to Schorsch and [Marcus](https://github.com/molk)!

Thanks to [Hinnerk](https://github.com/hinnerkoetting) for the custom background
image feature!
