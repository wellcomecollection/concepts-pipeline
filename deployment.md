# Deployment

## Background

This follows a similar model to [weco-deploy](https://github.com/wellcomecollection/weco-deploy).
Terraform manages the resources, and the services it creates use tagged ECR images to choose
the code to run.  Separately, the images are pushed to an ECR repository, tagged to match the 
pipeline it should run on, and the pipeline services in the live pipeline are notified.

## In Practice
### Terraform

New pipelines are created by [Terraform](infrastructure).

* Create a new [concepts_pipeline module](infrastructure/main.tf)
* Ensure that matching image tags exist:
```
sh scripts/retag.sh weco/concepts_aggregator latest my_new_namespace
sh scripts/retag.sh weco/concepts_ingestor latest my_new_namespace
```
* run the terraform

### Buildkite

A successful Buildkite run will push the images to the ECR Repository, tag them with 
the namespace of the "live" pipeline, and refresh the services.  Which pipeline
is "live" is defined at the top of the [buildkite file](.buildkite/pipeline.yml).

This allows us to continually deploy the latest code to one pipeline, while leaving 
any others undisturbed, so an old and a new (or new and newer) pipeline can be run in parallel.

### Manual deployment

As stated above, Buildkite should be doing all the deployment, but there may be a situation in 
which you wish to deploy code to a pipeline, bypassing buildkite.
For example, if the newest version is broken and you need to roll it back.

In this situation:

* Be authorised to make changes on the catalogue account (See [accounts](https://github.com/wellcomecollection/platform-infrastructure/blob/main/accounts/README.md))
* Find a tag for the image you wish to use (ref.89blahblah0 in the example below)
* Call retag.sh to set this as the image for the pipeline (2022-08-31, in the example below)
* call notify_services to make the pipeline pick up the change.

```shell
sh scripts/retag.sh weco/concepts_aggregator ref.89blahblah0  2022-08-31
sh scripts/notify_services.sh 2022-08-31
```

