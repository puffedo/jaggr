# JAGGR

JAGGR is a CI traffic light for Jenkins, that makes use of the Jenkins Claims plugin. The name stands for "Jenkins
status AGGRegator", since it shows a single aggregated status (red, yellow, green) for all jobs of a project.

## JAGGR status semantics

Most Jenkins jobs are owned by the whole team and should never be broken for a long time. Other jobs,
like build jibs for experimental branches or long living feature branches owned by individual
developers, are less critical, so a failed build should not stop the world.

JAGGR presumes, that all critical, team-owned jobs are configured to be claimable, so one team member claims a broken
job and all others can continue working. Non-critical jobs are presumed to be not claimable, since there is no
full team ownership.

JAGGR will watch all jobs of a given project and show on overall status for all jobs:

* RED: At least one job has failed (status red or yellow) and has not bee claimed. Stop work immediately until somebody
claims the broken job and fixes it as soon as possible
* YELLOW: Some jobs have failed, but all of them have been claimed. Check in and merge with care.
* GREEN: Hooray, all jobs that are in your teams responsibility, are in a healthy state!

## Installation and usage

_TODO_

## Build from source

_TODO_

## Development

_TODO_

## License

Copyright (C) 2016 Steffen Frank

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
