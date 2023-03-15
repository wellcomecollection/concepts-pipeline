// A script used during an update to append new values to selected fields.
//
resource "elasticstack_elasticsearch_script" "append_fields" {
  script_id = "append-fields"
  lang      = "painless"
  source    = <<-EOT
  ctx.op = 'noop';
  for(entry in params.entrySet()) {
    def k = entry.getKey();
    def oldField = ctx._source[k];
    def idSet = new HashSet(oldField instanceof List ? oldField : [oldField]);
    if (idSet.addAll(entry.getValue())){
      ctx._source[k] = idSet.toArray();
      ctx.op = 'index'
    }
  }
  EOT
  // Store the script in the update context.
  // For some reason, if you store it without context, it will
  // recompile on each use, and then fail because you are running too many
  // script compilations during a bulk update.d
  context = "update"
}