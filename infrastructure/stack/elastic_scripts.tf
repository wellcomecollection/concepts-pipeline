// A script used during an update to append new values to selected fields.
data "local_file" "append_fields" {
  filename = "${path.module}/append_fields.painless"
}

resource "elasticstack_elasticsearch_script" "append_fields" {
  script_id = "append-fields"
  lang      = "painless"
  source    = data.local_file.append_fields.content

  // Store the script in the update context.
  // For some reason, if you store it without context, it will
  // recompile on each use, and then fail because you are running too many
  // script compilations during a bulk update.
  context = "update"
}