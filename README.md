**Disclaimer: this is _very_ alpha...**

![Jaggr](jaggr-logo.png?raw=true)

___Jaggr___ is a build monitor for Jenkins, that makes use of the Jenkins Claims plugin.
It shows a single aggregated status (red, yellow, green) for all jobs of a project
and takes claimed builds into account.

## Why?

Most Jenkins jobs are owned by the whole team and should never be broken for a long time. Most teams have a "stop the
world"-agreement to fix broken builds before doing anything else.

However, there are less critical jobs, like build jobs for experimental branches or long living feature branches owned by
individual developers, where a failed build should not stop the world.

The jenkins claims plugin can help here: Jenkins jobs can be configured to be claimable,
so any developer can claim a broken build job, telling the others developers that they can continue doing
something else.

However, this is not reflected in the build monitors. Claimed broken builds or failed jobs owned by individuals cannot
be distinguished from problems that the whole team should handle. Developers get used to seeing lots of
red end yellow jobs on their build monitor and hope, somebody else cares.

___Jaggr___ tries to solve this problem.

## Claims and job status semantics

___Jaggr___ presumes, that all critical, team-owned jobs are configured to be claimable, so one team member can claim a broken
job and all others can continue working. Non-critical jobs should be not claimable, since there is no
full team ownership.

___Jaggr___ will watch all _claimable_ jobs of a given project and show an overall status for all jobs:

* **RED**

    At least one job has failed (status red or yellow) and has not been claimed. Stop work immediately until somebody
claims the broken job and fixes it as soon as possible

* **YELLOW**

    Some jobs have failed, but all of them have been claimed. Check in and merge with care.

* **GREEN**

    Hooray, all jobs that are in your teams responsibility, are in a healthy state!

## Installation and usage

**Jenkins**

* Install the claims plugin
* make all jobs owned by the team claimable

**Jaggr**

There are no binaries on github yet, so you have to build them from source, using:

* [java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [leiningen](http://leiningen.org/#install)

In the project base directory:

```sh
lein uberjar
java -jar target/___Jaggr___-0.1.0-standalone.jar --port 8080 --user me --user-token asdfghjkl --base-url http://my-ci/jenkins/view/my-project/
```

Parameters:

* `user`

    a jenkins user with privileges to see the project's jobs

* `user-token`

    the users api-token. It  can be obtained from the jenkins user profile configuration page

* `base-url`

    the url of the page that shows all jobs to be monitored. Must end with a slash!

Optional Parameters:

* `port`

    defaults to 3000

* `refresh-rate`

    defaults to 60s

All parameters can also be specified as environment variables (`USER`, `USER_TOKEN`, `BASE_URL`, ...)


## Development

Install

* [java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [leiningen](http://leiningen.org/#install)

Set environment variables for `USER`, `USER_TOKEN` and `BASE_URL` (see above)

Build and start the server with leiningen:

```sh
lein ring server
```

Changes in the code will be visible in the browser after a page reload.



Contributions welcome, there is also a [trello board](https://trello.com/b/uzKqvnY8/___Jaggr___) for tasks and ideas.


## License

Copyright (C) 2016 Steffen Frank

licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html)

## Kudos

Logo based on an illustration by [vectorportal.com](http://www.vectorportal.com/subcategory/167/MICK-JAGGER-VECTOR-ILLUSTRATION.eps/ifile/10647/detailtest.asp), thanks to Schorsch and Marcus!